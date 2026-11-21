package com.example.moscowtimesweb;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshTestRunner extends Instrumentation {
    private static final String FIRST = "https://refresh.test/first";
    private static final String SECOND = "https://refresh.test/second";
    private static final byte[] PAGE = ("<!doctype html>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<main style='height:3500px'>Refresh test</main>").getBytes(StandardCharsets.UTF_8);

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle status = new Bundle();
        status.putString("id", "RefreshTestRunner");
        status.putString("class", getClass().getName());
        status.putString("test", "verifyRefresh");
        status.putInt("numtests", 1);
        status.putInt("current", 1);
        sendStatus(1, status);
        Bundle result = new Bundle();
        try {
            verifyDownloadAddress();
            verifyRefresh();
            result.putString("stream", "\nActive-tab reload, scrolling and zoom checks passed.\n");
            sendStatus(0, status);
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            String trace = android.util.Log.getStackTraceString(failure);
            status.putString("stack", trace);
            sendStatus(-2, status);
            result.putString("stream", trace);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private static void verifyDownloadAddress() {
        String page = "https://github.com/ae6820dc/4062/blob/main/apk/8/test%20file.apk";
        String raw = "https://github.com/ae6820dc/4062/raw/main/apk/8/test%20file.apk";
        check(raw.equals(WebDownload.resolveAddress("blob:https://github.com/123", page)),
                "GitHub blob must use the repository file download");
        check(raw.equals(WebDownload.resolveAddress(raw, page)),
                "Existing HTTPS downloads must remain unchanged");
        check(WebDownload.resolveAddress("blob:https://example.org/123", page) == null,
                "An unrelated site's blob must be rejected");
        check(WebDownload.resolveAddress("blob:https://github.com/123",
                "https://example.org/owner/repo/blob/main/file.apk") == null,
                "The page must be a GitHub repository file");
    }

    private void verifyRefresh() {
        Intent intent = new Intent(getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = startActivitySync(intent);
        WebView[] tabs = new WebView[2];
        AtomicInteger firstLoads = new AtomicInteger();
        AtomicInteger secondLoads = new AtomicInteger();
        AtomicInteger secondFinishes = new AtomicInteger();
        runOnMainSync(() -> {
            ViewGroup content = activity.findViewById(android.R.id.content);
            ViewGroup root = (ViewGroup) content.getChildAt(0);
            ViewGroup toolbar = (ViewGroup) root.getChildAt(1);
            ViewGroup page = (ViewGroup) root.getChildAt(2);
            tabs[0] = (WebView) page.getChildAt(0);
            loadFixture(tabs[0], FIRST, firstLoads, new AtomicInteger());
            ((Button) toolbar.getChildAt(1)).performClick();
            tabs[1] = (WebView) page.getChildAt(0);
            check(tabs[0] != tabs[1], "New tab must use a separate WebView");
            loadFixture(tabs[1], SECOND, secondLoads, secondFinishes);
        });
        awaitLoads("first tab", firstLoads, 1);
        awaitLoads("second tab", secondLoads, 1);
        awaitLoads("second tab rendered", secondFinishes, 1);
        WebView active = tabs[1];
        waitForIdleSync();

        swipe(active, 0.30f, 0.58f);
        awaitLoads("active tab after pull", secondLoads, 2);
        awaitLoads("active tab rendered after pull", secondFinishes, 2);
        check(firstLoads.get() == 1, "Background tab was reloaded");

        swipe(active, 0.78f, 0.25f);
        SystemClock.sleep(750);
        runOnMainSync(() -> check(active.getScrollY() > 0,
                "An upward swipe must scroll the fixture"));
        check(secondLoads.get() == 2, "An upward swipe unexpectedly reloaded the fixture");

        swipe(active, 0.30f, 0.58f);
        SystemClock.sleep(500);
        check(secondLoads.get() == 2,
                "A pull inside a scrolled page reloaded it (" + secondLoads.get() + " requests)");
        runOnMainSync(() -> check(active.getSettings().getBuiltInZoomControls() && active.zoomIn(),
                "WebView zoom is unavailable"));
    }

    private static void loadFixture(WebView tab, String url, AtomicInteger loads,
                                    AtomicInteger finishes) {
        tab.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String finishedUrl) {
                if (url.equals(finishedUrl)) {
                    finishes.incrementAndGet();
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!url.equals(request.getUrl().toString())) {
                    return null;
                }
                loads.incrementAndGet();
                return new WebResourceResponse("text/html", "UTF-8", 200, "OK",
                        Collections.singletonMap("Cache-Control", "no-store"),
                        new ByteArrayInputStream(PAGE));
            }
        });
        tab.loadUrl(url);
    }

    private void swipe(WebView tab, float from, float to) {
        int[] size = new int[2];
        runOnMainSync(() -> {
            size[0] = tab.getWidth();
            size[1] = tab.getHeight();
        });
        float x = size[0] / 2f;
        long down = SystemClock.uptimeMillis();
        for (int step = 0; step <= 12; step++) {
            if (step > 0) {
                SystemClock.sleep(16);
            }
            int action = step == 0 ? MotionEvent.ACTION_DOWN
                    : step == 12 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
            float y = size[1] * (from + (to - from) * step / 12);
            MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(),
                    action, x, y, 0);
            event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
            try {
                runOnMainSync(() -> tab.dispatchTouchEvent(event));
            } finally {
                event.recycle();
            }
        }
    }

    private static void awaitLoads(String label, AtomicInteger loads, int expected) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (loads.get() < expected && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50);
        }
        check(loads.get() == expected, label + " expected " + expected + " requests, saw " + loads.get());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
