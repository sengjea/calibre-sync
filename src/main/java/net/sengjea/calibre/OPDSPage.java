package net.sengjea.calibre;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * User: sengjea
 * Date: 23/12/13
 * Time: 18:23
 */
public class OPDSPage {
    private MatrixCursor books_cursor;
    private Stack<URL> object_urls;
    private Map<String,String> nav_urls;
    private Map<String, String> cat_urls;

    public OPDSPage(URL url) {
        object_urls = new Stack<URL>();
        object_urls.push(url);
        cat_urls = new HashMap<String, String>();
        nav_urls = new HashMap<String,String>();
        books_cursor = new MatrixCursor(Book.COLUMNS);
    }
    public MatrixCursor getBooks() {
        return books_cursor;
    }
    public Map<String,String> getCatalogLinks() {
        return cat_urls;
    }
    public void addCatalogLink(String k, String u) {
        cat_urls.put(k, u);
    }
    public void clearCatalogLinks() {
        cat_urls.clear();
    }
    public String getCatalogLinkFor(String s) {
        if (cat_urls.containsKey(s))
            return cat_urls.get(s);
        return null;
    }
    public Map<String,String> getNavigationalLinks() {
        return nav_urls;
    }
    public void addNavigationalLink(String k, String u) {
        nav_urls.put(k, u);
    }
    public void clearNavigationalLinks() {
        nav_urls.clear();
    }
    public void addBook(OPDSEntry e) {
        books_cursor.addRow(new Object[] { e.id,
                e.uuid, e.href, e.author,
                e.title, e.thumb });
    }
    public URL addURL(URL url) {
        return object_urls.push(url);
    }
    public boolean removeURLs() {
        return object_urls.empty();
    }
    public URL peekURL() {
        return object_urls.peek();
    }

    public boolean hasCatalogLinks() {
        return (!cat_urls.isEmpty());
    }
}
