package com.zhanpenl.stockquote;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hectorlueng on 11/17/17.
 */

public class CurrentStockFrag extends Fragment {
    private StockActivity stockActivity;
    private ListView infoTabView;
    private TextView errorText;
    private ProgressBar progressBar;
    private WebView webView;
    private ScrollView scrollView;

    private String symbol;
    private String indicator = "Price";

    private final String[] indicators = {"SMA", "EMA", "STOCH", "RSI", "ADX", "CCI",
            "BBANDS", "MACD"};

    private JSONObject sharePlotObject;


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
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = getLayoutInflater().inflate(R.layout.inforowlayout, null);
            TextView fieldNameView = (TextView) view.findViewById(R.id.form_head);
            TextView fieldDataView = (TextView) view.findViewById(R.id.form_data);
            ImageView arrowView = (ImageView) view.findViewById(R.id.image_form_arrow);
            String[] pair = infoPairs.get(i);

            fieldNameView.setText(pair[0]);
            fieldDataView.setText(pair[1]);
            if (pair[0].equals("Change")) {
                if (Float.parseFloat(pair[1].split(" ")[0]) >= 0) {
                    arrowView.setImageResource(R.drawable.up);
                }
                else {
                    arrowView.setImageResource(R.drawable.down);
                }
            }
            else { arrowView.setVisibility(view.GONE); }
            return view;
        }
    }

    class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) { mContext = c; }

        @JavascriptInterface
        public String getSymbol() {
            return CurrentStockFrag.this.symbol;
        }

        @JavascriptInterface
        public String getIndicator() {
            return CurrentStockFrag.this.indicator;
        }

        @JavascriptInterface
        public String getPricePlotObject() {
            return CurrentStockFrag.this.stockActivity.getPricePlotObject().toString();
        }

        @JavascriptInterface
        public void setPricePlotObject(String objString) {
            if (objString == null || objString.equals("null")) { return; }
            JSONParser parser = new JSONParser();
            try {
                JSONObject obj = (JSONObject) parser.parse(objString);
                CurrentStockFrag.this.stockActivity.setPricePlotObject(obj);
            }
            catch (ParseException e) {
                Log.d("Error", "setPricePlotObject: parse error");
                return;
            }
        }

        @JavascriptInterface
        public void setSharePlotObject(String objString) {
            JSONParser parser = new JSONParser();

            try {
                // not sure if it works
                CurrentStockFrag.this.sharePlotObject = (JSONObject) parser.parse(objString);
            }
            catch (ParseException e) {
                Log.d("Error", "setSharePlotObject: wrong json format");
            }
        }

        @JavascriptInterface
        public String getSharePlotObject(String indicator) {
            JSONObject obj = CurrentStockFrag.this.stockActivity.getIndPlotObject(indicator);
            return obj.toString();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        stockActivity = (StockActivity) getActivity();
        symbol = stockActivity.symbol;

        View view = inflater.inflate(R.layout.current_stock_frag, container, false);
        scrollView = (ScrollView) view.findViewById(R.id.scroll_current);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar_current);
        infoTabView = (ListView) view.findViewById(R.id.info_tab);
        webView = (WebView) view.findViewById(R.id.webview_indicator);
        errorText = (TextView) view.findViewById(R.id.errMsg_current);

//        scrollView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        // load table and chart
        loadInfoTable();

        initWebView();

        return view;
    }

    private void loadInfoTable() {
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
                        progressBar.setVisibility(View.GONE);
//                        scrollView.setVisibility(View.VISIBLE);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        errorText.setText("can't load data");
                        progressBar.setVisibility(View.GONE);
                        errorText.setVisibility(View.VISIBLE);
                    }
                });
        stockActivity.requestQueue.add(request);
    }

    private void initWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }
}
