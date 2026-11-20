package com.example.whonews;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://www.who.int/news";
    private static final String SESSION_PREFS = "browserSession";
    private static final String TAB_URLS = "tabUrls";
    private static final String TAB_STATES = "tabStates";
    private static final String ACTIVE_TAB = "activeTab";
    private static final String PROGRAM_ID_EXPANDED = "programIdExpanded";

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private final HashMap<WebView, String> tabUrls = new HashMap<>();
    private SharedPreferences sessionPrefs;
    private FrameLayout page;
    private Button tabPicker;
    private Button closeButton;
    private Button shareButton;
    private WebView activeTab;
    private boolean programIdExpanded;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CookieManager.getInstance().setAcceptCookie(true);
        sessionPrefs = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout toolbar = new LinearLayout(this);
        tabPicker = addToolbarButton(toolbar, R.string.open_tabs, this::pickTab);
        addToolbarButton(toolbar, R.string.new_tab, this::openHomeTab);
        closeButton = addToolbarButton(toolbar, R.string.close_tab, () -> closeTab(activeTab));
        shareButton = addToolbarButton(toolbar, R.string.share, this::shareActivePage);
        programIdExpanded = state != null && state.getBoolean(PROGRAM_ID_EXPANDED);
        TextView programId = new TextView(this);
        programId.setText(getString(R.string.program_id_display, getString(R.string.program_id)));
        programId.setTextSize(16);
        programId.setTypeface(Typeface.MONOSPACE);
        programId.setGravity(Gravity.CENTER);
        programId.setTextIsSelectable(true);
        root.addView(programId, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button toggle = new Button(this);
        toggle.setText(R.string.program_id_toggle);
        toggle.setTextSize(24);
        toggle.setPadding(0, 0, 0, 0);
        int touchSize = Math.round(48 * getResources().getDisplayMetrics().density);
        toggle.setMinimumHeight(touchSize);
        toolbar.addView(toggle, new LinearLayout.LayoutParams(
                touchSize, LinearLayout.LayoutParams.MATCH_PARENT));
        root.addView(toolbar);

        updateProgramId(programId, toggle);
        toggle.setOnClickListener(view -> {
            programIdExpanded = !programIdExpanded;
            updateProgramId(programId, toggle);
        });
        toolbar.addOnLayoutChangeListener((view, left, top, right, bottom,
                                           oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = bottom - top;
            if (programId.getLayoutParams().height != height) {
                programId.getLayoutParams().height = height;
                programId.requestLayout();
            }
        });

        page = new FrameLayout(this);
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets padding = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
                return insets;
            });
        }
        setContentView(root);

        ArrayList<Bundle> savedTabs = state == null ? null : state.getParcelableArrayList(TAB_STATES);
        if (savedTabs == null || savedTabs.isEmpty()) {
            restoreSession();
        } else {
            for (Bundle savedTab : savedTabs) {
                WebView tab = createTab();
                tabs.add(tab);
                WebBackForwardList history = tab.restoreState(savedTab);
                if (history != null && history.getCurrentItem() != null) {
                    rememberUrl(tab, history.getCurrentItem().getUrl());
                } else {
                    String url = savedTab.getString(TAB_URLS, HOME_URL);
                    tabUrls.put(tab, url);
                    tab.loadUrl(url);
                }
            }
            showTab(tabs.get(state.getInt(ACTIVE_TAB)));
        }
    }

    private void restoreSession() {
        JSONArray urls;
        try {
            urls = new JSONArray(sessionPrefs.getString(TAB_URLS, "[]"));
        } catch (JSONException invalidSession) {
            urls = new JSONArray();
        }

        for (int i = 0; i < urls.length(); i++) {
            String url = urls.optString(i, "");
            if (!isWebUrl(url)) {
                continue;
            }
            WebView tab = createTab();
            tabs.add(tab);
            tabUrls.put(tab, url);
            tab.loadUrl(url);
        }
        if (tabs.isEmpty()) {
            openHomeTab();
        } else {
            int index = sessionPrefs.getInt(ACTIVE_TAB, 0);
            showTab(tabs.get(Math.max(0, Math.min(index, tabs.size() - 1))));
        }
    }

    private void rememberUrl(WebView tab, String url) {
        if (tabs.contains(tab) && isWebUrl(url)) {
            tabUrls.put(tab, url);
        }
    }

    private void saveSession() {
        if (activeTab == null) {
            return;
        }
        JSONArray urls = new JSONArray();
        int activeIndex = 0;
        for (WebView tab : tabs) {
            String url = tabUrls.get(tab);
            if (url != null) {
                if (tab == activeTab) {
                    activeIndex = urls.length();
                }
                urls.put(url);
            }
        }
        sessionPrefs.edit().putString(TAB_URLS, urls.toString())
                .putInt(ACTIVE_TAB, activeIndex).apply();
    }

    private void updateProgramId(TextView programId, Button toggle) {
        programId.setVisibility(programIdExpanded ? View.VISIBLE : View.GONE);
        toggle.setContentDescription(getString(programIdExpanded
                ? R.string.collapse_program_id : R.string.expand_program_id));
    }

    private Button addToolbarButton(LinearLayout toolbar, int label, Runnable action) {
        Button button = new Button(this);
        int sidePadding = Math.round(4 * getResources().getDisplayMetrics().density);
        int touchHeight = Math.round(48 * getResources().getDisplayMetrics().density);
        button.setMinimumWidth(0);
        button.setMinWidth(0);
        button.setMinimumHeight(touchHeight);
        button.setPadding(sidePadding, button.getPaddingTop(), sidePadding, button.getPaddingBottom());
        button.setAllCaps(false);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        toolbar.addView(button, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return button;
    }

    private String shareableUrl() {
        String url = activeTab == null ? null : activeTab.getUrl();
        return isWebUrl(url) ? url : null;
    }

    private static boolean isWebUrl(String url) {
        String scheme = url == null ? null : Uri.parse(url).getScheme();
        return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
    }

    private void shareActivePage() {
        String url = shareableUrl();
        if (url == null) {
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(share, getString(R.string.share)));
    }

    private WebView createTab() {
        WebView tab = new WebView(this);
        WebSettings settings = tab.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportMultipleWindows(true);

        tab.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView opener, boolean dialog, boolean userGesture,
                                          Message resultMsg) {
                int openerIndex = tabs.indexOf(opener);
                if (!userGesture || openerIndex < 0) {
                    return false;
                }
                WebView child = createTab();
                tabs.add(openerIndex + 1, child);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(child);
                resultMsg.sendToTarget();
                showTab(child);
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                closeTab(window);
            }
        });
        tab.setWebViewClient(new WebViewClient() {
            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                rememberUrl(view, url);
                saveSession();
                if (view == activeTab) {
                    updateToolbar();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == activeTab) {
                    updateToolbar();
                }
                view.evaluateJavascript("(function(){var m=document.querySelector('meta[name=viewport]');"
                        + "if(!m){m=document.createElement('meta');m.name='viewport';"
                        + "(document.head||document.documentElement).appendChild(m);}"
                        + "m.setAttribute('content','width=device-width, initial-scale=1, "
                        + "minimum-scale=0.25, maximum-scale=8, user-scalable=yes');})();", null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)
                        || "about".equalsIgnoreCase(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException | SecurityException ignored) {
                    // No available external handler.
                }
                return true;
            }
        });
        return tab;
    }

    private void openHomeTab() {
        WebView tab = createTab();
        tabs.add(tab);
        tabUrls.put(tab, HOME_URL);
        showTab(tab);
        tab.loadUrl(HOME_URL);
    }

    private void showTab(WebView tab) {
        page.removeAllViews();
        page.addView(tab, new FrameLayout.LayoutParams(-1, -1));
        activeTab = tab;
        updateToolbar();
        saveSession();
    }

    private void updateToolbar() {
        tabPicker.setText(getString(R.string.tabs_count, tabs.size()));
        closeButton.setEnabled(tabs.size() > 1);
        shareButton.setEnabled(shareableUrl() != null);
    }

    private void pickTab() {
        ArrayList<WebView> choices = new ArrayList<>(tabs);
        String[] titles = new String[choices.size()];
        for (int i = 0; i < choices.size(); i++) {
            WebView tab = choices.get(i);
            String title = tab.getTitle();
            titles[i] = i + 1 + ". " + (title == null || title.isEmpty() ? getString(R.string.new_tab) : title);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_tabs)
                .setSingleChoiceItems(titles, choices.indexOf(activeTab), (dialog, index) -> {
                    dialog.dismiss();
                    WebView selected = choices.get(index);
                    if (tabs.contains(selected)) {
                        showTab(selected);
                    }
                })
                .show();
    }

    private void closeTab(WebView tab) {
        int index = tabs.indexOf(tab);
        if (index < 0 || tabs.size() == 1) {
            return;
        }
        tabs.remove(index);
        tabUrls.remove(tab);
        if (tab == activeTab) {
            showTab(tabs.get(Math.max(0, index - 1)));
        } else {
            updateToolbar();
            saveSession();
        }
        tab.destroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        ArrayList<Bundle> savedTabs = new ArrayList<>();
        for (WebView tab : tabs) {
            Bundle savedTab = new Bundle();
            tab.saveState(savedTab);
            savedTab.putString(TAB_URLS, tabUrls.get(tab));
            savedTabs.add(savedTab);
        }
        state.putParcelableArrayList(TAB_STATES, savedTabs);
        state.putInt(ACTIVE_TAB, tabs.indexOf(activeTab));
        state.putBoolean(PROGRAM_ID_EXPANDED, programIdExpanded);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onBackPressed() {
        if (activeTab.canGoBack()) {
            activeTab.goBack();
        } else if (tabs.size() > 1) {
            closeTab(activeTab);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        saveSession();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        page.removeAllViews();
        for (WebView tab : tabs) {
            tab.destroy();
        }
        tabs.clear();
        tabUrls.clear();
        activeTab = null;
        super.onDestroy();
    }
}
