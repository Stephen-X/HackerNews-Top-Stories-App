package hackernews.api.client.hackernewsapp;

import android.text.Html;
import android.text.Spanned;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A story model for the HackerNews stories.
 *
 * @author Stephen Xie &lt;[redacted]@andrew.cmu.edu&gt;
 */
class Comment {
    private String id;
    private String by;
    private long time;  // this comment's creation date in Unix time
    private String text;
    private String[] comments;  // IDs of direct sub-comments

    // date format sample: 1:44 PM, Wed, 4 Jul 2001
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm a, EEE, d MMM yyyy");

    public Comment(String id, String by, long time, String text) {
        this.id = id;
        this.by = by;
        this.time = time;
        this.text = text;
    }

    public Comment(String id, String by, long time, String text, String[] comments) {
        this.id = id;
        this.by = by;
        this.time = time;
        this.text = text;
        this.comments = comments;
    }

    public String getId() {
        return id;
    }

    public String getBy() {
        return by;
    }

    /**
     * Parse HTML code in text before returning it.
     * @return displayable styled text from the original HTML text
     */
    public Spanned getText() {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
    }

    /**
     * Converts unix timestamp to Java Date before returning it.
     * @return date this story is created
     */
    public Date getTime() {
        return new Date(time * 1000);
    }

    /**
     * Returns a formatted time string.
     *
     * @return a formatted time string
     */
    public String getTimeString() {
        Date date = new Date(time * 1000);
        return dateFormatter.format(date);
    }

    public String[] getComments() {
        return comments;
    }
}
