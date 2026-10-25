package io.github.ae6820dc.studio;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Installed only on the disposable CI emulator. Checks every discovered program. */
public final class SmokeInstrumentation extends Instrumentation {
    private Bundle args;
    private Activity activity;
    private WebView web;
    private int checks;
    @Override public void onCreate(Bundle arguments){args=arguments;start();}
    private void require(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private String eval(String js) throws Exception {
        CountDownLatch latch=new CountDownLatch(1);String[] answer={null};
        runOnMainSync(()->web.evaluateJavascript(js,v->{answer[0]=v;latch.countDown();}));
        if(!latch.await(30,TimeUnit.SECONDS))throw new AssertionError("JavaScript timeout");
        return answer[0];
    }
    private void waitFor(String js) throws Exception {
        long deadline=System.currentTimeMillis()+30000;
        while(System.currentTimeMillis()<deadline){if("true".equals(eval(js)))return;Thread.sleep(100);}
        throw new AssertionError("Page not ready: "+js);
    }
    private ListView list(View v){
        if(v instanceof ListView)return (ListView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){ListView l=list(((ViewGroup)v).getChildAt(i));if(l!=null)return l;}
        return null;
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Intent intent=getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity=startActivitySync(intent);
            Field field=MainActivity.class.getDeclaredField("web");field.setAccessible(true);web=(WebView)field.get(activity);
            Field cf=MainActivity.class.getDeclaredField("programs");cf.setAccessible(true);
            @SuppressWarnings("unchecked") List<JSONObject> catalog=(List<JSONObject>)cf.get(activity);
            JSONArray expected=new JSONArray(new String(getTargetContext().getAssets().open("catalog.json").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            require(!catalog.isEmpty()&&catalog.size()==expected.length(),"complete dynamic catalog");
            require(getTargetContext().checkSelfPermission("android.permission.INTERNET")!=android.content.pm.PackageManager.PERMISSION_GRANTED,"no Internet permission");
            boolean[] settings=new boolean[2];
            runOnMainSync(()->{settings[0]=web.getSettings().getJavaScriptEnabled()&&web.getSettings().getDomStorageEnabled();settings[1]=!web.getSettings().getAllowFileAccess();});
            require(settings[0],"JavaScript and DOM storage enabled");
            require(settings[1],"file access disabled");
            Method open=MainActivity.class.getDeclaredMethod("open",JSONObject.class);open.setAccessible(true);
            Method leave=MainActivity.class.getDeclaredMethod("leave",Runnable.class);leave.setAccessible(true);
            Method home=MainActivity.class.getDeclaredMethod("home");home.setAccessible(true);
            Method menu=MainActivity.class.getDeclaredMethod("menu");menu.setAccessible(true);
            runOnMainSync(()->{try{menu.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});
            boolean[] matched={false};
            runOnMainSync(()->{for(View root:android.view.inspector.WindowInspector.getGlobalWindowViews()){
                ListView l=list(root);if(l!=null){boolean ok=l.getCount()==catalog.size();for(int i=0;ok&&i<catalog.size();i++)ok=String.valueOf(l.getAdapter().getItem(i)).equals(catalog.get(i).optString("group")+" · "+catalog.get(i).optString("name"));matched[0]|=ok;}
                View cancel=root.findViewById(android.R.id.button2);if(cancel!=null)cancel.performClick();
            }});
            require(matched[0],"switch menu lists every sample");
            boolean restore="restore".equals(args.getString("phase"));
            for(JSONObject p:catalog){
                CountDownLatch switched=new CountDownLatch(1);
                runOnMainSync(()->{try{leave.invoke(activity,(Runnable)()->{try{open.invoke(activity,p);switched.countDown();}catch(Exception e){throw new RuntimeException(e);}});}catch(Exception e){throw new RuntimeException(e);}});
                require(switched.await(30,TimeUnit.SECONDS),"switch completed");
                String host=p.getString("id")+".appassets.androidplatform.net";
                waitFor("location.hostname==="+JSONObject.quote(host)+" && document.querySelectorAll('#board .sq').length===64 && typeof studioExport==='function'");
                require("true".equals(eval("document.querySelector('header code').textContent==="+JSONObject.quote(p.getString("programId")))),"original Program ID "+p.getString("name"));
                String key=JSONObject.quote(p.getString("storageKey"));
                if(restore){
                    require("true".equals(eval("b[4][4]?.t==='p' && b[4][4]?.c==='w' && b[6][4]===null && turn==='b' && flipped===true")),"saved board/turn/flip after process restart "+p.getString("name"));
                    require("true".equals(eval("localStorage.getItem('__ci_origin')==="+JSONObject.quote(p.getString("id")))),"isolated persistent origin "+p.getString("name"));
                    require("true".equals(eval("JSON.parse(localStorage.getItem("+key+")).flipped===true")),"original storage key retained");
                }else{
                    require("true".equals(eval("b.flat().filter(Boolean).length===32 && turn==='w'")),"initial board");
                    eval("document.querySelector('[data-r=\"6\"][data-c=\"4\"]').click();document.querySelector('[data-r=\"4\"][data-c=\"4\"]').click();");
                    require("true".equals(eval("b[4][4]?.t==='p' && b[6][4]===null && turn==='b'")),"legal e2-e4 via square taps");
                    require("true".equals(eval("JSON.parse(localStorage.getItem("+key+")).turn==='b'")),"original move autosave");
                    eval("flip();localStorage.setItem('__ci_origin',"+JSONObject.quote(p.getString("id"))+");");
                    require("true".equals(eval("flipped && document.querySelector('#board .sq').dataset.r==='7'")),"flip interaction");
                }
                require("true".equals(eval("Object.keys(localStorage).every(k=>k==="+key+" || k==='__ci_origin')")),"no storage keys from other programs");
                System.out.println("PASS "+(restore?"restore ":"seed ")+p.getString("name"));
            }
            CountDownLatch done=new CountDownLatch(1);
            runOnMainSync(()->{try{leave.invoke(activity,(Runnable)()->{try{home.invoke(activity);done.countDown();}catch(Exception e){throw new RuntimeException(e);}});}catch(Exception e){throw new RuntimeException(e);}});
            require(done.await(30,TimeUnit.SECONDS),"save before Home");
            result.putString("stream","PASS "+checks+" checks across "+catalog.size()+" samples ("+(restore?"restore":"seed")+")\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
}
