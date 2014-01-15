package net.sengjea.calibre;

public class OPDSEntry {
	public static final int LINK_TYPE_NEXT = 4;
	public static final int LINK_TYPE_THUMB = 3;
	public static final int LINK_TYPE_ACQ = 2;
	public static final int LINK_TYPE_CAT = 1;
	public static final int LINK_TYPE_UNKNOWN = 0;
	public int id = -1;
	public String uuid;
	public String title;
	public String author;
	public String href;
	public byte[] thumb;
	public String[] tags;
	public int linkType;
}
