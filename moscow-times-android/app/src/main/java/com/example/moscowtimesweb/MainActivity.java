package com.example.moscowtimesweb;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;
public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle b) {
 super.onCreate(b); web=new WebView(this); setContentView(web);
 WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true); s.setSupportZoom(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
 CookieManager.getInstance().setAcceptCookie(true);
 web.setWebChromeClient(new WebChromeClient());
 web.setWebViewClient(new WebViewClient() {
 @Override public void onPageFinished(WebView v,String url) {
 super.onPageFinished(v,url);
 v.evaluateJavascript("(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';(document.head||document.documentElement).appendChild(m);}m.setAttribute('content','width=device-width, initial-scale=1, minimum-scale=0.25, maximum-scale=8, user-scalable=yes');})();",null);
 }

 @Override public boolean shouldOverrideUrlLoading(WebView v,String url) {
 if(url.startsWith("https://")||url.startsWith("http://")) return false;
 try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));} catch(Exception ignored){}
 return true;
 }
 });
 web.loadUrl("https://ru.themoscowtimes.com/");
 }
 @Override public void onBackPressed() {if(web.canGoBack())web.goBack();else super.onBackPressed();}
}