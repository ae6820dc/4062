package io.github.ae6820dc.web4062;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Insets;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;
public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  web = new WebView(this);
  FrameLayout content = new FrameLayout(this);
  content.addView(web);
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
   content.setOnApplyWindowInsetsListener((view, windowInsets) -> {
    Insets insets = windowInsets.getInsets(
      WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
    view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
    return windowInsets;
   });
  }
  setContentView(content);
  WebSettings s=web.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setLoadWithOverviewMode(true);
  s.setUseWideViewPort(true);
  s.setSupportZoom(true);
  s.setBuiltInZoomControls(true);
  s.setDisplayZoomControls(false);
  CookieManager.getInstance().setAcceptCookie(true);
  web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){
    super.onPageFinished(view,url);
    view.evaluateJavascript("(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';(document.head||document.documentElement).appendChild(m);}m.setAttribute('content','width=device-width, initial-scale=1, minimum-scale=0.25, maximum-scale=8, user-scalable=yes');})();",null);
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,String url){
    if(url.startsWith("https://")||url.startsWith("http://"))return false;
    try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}
    return true;
   }
  });
  if (b == null || web.restoreState(b) == null) {
   web.loadUrl("https://ae6820dc.github.io/4062/index.html");
  }
 }
 @Override protected void onSaveInstanceState(Bundle state) {
  web.saveState(state);
  super.onSaveInstanceState(state);
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
