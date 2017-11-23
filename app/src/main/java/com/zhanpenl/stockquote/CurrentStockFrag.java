package com.zhanpenl.stockquote;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hectorlueng on 11/17/17.
 */

public class CurrentStockFrag extends Fragment {
    private StockActivity stockActivity;
    private ListView infoTabView;
    private TextView errorView;
    private ProgressBar progressBar;

    private ImageView favToggle;
    private ImageView facebookShare;

    private SharedPreferences sharedPref;

    private String symbol;
    private String indicator = "Price";
    private String indicatorOnSpinner = "Price";
    private String showedIndicator = "Price";

    private String[] indicators;

    private JSONObject shareExportObject;


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
                        indicatorOnSpinner = indicators[i];
                        Log.d("CURR_STOCK", "onItemSelected: changed to " + indicatorOnSpinner
                            + ", showed Indicator is " + showedIndicator + ", they are equal? "
                            + indicatorOnSpinner.equals(showedIndicator));
                        changeButtonView.setEnabled(true);
                        if (showedIndicator.equals(indicatorOnSpinner)) {
                            changeButtonView.setEnabled(false);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // do nothing
                    }
                });
                changeButtonView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO: loadURL, ansync, on success do the next line
                        Log.d("CURR_STOCK", "changeButtonView onClick: should reload page");
                        indicator = indicatorOnSpinner;
                        notifyDataSetChanged();
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
            else { arrowView.setVisibility(View.GONE); }

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
                    "CurrentStock");
        }

        // change indicator
        private void getIndicatorInfo(String indicator) {
            String url =
                    "http://zhpnl-web571.us-west-1.elasticbeanstalk.com/indicatorQuery.php?symbol="
                    + symbol + "?indicator=" + indicator;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }
            );

            stockActivity.requestQueue.add(request);
        }
    }

    class WebAppInterface {
        Context mContext;

        public WebAppInterface(Context c) { mContext = c; }

        @JavascriptInterface
        public String getSymbol() {
            return symbol;
        }

        @JavascriptInterface
        public String getIndicator() {
            return indicator;
        }

        @JavascriptInterface
        public void setShowedIndicator(String ind) { showedIndicator = ind; }

        @JavascriptInterface
        public String getPricePlotObject() {
            return stockActivity.getPricePlotObject().toString();
        }

        @JavascriptInterface
        public String getExportObject(String indicator) {
            JSONObject obj = stockActivity.getExportObject(indicator);
            if (obj == null) {  return "null"; }
            return obj.toString();
        }

        @JavascriptInterface
        public void putExportObject(String indicator, String objString) {
            Log.d("CURR_STOCK", "putExportObject: " + objString);
            JSONObject obj = null;
            try {
                obj = (JSONObject) new JSONTokener(objString).nextValue();
            }
            catch (JSONException e) {
                Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT).show();
                return;
            }
            stockActivity.putExportObject(indicator, obj);
            // should put here?
//            shareExportObject = obj;
        }

        @JavascriptInterface
        public void showToastMessage(String msg) {
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        stockActivity = (StockActivity) getActivity();
        symbol = stockActivity.symbol;

        View view = inflater.inflate(R.layout.frag_current_stock, container, false);
        progressBar = view.findViewById(R.id.progressBar_current);
        infoTabView = view.findViewById(R.id.info_tab);
        errorView = view.findViewById(R.id.errMsg_current);

        favToggle = view.findViewById(R.id.toggle_fav);
        facebookShare = view.findViewById(R.id.btn_share);

        // styling
        progressBar.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        sharedPref = getActivity().getSharedPreferences(getString(R.string.sharePrefKey),
                Context.MODE_PRIVATE);
        if (sharedPref.contains(symbol)) {
            favToggle.setImageResource(R.drawable.filled);
        }

        setListeners();

        // load table and chart
        loadInfoTable();

        return view;
    }

    private void setListeners() {
        favToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPref.edit();
                if (sharedPref.contains(symbol)) {
                    editor.remove(symbol);
                    Log.d("FavToggle", "onClick: removed " + symbol);
                    favToggle.setImageResource(R.drawable.empty);
                }
                else {
                    JSONObject infoObj = stockActivity.getPricePlotObject();
                    if (infoObj != null) {
                        JSONObject storedObj = new JSONObject();

                        try {
                            storedObj.put("symbol", infoObj.getString("Stock Ticker"));
                            storedObj.put("price", infoObj.getDouble("Last Price"));
                            storedObj.put("change", infoObj.getString(("Change")));
                            storedObj.put("orderTag", sharedPref.getAll().size());
                            editor.putString(symbol, storedObj.toString());
                            favToggle.setImageResource(R.drawable.filled);
                            Log.d("FAV_LIST", "add item: " + storedObj.toString());
                        }
                        catch (JSONException e) {
                            Log.d("FAV_LIST_ERR", "add item: " + e.toString());
                        }
                    }
                }
            }
        });
    }

    private void loadInfoTable() {
        String url = "http://zhpnl-web571.us-west-1.elasticbeanstalk.com/stockQuote.php?symbol=" + symbol;
        Log.d("CURRENT", "loadInfoTable: " + url);

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
                            errorView.setText(e.toString());
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
                        errorView.setText(getResources().getString(R.string.cantLoadMsg));
                        progressBar.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);
                    }
                });
        stockActivity.requestQueue.add(request);
    }
}
