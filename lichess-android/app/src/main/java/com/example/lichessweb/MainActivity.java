package com.example.lichessweb;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.content.Intent;
import android.net.Uri;
public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  web=new WebView(this);setContentView(web);
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
    if(url.startsWith("http://")||url.startsWith("https://"))return false;
    try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}
    return true;
   }
  });
  web.loadUrl("https://lichess.org/");
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
