package com.zhanpenl.stockquote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hectorlueng on 11/17/17.
 */

public class NewsFeedFrag extends Fragment {
    private StockActivity stockActvity;
    private ListView newsListView;
    private TextView errorView;
    private String symbol;

    class NewsAdapter extends BaseAdapter {
        List<String[]> newsList;

        NewsAdapter(List<String[]> list) {
            newsList = list;
        }


        @Override
        public int getCount() {
            return newsList.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.newsrowlayout, null);
            }
            TextView titleView = (TextView) view.findViewById(R.id.news_title);
            TextView authorView = (TextView) view.findViewById(R.id.news_author);
            TextView dateView = (TextView) view.findViewById(R.id.news_date);
            final String[] news = newsList.get(i); // final makes it accessable from callback

            titleView.setText(news[0]);
            authorView.setText(getResources().getString(R.string.newsAuthor, news[2]));
            dateView.setText(getResources().getString(R.string.newsDate, news[3]));

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browseIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(news[1]));
                    startActivity(browseIntent);
                }
            });
            return view;
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_news, container, false);

        stockActvity = (StockActivity) getActivity();
        symbol = stockActvity.symbol;

        newsListView = (ListView) view.findViewById(R.id.news_list);
        errorView = (TextView) view.findViewById(R.id.errMsg_news);

        // load news
        loadNews();

        Log.d("NEWS", "NEWS onCreateView: ++++++++++++++++++++++++++++++++++++++");
        return view;
    }

    private void loadNews() {
        String url = "http://zhpnl-web571.us-west-1.elasticbeanstalk.com/newsfeed.php?symbol=" + symbol;
        Log.d("NEWS", "loadNews: " + url);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        List<String[]> newsArray = new ArrayList<>();
                        String[] fields = {"title", "link", "author", "pubDate"};

//                        Log.d("NEWS", "onResponse: " + response.toString());
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                String[] news = new String[4];

                                for (int j = 0; j < fields.length; j++) {
                                    news[j] = obj.getString(fields[j]);
                                }
                                newsArray.add(news);
                            }
                        }
                        catch (JSONException e) {
                            errorView.setText(e.toString());
                        }
                        if (newsArray.size() == 0) {
                            errorView.setText(getResources().getString(R.string.cantLoadMsg));
                            return;
                        }
                        NewsAdapter newsAdapter = new NewsAdapter(newsArray);
                        newsListView.setAdapter(newsAdapter);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        errorView.setText(getResources().getString(R.string.cantLoadMsg));
                    }
                }
        );
        stockActvity.requestQueue.add(request);
    }
}
