package net.sengjea.calibre;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

public abstract class OPDSLoader {


    public OPDSLoader() {
        super();
        resetPages();
    }


    private Thread xml_thread;
    private final String uuid_identifier = "urn:uuid:";
    private final String tags_identifier = "TAGS: ";
    private MetadataDatabaseHelper mDB;
    protected Context context;
    private URL OPDSUrl;
    private Stack<OPDSPage> pages;
    private OPDSPage current_page;
    boolean triggerStop = false;
    private static final int XML_PARSE_COMPLETE = 0;
    private Handler mainThreadHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == XML_PARSE_COMPLETE) {
                onParseComplete(triggerStop ? null: current_page);
                triggerStop = false;
            }
        }
    };

    private Runnable xml_runnable = new Runnable() {
        public void run() {
            try {
                mDB = new MetadataDatabaseHelper(context);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                HttpURLConnection connection = (HttpURLConnection) current_page.peekURL().openConnection();
                int linkType = OPDSEntry.LINK_TYPE_UNKNOWN;
                xpp.setInput(connection.getInputStream(), null);
                int eventType = xpp.getEventType();
                StringBuilder heirarchy = new StringBuilder();
                Stack<Integer> heirarchy_length = new Stack<Integer>();
                current_page.clearNavigationalLinks();
                OPDSEntry current_entry = new OPDSEntry();
                while (eventType != XmlPullParser.END_DOCUMENT && !triggerStop) {
                    if(eventType == XmlPullParser.START_DOCUMENT) {
                        //Log.d(LOG_TAG,"Start document");
                    } else if(eventType == XmlPullParser.START_TAG) {
                        heirarchy_length.push(heirarchy.length());
                        heirarchy.append("/"+xpp.getName());
                        if (heirarchy.toString().endsWith("feed/entry")) {
                            current_entry = new OPDSEntry();
                        } else if (heirarchy.toString().endsWith("entry/link")) {
                            linkType = getLinkType(xpp);
                            if (linkType == OPDSEntry.LINK_TYPE_THUMB) {
                                current_entry.thumb = getThumb(current_entry.uuid,xpp.getAttributeValue(null, "href"));
                            } else if (linkType != OPDSEntry.LINK_TYPE_UNKNOWN) {
                                current_entry.linkType = Integer.valueOf(linkType);
                                current_entry.href = xpp.getAttributeValue(null, "href");
                            }
                        }
//						else if (heirarchy.toString().endsWith("entry/content")) {
//							skip(xpp);
//							heirarchy.setLength(heirarchy_length.pop());
//						} 
                        else if (heirarchy.toString().endsWith("feed/link") && xpp.getAttributeValue(null, "rel") != null) {
                            //Logger.d(xpp.getAttributeValue(null, "rel") + " = "+ xpp.getAttributeValue(null, "href"));
                            current_page.addNavigationalLink(xpp.getAttributeValue(null, "rel"), xpp.getAttributeValue(null, "href"));
                        }
                        //Log.d(LOG_TAG,"Now in: "+heirarchy.toString());
                    } else if(eventType == XmlPullParser.TEXT) {
                        if (heirarchy.toString().endsWith("entry/title")) {
                            current_entry.title = xpp.getText();
                        } else if (heirarchy.toString().endsWith("entry/author/name")) {
                            current_entry.author = xpp.getText();
                        } else if (heirarchy.toString().endsWith("entry/id") && xpp.getText().startsWith(uuid_identifier)) {
                            Cursor cs;
                            current_entry.uuid = xpp.getText().substring(uuid_identifier.length());
                            cs = mDB.getByUuid(new String[] {"_id" }, current_entry.uuid);
                            if (cs.moveToFirst()) {
                                current_entry.id = cs.getInt(0);
                            }
                        } else if (heirarchy.toString().endsWith("entry/content/div") && xpp.getText().startsWith(tags_identifier)) {
                            current_entry.tags = xpp.getText().substring(tags_identifier.length()).split(",");

                        }
                    } else if(eventType == XmlPullParser.END_TAG) {
                        if (heirarchy.toString().endsWith("feed/entry")) {
                            if (current_entry.linkType == OPDSEntry.LINK_TYPE_ACQ) {
                                if (current_entry.id < 0 && current_entry.uuid != null && current_entry.author != null &&
                                        current_entry.title != null)  {
                                    current_entry.id = mDB.insertMetadata(current_entry.uuid,null,
                                            current_entry.author,
                                            current_entry.title,
                                            current_entry.tags,
                                            current_entry.thumb);
                                }
                                if (current_entry.id >= 0) {
                                     current_page.addBook(current_entry);
                                }
                            } else if (current_entry.linkType == OPDSEntry.LINK_TYPE_CAT) {
                               current_page.addCatalogLink(current_entry.title, current_entry.href);
                            }
                        }
                        heirarchy.setLength(heirarchy_length.pop());
                    }
                    eventType = xpp.next();

                }

            } catch (Exception e) {
                Logger.e(e);
                current_page = null;
            } finally {
                //Log.d(LOG_TAG, "XMLParsing Complete");
                mainThreadHandler.sendEmptyMessage(XML_PARSE_COMPLETE);
                mDB.close();
            }
        }
    };

    abstract void onStartParse();
    abstract void onParseComplete(OPDSPage p);
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
    private int getLinkType(XmlPullParser xpp) throws XmlPullParserException, IOException  {
        String rel = xpp.getAttributeValue(null, "rel");
        String type = xpp.getAttributeValue(null, "type");
        if (rel != null && rel.endsWith("acquisition")) return OPDSEntry.LINK_TYPE_ACQ;
        if (rel != null && rel.endsWith("thumbnail")) return OPDSEntry.LINK_TYPE_THUMB;
        //if (rel != null && rel.equals("next")) return LINK_TYPE_NEXT;
        if (type != null && type.endsWith("opds-catalog")) return OPDSEntry.LINK_TYPE_CAT;
        return OPDSEntry.LINK_TYPE_UNKNOWN;
    }

    public void setContext(Context context) {
        this.context = context;
    }
    private byte[] getThumb(String uuid, String url) {
        Cursor cs = mDB.getByUuid(new String[] {"thumbnail"}, uuid);
        if (cs.moveToFirst() && cs.getBlob(0) != null) {
            return cs.getBlob(0);
        } else {

            try {
                if (triggerStop) return null;
                URL tmp_cs = new URL(current_page.peekURL().getProtocol(),
                                     current_page.peekURL().getHost(),
                                     current_page.peekURL().getPort(),
                                    url);
                if (triggerStop) return null;
                HttpURLConnection connection = (HttpURLConnection) tmp_cs.openConnection();
                int content_length = connection.getContentLength();
                byte[] thumb_bytes = new byte[content_length];
                connection.getInputStream().read(thumb_bytes);
                connection.disconnect();
                if (BitmapFactory.decodeByteArray(thumb_bytes,0,thumb_bytes.length) != null) return thumb_bytes;
            } catch (Exception e) {
                Logger.e(e);
            }
        }
        return null;
    }
    public OPDSPage getPreviousPage() {
        if (pages.isEmpty()) return null;
        if (isParsingXml()) triggerStop = true;
        current_page = pages.pop();
        return current_page;
    }

    public void resetPages() {
        if (pages != null ) {
            pages.empty();
        } else {
            pages = new Stack<OPDSPage>();

        }
    }
    public boolean pageHasParent() {
        return (pages != null && !pages.isEmpty());
    }
    public boolean isParsingXml() {
        if (xml_thread != null && xml_thread.isAlive()) return true;
        return false;
    }
    public boolean beginParsingXml(URL url, boolean more) {
        if (!isParsingXml()) {
            if (current_page == null)
                current_page = new OPDSPage(url);
            else {
                if (more)
                    current_page.addURL(url);
                else  {
                    pages.push(current_page);
                    current_page = new OPDSPage(url);
                }
            }
            xml_thread = new Thread(xml_runnable);
            xml_thread.start();
            return true;
        }
        return false;
    }
}
