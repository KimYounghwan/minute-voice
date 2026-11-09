package kr.co.minutevoice;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.Bitmap;import android.graphics.Canvas;import android.widget.ScrollView;import java.io.*;
/** Creates isolated theme samples in the test emulator only. */
public class ThemePreviewRunner extends Instrumentation {
 boolean playMode;
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle result=new Bundle();try{
 Context c=getTargetContext();for(int id:AlarmStore.ids(c))AlarmStore.delete(c,id);
 for(int n=0;n<3;n++){int id=AlarmStore.allocate(c);AlarmStore.prefs(c,id).edit().putInt("hour",7+n).putInt("minute",n==0?0:30).putString("title",new String[]{"출근 준비","아침 운동","하루 시작"}[n]).putInt("weekdays",n==2?127:62).putInt("before",20).putInt("after",10).putBoolean("enabled",n!=1).putBoolean("skipHolidays",true).commit();AlarmStore.register(c,id);}
 for(int theme=playMode?5:0;theme<6;theme++){
 c.getSharedPreferences("appearance",0).edit().putInt("theme",theme).commit();
 String prefix=playMode?"play":"theme-"+theme;
 Activity list=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();Thread.sleep(700);capture(c,prefix+"-list.png");runOnMainSync(list::finish);waitForIdleSync();
 int id=AlarmStore.ids(c).get(0);Activity edit=startActivitySync(new Intent(c,AlarmEditActivity.class).putExtra("id",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();Thread.sleep(500);capture(c,prefix+"-edit.png");
 runOnMainSync(()->{ScrollView scroll=findScroll(edit.getWindow().getDecorView());if(scroll!=null)scroll.scrollTo(0,Math.round(530*edit.getResources().getDisplayMetrics().density));});waitForIdleSync();Thread.sleep(300);capture(c,prefix+"-days.png");
 runOnMainSync(edit::finish);waitForIdleSync();
 }
 result.putString("stream",(playMode?"3 Play Store":"18 theme sample")+" screens captured\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("stream",android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}}
 void capture(Context c,String name)throws Exception{Bitmap bitmap=getUiAutomation().takeScreenshot();Bitmap rgb=Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),Bitmap.Config.RGB_565);new Canvas(rgb).drawBitmap(bitmap,0,0,null);try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),name))){rgb.compress(Bitmap.CompressFormat.PNG,100,out);}rgb.recycle();bitmap.recycle();}
 ScrollView findScroll(android.view.View v){if(v instanceof ScrollView)return (ScrollView)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++){ScrollView found=findScroll(group.getChildAt(i));if(found!=null)return found;}}return null;}
}
