package io.github.ae6820dc.web4062;

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
        Bundle result = new Bundle();
        try {
            verifyRefresh();
            result.putString("stream", "\nActive-tab reload, scrolling and zoom checks passed.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", android.util.Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void verifyRefresh() {
        Intent intent = new Intent(getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = startActivitySync(intent);
        WebView[] tabs = new WebView[2];
        AtomicInteger firstLoads = new AtomicInteger();
        AtomicInteger secondLoads = new AtomicInteger();
        runOnMainSync(() -> {
            ViewGroup content = activity.findViewById(android.R.id.content);
            ViewGroup root = (ViewGroup) content.getChildAt(0);
            ViewGroup toolbar = (ViewGroup) root.getChildAt(1);
            ViewGroup page = (ViewGroup) root.getChildAt(2);
            tabs[0] = (WebView) page.getChildAt(0);
            loadFixture(tabs[0], FIRST, firstLoads);
            ((Button) toolbar.getChildAt(1)).performClick();
            tabs[1] = (WebView) page.getChildAt(0);
            check(tabs[0] != tabs[1], "New tab must use a separate WebView");
            loadFixture(tabs[1], SECOND, secondLoads);
        });
        awaitLoads("first tab", firstLoads, 1);
        awaitLoads("second tab", secondLoads, 1);
        WebView active = tabs[1];
        waitForIdleSync();

        swipe(active, 0.30f, 0.58f);
        awaitLoads("active tab after pull", secondLoads, 2);
        check(firstLoads.get() == 1, "Background tab was reloaded");

        runOnMainSync(() -> active.scrollTo(0, 900));
        swipe(active, 0.30f, 0.58f);
        SystemClock.sleep(500);
        check(secondLoads.get() == 2, "A pull inside a scrolled page reloaded it");
        runOnMainSync(() -> check(active.getSettings().getBuiltInZoomControls() && active.zoomIn(),
                "WebView zoom is unavailable"));
    }

    private static void loadFixture(WebView tab, String url, AtomicInteger loads) {
        tab.setWebViewClient(new WebViewClient() {
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
        int[] location = new int[2];
        int[] size = new int[2];
        runOnMainSync(() -> {
            tab.getLocationOnScreen(location);
            size[0] = tab.getWidth();
            size[1] = tab.getHeight();
        });
        float x = location[0] + size[0] / 2f;
        long down = SystemClock.uptimeMillis();
        for (int step = 0; step <= 12; step++) {
            if (step > 0) {
                SystemClock.sleep(16);
            }
            int action = step == 0 ? MotionEvent.ACTION_DOWN
                    : step == 12 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
            float y = location[1] + size[1] * (from + (to - from) * step / 12);
            MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(),
                    action, x, y, 0);
            event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
            try {
                sendPointerSync(event);
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
