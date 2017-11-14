package hackernews.api.client.hackernewsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class StoryActivity extends AppCompatActivity {

    private ActionBar actionBar;
    private ProgressBar loadingIndicator;
    private Button webLinkBtn;
    private TextView commentNote;
    private ListView commentsField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        // adds an Up button for returning to home page to the app bar
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        loadingIndicator = findViewById(R.id.progress_bar);
        webLinkBtn = findViewById(R.id.web_link_btn);
        commentNote = findViewById(R.id.comments_note);
        commentsField = findViewById(R.id.comments);

        // get story ID from the main activity
        String storyID = getIntent().getStringExtra("id");
        // query story details and display result on screen
        new GetStory().execute(storyID);

    }


    private class GetStory extends AsyncTask<String, Void, Story> {

        // base URL to the story API
        private final String STORY_API_BASE = API_URL.STORY_BASE.toString();

        /**
         * Before entering the story page, hide all page elements except the loading
         * indicator.
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            webLinkBtn.setVisibility(View.GONE);
            commentNote.setVisibility(View.GONE);
            loadingIndicator.setVisibility(View.VISIBLE);
        }

        /**
         * Tasks to be executed in separate thread from the main interface; this queries the remote
         * API and parse the feedback.
         *
         * @param ids ids[0] is the ID of the story to be queried
         * @return the result to be returned to the main thread
         */
        @Override
        protected Story doInBackground(String... ids) {
            StringBuilder jsonStr = new StringBuilder();
            try {  // read from remote API; must enable internet access permission in Manifest!
                URL url = new URL(STORY_API_BASE + ids[0]);
                URLConnection conn = url.openConnection();
                try (
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(conn.getInputStream(), "UTF-8"))
                ) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        jsonStr.append(line);
                    }
                }
            } catch (IOException e) {
                Log.w("API Fetch Error", e.getMessage());
            }

            // parse to an array list of Story objects
            Gson gson = new Gson();
            return gson.fromJson(jsonStr.toString(), Story.class);
        }

        /**
         * Tasks to be executed after this async task is done; this passes the result back to the
         * main thread.
         */
        @Override
        protected void onPostExecute(final Story story) {
            // update activity title
            actionBar.setTitle(story.getTitle());
            // update button with link included in the story
            if (story.getUrl() != null) {
                webLinkBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // use implicit intent to tell Android to open web link in a browser
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(story.getUrl())));
                    }
                });
                webLinkBtn.setVisibility(View.VISIBLE);
            }
            // list top level comments in the list
            if (story.getComments() != null) {
                final Comment[] comments = story.getComments();
                ArrayAdapter<Comment> listAdapter = new ArrayAdapter<Comment>(StoryActivity.this,
                        android.R.layout.simple_list_item_2, android.R.id.text1, comments) {
                    // override the getView method to correctly map attributes of the Story object
                    // to their respective text views
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        // note: must supply the android.R.id.text1 parameter in ArrayAdapter, otherwise
                        // the call to super below will throw an exception
                        View view = super.getView(position, convertView, parent);
                        // text views from simple_list_item_2
                        TextView text1 = view.findViewById(android.R.id.text1);
                        TextView text2 = view.findViewById(android.R.id.text2);

                        Comment comment = comments[position];
                        text1.setText(comment.getBy() + " - " + comment.getTimeString());  // author of the story
                        text2.setText(comment.getText());

                        return view;
                    }
                };
                commentsField.setAdapter(listAdapter);
                commentNote.setVisibility(View.VISIBLE);
            }

            loadingIndicator.setVisibility(View.GONE);
        }
    }
}
