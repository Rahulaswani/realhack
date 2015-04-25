package com.rahulaswani.realhack.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.rahulaswani.realhack.R;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;

import com.rahulaswani.realhack.MainFragmentInteractionListener;
import com.rahulaswani.realhack.SECRETS;
import com.rahulaswani.realhack.Util;
import com.rahulaswani.realhack.fragment.MainFragment;
import com.rahulaswani.realhack.fragment.StreamListFragment;
import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.BroadcastListener;
import io.kickflip.sdk.av.SessionConfig;
import io.kickflip.sdk.exception.KickflipException;
import io.kickflip.sdk.fragment.BroadcastFragment;

import static io.kickflip.sdk.Kickflip.isKickflipUrl;


public class MainActivity extends Activity implements MainFragmentInteractionListener, StreamListFragment.StreamListFragmenListener {
    private static final String TAG = "MainActivity";

    private boolean mKickflipReady = false;

    private String[] mDrawerMenuItems;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private ListView mDrawerList;
    private CharSequence mTitle;

    private BroadcastListener mBroadcastListener = new BroadcastListener() {
        @Override
        public void onBroadcastStart() {
            Log.i(TAG, "onBroadcastStart");
        }

        @Override
        public void onBroadcastLive(Stream stream) {
            Log.i(TAG, "onBroadcastLive @ " + stream.getKickflipUrl());
        }

        @Override
        public void onBroadcastStop() {
            Log.i(TAG, "onBroadcastStop");

            // If you're manually injecting the BroadcastFragment,
            // you'll want to remove/replace BroadcastFragment
            // when the Broadcast is over.

            //getFragmentManager().beginTransaction()
            //    .replace(R.id.container, MainFragment.getInstance())
            //    .commit();
        }

        @Override
        public void onBroadcastError(KickflipException error) {
            Log.i(TAG, "onBroadcastError " + error.getMessage());
        }
    };
    // By default, we are stores video in a "Kickflip" directory on external storage
    private String mRecordingOutputPath = new File(Environment.getExternalStorageDirectory(), "MySampleApp/index.m3u8").getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowHomeEnabled(false);
        setContentView(R.layout.activity_main);

        mDrawerMenuItems = getResources().getStringArray(R.array.drawer_menu_items);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
                //(View) findViewById(R.layout.drawer_list_item).getRootView();
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item , mDrawerMenuItems));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // This must happen before any other Kickflip interactions
        Kickflip.setup(this, SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                mKickflipReady = true;
            }

            @Override
            public void onError(KickflipException error) {

            }
        });

        if (!handleLaunchingIntent()) {
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new StreamListFragment())
                        .commit();
            }
        }
        tintStatusBar();
    }

/* Called whenever we call invalidateOptionsMenu() *//*
@Override
public boolean onPrepareOptionsMenu(Menu menu){
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen=mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
        }*/

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {
        if (position == 1) {
                    if (mKickflipReady) {
                        startBroadcastingActivity();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle(getString(R.string.dialog_title_not_ready))
                                .setMessage(getString(R.string.dialog_msg_not_ready))
                                .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
        } else {
            // Create a new fragment and specify the planet to show based on position
            StreamListFragment fragment = new StreamListFragment();
            /*Bundle args = new Bundle();
            args.putInt(StreamListFragment.ARG_PLANET_NUMBER, position);
            fragment.setArguments(args);*/

            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();

            // Highlight the selected item, update the title, and close the drawer
            mDrawerList.setItemChecked(position, true);
            setTitle(mDrawerMenuItems[position]);
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    private void tintStatusBar() {
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(false);
        tintManager.setTintColor(getResources().getColor(R.color.primary_app_color_dark));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_broadcast:
                if (mKickflipReady) {
                    startBroadcastingActivity();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.dialog_title_not_ready))
                            .setMessage(getString(R.string.dialog_msg_not_ready))
                            .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentEvent(MainFragment.EVENT event) {
        startBroadcastingActivity();
    }

    /**
     * Unused method demonstrating how to use
     * Kickflip's BroadcastFragment.
     * <p/>
     * Note that in this scenario your Activity is responsible for
     * removing the BroadcastFragment in your onBroadcastStop callback.
     * When the user stops recording, the BroadcastFragment begins releasing
     * resources and freezes the camera preview.
     */
    public void startBroadcastFragment() {
        // Before using the BroadcastFragment, be sure to
        // register your BroadcastListener with Kickflip
        configureNewBroadcast();
        Kickflip.setBroadcastListener(mBroadcastListener);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, BroadcastFragment.getInstance())
                .commit();
    }


    @Override
    public void onStreamPlaybackRequested(String streamUrl) {
        // Play with Kickflip's built-in Media Player
        Kickflip.startMediaPlayerActivity(this, streamUrl, false);

        // Play via Intent for 3rd party Media Player
        //Intent i = new Intent(Intent.ACTION_VIEW);
        //i.setDataAndType(Uri.parse(stream.getStreamUrl()), "application/vnd.apple.mpegurl");
        //startActivity(i);
    }

    private void startBroadcastingActivity() {
        configureNewBroadcast();
        Kickflip.startBroadcastActivity(this, mBroadcastListener);
    }

    private void configureNewBroadcast() {
        // Should reset mRecordingOutputPath between recordings
        SessionConfig config = Util.create720pSessionConfig(mRecordingOutputPath);
        //SessionConfig config = Util.create420pSessionConfig(mRecordingOutputPath);
        Kickflip.setSessionConfig(config);
    }

    private boolean handleLaunchingIntent() {
        Uri intentData = getIntent().getData();
        if (isKickflipUrl(intentData)) {
            Kickflip.startMediaPlayerActivity(this, intentData.toString(), true);
            finish();
            return true;
        }
        return false;
    }
}
