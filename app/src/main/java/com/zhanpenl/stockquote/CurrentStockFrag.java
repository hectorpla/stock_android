package com.zhanpenl.stockquote;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hectorlueng on 11/17/17.
 */

public class CurrentStockFrag extends Fragment {
    private StockActivity stockActivity;
    private ListView infoTabView;
    private TextView errorText;
    private String symbol;


    class InfoAdapter extends BaseAdapter {
        private List<String[]> infoPairs;

        InfoAdapter(List<String[]> infoList) {
            infoPairs = infoList;
        }

        @Override
        public int getCount() {
            return infoPairs.size();
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
            view = getLayoutInflater().inflate(R.layout.inforowlayout, null);
            TextView fieldNameView = (TextView) view.findViewById(R.id.form_head);
            TextView fieldDataView = (TextView) view.findViewById(R.id.form_data);
            ImageView arrowView = (ImageView) view.findViewById(R.id.image_form_arrow);

            fieldNameView.setText(infoPairs.get(i)[0]);
            fieldDataView.setText(infoPairs.get(i)[1]);
            // TODO: add image

            return view;
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.current_stock_frag, container, false);

        stockActivity = (StockActivity) getActivity();
        symbol = stockActivity.symbol;

        infoTabView = (ListView) view.findViewById(R.id.info_tab);
        errorText = (TextView) view.findViewById(R.id.errmsg_current);

        loadInfotable();

        return view;
    }

    private void loadInfotable() {
        String url = "http://zhpnl-web571.us-west-1.elasticbeanstalk.com/stockQuote.php?symbol=" + symbol;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String[] fields = {"Stock Ticker", "Last Price", "Change", "TimeStamp",
                            "Open", "prevPrice", "Day's Range", "Volume"};
                        List<String[]> rows = new ArrayList<>();

                        try {
                            for (String field : fields) {
                                String data = response.getString(field);
                                rows.add(new String[] {field, data});
                            }
                            // TODO: get series data

                        }
                        catch (JSONException e) {
                            errorText.setText(e.toString());
                            return;
                        }

                        InfoAdapter infoAdapter = new InfoAdapter(rows);
                        infoTabView.setAdapter(infoAdapter);
                        Log.d("current", "onResponse: " + rows);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        errorText.setText("can't load data");
                    }
                });
                stockActivity.requestQueue.add(request);
    }
}
