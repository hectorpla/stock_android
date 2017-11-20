package com.zhanpenl.stockquote;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
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

    private String symbol;
    private String indicator = "Price";
    private String showedIndicator = "Price";

    private String[] indicators;

    private JSONObject sharePlotObject;


    class InfoAdapter extends BaseAdapter {
        private List<String[]> infoPairs;
        private final int TYPE_TABROW = 0;
        private final int TYPE_CHART = 1;

        private Spinner spinner;
        private TextView changeButtonView;
        private WebView webView;

        InfoAdapter(List<String[]> infoList) {
            infoPairs = infoList;
        }

        @Override
        public int getCount() {
            return infoPairs.size() + 1;
        }

        @Override
        public Object getItem(int i) {
            return infoPairs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position >= infoPairs.size() ? TYPE_CHART : TYPE_TABROW;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);

            if (type == TYPE_CHART) {
                indicators = getResources().getStringArray(R.array.indicator_names);

                view = getLayoutInflater().inflate(R.layout.indicatorchartlayout, null);
                spinner = view.findViewById(R.id.spinner_indicators);
                changeButtonView = view.findViewById(R.id.btn_changeIndicator);
                webView = view.findViewById(R.id.webview_indicator);

                ArrayAdapter<String> indicatorAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_dropdown_item_1line, indicators);
                spinner.setAdapter(indicatorAdapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        indicator = indicators[i];
                        Log.d(getResources().getString(R.string.curTag), "onItemSelected: " +
                            indicator);
                        changeButtonView.setEnabled(indicator != showedIndicator);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // do nothing
                    }
                });
                changeButtonView.setEnabled(false);
                changeButtonView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO: loadURL, ansync, on success do the next line
                        showedIndicator = indicator;
                        changeButtonView.setEnabled(false);
                    }
                });

                initWebView();
                webView.loadUrl("file:///android_asset/indicator.html");
                return view;
            }

            // table row case
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.inforowlayout, null);
            }
            TextView fieldNameView = view.findViewById(R.id.form_head);
            TextView fieldDataView = view.findViewById(R.id.form_data);
            ImageView arrowView = view.findViewById(R.id.image_form_arrow);
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

            // styling
            fieldNameView.setTypeface(Typeface.DEFAULT_BOLD);
            return view;
        }

        private void initWebView() {
            webView.setWebViewClient(new WebViewClient());
            WebSettings webSettings = webView.getSettings();
            webSettings.setDomStorageEnabled(true);
            webSettings.setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new WebAppInterface(CurrentStockFrag.this.stockActivity),
                    "Android");
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
        progressBar = view.findViewById(R.id.progressBar_current);
        infoTabView = view.findViewById(R.id.info_tab);
        errorText = view.findViewById(R.id.errMsg_current);

        progressBar.setVisibility(View.VISIBLE);

        // load table and chart
        loadInfoTable();

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

                        // set the obj obtained from the back-end
                        stockActivity.setPricePlotObject(response);
                        try {
                            for (String field : fields) {
                                String data = response.getString(field);
                                rows.add(new String[] {field, data});
                            }
                        }
                        catch (JSONException e) {
                            errorText.setText(e.toString());
                            return;
                        }

                        progressBar.setVisibility(View.GONE);

                        InfoAdapter infoAdapter = new InfoAdapter(rows);
                        infoTabView.setAdapter(infoAdapter);
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
}
