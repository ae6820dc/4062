package io.github.ae6820dc.chesssample3;
import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import android.webkit.*;
public class MainActivity extends Activity {
 WebView web;
 FrameLayout root;
 View custom;
 WebChromeClient.CustomViewCallback customCallback;
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  root=new FrameLayout(this);root.setBackgroundColor(0xff001a33);
  root.setOnApplyWindowInsetsListener((v,wi)->{
   if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=wi.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom);}
   else v.setPadding(wi.getSystemWindowInsetLeft(),wi.getSystemWindowInsetTop(),wi.getSystemWindowInsetRight(),wi.getSystemWindowInsetBottom());
   return wi;
  });
  web=new WebView(this);web.setBackgroundColor(0xff001a33);
  web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
  web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}});
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onShowCustomView(View view,CustomViewCallback callback){
    if(custom!=null){callback.onCustomViewHidden();return;}
    custom=view;customCallback=callback;web.setVisibility(View.GONE);root.addView(view,new FrameLayout.LayoutParams(-1,-1));
   }
   @Override public void onHideCustomView(){closeFullscreen();}
  });
  root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
  if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,this::back);
  web.loadUrl("file:///android_asset/index.html");
 }
 private void closeFullscreen(){if(custom==null)return;root.removeView(custom);custom=null;web.setVisibility(View.VISIBLE);if(customCallback!=null){WebChromeClient.CustomViewCallback c=customCallback;customCallback=null;c.onCustomViewHidden();}}
 private void back(){if(custom!=null){closeFullscreen();return;}web.evaluateJavascript("(()=>{const o=document.getElementById('jumpOverlay');if(o&&o.classList.contains('open')){document.getElementById('jumpCancel').click();return true;}return false;})()",closed->{if(!"true".equals(closed))moveTaskToBack(true);});}
 @Override public void onBackPressed(){back();}
 @Override protected void onPause(){web.evaluateJavascript("if(typeof saveGame==='function')saveGame()",null);web.onPause();web.pauseTimers();super.onPause();}
 @Override protected void onResume(){super.onResume();if(web!=null){web.onResume();web.resumeTimers();}}
 @Override protected void onDestroy(){web.destroy();super.onDestroy();}
}
