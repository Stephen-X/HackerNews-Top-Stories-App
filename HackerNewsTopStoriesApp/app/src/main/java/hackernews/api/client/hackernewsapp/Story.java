package hackernews.api.client.hackernewsapp;

import java.util.Date;

/**
 * A story model for the HackerNews stories.
 *
 * @author Stephen Xie &lt;[redacted]@andrew.cmu.edu&gt;
 */
class Story {
    private String id;
    private String title;
    private String by;  // author of this story
    private String url;
    private long time;  // the story's creation date in Unix time
    private Comment[] comments;

    public Story(String id, String title, String by, long time) {
        this.id = id;
        this.title = title;
        this.by = by;
        this.time = time;
    }

    public Story(String id, String title, String by, String url, long time, Comment[] comments) {
        this.id = id;
        this.title = title;
        this.by = by;
        this.url = url;
        this.time = time;
        this.comments = comments;
    }

    // --- getters --------------------------

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBy() {
        return by;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Converts unix timestamp to Java Date before returning it.
     * @return date this story is created
     */
    public Date getTime() {
        return new Date(time * 1000);
    }

    public Comment[] getComments() {
        return comments;
    }
}
