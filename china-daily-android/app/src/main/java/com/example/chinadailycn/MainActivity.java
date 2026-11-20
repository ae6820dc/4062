package com.example.chinadailycn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://cn.chinadaily.com.cn/";
    private static final String TAB_STATES = "tabStates";
    private static final String ACTIVE_TAB = "activeTab";

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private FrameLayout page;
    private Button tabPicker;
    private Button closeButton;
    private WebView activeTab;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CookieManager.getInstance().setAcceptCookie(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout toolbar = new LinearLayout(this);
        tabPicker = new Button(this);
        tabPicker.setOnClickListener(view -> pickTab());
        toolbar.addView(tabPicker, new LinearLayout.LayoutParams(0, -2, 1));

        Button newButton = new Button(this);
        newButton.setText(R.string.new_tab);
        newButton.setOnClickListener(view -> openHomeTab());
        toolbar.addView(newButton);

        closeButton = new Button(this);
        closeButton.setText(R.string.close_tab);
        closeButton.setOnClickListener(view -> closeTab(activeTab));
        toolbar.addView(closeButton);
        root.addView(toolbar);

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
            openHomeTab();
        } else {
            for (Bundle savedTab : savedTabs) {
                WebView tab = createTab();
                tabs.add(tab);
                if (tab.restoreState(savedTab) == null) {
                    tab.loadUrl(HOME_URL);
                }
            }
            showTab(tabs.get(state.getInt(ACTIVE_TAB)));
        }
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
            public void onPageFinished(WebView view, String url) {
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
        showTab(tab);
        tab.loadUrl(HOME_URL);
    }

    private void showTab(WebView tab) {
        page.removeAllViews();
        page.addView(tab, new FrameLayout.LayoutParams(-1, -1));
        activeTab = tab;
        updateToolbar();
    }

    private void updateToolbar() {
        tabPicker.setText(getString(R.string.tabs_count, tabs.size()));
        closeButton.setEnabled(tabs.size() > 1);
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
        if (tab == activeTab) {
            showTab(tabs.get(Math.max(0, index - 1)));
        } else {
            updateToolbar();
        }
        tab.destroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        ArrayList<Bundle> savedTabs = new ArrayList<>();
        for (WebView tab : tabs) {
            Bundle savedTab = new Bundle();
            tab.saveState(savedTab);
            savedTabs.add(savedTab);
        }
        state.putParcelableArrayList(TAB_STATES, savedTabs);
        state.putInt(ACTIVE_TAB, tabs.indexOf(activeTab));
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
    protected void onDestroy() {
        page.removeAllViews();
        for (WebView tab : tabs) {
            tab.destroy();
        }
        tabs.clear();
        super.onDestroy();
    }
}
