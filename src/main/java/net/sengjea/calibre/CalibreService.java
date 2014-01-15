package net.sengjea.calibre;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.ArrayList;


/**
 * @author Seng Jea Lee
 */
@SuppressLint("NewApi")
public class CalibreService extends Service {

    private static final int[] BROADCAST_PORTS = {54982, 48123, 39001, 44044, 59678};
    private static final int BASE_PACKET_LEN = 4096;
    //Some network protocol constants (Mirrored from Calibre)
    /* A few "random" port numbers to use for detecting clients using broadcast
    The clients are expected to broadcast a UDP 'hi there' on all of these
    ports when they attempt to connect. Calibre will respond with the port
    number the client should use. This scheme backs up mdns. And yes, we
    must hope that no other application on the machine is using one of these
    ports in datagram mode.
    If you change the ports here, all clients will also need to change. */

    private static final int PROTOCOL_VERSION = 1;
    private static final String ZEROCONF_CLIENT_STRING = "calibre wireless device client";
    private static final String BROADCAST_STRING = "hello calibre";
    //UDP Packet numbers Format: ZEROCONF_CLIENT_STRING (on HOSTNAME);CONTENT_SERVER_PORT,WIRELESS_DEVICE_PORT
    private static final int CONTENT_SERVER_PORT = 0;
    private static final int WIRELESS_DEVICE_PORT = 1;
    private static final int OPCODE_INDEX = 0;
    private static final int ARGS_INDEX = 1;
    private static final int OP_NOOP = 12;
    private static final int OP_OK = 0;
    //private static final int OP_BOOK_DATA            = 10; Unused because we stream
    //private static final int OP_BOOK_DONE            = 11; Unused because we stream
    private static final int OP_CALIBRE_BUSY = 18;
    private static final int OP_DELETE_BOOK = 13;
    private static final int OP_DISPLAY_MESSAGE = 17;
    private static final int OP_FREE_SPACE = 5;
    private static final int OP_GET_BOOK_FILE_SEGMENT = 14;
    //private static final int OP_GET_BOOK_METADATA    = 15; Unused because we stream
    private static final int OP_GET_BOOK_COUNT = 6;
    private static final int OP_GET_DEVICE_INFORMATION = 3;
    private static final int OP_GET_INITIALIZATION_INFO = 9;
    private static final int OP_SEND_BOOKLISTS = 7;
    private static final int OP_SEND_BOOK = 8;
    private static final int OP_SEND_BOOK_METADATA = 16;
    private static final int OP_SET_CALIBRE_DEVICE_INFO = 1;
    private static final int OP_SET_CALIBRE_DEVICE_NAME = 2;
    private static final int OP_TOTAL_SPACE = 4;
    private static int MAX_CLIENT_COMM_TIMEOUT = 6000; //Wait at most N mseconds for an answer
    private static String APP_VERSION = "beta";
    private static InetAddress InetAddressBroadcast;
    private static WifiManager mWifiManager;
    private static NotificationManager mNotificationManager;
    private static SharedPreferences mSettings;
    private final int NOTIFY_TAG = 42;
    private final IBinder mBinder = new CalibreBinder();
    private DatagramSocket discovery;
    private Socket connection = new Socket();
    private byte[] connBuf = new byte[BASE_PACKET_LEN];
    private byte[] fileBuf = new byte[BASE_PACKET_LEN];
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private Thread discovery_thread, connection_thread;//, metadata_thread;
    //private ArrayList<JSONObject> newBooks = new ArrayList<JSONObject>();
    private CalibreListener mListener;
    private File rootDirectory = null;//, newsDirectory = null;
    private ArrayList<ConnectionInfo> listOfServers = new ArrayList<ConnectionInfo>();
    private ConnectionInfo currentServer;
    private MetadataDatabaseHelper mDB;
    private boolean restartDiscovery = false;
    private boolean enable_for_emulator = false;
    private boolean isInDebugMode = false;
    private BookCrawler mBookCrawler;
    private OnSharedPreferenceChangeListener mOSPCListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sp, String key) {
            Logger.d("OnSharedPreferenceChangeListener called for " + key);
            if (key.equals("pref_root_dir")) {
                try {
                    setDirectories();
                    if (mBookCrawler.scanFolder(rootDirectory)) {
                        restartConnectionThread();
                    }

                } catch (Exception e) {
                    logAndDie(e);
                }
            } else if (key.equals("pref_debug")) {
                if (rootDirectory != null && (sp.getBoolean(key, false) || isInDebugMode)) {
                    Logger.set(new File(rootDirectory, "calibre-debug.log"));
                } else {
                    Logger.set(null);
                }
            } else if (key.equals("pref_file_types")) {
                mBookCrawler.setAcceptedExtensions(sp.getString(key,"epub pdf mobi azw3 pdb fb2 rtf html").split(" "));
            }
        }
    };

    private Runnable discoveryRunnable = new Runnable() {
        public void run() {
            ConnectionInfo discoveredCI;
            try {
                InetAddressBroadcast = getBroadcastAddress(mWifiManager.getDhcpInfo());
            } catch (Exception e) {
                logAndDie(e);
            }

            if (enable_for_emulator) {
                try {
                    broadcastServer(new ConnectionInfo("10.0.2.2", 9095, 8080));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                for (int i = 0; i < BROADCAST_PORTS.length; i++) {
                    if (restartDiscovery || i == 0) {
                        i = 0;
                        restartDiscovery = false;
                        broadcastServer(null);
                        broadcastDiscoveryStateChange(DiscoveryState.BEGIN_DISCOVERY);
                    }
                    try {
                        Logger.d("InetAddressBroadcast " + InetAddressBroadcast.toString() + ":" + Integer.toString(BROADCAST_PORTS[i]));
                        discovery = new DatagramSocket();
                        discovery.setSoTimeout(MAX_CLIENT_COMM_TIMEOUT);
                        discovery.setBroadcast(true);
                        byte[] discBuf = new byte[BASE_PACKET_LEN];
                        DatagramPacket recvPacket = new DatagramPacket(discBuf, discBuf.length);
                        DatagramPacket sendPacket = new DatagramPacket(BROADCAST_STRING.getBytes(), BROADCAST_STRING.length(), InetAddressBroadcast, BROADCAST_PORTS[i]);
                        discovery.send(sendPacket);
                        while (true) {
                            if (restartDiscovery) break;
                            discovery.receive(recvPacket);
                            String tmp = new String(discBuf, 0, recvPacket.getLength());
                            if (tmp.startsWith(ZEROCONF_CLIENT_STRING)) {
                                String ports[] = tmp.split(";")[1].split(",");
                                discoveredCI = new ConnectionInfo(recvPacket.getAddress().getHostAddress(), Integer.parseInt(ports[WIRELESS_DEVICE_PORT]), Integer.parseInt(ports[CONTENT_SERVER_PORT]));
                                broadcastServer(discoveredCI);
                            }
                        }
                    } catch (SocketTimeoutException e) {
                    } catch (SocketException e) {
                    } catch (Throwable e) {
                    } finally {
                        try {
                            discovery.close();  //Try until there are no more servers replying then attempt to close socket.
                        } catch (Exception e) {
                            logAndDie(e);
                        }

                    }

                }
            }
            broadcastDiscoveryStateChange(DiscoveryState.DONE_DISCOVERY);
        }
    };
    private Runnable connectionRunnable = new Runnable() {
        public void run() {
            StringBuilder incoming;
            if (currentServer == null) {
                return;
            }
            if (rootDirectory == null || !rootDirectory.canRead()) {
                broadcastError(ErrorType.ROOT_NO_ACCESS);
                return;
            }
            if (mBookCrawler.isCrawling()) {
                broadcastConnectionStateChange(ConnectionState.WAITING_FOR_FOLDERSCAN);
                mBookCrawler.waitForScan();
            }
            try {
                broadcastConnectionStateChange(ConnectionState.CONNECTING);
                Logger.d("cRunnable: " + currentServer.toString());
                currentServer.resolveAddresses();
                connection = new Socket();
                connection.connect(currentServer.getWirelessDeviceSocket());
                inputStream = new BufferedInputStream(connection.getInputStream());
                outputStream = new BufferedOutputStream(connection.getOutputStream());
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putInt("pref_networkid", mWifiManager.getConnectionInfo().getNetworkId());
                editor.commit();

            } catch (Exception e) {
                broadcastError(ErrorType.CANT_CONNECT);
                Logger.e(e);
                disconnectFromServer();
                return;
            }
            broadcastConnectionStateChange(ConnectionState.COMMUNICATING);
            while (!connection.isClosed() && connection.isConnected()) {
                incoming = read_from_server();
                if (incoming != null) {
                    process_json_from_server(incoming);
                }
            }
            broadcastConnectionStateChange(ConnectionState.DISCONNECTED);
        }
    };
    private Handler serviceHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mListener == null) {
                Logger.d("No mListener Found!");
                return;
            }
            if (msg.what == HandlerMessageType.SERVER.id()) {
                if (msg.obj != null) {
                    listOfServers.add((ConnectionInfo) msg.obj);
                } else {
                    listOfServers.clear();
                }
                mListener.onDiscoveryStateChanged(DiscoveryState.NEW_SERVER);

            } else if (msg.what == HandlerMessageType.CONNECTION.id()) {
                mListener.onConnectionStateChanged((ConnectionState) msg.obj);

            } else if (msg.what == HandlerMessageType.DISCOVERY.id()) {
                mListener.onDiscoveryStateChanged((DiscoveryState) msg.obj);
                if ((DiscoveryState) msg.obj == DiscoveryState.DONE_DISCOVERY &&
                        listOfServers.isEmpty() && !connectionThreadIsRunning()) {
                    mListener.onDiscoveryStateChanged(DiscoveryState.NO_SERVER);
                }

            } else if (msg.what == HandlerMessageType.ERROR.id()) {
                mListener.onErrorReported((ErrorType) msg.obj);
            }
        }
    };

    private static String toHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(Character.forDigit((b & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(b & 0x0f, 16));
        }
        return sb.toString();
    }

    private static InetAddress getBroadcastAddress(DhcpInfo ip) throws UnknownHostException {
        int broadcast = (ip.ipAddress & ip.netmask) | ~ip.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    @SuppressWarnings("deprecation")
    private double getTotalSpace() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            StatFs sf = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return (double) sf.getBlockCount() * (double) sf.getBlockSize();
        } else {
            return (double) rootDirectory.getTotalSpace();
        }
    }

    @SuppressWarnings("deprecation")
    private double getFreeSpace() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            StatFs sf = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return (double) sf.getAvailableBlocks() * (double) sf.getBlockSize();
        } else {
            return (double) rootDirectory.getFreeSpace();
        }
    }

    private synchronized void  setDirectories() {
        File oldRoot = rootDirectory;
        rootDirectory = new File(Environment.getExternalStorageDirectory(), mSettings.getString("pref_root_dir", "eBooks/"));

        if (oldRoot != null && oldRoot.renameTo(rootDirectory)) {
            Toast.makeText(getApplicationContext(), R.string.toast_root_folder_renamed, Toast.LENGTH_SHORT).show();
        } else if (!rootDirectory.exists()) {
            rootDirectory.mkdirs();
            Toast.makeText(getApplicationContext(), R.string.toast_root_folder_changed, Toast.LENGTH_SHORT).show();
        }
        if (!rootDirectory.canRead()) {
            rootDirectory = null;
        }
        if (rootDirectory != null && (mSettings.getBoolean("pref_debug", false) || isInDebugMode)) {
            Logger.set(new File(rootDirectory, "calibre-debug.log"));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //get calibre metadata

        Logger.d("onCreate");
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSettings.registerOnSharedPreferenceChangeListener(mOSPCListener);
        setDirectories();
        mDB = new MetadataDatabaseHelper(this);
        mBookCrawler = new BookCrawler(mDB, mSettings.getString("pref_filetypes", "epub pdf mobi azw3 pdb fb2 rtf html").split(" "));


        try {
            APP_VERSION = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            APP_VERSION = "error";
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.d("onBind");
        return mBinder;
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public CalibreListener getListener() {
        return mListener;
    }

    /**
     * Sets the CalibreListener that will be used by the Service for callbacks
     *
     * @param listener
     */
    public void setListener(CalibreListener listener) {
        mListener = listener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Time n = new Time();
        n.setToNow();
        Logger.d("onStartCommand at " + n.format3339(false));
        discoverServers();
        mBookCrawler.scanFolder(rootDirectory);
        connectWithSavedPreferences();
        return START_STICKY;
    }

//    private boolean metadataThreadIsRunning() {
//        return (metadata_thread != null) && metadata_thread.isAlive();
//    }

    private boolean discoveryThreadIsRunning() {
        return (discovery_thread != null) && discovery_thread.isAlive();
    }

    private boolean connectionThreadIsRunning() {
        return (connection_thread != null) && connection_thread.isAlive();
    }

    private boolean isConnected() {
        return connectionThreadIsRunning() && !connection.isClosed();
    }

    public void discoverServers() {
        try {
            if (discoveryThreadIsRunning()) {
                Logger.d("discoverServers: reset");
                restartDiscovery = true;
                discovery.close();

            } else {
                Logger.d("discoverServers: new");
                discovery_thread = new Thread(discoveryRunnable);
                discovery_thread.start();
            }
        } catch (Exception e) {
            logAndDie(e);
        }
    }

    private boolean connectWithSavedPreferences() {
        String wdAddress = mSettings.getString("pref_wd_address", "");
        Logger.d("connectWithSavedPreferences");
        if (!mSettings.getBoolean("pref_autocon", false)) return false;
        else if (mSettings.getInt("pref_networkid", 0) != mWifiManager.getConnectionInfo().getNetworkId()) return false;
        else if (connectionThreadIsRunning()) return true;
        else {
            try {
                String[] wd_array = wdAddress.split(":");
                URL cs_url = new URL(mSettings.getString("pref_cs_url", ""));
                connectToServer(new ConnectionInfo(wd_array[0],
                        Integer.parseInt(wd_array[1]),
                        cs_url.getPort(),
                        mSettings.getString("pref_password", "")));
            } catch (Exception e) {
                Logger.e(e);
                currentServer = null;
                return false;
            }
            return true;
        }
    }
    private void restartConnectionThread() {
        connectToServer(null);
    }
    public synchronized void connectToServer(ConnectionInfo ci) {
        Logger.d("connectToServer: " + (ci != null ? ci.toString() : "(restart)"));
        disconnectFromServer();
        try {
            if (isConnected()) {
                connection_thread.join();
            }
        } catch (Exception e) {
            logAndDie(e);
        }
        if (ci != null) currentServer = ci;
        connection_thread = new Thread(connectionRunnable);
        connection_thread.start();
    }

    public void onDestroy() {
        Logger.d("onDestroy:");
        disconnectFromServer();
        if (isInDebugMode) {
            Logger.d("Execution isInDebugMode");
            SharedPreferences.Editor editor = mSettings.edit();
            editor.clear();
            editor.commit();
            mDB.emptyAll();
        }
        try {
            mDB.close();
        } catch (Throwable e) {
            Logger.d("Unclean close!");
        }


    }

    public void disconnectFromServer() {
        try {
            Logger.d("disconnectFromServer");
            connection.close();
            broadcastConnectionStateChange(ConnectionState.DISCONNECTED);
        } catch (Exception e) {
            broadcastConnectionStateChange(ConnectionState.IDLING);
        }

    }

    /**
     * Parses an input JSON String and sends a corresponding JSONArray reply if need be.
     *
     * @param input
     */
    private synchronized void process_json_from_server(StringBuilder input) {
        JSONArray data;
        JSONObject args;
        try {
            data = new JSONArray(input.toString());
            int opcode = data.getInt(OPCODE_INDEX);
            args = data.getJSONObject(ARGS_INDEX);
            broadcastConnectionStateChange(ConnectionState.COMMUNICATING);
            switch (opcode) {
                case OP_CALIBRE_BUSY:
                    broadcastError(ErrorType.CALIBRE_BUSY);
                    disconnectFromServer();

                    break;
                case OP_GET_INITIALIZATION_INFO:
                    currentServer.setLibraryName(args.getString("currentLibraryName"));
                    send_to_server(OP_OK, initialisation_reply(args));

                    break;
                case OP_GET_DEVICE_INFORMATION:
                    send_to_server(OP_OK, device_information_reply());

                    break;
                case OP_SET_CALIBRE_DEVICE_INFO:
                    mBookCrawler.setDriveInfo(args);
                    send_to_server(OP_OK, null);

                    break;
                case OP_SET_CALIBRE_DEVICE_NAME:
                    mBookCrawler.getDriveInfo().put("device_name", args.getString("name"));

                    break;
                case OP_FREE_SPACE:
                    send_to_server(OP_OK, new JSONObject().put("free_space_on_device", getFreeSpace()));

                    break;
                case OP_TOTAL_SPACE:
                    send_to_server(OP_OK, new JSONObject().put("total_space_on_device", getTotalSpace()));

                    break;
                case OP_GET_BOOK_COUNT:
                    stream_meta_to_servers();

                    break;
                case OP_SEND_BOOKLISTS:

                    break;
                case OP_SEND_BOOK_METADATA:
                    mDB.insertJSON(args.getJSONObject("data"));

                    break;
                case OP_NOOP:
                    send_to_server(OP_OK, null);

                    break;
                case OP_SEND_BOOK:
                    File fn = new File(rootDirectory, args.getString("lpath"));
                    Boolean update = false;
                    Cursor cs = mDB.getByUuid(new String[]{"_id", "lpath"}, args.getJSONObject("metadata").getString("uuid"));
                    if (cs.moveToFirst() && !cs.isNull(1) && mDB.deleteBook(cs.getInt(0))) {
                        new File(rootDirectory, cs.getString(1)).delete();
                        update = true;
                    }
                    cs.close();
                    download_file(args.getInt("length"), fn);
                    byte[] thumbnail = mDB.insertJSON(args.getJSONObject("metadata"));
                    if (args.getInt("totalBooks") == 1 && args.getInt("thisBook") == 0) {
                        ui_notify(args.getString("lpath"), fn, generateBitmap(thumbnail), update);
                        //broadcastDiscoveryStateChange(UIStatus.BOOKS_CHANGED, null);
                    } else {
                        Logger.d(String.format("Downloading Book %d of %d", args.getInt("thisBook") + 1, args.getInt("totalBooks")));
                    }

                    break;
                case OP_GET_BOOK_FILE_SEGMENT:
                    File f = new File(rootDirectory, args.getString("lpath"));
                    boolean isValidFile = f.isFile() && f.exists();
                    args = new JSONObject();
                    args.put("willStreamBinary", true);
                    if (isValidFile) {
                        args.put("fileLength", f.length());
                    } else {
                        args.put("fileLength", 0);
                    }
                    send_to_server(OP_OK, args);
                    if (isValidFile) {
                        upload_file(f);
                    }

                    break;
                case OP_DELETE_BOOK:
                    delete_files(args.getJSONArray("lpaths"));
                    mNotificationManager.cancel(args.getJSONArray("lpaths").getString(0), NOTIFY_TAG);
                    break;

                case OP_DISPLAY_MESSAGE:
                    broadcastError(ErrorType.WRONG_PASSWORD);
                    SharedPreferences.Editor editor = mSettings.edit();
                    editor.remove("pref_wd_port");
                    editor.remove("pref_password");
                    editor.remove("pref_autocon");
                    editor.remove("pref_networkid");
                    editor.commit();
                    send_to_server(OP_OK, null);
                    break;
            }
            broadcastConnectionStateChange(ConnectionState.IDLING);

        } catch (JSONException e) {
            logAndDie(e);
        }
    }

    private JSONObject initialisation_reply(JSONObject init_args) throws JSONException {
        JSONArray exts = new JSONArray();
        for (String e : mSettings.getString("pref_filetypes", "epub pdf mobi azw3 pdb fb2 rtf html").split(" ")) {
            exts.put(e);
        }
        JSONObject replyJSON = new JSONObject()
                .put("versionOK", (init_args.getInt("serverProtocolVersion") == PROTOCOL_VERSION))
                .put("maxBookContentPacketLen", BASE_PACKET_LEN)
                .put("ccVersionNumber", APP_VERSION)
                .put("acceptedExtensions", exts)
                .put("canStreamBooks", true)
                .put("canStreamMetadata", true)
                .put("canReceiveBookBinary", true)
                .put("canDeleteMultipleBooks", true)
                .put("deviceKind", Build.PRODUCT)
                .put("coverHeight", 80);
        try {
            if (init_args.has("passwordChallenge")) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                String c = init_args.getString("passwordChallenge");
                md.update(currentServer.getPassword().getBytes("utf-8"));
                md.update(c.getBytes("utf-8"));
                replyJSON.put("passwordHash", toHex(md.digest()));
            }
        } catch (Exception e) {
            logAndDie(e);
        }
        return replyJSON;
    }

    private JSONObject device_information_reply() throws JSONException {
        return new JSONObject()
                .put("device_version", Build.VERSION.RELEASE)
                .put("device_info", mBookCrawler.getDriveInfo())
                .put("version", APP_VERSION);

    }

    private void stream_meta_to_servers() {
        try {
            JSONObject replyJSON = new JSONObject();
            Cursor cs = mDB.getNotNull(Book.COLUMNS, "lpath");
            replyJSON.put("count", cs.getCount() + mBookCrawler.numNewBooks())
                    .put("willStream", true)
                    .put("willScan", true);
            send_to_server(OP_OK, replyJSON);
            cs.moveToFirst();
            Logger.d(String.format("stream_meta_to_servers Loop:%d and %d", cs.getCount(), mBookCrawler.numNewBooks()));
            while (!cs.isAfterLast()) {
                JSONObject tmp_book = new JSONObject()
                        .put("uuid", cs.getString(Book.UUID))
                        .put("lpath", cs.getString(Book.LPATH))
                        .put("authors", new JSONArray().put(cs.getString(Book.AUTHOR)))
                        .put("title", cs.getString(Book.TITLE));
                send_to_server(OP_OK, tmp_book);
                cs.moveToNext();

            }
            cs.close();
            for (JSONObject o : mBookCrawler.getNewBooks()) {
                send_to_server(OP_OK, o);
            }
            //TODO: Clear new books here?
        } catch (JSONException e) {
            logAndDie(e);
        }
    }

    /**
     * Reads from Socket Input Stream
     *
     * @return A 'likely' well-formed JSON StringBuilder
     */
    private StringBuilder read_from_server() {
        StringBuilder incomingString = new StringBuilder();
        int jsonLength = 0, jsonStringOffset = 0, bytesRead = 0, bytesToRead = 16;
        //Incoming string format <length of JSON in bytes>[JSONArray (incl. square brackets)]
        try {
            while (bytesToRead > 0) {
                bytesRead = inputStream.read(connBuf, 0, Math.min(connBuf.length, bytesToRead));
                if (bytesRead < 0) break;
                incomingString.append(new String(connBuf, 0, bytesRead, "utf-8"));
                if (jsonLength == 0) {
                    jsonStringOffset = incomingString.indexOf("[");
                    if (jsonStringOffset < 0) continue;
                    jsonLength = Integer.parseInt(incomingString.substring(0, jsonStringOffset));
                    bytesToRead = jsonLength + jsonStringOffset;
                }
                if (bytesToRead < bytesRead) {
                    throw (new Exception());
                } else {
                    bytesToRead -= bytesRead;
                }
            }
            if (jsonLength > 0 && jsonStringOffset > 0 &&
                    incomingString.length() == jsonLength + jsonStringOffset) {
                incomingString.delete(0, jsonStringOffset);
                Logger.d("<- " + incomingString);
                return incomingString;
            } else {
                throw (new Exception());
            }
        } catch (Exception e) {
            disconnectFromServer();
            logAndDie(e);
        }
        return null;
    }

    private void send_to_server(int op, JSONObject args) {
        try {
            JSONArray packet = new JSONArray();
            packet.put(op);
            if (args != null) {
                packet.put(args);
            } else {
                packet.put(new JSONObject());
            }
            Logger.d("-> " + packet.toString());
            byte[] raw_packet = packet.toString().getBytes();
            outputStream.write(String.format("%d", raw_packet.length).getBytes());
            outputStream.write(raw_packet);
            outputStream.flush();
        } catch (Exception e) {
            logAndDie(e);
        }
    }

    private synchronized void download_file(int remaining, File f) {
        int c = 0, last_c = 0;
        try {
            BufferedOutputStream fw = new BufferedOutputStream(new FileOutputStream(f));
            while ((c = inputStream.read(connBuf, 0, Math.min(connBuf.length, remaining))) > 0) {
                fw.write(connBuf, 0, c);
                remaining -= c;
                last_c = c;
            }
            fw.flush();
            fw.close();
            if (remaining > 0) {
                byte[] last = new byte[32];
                System.arraycopy(connBuf, Math.max(0, last_c - 32), last, 0, Math.min(last_c, 32));
                Logger.d("Malformed file, Last hex: " + toHex(last));
                broadcastError(ErrorType.CANT_DOWNLOAD_FILE);
            }

        } catch (IOException e) {
            logAndDie(e);
        }
    }

    private synchronized void upload_file(File f) {
        int c = 0;
        try {
            BufferedInputStream fr = new BufferedInputStream(new FileInputStream(f));
            while ((c = fr.read(fileBuf, 0, fileBuf.length)) > 0) {
                outputStream.write(fileBuf, 0, c);
            }
            outputStream.flush();
        } catch (IOException e) {
            logAndDie(e);
        }
    }



    private synchronized void delete_files(JSONArray f) {
        JSONObject rep = new JSONObject();
        try {
            send_to_server(OP_OK, null);
            for (int i = 0; i < f.length(); i++) {
                rep.put("uuid", delete_file(f.getString(i)));
                send_to_server(OP_OK, rep);

            }
        } catch (Exception e) {
            logAndDie(e);

        }
    }

    private String delete_file(String lpath) {
        File f = new File(rootDirectory, lpath);
        if (f.delete())
            return mDB.deleteByPath(lpath);
        return null;
    }

    private Bitmap generateBitmap(byte[] raw_thumb) {
        try {
            return BitmapFactory.decodeByteArray(raw_thumb, 0, raw_thumb.length);
        } catch (Exception e) {
            Logger.e(e);
            return null;
        }
    }

    private void ui_notify(String lpath, File fn, Bitmap pic, boolean update) {
        Notification mNotification;
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(fn);
        String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (ext != null) {
            viewIntent.setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext));
            PendingIntent pendingViewIntent = PendingIntent.getActivity(this, 0, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
            builder.setContentIntent(pendingViewIntent)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentText(lpath)
                    .setWhen(System.currentTimeMillis());
            if (update) {
                builder.setContentTitle(getText(R.string.note_updated));
            } else {
                builder.setContentTitle(getText(R.string.note_received));
            }
            if (pic != null) builder.setLargeIcon(pic);
            mNotification = builder.build();
            mNotificationManager.notify(NOTIFY_TAG, mNotification);
        }
    }

    private void broadcastServer(ConnectionInfo ci) {
        if (serviceHandler != null) {
            serviceHandler.sendMessage(serviceHandler.obtainMessage(HandlerMessageType.SERVER.id(), ci));
            Logger.d("broadcastServer:" + (ci != null ? ci.toString() : "null (clear)"));
        } else {
            Logger.d("serviceLog: SerivceHandler null");
        }
    }

    private void broadcastDiscoveryStateChange(DiscoveryState d) {
        if (serviceHandler != null) {
            serviceHandler.sendMessage(serviceHandler.obtainMessage(HandlerMessageType.DISCOVERY.id(), d));
            Logger.d("broadcastDiscoveryStateChange: " + d.toString());
        } else {
            Logger.d("serviceLog: SerivceHandler null");
        }
    }

    private void broadcastConnectionStateChange(ConnectionState s) {
        if (serviceHandler != null) {
            serviceHandler.sendMessage(serviceHandler.obtainMessage(HandlerMessageType.CONNECTION.id(), s));
            Logger.d("broadcastConnectionStateChange: " + s.toString());
        } else {
            Logger.d("broadcastConnectionStateChange: SerivceHandler null");
        }
    }

    private void broadcastError(ErrorType e) {
        if (serviceHandler != null) {
            serviceHandler.sendMessage(serviceHandler.obtainMessage(HandlerMessageType.ERROR.id(), e));
            Logger.d("broadcastError: " + e.toString());
        } else {
            Logger.d("broadcastError: SerivceHandler null");
        }
    }

    public void enquireConnectionState() {
        if (isConnected()) {
            broadcastConnectionStateChange(ConnectionState.COMMUNICATING);
        } else if (connectionThreadIsRunning()) {
            broadcastConnectionStateChange(ConnectionState.CONNECTING);
        } else {
            broadcastConnectionStateChange(ConnectionState.DISCONNECTED);
        }
        if (discoveryThreadIsRunning()) {
            broadcastDiscoveryStateChange(DiscoveryState.BEGIN_DISCOVERY);
        }
    }

    public ArrayList<ConnectionInfo> getListOfServers() {
        return listOfServers;
    }

    public ConnectionInfo getCurrentServer() {
        return currentServer;
    }

    private void logAndDie(Throwable e) {
        Logger.e(e);
        stopSelf();
    }

    public enum ConnectionState {
        CONNECTING, COMMUNICATING, IDLING, DISCONNECTED, WAITING_FOR_FOLDERSCAN
    }

    public enum ErrorType {
        CALIBRE_BUSY, CANT_CONNECT, CANT_DOWNLOAD_FILE, WRONG_PASSWORD, ROOT_NO_ACCESS
    }

    public enum DiscoveryState {
        BEGIN_DISCOVERY, DONE_DISCOVERY, NEW_SERVER, NO_SERVER
    }

    public enum HandlerMessageType {
        CONNECTION(0), DISCOVERY(1), ERROR(2), SERVER(3);
        int code;

        HandlerMessageType(int code) {
            this.code = code;
        }

        public int id() {
            return code;
        }

    }

    public static interface CalibreListener {
        public void onConnectionStateChanged(ConnectionState connectionState);

        public void onDiscoveryStateChanged(DiscoveryState discoveryState);

        public void onErrorReported(ErrorType e);
    }

    public class CalibreBinder extends Binder {

        public CalibreService getService() {
            return CalibreService.this;
        }
    }
}