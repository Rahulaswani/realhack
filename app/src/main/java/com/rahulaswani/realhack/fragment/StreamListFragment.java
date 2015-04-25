package com.rahulaswani.realhack.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rahulaswani.realhack.EndlessScrollListener;
import com.rahulaswani.realhack.LocalPersistence;
import com.rahulaswani.realhack.R;
import com.rahulaswani.realhack.SECRETS;
import com.rahulaswani.realhack.adapter.StreamAdapter;
import io.kickflip.sdk.Share;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.StreamList;
import io.kickflip.sdk.exception.KickflipException;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class StreamListFragment extends Fragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = "StreamListFragment";
    private static final String SERIALIZED_FILESTORE_NAME = "streams";
    private static final boolean VERBOSE = true;

    private StreamListFragmenListener mListener;
    private SwipeRefreshLayout mSwipeLayout;
    private KickflipApiClient mKickflip;
    private List<Stream> mStreams;
    private boolean mRefreshing;

    private int mCurrentPage = 1;
    private static final int ITEMS_PER_PAGE = 10;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private StreamAdapter mAdapter;

    private StreamAdapter.StreamAdapterActionListener mStreamActionListener = new StreamAdapter.StreamAdapterActionListener() {
        @Override
        public void onFlagButtonClick(final Stream stream) {
            KickflipCallback cb = new KickflipCallback() {
                @Override
                public void onSuccess(Response response) {
                    if (getActivity() != null) {
                        if (mKickflip.activeUserOwnsStream(stream)) {
                            mAdapter.remove(stream);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.stream_flagged), Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onError(KickflipException error) {}
            };

            if (mKickflip.activeUserOwnsStream(stream)) {
                stream.setDeleted(true);
                mKickflip.setStreamInfo(stream, cb);
            } else {
                mKickflip.flagStream(stream, cb);
            }
        }

        @Override
        public void onShareButtonClick(Stream stream) {
            Intent shareIntent = Share.createShareChooserIntentWithTitleAndUrl(getActivity(), getString(io.kickflip.sdk.R.string.share_broadcast), stream.getKickflipUrl());
            startActivity(shareIntent);
        }
    };

    private EndlessScrollListener mEndlessScrollListener = new EndlessScrollListener() {
        @Override
        public void onLoadMore(int page, int totalItemsCount) {
            Log.i(TAG, "Loading more streams");
            getStreams(false);
        }
    };


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StreamListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKickflip = new KickflipApiClient(getActivity(), SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (mAdapter != null) {
                    mAdapter.setUserName(mKickflip.getActiveUser().getName());
                }
                getStreams(true);
                // Update profile display when we add that
            }

            @Override
            public void onError(KickflipException error) {
                showNetworkError();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        loadPersistedStreams();
        getStreams(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        persistStreams();
    }


    /**
     * Load persisted Streams from disk if available.
     */
    private void loadPersistedStreams() {
        if (getActivity() != null) {
            Object streams = LocalPersistence.readObjectFromFile(getActivity(), SERIALIZED_FILESTORE_NAME);
            if (streams != null) {
                displayStreams((List<Stream>) streams, false);
            }
        }
    }

    /**
     * Serialize a few Streams to disk so the UI is quickly populated
     * on application re-launch
     *
     * If we had reason to keep a robust local copy of the data, we'd use sqlite
     */
    private void persistStreams() {
        if (getActivity() != null) {
            while (mStreams.size() > 7) {
                mStreams.remove(mStreams.size()-1);
            }
            LocalPersistence.writeObjectToFile(getActivity(), mStreams, SERIALIZED_FILESTORE_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stream, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setOnScrollListener(mEndlessScrollListener);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        // Why does this selection remain if I long press, release
        // without activating onListItemClick?
        //mListView.setSelector(R.drawable.stream_list_selector_overlay);
        //mListView.setDrawSelectorOnTop(true);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorScheme(R.color.kickflip_green,
                R.color.kickflip_green_shade_2,
                R.color.kickflip_green_shade_3,
                R.color.kickflip_green_shade_4);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        setupListViewAdapter();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (StreamListFragmenListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement StreamListFragmenListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Stream stream = mAdapter.getItem(position);
        mListener.onStreamPlaybackRequested(stream.getStreamUrl());
    }

    /**
     * Fetch Streams and display in ListView
     *
     * @param refresh whether this fetch is for a subsequent page
     *                      or to refresh the first page
     */
    private void getStreams(final boolean refresh) {
        if (mKickflip.getActiveUser() == null || mRefreshing) return;
        mRefreshing = true;
        if (refresh) mCurrentPage = 1;
        mKickflip.getStreamsByKeyword(null, mCurrentPage, ITEMS_PER_PAGE, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (VERBOSE) Log.i("API", "request succeeded " + response);
                if (getActivity() != null) {
                    displayStreams(((StreamList) response).getStreams(), !refresh);
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
                mCurrentPage++;
            }

            @Override
            public void onError(KickflipException error) {
                if (VERBOSE) Log.i("API", "request failed " + error.getMessage());
                if (getActivity() != null) {
                    showNetworkError();
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
            }
        });
    }

    private void setupListViewAdapter() {
        if (mAdapter == null) {
            mStreams = new ArrayList<>(0);
            mAdapter = new StreamAdapter(getActivity(), mStreams, mStreamActionListener);
            mAdapter.setNotifyOnChange(false);
            mListView.setAdapter(mAdapter);
            if (mKickflip.getActiveUser() != null) {
                mAdapter.setUserName(mKickflip.getActiveUser().getName());
            }
        }
    }

    /**
     * Display the given List of {@link io.kickflip.sdk.api.json.Stream} Objects
     *
     * @param streams a List of {@link io.kickflip.sdk.api.json.Stream} Objects
     * @param append whether to append the given streams to the current list
     *               or use the given streams as the absolute dataset.
     */
    private void displayStreams(List<Stream> streams, boolean append) {
        if (append) {
            mStreams.addAll(streams);
        } else {
            mStreams = streams;
        }
        Collections.sort(mStreams);
        mAdapter.refresh(mListView, mStreams);
        if (mStreams.size() == 0) {
            showNoBroadcasts();
        }
    }

    /**
     * Inform the user that a network error has occured
     */
    public void showNetworkError() {
        setEmptyListViewText(getString(R.string.no_network));
    }

    /**
     * Inform the user that no broadcasts were found
     */
    public void showNoBroadcasts() {
        setEmptyListViewText(getString(R.string.no_broadcasts));
    }

    /**
     * If the ListView is hidden, show the
     *
     * @param text
     */
    private void setEmptyListViewText(String text) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(text);
        }
    }

    @Override
    public void onRefresh() {
        if (!mRefreshing) {
            getStreams(true);
        }

    }

    public interface StreamListFragmenListener {
        public void onStreamPlaybackRequested(String url);
    }

}
