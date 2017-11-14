package hackernews.api.client.hackernewsapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class TopStoriesActivity extends AppCompatActivity {

    // an array adapter that's used to populate data to the ListView
    private ArrayAdapter<Story> listAdapter;
    // data source of the ArrayAdapter
    private ArrayList<Story> items;
    // used to implement pull-to-refresh functionality
    SwipeRefreshLayout swipeContainer;
    // the loading indicator on top of list
    private ProgressBar loadingIndicator;
    private boolean isProgressBarActive;


    // this is called when the activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_stories);  // sets the layout this activity uses

        // find the list view on the interface
        ListView topStoriesList = findViewById(R.id.list_view);

        // initialize the array adapter
        items = new ArrayList<>();  // this will be the data source of the ListView
        // simple_list_item_2 contains two text views; see source code for more:
        // https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/res/res/layout/simple_list_item_2.xml
        listAdapter = new ArrayAdapter<Story>(this, android.R.layout.simple_list_item_2, android.R.id.text1, items) {
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

                text1.setText(items.get(position).getTitle());
                text2.setText(items.get(position).getBy());  // author of the story

                return view;
            }
        };

        // load data from the remote API using AsyncTask, and show the loading progress bar
        // Note: the progress bar will only be shown once; then it's replaced by SwipeRefresh
        loadingIndicator = findViewById(R.id.progress_bar);
        loadingIndicator.setVisibility(View.VISIBLE);
        isProgressBarActive = true;
        new UpdateTopStories().execute();

        // connect adapter to the ListView
        topStoriesList.setAdapter(listAdapter);

        // allows clicking on items of the list
        topStoriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View itemView, int position, long id) {
                // use explicit intent to open the story activity page
                Intent intent = new Intent(TopStoriesActivity.this, StoryActivity.class);
                // pass the story id to the next activity
                intent.putExtra("id", items.get(position).getId());
                startActivity(intent);
            }
        });

        // finally, sets up pull-to-refresh functionality to update content
        swipeContainer = findViewById(R.id.swipe_refresh);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new UpdateTopStories().execute();
            }
        });
    }


    // inner class to update the top stories list asynchronously (i.e. separate from the main thread
    // that also processes user interface)
    private class UpdateTopStories extends AsyncTask<Void, Void, ArrayList<Story>> {

        // URL to the top stories API
        private final String TOP_STORIES_API = API_URL.TOP_STORIES.toString();

        /**
         * Tasks to be executed in separate thread from the main interface; this queries the remote
         * API and parse the feedback.
         *
         * @param voids no parameters required
         * @return the result to be returned to the main thread
         */
        @Override
        protected ArrayList<Story> doInBackground(Void... voids) {
            StringBuilder jsonStr = new StringBuilder();
            try {  // read from remote API; must enable internet access permission in Manifest!
                URL url = new URL(TOP_STORIES_API);
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
            return gson.fromJson(jsonStr.toString(), new TypeToken<ArrayList<Story>>(){}.getType());
        }

        /**
         * Tasks to be executed after this async task is done; this passes the result back to the
         * main thread.
         */
        @Override
        protected void onPostExecute(ArrayList<Story> topStories) {
            // hide the loading indicator if it's present
            if (isProgressBarActive) {
                loadingIndicator.setVisibility(View.GONE);  // GONE will also remove the empty space left behind
                isProgressBarActive = false;
            }
            if (swipeContainer != null && swipeContainer.isRefreshing()) {
                swipeContainer.setRefreshing(false);
            }
            // load stories into the array adapter
            items.clear();
            items.addAll(topStories);
            listAdapter.notifyDataSetChanged();
        }

    }

}
