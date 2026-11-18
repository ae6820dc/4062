package com.example.alhadathweb;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;
public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle b) {
 super.onCreate(b); web=new WebView(this); setContentView(web);
 WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true);
 CookieManager.getInstance().setAcceptCookie(true);
 web.setWebChromeClient(new WebChromeClient());
 web.setWebViewClient(new WebViewClient() {
 @Override public boolean shouldOverrideUrlLoading(WebView v,String url) {
 if(url.startsWith("https://")||url.startsWith("http://")) return false;
 try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));} catch(Exception ignored){}
 return true;
 }
 });
 web.loadUrl("https://www.alhadath.net/");
 }
 @Override public void onBackPressed() {if(web.canGoBack())web.goBack();else super.onBackPressed();}
}