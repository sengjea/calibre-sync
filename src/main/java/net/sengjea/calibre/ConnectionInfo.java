package net.sengjea.calibre;

import java.net.*;

public class ConnectionInfo {
	private String host;
	private int wd_port;
	private int cs_port;
	private SocketAddress wd_socket;
	private URL cs_url;
	private String password;
	private String libraryName;
    private boolean resolved;
	
	public ConnectionInfo(String host, int wd_port, int cs_port) {
		this.host = host;
		this.wd_port = wd_port;
		this.cs_port = cs_port;
	}
    public ConnectionInfo(String host, int wd_port, int cs_port, String password) {
        this.host = host;
        this.wd_port = wd_port;
        this.cs_port = cs_port;
        this.password = password;
    }

	public void resolveAddresses() throws MalformedURLException, UnknownHostException {
        if (cs_url == null || wd_socket == null) {
			wd_socket = new InetSocketAddress(InetAddress.getByName(host), wd_port);
			cs_url = new URL("http", host, cs_port, "/opds");
        }
	}

	public String toString() {
		return getHost() + ":" + getWirelessDevicePort();
	}
	public String getWirelessDevicePort() {
		return Integer.toString(wd_port);
	}
	public String getContentServerPort() {
		return Integer.toString(cs_port);
	}
	public String getHost() {
		return host;
	}
	public SocketAddress getWirelessDeviceSocket() {
		return wd_socket;
	}
	public URL getContentServerURL() {
		return cs_url;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLibraryName() {
		return libraryName;
	}

	public void setLibraryName(String libraryName) {
		this.libraryName = libraryName;
	}

	public String repr() {
		return String.format("{ Socket=%s, URL=%s, Password=%s, Name=%s }",
				wd_socket.toString(),
				cs_url.toString(),
				password,libraryName);
	}
}
