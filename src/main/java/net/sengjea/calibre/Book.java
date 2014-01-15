package net.sengjea.calibre;

import android.database.Cursor;

public class Book {
	public static final String[] COLUMNS = {"_id", "uuid","lpath","author", "title", "thumbnail"};
	public static final int ID 			= 0;
	public static final int UUID 		= 1;
	public static final int LPATH 		= 2;
	public static final int AUTHOR 		= 3;
	public static final int TITLE 		= 4;
	public static final int THUMB 		= 5;
//	private String uuid, lpath, author, title;
//    private byte[] thumbnail;
//    public Book(String uuid, String lpath, String author, String title) {
//
//        this.uuid = uuid;
//        this.lpath = lpath;
//        this.author = author;
//        this.title = title;
//    }
//    public String getTitle() {
//        return title;
//    }
//    public String getUuid() {
//        return uuid;
//    }
//    public String getAuthor() {
//        return author;
//    }
//    public byte[] getThumbnail() {
//        return thumbnail;
//    }
//    public void setThumbnail(byte[] thumb) {
//        thumbnail = thumb;
//    }
//
//    public static Book makeFromCursor(Cursor cs) {
//        try {
//            return new Book(cs.getString(UUID),cs.getString(LPATH),cs.getString(AUTHOR), cs.getString(TITLE));
//        } catch (Exception e) {
//            return null;
//        }
//    }
}
