package net.sengjea.calibre;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint("NewApi")
class MetadataDatabaseHelper extends SQLiteOpenHelper {

	private final static int DATABASE_VERSION 		=	4;
	private final static String DATABASE_NAME		= "calibre.metadata.db";
	private final static String TABLE_METADATA		= "metadata";
	private final static String TABLE_BOOK_TAG		= "book_tag";
	private final static String TABLE_TAGS			= "tags";



	private final String[] CREATE_TABLES = { 
			"CREATE TABLE " + TABLE_METADATA + "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, uuid TEXT UNIQUE, lpath TEXT UNIQUE, author TEXT, title TEXT, thumbnail BLOB)",
			"CREATE TABLE " + TABLE_BOOK_TAG + "(metadata_id INTEGER, tag_id INTEGER, PRIMARY KEY (metadata_id, tag_id))",
			"CREATE TABLE " + TABLE_TAGS + "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,tag TEXT UNIQUE)",

	};

	public MetadataDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for(String stmt : CREATE_TABLES) db.execSQL(stmt);			
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (int i=oldVersion; i<newVersion; i++) {
			switch (i) {
			//case 3 means upgrading from version 3 to version 4, and so on
			case 2:
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_METADATA);
				db.execSQL(CREATE_TABLES[0]); //TABLE_METADATA
				break;
			case 3:
				db.execSQL(CREATE_TABLES[1]); //TABLE BOOK_TAG
				db.execSQL(CREATE_TABLES[2]); //TABLE TAGS
				break;
			}
		}
	}
	/**
	 * @param uuid
	 * @param lpath
	 * @param author
	 * @param title
	 * @param tags 
	 * @param thumbnail
	 * @return
	 */
	public int insertMetadata(String uuid, String lpath, String author, String title, String[] tags, byte[] thumbnail) {
		ContentValues cv = new ContentValues();
		SQLiteDatabase db = this.getWritableDatabase();
		int metadata_id = -1;
		int tag_id;
		try {
			if (thumbnail != null) {
				cv.put("thumbnail",thumbnail);
			} else {
				cv.putNull("thumbnail");
			}
			cv.put("uuid", uuid);
			if (lpath != null) {
				cv.put("lpath", lpath);
			} else {
				cv.putNull("lpath");
			}
			cv.put("author", author);
			cv.put("title", title);
			try {
			db.insertOrThrow(TABLE_METADATA, null, cv);
			} catch (SQLiteConstraintException e)  {
				db.update(TABLE_METADATA, cv, "uuid=?", new String[] {uuid});
			} catch (Exception e) {
				throw e;
			}
			Cursor cs = getByUuid(new String[] {"_id"}, uuid);
            if (cs.moveToFirst()) {
                metadata_id = cs.getInt(0);
            }
			if (metadata_id >= 0 && tags != null) {
				for (String tag : tags) {
					if ((tag_id = insertTag(tag)) >= 0) {
					try {
						ContentValues cvt = new ContentValues();
						cvt.put("metadata_id", metadata_id);
						cvt.put("tag_id", tag_id);
						db.insertOrThrow(TABLE_BOOK_TAG, null, cvt);
					} catch (SQLiteConstraintException e) {
						Logger.d("Unable to add ( " + metadata_id + ", " + tag_id + " ) to "+ TABLE_BOOK_TAG);
						continue;
					}
					}
				}
			}
            return metadata_id;
		} catch (Exception e) {
			Logger.e(e);
            return -1;
		}
	}
	public boolean insertBook(String uuid, String lpath) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("lpath",lpath);
		if (db.update(TABLE_METADATA, cv, "uuid=?", new String[] {uuid}) < 0) {
			Logger.d("unable to insertBook");
			return false;
		} else {
			Logger.d("insertBook successful: "+ uuid + " " + lpath);
			return true;
		}
	}

	public String deleteByPath(String lpath) {
		Cursor cs = getByPath(new String[] {"_id", "uuid"}, lpath);
		if (cs.moveToFirst() && deleteBook(cs.getInt(0))) {
			return cs.getString(1);
		} else {
			Logger.d("Did not delete book " + lpath );
			return null;
		}

	}
	public boolean deleteBook(int id) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.putNull("lpath");
		if ( db.update(TABLE_METADATA, cv, String.format("_id=%d",id), null) > 0) {
			db.delete(TABLE_BOOK_TAG, String.format("metadata_id=%d",id) , null );
			return true;
		} else {
			return false;
		}
	}
	
	public Cursor getNotNull(String[] columns, String criteria) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.query(TABLE_METADATA, columns, criteria + " IS NOT NULL", null, null, null, "title");
	}
	
	public Cursor getByPath(String[] columns, String lpath) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.query(TABLE_METADATA, columns, "lpath=?",new String[] {lpath}, null, null, null);
	}
	
	public Cursor getByUuid(String[] columns, String uuid) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.query(TABLE_METADATA, columns, "uuid=?",new String[] {uuid}, null, null, null);
	}
	
	public Cursor getById(String[] columns, int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.query(TABLE_METADATA, columns, String.format("_id=%d",id), null, null, null, null);
	}
    public Cursor search(String[] columns, String s) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_METADATA, columns, "title LIKE ?", new String[] {"%"+s+"%"}, null, null, null);
    }
	public Cursor getBooksWithTag(String[] columns, int tag_id) {
		SQLiteDatabase db = this.getReadableDatabase();
		// Join all three tables, metadata, book_tag, tags based on
		// the many-to-many relationship on book_tag and returns the desired metadata
		return db.query(TABLE_METADATA	+ " INNER JOIN "+ TABLE_BOOK_TAG
							+ " ON " + TABLE_METADATA + "._id = " + TABLE_BOOK_TAG + ".metadata_id "
		//								+ " INNER JOIN " + TABLE_TAGS
		//					+ " ON " + TABLE_BOOK_TAG + ".tag_id = " + TABLE_TAGS + "._id"
							, 
							columns ,
//							new String[] {	TABLE_METADATA +"._id AS _id", TABLE_METADATA +".uuid AS uuid"
//											,TABLE_METADATA +".lpath AS lpath",TABLE_METADATA +".author AS author"
//											,TABLE_METADATA +".title AS title",TABLE_METADATA +".thumbnail AS thumbnail"
//											},
								TABLE_BOOK_TAG + String.format(".tag_id=%d AND ",tag_id) + TABLE_METADATA + ".lpath IS NOT NULL", null, null, null, null);
	}

    public int insertTag(String tag) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("tag", tag);
            db.insertOrThrow(TABLE_TAGS, null, cv);
        } catch (SQLiteConstraintException e) {
            Logger.e(e);
        }
        Cursor cs = db.query(TABLE_TAGS, new String[] {"_id"}, "tag=?", new String[] { tag }, null, null, null);
        if (cs.moveToFirst()) {
            return cs.getInt(0);
        } else {
            Logger.d("Tag " + tag + " not found");
            return -1;
        }
    }

	public Cursor getTagsWithBooks() {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.query(TABLE_METADATA	+ " INNER JOIN "+ TABLE_BOOK_TAG
				+ " ON " + TABLE_METADATA + "._id = " + TABLE_BOOK_TAG + ".metadata_id "
								+ " INNER JOIN " + TABLE_TAGS
					+ " ON " + TABLE_BOOK_TAG + ".tag_id = " + TABLE_TAGS + "._id"
				, 
				new String[] {	TABLE_TAGS +"._id AS _id", TABLE_TAGS +".tag AS tag"
//								,TABLE_METADATA +".lpath AS lpath",TABLE_METADATA +".author AS author"
//								,TABLE_METADATA +".title AS title",TABLE_METADATA +".thumbnail AS thumbnail"
								},
								TABLE_METADATA + ".lpath IS NOT NULL", null, null, null, null);
	}

    public void printMetadataTable() {
       String output = "";
       SQLiteDatabase db = this.getReadableDatabase();
       Cursor cs = db.query(TABLE_METADATA, new String[] {"uuid", "lpath"}, null, null, null, null, "lpath");
       cs.moveToFirst();
       while (!cs.isAfterLast()) {
           Logger.d("Book in DB: " + cs.getString(0) + "   " +cs.getString(1));
           cs.moveToNext();
       }
    }

    public void emptyAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BOOK_TAG, null, null );
        db.delete(TABLE_METADATA, null, null );
        db.delete(TABLE_TAGS, null, null);

    }

    public byte[] insertJSON(JSONObject book) throws JSONException {
        String author, title, lpath;
        String[] tags = null;
        byte[] raw_thumb;
        if (book.optString("uuid") == null) {
            //broadcastError(ErrorType.BOOK_PATH_NULL);
            return null;
        }
        if (book.optString("author_sort") != null)
            author = book.getString("author_sort");
        else if (book.optJSONArray("authors") != null &&
                book.getJSONArray("authors").optString(0) != null)
            author = book.getJSONArray("authors").getString(0);
        else {
            //broadcastError(ErrorType.BOOK_UUID_NULL);
            return null;
        }
        if (book.optString("title") != null)
            title = book.getString("title");
        else if (book.optString("title_sort") != null)
            title = book.getString("title_sort");
        else {
            //broadcastError(ErrorType.BOOK_TITLE_NULL);
            return null;
        }
        if (book.optString("lpath") != null)
            lpath = book.getString("lpath");
        else {
            lpath = null;
        }
        if (book.optJSONArray("thumbnail") != null
                && book.getJSONArray("thumbnail").length() > 2)
            raw_thumb = Base64.decode(book.getJSONArray("thumbnail").getString(2), Base64.DEFAULT);
        else
            raw_thumb = null;

        if (book.optJSONArray("tags") != null) {
            JSONArray json_tags = book.getJSONArray("tags");
            tags = new String[json_tags.length()];
            for (int i = 0; i < json_tags.length(); i++) {
                tags[i] = json_tags.optString(i);
            }
        }

        this.insertMetadata(book.getString("uuid"), lpath, author, title, tags, raw_thumb);
        return raw_thumb;
    }
}
