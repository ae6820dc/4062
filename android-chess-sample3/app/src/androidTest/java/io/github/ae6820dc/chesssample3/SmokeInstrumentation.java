package io.github.ae6820dc.chesssample3;
import android.app.*;
import android.os.*;
import android.content.Intent;
import android.view.*;
import java.util.concurrent.*;
public class SmokeInstrumentation extends Instrumentation {
 MainActivity a;
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 String js(String s)throws Exception{ArrayBlockingQueue<String> q=new ArrayBlockingQueue<>(1);runOnMainSync(()->a.web.evaluateJavascript(s,q::add));String v=q.poll(15,TimeUnit.SECONDS);if(v==null)throw new AssertionError("JS timeout");return v;}
 void require(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 void open()throws Exception{
  a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  for(int i=0;i<100;i++){if("true".equals(js("document.readyState==='complete' && document.querySelectorAll('#board .sq').length===64")))return;Thread.sleep(100);}throw new AssertionError("Page load");
 }
 void move(int fr,int fc,int tr,int tc)throws Exception{js("click("+fr+","+fc+");click("+tr+","+tc+")");}
 void dragPawn()throws Exception{
  int[] p=new int[2],width=new int[1];runOnMainSync(()->{a.web.requestFocus();a.web.getLocationOnScreen(p);width[0]=a.web.getWidth();});
  org.json.JSONArray q=new org.json.JSONArray(js("(()=>{let a=document.querySelector('[data-r=\"6\"][data-c=\"4\"]').getBoundingClientRect(),b=document.querySelector('[data-r=\"4\"][data-c=\"4\"]').getBoundingClientRect();return [a.left+a.width/2,a.top+a.height/2,b.left+b.width/2,b.top+b.height/2,innerWidth]})()"));
  float scale=width[0]/(float)q.getDouble(4),x=p[0]+(float)q.getDouble(0)*scale,y=p[1]+(float)q.getDouble(1)*scale,x2=p[0]+(float)q.getDouble(2)*scale,y2=p[1]+(float)q.getDouble(3)*scale;
  long t=SystemClock.uptimeMillis();
  MotionEvent e=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(e);e.recycle();
  for(int i=1;i<=12;i++){Thread.sleep(20);e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE,x+(x2-x)*i/12,y+(y2-y)*i/12,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(e);e.recycle();}
  Thread.sleep(20);e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x2,y2,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(e);e.recycle();Thread.sleep(200);
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  open();Thread.sleep(500);
  require("Chess".contentEquals(getTargetContext().getApplicationInfo().loadLabel(getTargetContext().getPackageManager())),"App name");
  require("true".equals(js("document.querySelector('header').textContent.includes('76EA45A82D24E05C2F9A6DE5B2A0A0C4')")),"Original ID");
  require("true".equals(js("getComputedStyle(document.body).backgroundColor==='rgb(0, 26, 51)' && document.querySelectorAll('button').length===6")),"Original theme and controls");
  dragPawn();
  require("true".equals(js("b[4][4]?.t==='p' && b[4][4].c==='w' && b[6][4]===null && turn==='b'")),"Native pawn drag");
  move(1,4,3,4);
  require("true".equals(js("b[3][4]?.c==='b' && turn==='w'")),"Black pawn move");
  js("flip();saveGame()");
  require("true".equals(js("document.querySelector('#board .sq').dataset.r==='7'")),"Flip board");
  String state=js("localStorage.getItem('a6164a94dc5fcfaa')");
  runOnMainSync(()->a.finish());waitForIdleSync();open();
  require(state.equals(js("localStorage.getItem('a6164a94dc5fcfaa')")),"Saved state on reopen");
  require("true".equals(js("b[4][4]?.c==='w' && b[3][4]?.c==='b' && flipped")),"Restored pieces and orientation");
  js("newGame()");
  require("true".equals(js("turn==='w' && b.flat().filter(Boolean).length===32 && !over")),"New Game");
  move(6,5,5,5);move(1,4,3,4);move(6,6,4,6);move(0,3,4,7);
  require("true".equals(js("over==='Black wins!' && gameCounter===1 && document.getElementById('game-counter').textContent==='1'")),"Checkmate and game counter");
  runOnMainSync(()->a.finish());waitForIdleSync();open();
  require("true".equals(js("over==='Black wins!' && gameCounter===1")),"Finished game persistence");
  result.putString("stream","PASS: Android 16 offline launch, original ID/theme, 64 squares, native pawn drag, legal moves, Flip Board, New Game, checkmate, game counter and saved state on reopen.\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}