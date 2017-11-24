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
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    AutoCompleteTextView autoTex;
    Button getButton;
    Button clearButton;
    ImageButton manRefresh;
    Switch autoRefresh;
    Spinner catSpinner;
    Spinner ascDescSpinner;
    ListView favListView;
    ProgressBar progressBar;

    SharedPreferences sharedPref;
    final String makitonURL = "http://zhpnl-web571.us-west-1.elasticbeanstalk.com/autocomplete.php?search=";

    int sortType = 0;
    boolean isSortAsc = true;
    ArrayAdapter<JSONObject> favListAdapter;
    final List<JSONObject> favList = new ArrayList<>();

    List<Comparator<JSONObject>> comparators;

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getButton = (Button) findViewById(R.id.btn_get);
        clearButton = (Button) findViewById(R.id.btn_clear);
        catSpinner = (Spinner) findViewById(R.id.spinner_cat);
        ascDescSpinner = (Spinner) findViewById(R.id.spinner_asc_desc);
        favListView = (ListView) findViewById(R.id.list_fav);
        progressBar = (ProgressBar) findViewById(R.id.progressBar_main);
        autoRefresh = (Switch) findViewById(R.id.switch_refresh);
        manRefresh = (ImageButton) findViewById(R.id.btn_refresh);

        // initialize functional constructs
        setComparators();
        requestQueue = Volley.newRequestQueue(this);
        sharedPref = getSharedPreferences(getString(R.string.sharePrefKey), Context.MODE_PRIVATE);

        // favorite list
        favListAdapter = new ArrayAdapter<JSONObject>(this,
                android.R.layout.simple_spinner_dropdown_item, favList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.favrowlayout, null);
                }

                TextView symbolText = (TextView) convertView.findViewById(R.id.fav_symbol);
                TextView priceText = (TextView) convertView.findViewById(R.id.fav_price);
                TextView changeText = (TextView) convertView.findViewById(R.id.fav_change);
                JSONObject favItem = getItem(position);
                final String symbol;

                try {
                    symbol = favItem.getString("symbol");
                    symbolText.setText(symbol);
                    priceText.setText(favItem.getString("price"));

                    String change = favItem.getString("change");
                    changeText.setText(change);
                    if (Float.parseFloat(change.split(" ")[0]) >= 0) {
                        changeText.setTextColor(Color.GREEN);
                    }
                    else {
                        changeText.setTextColor(Color.RED);
                    }
                }
                catch (JSONException e) {
                    Log.d("FAV_LIST_ERR", "favAdapter getView: " + e.toString());
                    return null;
                }
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startStockActivity(symbol);
                    }
                });
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        return false;
                    }
                });
                return convertView;
            }
        };
        favListView.setAdapter(favListAdapter);
        loadFavList();
        registerForContextMenu(favListView);

        // refresh mechanism
        manRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int sz = sharedPref.getAll().size() - 1;
                int count = 0;
                final String url = "http://10.0.2.2/~hectorlueng/hw8/stockQuote.php?realtime=true&symbol=";
                final SharedPreferences.Editor editor = sharedPref.edit();

                for (final String sym : sharedPref.getAll().keySet()) {
                    if (sym.equals("count")) { continue; }
                    count++;
                    final int currentCount = count;
                    JsonObjectRequest refreshRequest = new JsonObjectRequest(url + sym, null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        JSONObject record =
                                                (JSONObject) new JSONTokener(sharedPref.getString(sym,
                                                        null)).nextValue();
                                        double newPrice = response.getDouble("Last Price");
                                        double prevPrice = record.getDouble("prevPrice");
                                        double change = newPrice - prevPrice;
                                        double changePer = change / prevPrice * 100; // TODO: round up
                                        String changeString = change + " (" + changePer + "%)";

                                        // TODO: should update prevPrice
                                        record.put("price", newPrice);
                                        record.put("change", changeString);
                                        editor.putString(sym, record.toString());
                                        editor.commit();
                                    }
                                    catch (JSONException e) {
                                        Log.d("FAV_LIST_UPDATE", e.toString());
                                    }
                                    if (currentCount == sz) {
                                        // update list, notify change
                                        loadFavList();
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Toast.makeText(getApplication(),
                                            "can't refresh info for " + sym,
                                            Toast.LENGTH_SHORT).show();
                                    Log.d("FAV_LIST_UPDATE", url + sym + ": "
                                            + error.toString());
                                    if (currentCount == sz) {
                                        loadFavList();
                                    }
                                }
                            }
                    );
                    requestQueue.add(refreshRequest);
                }
            }
        });

        // sort type Spinner
        final ArrayAdapter<String> sortCatAdapter = new ArrayAdapter<String>(this,
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
                else { myView.setTextColor(Color.BLACK); }
                return myView;
            }
        };
        catSpinner.setAdapter(sortCatAdapter);
        catSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sortType = i;
                // sort, may be not good to put here
                sortFavList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        ArrayAdapter<String> sortOrderAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.sort_order)) {
            @Override
            public boolean isEnabled(int position) {
                return !(position == 0 || (position == 1 && isSortAsc)
                        || (position == 2 && !isSortAsc));
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView myView = (TextView) super.getDropDownView(position, convertView, parent);
                if (!isEnabled(position)) { myView.setTextColor(Color.GRAY); }
                else { myView.setTextColor(Color.BLACK); }
                return myView;
            }
        };
        ascDescSpinner.setAdapter(sortOrderAdapter);
        ascDescSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                isSortAsc = i == 1;
                sortFavList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // connection to stock activity
        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String symbol = autoTex.getText().toString().split(" ")[0];
                startStockActivity(symbol);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoTex.setText("");
            }
        });

        // autocomplete
        autoTex = (AutoCompleteTextView) findViewById(R.id.autocomp_text);
        autoTex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d("auto", "beforeTextChanged: " + charSequence.toString());
            }

            @Override
            public void onTextChanged(CharSequence text, int i, int i1, int i2) {
                // TODO: check bug -> only refresh list when deleting characters
                // hide refresh icon
                if (text.length() == 0) { return; }
                String url = makitonURL + text.toString();

                progressBar.setVisibility(View.VISIBLE);
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
                                progressBar.setVisibility(View.GONE);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("auto", "onErrorResponse: cc");
                                Toast.makeText(getApplicationContext(), "autocomplete failed",
                                        Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
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

    private void startStockActivity(String symbol) {
        Intent i = new Intent(getApplicationContext(), StockActivity.class);

        if (symbol.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please enter a symbol",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        i.putExtra("symbol", symbol.toUpperCase());
        startActivity(i);
    }

    private void setComparators() {
        comparators = new ArrayList<>();
        comparators.add(new Comparator<JSONObject>() {
            public int compare(JSONObject lhs, JSONObject rhs) {
                try {
                    return lhs.getInt("orderTag") - rhs.getInt("orderTag");
                } catch (JSONException e) {
                    Log.d("FAV_LIST_SORT", "compare: " + e.toString());
                }
                return 0;
            }
        });
        comparators.add(new Comparator<JSONObject>() {
            public int compare(JSONObject lhs, JSONObject rhs) {
                try {
                    return lhs.getString("symbol").compareTo(rhs.getString("symbol"));
                }
                catch (JSONException e) {
                    Log.d("FAV_LIST_SORT", "compare: " + e.toString());
                }
                return 0;
            }
        });
        comparators.add(new Comparator<JSONObject>() {
            public int compare(JSONObject lhs, JSONObject rhs) {
                try {
                    Double doubleLhs = lhs.getDouble("price");
                    Double doubleRhs = rhs.getDouble("price");
                    if (doubleLhs > doubleRhs) return 1;
                    if (doubleLhs < doubleRhs) return -1;
                }
                catch (JSONException e) {
                    Log.d("FAV_LIST_SORT", "compare: " + e.toString());
                }
                return 0;
            }
        });
        comparators.add(new Comparator<JSONObject>() {
            public int compare(JSONObject lhs, JSONObject rhs) {
                try {
                    double changePerLhs = extractChangePer(lhs);
                    double changePerRhs = extractChangePer(rhs);

                    if (changePerLhs > changePerRhs) return 1;
                    if (changePerLhs < changePerLhs) return -1;
                }
                catch (JSONException e) {
                    Log.d("FAV_LIST_SORT", "compare: " + e.toString());
                }
                return 0;
            }

            private double extractChangePer(JSONObject obj) throws JSONException {
                String str = obj.getString("change").split(" ")[1];
                str = str.substring(1, str.length() - 2);
                return Float.parseFloat(str);
            }
        });
    }

    private void loadFavList() {
        favList.clear();
        Log.d("FAV_LIST_TRAVERSE", "sharedPref size: " + sharedPref.getAll().size());
        for (Map.Entry<String, ?> entry : sharedPref.getAll().entrySet()) {
            Log.d("FAV_LOAD", "key: " + entry.getKey() + ", value: " + entry.getValue());
            if (entry.getKey().equals("count")) { continue; }
            try {
                JSONObject favListObj =
                        (JSONObject) new JSONTokener(entry.getValue().toString()).nextValue();
                favList.add(favListObj);
            }
            catch (JSONException e) {
                Log.d("FAV_LIST", "onCreate: failed getting in fav list: " + entry.getKey());
            }
        }
        sortFavList();
        favListAdapter.notifyDataSetChanged();
    }

    private void sortFavList() {
        Comparator<JSONObject> comp = comparators.get(Math.max(sortType - 1, 0));
        if (!isSortAsc) { comp = comp.reversed(); }
        favListAdapter.sort(comp);
        favListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_delete_fav, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (item.getItemId() == R.id.delete_yes) {
            SharedPreferences.Editor editor = sharedPref.edit();

            try {
                String symbol = favList.get(info.position).getString("symbol");
                editor.remove(symbol);
                editor.commit();
            }
            catch (JSONException e) {
                Log.d("FAV_DELETE", "onContextItemSelected: " + e.toString());
            }
            favList.remove(info.position);
            favListAdapter.notifyDataSetChanged();
            Toast.makeText(this, "selected true", Toast.LENGTH_SHORT).show();
            return true;
        }
        else {
            Toast.makeText(this, "selected no", Toast.LENGTH_SHORT).show();
            return  false;
        }
    }
}
