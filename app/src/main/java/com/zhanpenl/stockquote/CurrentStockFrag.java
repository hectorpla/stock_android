package com.zhanpenl.stockquote;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

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
    private int indicatorIndex = 0;
    private int indicatorIndexOnSpinner = 0;

    private String[] indicators;

    private JSONObject shareExportObject;


    class InfoAdapter extends BaseAdapter {
        private List<String[]> infoPairs;
        private final int TYPE_TABROW = 0;
        private final int TYPE_CHART = 1;

        private Spinner spinner;
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

                view = getActivity().getLayoutInflater().inflate(R.layout.indicatorchartlayout, null);
                spinner = (Spinner) view.findViewById(R.id.spinner_indicators);
                final TextView changeButtonView = (TextView) view.findViewById(R.id.btn_changeIndicator);
                webView = (WebView) view.findViewById(R.id.webview_indicator);

                final ArrayAdapter<String> indicatorAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_dropdown_item_1line, indicators);
                indicatorAdapter.setNotifyOnChange(false);
                spinner.setAdapter(indicatorAdapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        indicatorIndexOnSpinner = i;
                        Log.d("CURR_ChANGE_IND", "onItemSelected: on spinner -> " +
                                indicatorIndexOnSpinner + ", showed -> " + indicatorIndex);
                        changeButtonView.setEnabled(indicatorIndexOnSpinner != indicatorIndex);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // do nothing
                    }
                });
                // TODO: set previous selected category
                spinner.setSelection(indicatorIndex);

                changeButtonView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO: loadURL, ansync, on success do the next line
                        Log.d("CURR_STOCK", "changeButtonView onClick: should reload page");
                        indicatorIndex = indicatorIndexOnSpinner;
                        notifyDataSetChanged(); // key
                    }
                });

                initWebView();
                webView.loadUrl("file:///android_asset/indicator.html");
                return view;
            }

            // table row case
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.inforowlayout, null);
            }
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
            return indicators[indicatorIndex];
        }

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
            shareExportObject = obj;
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
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar_current);
        infoTabView = (ListView) view.findViewById(R.id.info_tab);
        errorView = (TextView) view.findViewById(R.id.errMsg_current);

        favToggle = (ImageView) view.findViewById(R.id.toggle_fav);
        facebookShare = (ImageView) view.findViewById(R.id.btn_share);

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
        // sharedPreferences
        favToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPref.edit();
                if (sharedPref.contains(symbol)) {
                    editor.remove(symbol);
                    editor.commit();
                    Log.d("FavToggle", "onClick: removed " + symbol);
                    favToggle.setImageResource(R.drawable.empty);
                }
                else {
                    JSONObject infoObj = stockActivity.getPricePlotObject();
                    if (infoObj != null) {
                        JSONObject storedObj = new JSONObject();

                        try {
                            int sz = sharedPref.getAll().size();
                            if (sz <= 1) {
                                editor.putInt("count", 0);
                                editor.commit();
                            }
                            int count = sharedPref.getInt("count", 0);

                            editor.putInt("count", count + 1);
                            storedObj.put("symbol", infoObj.getString("Stock Ticker"));
                            storedObj.put("price", infoObj.getDouble("Last Price"));
                            storedObj.put("change", infoObj.getString(("Change")));
                            storedObj.put("prevPrice", infoObj.getString("prevPrice"));
                            storedObj.put("orderTag", count);
                            editor.putString(symbol, storedObj.toString());
                            editor.commit(); // forgot to commit
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


        facebookShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String url = "http://export.highcharts.com/";
                final JSONObject data = new JSONObject();

                try {
                    data.put("async", true);
                    data.put("type", "jpeg");
                    data.put("options", shareExportObject);
                }
                catch (JSONException e) {
                    Log.d("CHART_EXPORT", "constructing json object error");
                }
                StringRequest chartExportRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                String imageURL = url + response;
                                ShareDialog shareDialog = new ShareDialog(getActivity());
                                ShareLinkContent content = new ShareLinkContent.Builder()
                                        .setContentUrl(Uri.parse(imageURL))
                                        .build();

                                Log.d("CHART_EXPORT", "onResponse: " + imageURL);
                                shareDialog.registerCallback(CallbackManager.Factory.create(),
                                        new FacebookCallback<Sharer.Result>() {
                                            @Override
                                            public void onSuccess(Sharer.Result result) {
                                                Toast.makeText(getActivity(), "shared successfully",
                                                        Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onCancel() {
                                                Toast.makeText(getActivity(), "sharing canceled",
                                                        Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onError(FacebookException error) {
                                                Toast.makeText(getActivity(),
                                                        "error happened",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                shareDialog.show(content);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("CHART_EXPORT", "onResponse: " + error.toString());
                            }
                        }) {
                        @Override
                        public byte[] getBody() {
                            Log.d("CHART_EXPORT", "getBody: " + data.toString());
                            try {
                                return data.toString().getBytes("utf-8");
                            }
                            catch (UnsupportedEncodingException e) {
                                Log.d("CHART_EXPORT", "getBody: " + e.toString());
                                return null;
                            }
                        }
                        @Override
                        public String getBodyContentType() {
                            return "application/json";
                        }
                };
                stockActivity.requestQueue.add(chartExportRequest);
            }
        });
    }

    private void loadInfoTable() {
        // http://zhpnl-web571.us-west-1.elasticbeanstalk.com
        // http://10.0.2.2/~hectorlueng/hw8/
        String url = "http://10.0.2.2/~hectorlueng/hw8/stockQuote.php?symbol=";
        url += symbol;
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
                                if (field == "Stock Ticker") { field = "Stock Symbol"; }
                                else if (field == "prevPrice") { field = "Close"; }
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
                        errorView.setText("Failed to stock details");
                        progressBar.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);
                    }
                });
        request.setRetryPolicy(
                new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stockActivity.requestQueue.add(request);
    }
}
