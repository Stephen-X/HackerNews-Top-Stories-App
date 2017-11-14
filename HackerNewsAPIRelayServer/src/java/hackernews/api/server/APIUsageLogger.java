package hackernews.api.server;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.bson.Document;

/**
 * This provides an interface to log API usage to the remote MongoDB database.
 * <p>
 * Logged data:
 * <ol>
 *     <li>Time used to get top story IDs from the Hacker News API.</li>
 *     <li>Time used to get information of individual story from the Hacker News API.</li>
 *     <li>ID of the story of which detailed information is queried.</li>
 *     <li>Date and time the Hacker News API service is found down when this server issues request.</li>
 *     <li>Date and time a user makes a <code>GET</code> request to the wrong resource.</li>
 *     <li>Date and time a user makes a <code>POST</code> request to the server.</li>
 * </ol>
 * <p>
 * References:
 * <ul>
 *     <li>MongoDB Driver sample code: https://blog.mlab.com/2011/11/ample-mongodb-examples/</li>
 *     <li>MongoDB Java Driver Documentation: http://api.mongodb.com/java/current/</li>
 *     <li>MongoDB manual on aggregation: https://docs.mongodb.com/manual/aggregation/</li>
 *     <li>MongoDB Java driver on read operation: http://mongodb.github.io/mongo-java-driver/3.4/driver/tutorials/perform-read-operations/</li>
 *     <li>MongoDB Java driver on aggregation: http://mongodb.github.io/mongo-java-driver/3.4/driver/tutorials/aggregation/</li>
 * </ul>
 * 
 * @author Stephen Xie &lt;[redacted]@andrew.cmu.edu&gt;
 */
public class APIUsageLogger {
    
    // singleton pattern
    private static APIUsageLogger instance = null;
    public static APIUsageLogger getInstance() {
        if (instance == null)
            instance = new APIUsageLogger();
        return instance;
    }
    
    // URI of the remote mongodb database service by mLab
    private static String dbURI = "mongodb://***:***@***.mlab.com:***/heroku_***";
    
    private MongoDatabase db;
    
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    
    public APIUsageLogger() {
        // connect to the remote MongoDB database
        MongoClientURI mongoUri = new MongoClientURI(dbURI);
        MongoClient mongoClient = new MongoClient(mongoUri);
        // get the database
        db = mongoClient.getDatabase(mongoUri.getDatabase());
    }
    
    // --- loggers -------------------------------------------------------
    
    /**
     * Logs time used to get the top stories list from the HN API.
     * 
     * @param time time in milliseconds
     */
    public void logTimeUsed2GetTopStories(long time) {
        // access the HNApiQueryLatency collection, or create one if not exists
        MongoCollection<Document> collection = db.getCollection("HNApiQueryLatency");
        // create the record document
        Document record = new Document("type", "getTopStories").append("time", time);
        // then push this record to database
        collection.insertOne(record);
    }
    
    /**
     * Logs time used to get the story details from the HN API.
     * 
     * @param time time in milliseconds
     */
    public void logTimeUsed2GetStory(long time) {
        // access the HNApiQueryLatency collection, or create one if not exists
        MongoCollection<Document> collection = db.getCollection("HNApiQueryLatency");
        // create the record document
        Document record = new Document("type", "getStory").append("time", time);
        // then push this record to database
        collection.insertOne(record);
    }
    
    /**
     * Logs story queried.
     * 
     * @param id ID of the story queried
     * @param title title of the story queried
     */
    public void logStoryQueried(String id, String title) {
        // access the StoryQueried collection, or create one if not exists
        MongoCollection<Document> collection = db.getCollection("StoryQueried");
        // create the record document
        Document record = new Document("id", id)
                .append("title", title)
                .append("date", Instant.now().getEpochSecond());
                // also logs the time the log is made in Unix time (long type)
        // then push this record to database
        collection.insertOne(record);
    }
    
    /**
     * Logs the time this server finds the HackerNews API service is down.
     * <p>
     * Note: time logged to the database will be a long type integer following
     * Unix time format.
     */
    public void logHNApiDown() {
        // access the HNApiDownTime collection, or create one if not exists
        MongoCollection<Document> collection = db.getCollection("HNApiDownTime");
        // create the record document
        Document record = new Document("date", Instant.now().getEpochSecond());
                // also logs the time the log is made in Unix time (long type)
        // then push this record to database
        collection.insertOne(record);
    }
    
    /**
     * Logs the time and info of a faulty <code>GET</code> request.
     * 
     * @param info information of the request
     */
    public void logWrongGetReqest(String info) {
        // access the MalformedRequest collection, or create one if not exists
        MongoCollection<Document> collection = db.getCollection("MalformedRequest");
        // create the record document
        Document record = new Document("type", "GET")
                .append("info", info)
                .append("date", Instant.now().getEpochSecond());
                // logs the time the log is made in Unix time (long type)
        // then push this record to database
        collection.insertOne(record);
    }
    
    /**
     * Logs the time of a faulty <code>POST</code> request.
     */
    public void logWrongPostReqest() {
        // access the MalformedRequest collection, or create one if not exists
        MongoCollection<Document> collection = db.getCollection("MalformedRequest");
        // create the record document
        Document record = new Document("type", "POST")
                .append("date", Instant.now().getEpochSecond());
                // logs the time the log is made in Unix time (long type)
        // then push this record to database
        collection.insertOne(record);
    }
    
    // --- analytics ------------------------------------------------------
    
    /**
     * Gets the average latency of making queries to the HackerNews API.
     * 
     * @return latency in seconds, or -1 if no record is found
     */
    public double getAverageQueryLatency() {
        MongoCollection<Document> collection = db.getCollection("HNApiQueryLatency");
        
        long totalTime = 0;
        long recordCount = collection.count();
        if (recordCount < 1) return -1;  // no record found
        
        // iterate through all documents in the collection, and calculate time
        // accordingly
        try (
                MongoCursor<Document> cursor = collection.find().iterator()
                ) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                totalTime += doc.getLong("time");
            }
        }
        
        return (totalTime / (double) recordCount) / 1000;  // convert to seconds
    }
    
    
    /**
     * Gets the ID and the title of the most queried story.
     * 
     * @return an array in which index 0 stores ID, index 1 stores title, index 2
     *         stores number of times; or null if no record found
     */
    public String[] getMostQueriedStory() {
        MongoCollection<Document> collection = db.getCollection("StoryQueried");
        
        if (collection.count() < 1)
            return null;  // no record found
        
        // count the number of records for each story ID, then sort them in
        // descending order according to the count result; the first result
        // contains the most queried story info
        Document mostQueried = collection.aggregate(
                Arrays.asList(
                        Aggregates.group("$id", Accumulators.sum("count", 1)),
                        Aggregates.sort(Sorts.descending("count")),
                        // we now get {"_id", "count"}, with "_id" being the original
                        // "id" field
                        Aggregates.project(Projections.fields(
                                Projections.excludeId(),
                                // don't include "_id" in final result
                                Projections.computed("id", "$_id"),
                                // make a new "id" field; it has the same value
                                // as "_id" (the new "id" is "computed from" "_id")
                                Projections.include("count")
                                // also include "count" field; final result:
                                // a series of {"id", "count"}
                        ))
                )
        ).first();
        String[] result = new String[3];
        result[0] = mostQueried.getString("id");
        result[2] = Integer.toString(mostQueried.getInteger("count"));
        // now query title using id; I couldn't figure out a way to somehow
        // include title in the above aggregation
        result[1] = collection.find(eq("id", result[0])).first().getString("title");
        return result;
    }
    
    
     /**
     * Gets how frequent the HackerNews API service goes down.
     * 
     * @return average number of down times per month
     */
    public double getHNApiDownFreq() {
        MongoCollection<Document> collection = db.getCollection("HNApiDownTime");
        
        // get the total number of down records
        long total = collection.count();
        if (total < 1) return 0;
        // find the earlist record
        Document earliest = collection.find().sort(Sorts.ascending("date")).first();
        // how many months passed since then?
        LocalDate earliestDate = LocalDate.ofEpochDay(earliest.getLong("date"));
        Period period = Period.between(earliestDate, LocalDate.now());
        int monthsBetween = period.getMonths();
        
        if (monthsBetween < 1)  // less than a month will count as one month
            return total;
        
        return total / (double) monthsBetween;
        
    }
    
    // --- logs -------------------------------------------
    
    /**
     * Returns all records in the HNApiQueryLatency collection.
     * 
     * @return results as a list of arrays of size 2 each: item at index 0 being
     *         type, 1 being time
     */
    public ArrayList<String[]> getAllHNAPIQueryLatency() {
        MongoCollection<Document> collection = db.getCollection("HNApiQueryLatency");
        if (collection.count() < 1) return null;  // collection is empty
        
        ArrayList<String[]> result = new ArrayList<>();
        collection.find().forEach(new Block<Document>() {
            @Override
            public void apply(final Document doc) {
                String[] record = new String[2];
                record[0] = doc.getString("type");
                record[1] = Long.toString(doc.getLong("time"));
                result.add(record);
            }
        });
        return result;
    }
    
    /**
     * Returns all records in the StoryQueried collection.
     * 
     * @return results as a list of arrays of size 3 each: item at index 0 being
     *         id, 1 being title, 2 being date
     */
    public ArrayList<String[]> getAllStoryQueried() {
        MongoCollection<Document> collection = db.getCollection("StoryQueried");
        if (collection.count() < 1) return null;  // collection is empty
        
        ArrayList<String[]> result = new ArrayList<>();
        collection.find().forEach(new Block<Document>() {
            @Override
            public void apply(final Document doc) {
                String[] record = new String[3];
                record[0] = doc.getString("id");
                record[1] = doc.getString("title");
                Date date = new Date(doc.getLong("date") * 1000);  // convert unix time to date
                record[2] = dateFormatter.format(date);
                result.add(record);
            }
        });
        return result;
    }
    
    /**
     * Returns all records in the HNApiDownTime collection.
     * 
     * @return date result as a list
     */
    public ArrayList<String> getAllHNApiDownTime() {
        MongoCollection<Document> collection = db.getCollection("HNApiDownTime");
        if (collection.count() < 1) return null;  // collection is empty
        
        ArrayList<String> result = new ArrayList<>();
        collection.find().forEach(new Block<Document>() {
            @Override
            public void apply(final Document doc) {
                Date date = new Date(doc.getLong("date") * 1000);
                result.add(dateFormatter.format(date));
            }
        });
        return result;
    }
    
    /**
     * Returns all records in the MalformedRequest collection.
     * 
     * @return results as a list of arrays of size 3 each: item at index 0 being
     *         type, 1 being date, 2 being info (could be N/A)
     */
    public ArrayList<String[]> getAllMalformedRequest() {
        MongoCollection<Document> collection = db.getCollection("MalformedRequest");
        if (collection.count() < 1) return null;  // collection is empty
        
        ArrayList<String[]> result = new ArrayList<>();
        collection.find().forEach(new Block<Document>() {
            @Override
            public void apply(final Document doc) {
                String[] record = new String[3];
                record[0] = doc.getString("type");
                Date date = new Date(doc.getLong("date") * 1000);
                record[1] = dateFormatter.format(date);
                String info = doc.getString("info");
                record[2] = (info != null) ? info : "N/A";
                result.add(record);
            }
        });
        return result;
    }
   
}
