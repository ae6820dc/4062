package com.example.mitnews;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.content.Intent;
import android.net.Uri;
public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle b){super.onCreate(b);web=new WebView(this);setContentView(web);
 WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setLoadWithOverviewMode(true);s.setUseWideViewPort(true);s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
 web.setWebChromeClient(new WebChromeClient());web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);view.evaluateJavascript("(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.setAttribute('content','width=device-width, initial-scale=1, minimum-scale=0.25, maximum-scale=8, user-scalable=yes');})();",null);}  @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);view.evaluateJavascript("(function(){var d=document;function enable(){var m=d.querySelector('meta[name=\"viewport\"]');if(!m){m=d.createElement('meta');m.name='viewport';(d.head||d.documentElement).appendChild(m);}var v=m.getAttribute('content')||'';v=v.replace(/(?:^|,)\\\\s*(?:user-scalable|maximum-scale|minimum-scale)\\\\s*=\\\\s*[^,]*/gi,'');m.setAttribute('content',v+', user-scalable=yes, maximum-scale=8.0, minimum-scale=0.25');}enable();if(!window.__zoomObserver){window.__zoomObserver=new MutationObserver(function(){var m=d.querySelector('meta[name=\"viewport\"]');if(m&&!/user-scalable=yes/.test(m.content))enable();});window.__zoomObserver.observe(d.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['content']});}})();",null);} @Override public boolean shouldOverrideUrlLoading(WebView view,String url){if(url.startsWith("http://")||url.startsWith("https://"))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}return true;}});
 web.loadUrl("https://news.mit.edu/");}
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
