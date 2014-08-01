package com.example.mupdf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.artifex.mupdf.*;
import com.artifex.mupdf.domain.OutlineActivityData;
import com.artifex.mupdf.domain.SearchTaskResult;
import com.artifex.mupdf.view.DocumentReaderView;
import com.artifex.mupdf.view.MuPDFPageView;
import com.artifex.mupdf.view.ReaderView;
import com.librelio.task.TinySafeAsyncTask;

import java.util.ArrayList;

public class MyActivity extends Activity {
    private static final String TAG = MyActivity.class.getSimpleName();
    private static final String FILE_NAME = "FileName";

    private ReaderView docView;
    private MuPDFPageAdapter docViewAdapter;
    private MuPDFCore core;
    private SparseArray<LinkInfoExternal[]> linkOfDocument;

    private View buttonsView;
    private EditText mSearchText;
    private ImageView mSearchButton;
    private ImageView mCancelButton;
    private ViewSwitcher mTopBarSwitcher;
    private RelativeLayout topBar2;
    private Button mSearchBack;
    private Button mSearchFwd;

    private AlertDialog.Builder alertBuilder;

    private String fileName;
    private int orientation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alertBuilder = new AlertDialog.Builder(this);
        orientation = getResources().getConfiguration().orientation;

        core = getMuPdfCore(savedInstanceState);

        RelativeLayout layout = createUI(savedInstanceState);
        setContentView(layout);
    }

    private RelativeLayout createUI(Bundle savedInstanceState) {
        if (core == null)
            return null;
        // Now create the UI.
        // First create the document view making use of the ReaderView's internal
        // gesture recognition
        docView = new DocumentReaderView(this, linkOfDocument) {
            ActivateAutoLinks mLinksActivator = null;

            @Override
            protected void onContextMenuClick() {

            }

            @Override
            protected void onMoveToChild(View view, int i) {
                Log.d(TAG, "onMoveToChild id = " + i);

//				if(core.getDisplayPages() == 1)
//					mPreview.scrollToPosition(i);
//				else
//					mPreview.scrollToPosition(((i == 0) ? 0 : i * 2 - 1));

                if (core == null) {
                    return;
                }
                MuPDFPageView pageView = (MuPDFPageView) docView.getDisplayedView();
                if (pageView != null) {
                    pageView.cleanRunningLinkList();
                }
                super.onMoveToChild(view, i);
                if (mLinksActivator != null)
                    mLinksActivator.cancel(true);
                mLinksActivator = new ActivateAutoLinks(pageView);
                mLinksActivator.safeExecute(i);
                setCurrentlyViewedPreview();
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

        };
        docViewAdapter = new MuPDFPageAdapter(this, core);
        docView.setAdapter(docViewAdapter);

        // Make the buttons overlay, and store all its
        // controls in variables
//        makeButtonsView();

        // Reinstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        int orientation = prefs.getInt("orientation", this.orientation);
        int pageNum = prefs.getInt("page" + fileName, 0);
        docView.setDisplayedViewIndex(pageNum);

        // Stick the document view and the buttons overlay into a parent view
        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(docView);

        layout.setBackgroundColor(Color.BLACK);
        return layout;
    }

    private MuPDFCore getMuPdfCore(Bundle savedInstanceState) {
        MuPDFCore core = null;
        if (savedInstanceState != null && savedInstanceState.containsKey(FILE_NAME)) {
            fileName = savedInstanceState.getString(FILE_NAME);
        }

        core = openFile("/storage/sdcard0/hotspot.pdf");
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            core.setDisplayPages(2);
        } else {
            core.setDisplayPages(1);
        }

        SearchTaskResult.recycle();

        return core;
    }

    private MuPDFCore openFile(String path) {
        Log.d(TAG, "Trying to open " + path);
        int lastSlashPos = path.lastIndexOf('/');
        fileName = new String(lastSlashPos == -1 ? path : path.substring(lastSlashPos + 1));

        try {
            core = new MuPDFCore(path);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
        } catch (Exception e) {
            Log.e(TAG, "get core failed", e);
            return null;
        }
        return core;
    }

    public void forwardClicked(View view) {
        Toast.makeText(this, "forwardClicked", 1000).show();
        search(-1);
    }

    public void backwardClicked(View view) {
        Toast.makeText(this, "backwardClicked", 1000).show();
        search(-1);

    }

    private class ActivateAutoLinks extends TinySafeAsyncTask<Integer, Void, ArrayList<LinkInfoExternal>> {
        private MuPDFPageView pageView;

        public ActivateAutoLinks(MuPDFPageView pParent) {
            pageView = pParent;
        }

        @Override
        protected ArrayList<LinkInfoExternal> doInBackground(Integer... params) {
            int page = params[0].intValue();
            Log.d(TAG, "Page = " + page);
            if (null != core) {
                LinkInfo[] links = core.getPageLinks(page);
                if (null == links) {
                    return null;
                }
                ArrayList<LinkInfoExternal> autoLinks = new ArrayList<LinkInfoExternal>();
                for (LinkInfo link : links) {
                    if (link instanceof LinkInfoExternal) {
                        LinkInfoExternal currentLink = (LinkInfoExternal) link;

                        if (null == currentLink.url) {
                            continue;
                        }
                        Log.d(TAG, "checking link for autoplay: " + currentLink.url);

                        if (currentLink.isMediaURI()) {
                            if (currentLink.isAutoPlay()) {
                                autoLinks.add(currentLink);
                            }
                        }
                    }
                }
                return autoLinks;
            }
            return null;
        }

        @Override
        protected void onPostExecute(final ArrayList<LinkInfoExternal> autoLinks) {
            if (isCancelled() || autoLinks == null) {
                return;
            }
            docView.post(new Runnable() {
                public void run() {
                    for (LinkInfoExternal link : autoLinks) {
                        if (pageView != null && null != core) {
                            String basePath = core.getFileDirectory();
                            MediaHolder mediaHolder = new MediaHolder(getContext(), link, basePath);
                            pageView.addMediaHolder(mediaHolder, link.url);
                            pageView.addView(mediaHolder);
                            mediaHolder.setVisibility(View.VISIBLE);
                            mediaHolder.requestLayout();
                        }
                    }
                }
            });
        }
    }

    private Context getContext() {
        return this;
    }

    private void setCurrentlyViewedPreview() {
        int i = docView.getDisplayedViewIndex();
        if (core.getDisplayPages() == 2) {
            i = (i * 2) - 1;
        }
    }

    private class SearchTask extends TinySafeAsyncTask<Void, Integer, SearchTaskResult> {
        private final int increment;
        private final int startIndex;
        private final ProgressDialog progressDialog;

        public SearchTask(Context context, int increment, int startIndex) {
            this.increment = increment;
            this.startIndex = startIndex;
            progressDialog = new ProgressDialog(context);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle(getString(R.string.searching_));
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    killSearch();
                }
            });
            progressDialog.setMax(core.countPages());
        }

        @Override
        protected SearchTaskResult doInBackground(Void... params) {
            int index = startIndex;

            while (0 <= index && index < core.countPages() && !isCancelled()) {
                publishProgress(index);
                String text = "surf";
                RectF searchHits[] = core.searchPage(index, text);

                if (searchHits != null && searchHits.length > 0) {
                    return SearchTaskResult.init(text, index, searchHits);
                }

                index += increment;
            }
            return null;
        }

        @Override
        protected void onPostExecute(SearchTaskResult result) {
            if (isCancelled()) {
                return;
            }
            progressDialog.cancel();
            if (result != null) {
                // Ask the ReaderView to move to the resulting page
                docView.setDisplayedViewIndex(result.pageNumber);
                SearchTaskResult.recycle();
                // Make the ReaderView act on the change to mSearchTaskResult
                // via overridden onChildSetup method.
                docView.resetupChildren();
            } else {
                alertBuilder.setTitle(SearchTaskResult.get() == null ? R.string.text_not_found : R.string.no_further_occurences_found);
                AlertDialog alert = alertBuilder.create();
                alert.setButton(AlertDialog.BUTTON_POSITIVE, "Dismiss",
                        (DialogInterface.OnClickListener) null);
                alert.show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressDialog.cancel();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0].intValue());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                public void run() {
//					if (!progressDialog.isCancelled())
//					{
                    progressDialog.show();
                    progressDialog.setProgress(startIndex);
//					}
                }
            }, 1000);
        }
    }


    void killSearch() {
    }

    void search(int direction) {
        hideKeyboard();
        if (core == null)
            return;
        killSearch();

        final int increment = direction;
        final int startIndex = SearchTaskResult.get() == null ? docView.getDisplayedViewIndex() : SearchTaskResult.get().pageNumber + increment;

        new SearchTask(getContext(), increment, startIndex).execute();
    }

    void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }
}
