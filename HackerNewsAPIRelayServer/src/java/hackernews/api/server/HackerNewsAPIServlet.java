package hackernews.api.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * API servlet for retrieving HackerNews top stories and details.
 * <p>
 * Supported queries:
 * <ol>
 *     <li><strong>/top-stories:</strong> get ids and titles of the latest top stories.</li>
 *     <li><strong>/story/{id}:</strong> get content of a specified story.</li>
 * </ol>
 * <p>
 * <strong>Additional note:</strong> I still don't get why we're not allowed to
 * use JAX-RS; using annotations to direct resource access is much simpler and
 * less prone to error than writing redirections ourselves in a plain servlet.
 * 
 * @author Stephen Xie &lt;[redacted]@andrew.cmu.edu&gt;
 */
@WebServlet(name = "HackerNewsAPIServlet", urlPatterns = {"/hackernews-api/*"})
public class HackerNewsAPIServlet extends HttpServlet {
    
    private static final String topStoryURL = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String itemBaseURL = "https://hacker-news.firebaseio.com/v0/item/";
    
    private APIUsageLogger logger;  // logger to the remote MongoDB database

    @Override
    public void init() throws ServletException {
        super.init();
        
        // initialize the logger service
        logger = APIUsageLogger.getInstance();
    }
    
    
    // --- Model ------------------------------------------------------------------------------

    /**
     * This returns a JSON string containing all the top story IDs as keys and
     * their respective titles as values.
     * 
     * @param limit maximum number of stories to be returned
     * @return the JSON string in this format: [{"id": "...", "title": "...", "by": "..."}]
     */
    private String getTopStories(int limit) {
        String topStories;
        try {
            long startTime = System.currentTimeMillis();
            topStories = fetch(topStoryURL);
            logger.logTimeUsed2GetTopStories(System.currentTimeMillis() - startTime);
            
        } catch (IOException ex) {
            Logger.getLogger(HackerNewsAPIServlet.class.getName()).log(Level.SEVERE, null, ex);
            logger.logHNApiDown();
            return "Server Error: " + ex.getMessage();
        }
        
        // the JSON object of which the string representation to be returned
        JSONArray response = new JSONArray();
        
        // extract all story IDs
        JSONArray jsonArr = new JSONArray(topStories);
        for (int i = 0; i < jsonArr.length() && i < limit; i++) {
            String id = Long.toString(jsonArr.getLong(i));
            // and get title and author of each story
            Story story = getStory(id);
            response.put(new JSONObject()
                    .put("id", id)
                    .put("title", story.title)
                    .put("by", story.by)
                    .put("time", story.time)
            );
            
        }
        
        return response.toString();
        
    }
    
    /**
     * This returns a JSON string containing all the top story IDs as keys and
     * titles as values.
     * 
     * @param id
     * @return the JSON string in this format: {"id": "...", "title": "...",
     *         "by": "...", "url": "...", "time": ..., "comments": [
     *             {"id": "...", "text": "...", "by": "...", "time": ..., "comments": [...]}
     *         ]}
     */
    private String viewStory(String id) {
        // the JSON object of which the string representation to be returned
        JSONObject jsObj = new JSONObject();
        
        Story story = getStory(id);
        if (story != null) {
            jsObj.put("id", id);
            jsObj.put("title", story.title);
            jsObj.put("by", story.by);
            jsObj.put("time", story.time);
            if (story.url != null)
                jsObj.put("url", story.url);
            
            // add comments
            if (story.comments != null) {
                JSONArray comms = new JSONArray();
                // get an array of comment details for each comment id
                Comment[] comments = getComments(story.comments);
                for (Comment c : comments) {
                    if (c == null) continue;  // comment is marked deleted by the HackerNews API
                    JSONObject commObj = new JSONObject()
                            .put("id", c.id)
                            .put("text", c.text)
                            .put("by", c.by)
                            .put("time", c.time);
                    if (c.comments != null) {
                        // add subcomments
                        commObj.put("comments", new JSONArray(c.comments));
                    }
                    comms.put(commObj);
                }
                jsObj.put("comments", comms);
            }
            
            logger.logStoryQueried(id, story.title);
        }
        
        return jsObj.toString();
        
    }
    
    /**
     * Returns the story details given the story ID.
     * 
     * @param id Story ID
     * @return the Story object representing the story details
     */
    private Story getStory(String id) {
        Story story = null;
        try {
            long startTime = System.currentTimeMillis();
            String response = fetch(itemBaseURL + id + ".json");
            logger.logTimeUsed2GetStory(System.currentTimeMillis() - startTime);
            JSONObject jsObj = new JSONObject(response);
            
            // extract comment IDs
            String[] comments = null;
            try {
                JSONArray jsComms = jsObj.getJSONArray("kids");
                comments = new String[jsComms.length()];
                for (int i = 0; i < comments.length; i++) {
                    comments[i] = Long.toString(jsComms.getLong(i));
                }
            } catch (JSONException e) {
                // no comments posted
            }
            
            String url = null;
            try {
                url = jsObj.getString("url");
            } catch (JSONException e) {
                // no url found
            }
            
            story = new Story(
                Long.toString(jsObj.getLong("id")),
                jsObj.getString("by"),
                jsObj.getString("title"),
                jsObj.getLong("time"),
                url,
                comments
            );
            
        } catch (IOException ex) {
            Logger.getLogger(HackerNewsAPIServlet.class.getName()).log(Level.SEVERE, null, ex);
            logger.logHNApiDown();
        }
        
        return story;
    }
    
    /**
     * Gets all the comment contents for a list of comment IDs.
     * 
     * @param ids an array of comment IDs
     * @return an array of comment objects representing the details
     */
    private Comment[] getComments(String[] ids) {
        Comment[] comments = new Comment[ids.length];
        for (int i = 0; i < comments.length; i++) {
            try {
                String raw = fetch(itemBaseURL + ids[i] + ".json");
                JSONObject jsObj = new JSONObject(raw);
                
                // extract subcomment IDs
                String[] subComms = null;
                try {
                    JSONArray jsComms = jsObj.getJSONArray("kids");
                    subComms = new String[jsComms.length()];
                    for (int j = 0; j < subComms.length; j++) {
                        subComms[j] = Long.toString(jsComms.getLong(j));
                    }
                } catch (JSONException e) {
                    // no subcomments posted
                }
                
                try {
                    comments[i] = new Comment(
                        Long.toString(jsObj.getLong("id")),
                        jsObj.getString("by"),
                        Long.toString(jsObj.getLong("parent")),
                        jsObj.getLong("time"),
                        jsObj.getString("text"),
                        subComms
                    );
                } catch (JSONException e) {
                    // if some essential attributes such as "by" and "text"
                    // is not found, it could be that the comment has been
                    // deleted; here's one comment ID as an example: 15664070
                }
                
            } catch (IOException ex) {
                Logger.getLogger(HackerNewsAPIServlet.class.getName()).log(Level.SEVERE, null, ex);
                logger.logHNApiDown();
                return null;
            }
        }
        // sort comments in reverse chronological order (latest comes first) and
        // nulls put to last of the array
        Arrays.sort(comments, Comparator.nullsLast(Comparator.reverseOrder()));
        return comments;
    }
    
    
    /**
     * Makes an HTTP request to a given URL and returns raw response data
     * from the HTTP GET.
     * 
     * @param urlString URL from which the data are fetched
     * @return raw response data from the HTTP GET
     * @throws IOException error fetching content from site; either the site is
     *                     down, or something's going wrong with our server
     */
    private static String fetch(String urlString) throws IOException {
        StringBuilder response = new StringBuilder();
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        
        try (
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))
                ) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    
    // a helper class representing the data structure of a HackerNews story
    private class Story {
        public String id;  // the story's unique ID
        public String by;  // username of the story's author
        public String title;  // title of the story
        public long time;  // creation date of the story, in Unix Time
        public String url;  // URL of the story
        public String[] comments;  // IDs of the story's comments, in ranked display order

        
        public Story(String id, String by, String title, long time, String url, String[] comments) {
            this.id = id;
            this.by = by;
            this.title = title;
            this.time = time;
            this.url = url;
            this.comments = comments;
        }
        
    }
    
    // a helper class representing the data structure of a HackerNews comment
    private class Comment implements Comparable<Comment> {
        public String id;  // the comment's unique id
        public String by;  // username of the comment's comment
        public String parent;  // parent content to which this comment belongs
        public long time;  // creation date of the comment, in Unix Time
        public String text;  // main comment content
        public String[] comments;  // IDs of the subcomments, in ranked display order

        public Comment(String id, String by, String parent, long time, String text, String[] comments) {
            this.id = id;
            this.by = by;
            this.parent = parent;
            this.time = time;
            this.text = text;
            this.comments = comments;
        }

        @Override
        public int compareTo(Comment o) {
            return (int)(this.time - o.time);
        }
        
    }
    
    // ---- Controller ------------------------------------------------------------

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");
        
        // get user query from URL
        String[] query;
        try {
            // note that user query starts with a "/" character
            query = request.getPathInfo().substring(1).toLowerCase().split("/");
        } catch (Exception e) {
            response.setStatus(404);
            return;
        }
        
//        if (!request.getHeader("Accept").contains("text/plain") ||
//                !request.getHeader("Accept").contains("application/json")) {
//            response.setStatus(415);  // unsupported media type
//            return;
//        }
        
        // redirect request to respective methods
        String resp = null;
        switch (query[0]) {
            case "top-stories":
                System.out.println("Received top stories query.");
                resp = getTopStories(20);
                break;
                
            case "story":
                try {
                    System.out.println("Received story query for " + query[1]);
                    resp = viewStory(query[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatus(400);  // Bad request: no / wrong id supplied
                }
                break;
                
            default:
                logger.logWrongGetReqest("Invalid URL path: " + request.getPathInfo());
                response.setStatus(404);
                return;
        }
        
        if (resp != null) {
            response.setStatus(200);
            // write the response back to the client
            try (
                PrintWriter out = response.getWriter()
                ) {
                out.println(resp);
            }
        } else {
            // malformed request
            logger.logWrongGetReqest("Malformed request.");
            response.setStatus(400);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(405);  // method not allowed
        logger.logWrongPostReqest();
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Hacker News API Servlet.";
    }

}
