package hackernews.api.client.hackernewsapp;

/**
 * One place to host all API related URLs.
 *
 * @author Stephen Xie &lt;[redacted]@andrew.cmu.edu&gt;
 */
enum API_URL {
    // use 10.0.2.2 in Android VM to access localhost on local test environment
//    TOP_STORIES("http://10.0.2.2:8080/hackernews-api/top-stories"),
//    STORY_BASE("http://10.0.2.2:8080/hackernews-api/story/");
    TOP_STORIES("https://desolate-cliffs-59321.herokuapp.com/hackernews-api/top-stories"),
    STORY_BASE("https://desolate-cliffs-59321.herokuapp.com/hackernews-api/story/");

    private final String url;

    API_URL(final String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }
}