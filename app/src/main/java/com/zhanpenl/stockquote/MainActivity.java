package com.zhanpenl.stockquote;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    AutoCompleteTextView autoTex;
    Button getButton;
    Button clearButton;
    String makitonURL = "http://zhpnl-web571.us-west-1.elasticbeanstalk.com/autocomplete.php?search=";

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

        getButton = (Button) findViewById(R.id.btn_get);
        clearButton = (Button) findViewById(R.id.btn_clear);

        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String symbol = autoTex.getText().toString().split(" ")[0];
                Intent i = new Intent(getApplicationContext(), StockActivity.class);

                if (symbol.length() == 0) {
                    Toast.makeText(getApplicationContext(), "Please enter a symbol",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // temporary
                i.putExtra("symbol", symbol);
                startActivity(i);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoTex.setText("");
            }
        });

        autoTex = (AutoCompleteTextView) findViewById(R.id.autocomp_text);
        autoTex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // pass
                Log.d("auto", "beforeTextChanged: " + charSequence.toString());
            }

            @Override
            public void onTextChanged(CharSequence text, int i, int i1, int i2) {
                // hide refresh icon
                if (text.length() == 0) { return; }
                String url = makitonURL + text.toString();
                JsonArrayRequest autoCompleteRequest = new JsonArrayRequest(Request.Method.GET, url,null,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                List<String> listing = new LinkedList<>();
                                try {
                                    for (int i = 0; i < response.length(); i++) {
                                        if (i >= 5) break;
                                        JSONObject item = response.getJSONObject(i);
                                        String symbol = item.getString("Symbol");
                                        String name = item.getString("Name");
                                        String exchange = item.getString("Exchange");

                                        String display = symbol + " - " + name + " (" + exchange + ")";
                                        listing.add(display);
                                    }
                                    autoTex.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                                            android.R.layout.simple_dropdown_item_1line, listing));
                                    autoTex.setThreshold(1);
                                    Log.d("auto", "onTextChanged: " + listing.toString());
                                }
                                catch (JSONException e) {
                                    // pass
                                    Log.d("auto", "onTextChanged: failed");
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("auto", "onErrorResponse: cc");
                                Toast.makeText(getApplicationContext(), "autocomplete failed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                requestQueue.add(autoCompleteRequest);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // show refresh icon
            }
        });
        autoTex.setThreshold(1);
    }
}
