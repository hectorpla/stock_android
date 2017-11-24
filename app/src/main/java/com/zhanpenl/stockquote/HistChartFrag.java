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
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Created by hectorlueng on 11/17/17.
 */

public class HistChartFrag extends Fragment {
    private StockActivity stockActivity;
    private WebView webView;
    private TextView errorView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_hist_stock, container, false);
        stockActivity = (StockActivity) getActivity();

        webView = (WebView) view.findViewById(R.id.webview_hist);
        errorView = (TextView) view.findViewById(R.id.errMsg_hist);

        WebSettings webSettings = webView.getSettings();

        webView.setWebViewClient(new WebViewClient());
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(stockActivity),"HistStock");

        Log.d("HIST", "HIST onCreateView: ++++++++++++++++++++++++++++");

//        webView.loadUrl("file:///android_asset/historical.html");
        errorView.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        Log.d("HIST", "setUserVisibleHint: called!!!!!!!");
        // not sure if it is a good way to address it
        if (webView != null) {
            webView.loadUrl("file:///android_asset/historical.html");
        }
    }

    public class WebAppInterface {
        Context mContext;

        public WebAppInterface(Context c) { mContext = c; }

        @JavascriptInterface
        public String getPricePlotObject() {
            JSONObject obj = stockActivity.getPricePlotObject();
            Log.d("Hist", "getPricePlotObject: " + obj);
            if (obj == null) return "null";
            return obj.toString();
        }
        @JavascriptInterface
        public void setErrorMessage(String msg) {
//            errorView.setText(msg);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    errorView.setVisibility(View.VISIBLE);
                }
            });
            Log.d("HIST", "setErrorMessage: javascript interface " + errorView.getText());
        }
    }
}
