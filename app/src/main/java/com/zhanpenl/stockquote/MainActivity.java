package com.zhanpenl.stockquote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
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
import org.json.JSONTokener;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    AutoCompleteTextView autoTex;
    Button getButton;
    Button clearButton;
    ImageButton manRefresh;
    Switch autoRefresh;
    Spinner catSpinner;
    Spinner ascDescSpinner;
    ListView favListView;


    SharedPreferences sharedPref;
    String makitonURL = "http://zhpnl-web571.us-west-1.elasticbeanstalk.com/autocomplete.php?search=";

    int sortType = 1;
    ArrayAdapter<JSONObject> favListAdapter;

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

        getButton = findViewById(R.id.btn_get);
        clearButton = findViewById(R.id.btn_clear);
        catSpinner = findViewById(R.id.spinner_cat);
        ascDescSpinner = findViewById(R.id.spinner_asc_desc);
        favListView = findViewById(R.id.list_fav);

        // favorite list
        sharedPref = getSharedPreferences(getString(R.string.sharePrefKey),
                Context.MODE_PRIVATE);
        favListAdapter = new ArrayAdapter<JSONObject>(this,
                android.R.layout.simple_spinner_dropdown_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.favrowlayout, null);
                }

                TextView symbolText = convertView.findViewById(R.id.fav_symbol);
                TextView priceText = convertView.findViewById(R.id.fav_change);
                TextView changeText = convertView.findViewById(R.id.fav_change);
                ImageView arrowView = convertView.findViewById(R.id.image_favList_arrow);
                JSONObject favItem = getItem(position);

                try {
                    symbolText.setText(favItem.getString("symbol"));
                    priceText.setText(favItem.getString("price"));

                    String change = favItem.getString("change");
                    changeText.setText(change);
                    if (Float.parseFloat(change.split(" ")[0]) >= 0) {
                        arrowView.setImageResource(R.drawable.up);
                    }
                    else {
                        arrowView.setImageResource(R.drawable.down);
                    }
                }
                catch (JSONException e) {
                    Log.d("FAV_LIST_ERR", "favAdapter getView: " + e.toString());
                }
                return convertView;
            }
        };
        favListAdapter.setNotifyOnChange(true);
        for (Map.Entry<String, ?> entry : sharedPref.getAll().entrySet()) {
            try {
                JSONObject favListObj =
                        (JSONObject) new JSONTokener(entry.getValue().toString()).nextValue();
//                favListObj.put("symbol", entry.getKey());
                favListAdapter.add(favListObj);
            }
            catch (JSONException e) {
                Log.d("FAV_LIST", "onCreate: failed getting in fav list: " + entry.getKey());
            }
        }
        favListView.setAdapter(favListAdapter);

        // sort type Spinner
        ArrayAdapter<String> sortCatAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.sort_categories)) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0 && position != sortType;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView myView = (TextView) super.getDropDownView(position, convertView, parent);
                if (!isEnabled(position)) { myView.setTextColor(Color.GRAY); }
                else { myView.setTextColor(Color.BLACK); }return myView;
            }
        };
        catSpinner.setAdapter(sortCatAdapter);
        catSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sortType = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // connection to stock activity
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
                i.putExtra("symbol", symbol.toUpperCase());
                startActivity(i);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoTex.setText("");
            }
        });

        //
        autoTex = (AutoCompleteTextView) findViewById(R.id.autocomp_text);
        autoTex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // pass
                Log.d("auto", "beforeTextChanged: " + charSequence.toString());
            }

            @Override
            public void onTextChanged(CharSequence text, int i, int i1, int i2) {
                // TODO: check bug -> only refresh list when deleting characters
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

    public void notifyFavChange() {
        favListAdapter.notifyDataSetChanged();
    }
}
