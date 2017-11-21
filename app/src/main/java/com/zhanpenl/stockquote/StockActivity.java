package com.zhanpenl.stockquote;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockActivity extends AppCompatActivity {
    public String test = "some test message, check if it works out";
    public String symbol = null;
    private JSONObject plotObject;
    private Map<String, JSONObject> indPlotObjects;
    private List<String> dates;
    private List<Integer> UTCDates;
    private final int INDICATOR_LENGTH = 128;
    private final int HIST_LENGTH = 1000;

    RequestQueue requestQueue;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        // retain pages
        mViewPager.setOffscreenPageLimit(2);

        // back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        symbol = getIntent().getStringExtra("symbol");
        this.setTitle(symbol);

        requestQueue = Volley.newRequestQueue(this);
        indPlotObjects = new HashMap<>();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            switch (position) {
                case 0:
                    return new CurrentStockFrag();
                case 1:
                    return new HistChartFrag();
                case 2:
                    return new NewsFeedFrag();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

    public List<String> getDates() {
        return new ArrayList<>(dates);
    }

    public void setDates(List<String> rowDates) {
        int sz = Math.min(rowDates.size(), INDICATOR_LENGTH);
        dates = rowDates.subList(0, sz);
    }

    public JSONObject getPricePlotObject() {
        return plotObject;
    }

    public void setPricePlotObject(JSONObject obj) {
        plotObject = obj;
    }

    public JSONObject getIndPlotObject(String indicator) {
        if (!indPlotObjects.containsKey(indicator)) { return null; }
        // TODO: might want to return a copy
        return indPlotObjects.get(indicator);
    }

    public void putIndPlotObject(String indicator, JSONObject obj) {
        indPlotObjects.put(indicator, obj);
    }
}
