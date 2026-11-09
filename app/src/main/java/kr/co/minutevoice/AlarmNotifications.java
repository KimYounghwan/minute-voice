package kr.co.minutevoice;

import android.app.*;
import android.content.*;
import android.net.Uri;
import java.util.Locale;

/** An alarm's notification survives the short speech service between minute ticks. */
final class AlarmNotifications {
    static final String CHANNEL="alarm_window";
    static int notificationId(int id){return 10_000+id;}
    static void show(Context c,int id,long target,long end){
        SharedPreferences p=Scheduler.prefs(c,id);
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,"진행 중인 알람",NotificationManager.IMPORTANCE_LOW));
        if(p.getLong("visibleTarget",0)==target&&p.getLong("visibleEnd",0)==end){
            for(android.service.notification.StatusBarNotification active:manager.getActiveNotifications())
                if(active.getId()==notificationId(id)&&active.getTag()==null)return;
        }
        String title=p.getString("title","").trim();
        String time=String.format(Locale.KOREA,"%02d:%02d",p.getInt("hour",7),p.getInt("minute",0));
        String display=title.isEmpty()?time+" · 알람 진행 중":title+" · "+time;
        p.edit().putLong("visibleTarget",target).putLong("visibleEnd",end).putString("visibleText",display).apply();
        manager.cancel("alarm_window",id); // Remove notifications created by older versions.
        manager.notify(notificationId(id),build(c,id,target,end,display));
    }
    static Notification current(Context c,int id,long target){
        SharedPreferences p=Scheduler.prefs(c,id);
        return build(c,id,target,p.getLong("visibleEnd",target),p.getString("visibleText","알람 진행 중"));
    }
    static void keepAfterSpeech(Context c,int id,long target){
        if(Scheduler.prefs(c,id).getLong("visibleTarget",0)==target)
            c.getSystemService(NotificationManager.class).notify(notificationId(id),current(c,id,target));
    }
    static void updateWeather(Context c,int id,long target){
        SharedPreferences p=Scheduler.prefs(c,id);
        if(p.getLong("visibleTarget",0)!=target||System.currentTimeMillis()>p.getLong("visibleEnd",0))return;
        c.getSystemService(NotificationManager.class).notify(notificationId(id),current(c,id,target));
    }
    static String weatherText(SharedPreferences p,long target){
        if(!p.getBoolean("hasLocation",false))return "현재 도시 · 위치 미설정";
        String city=p.getString("cityName","").trim();
        if(city.isEmpty())city="현재 위치";
        if(p.getLong("currentWeatherTarget",0)==target&&System.currentTimeMillis()-p.getLong("currentWeatherFetchedAt",0)<30*60_000L){
            String weather=p.getString("currentWeatherText","");
            if(!weather.isEmpty())return city+" · "+weather;
        }
        return city+(p.getLong("currentWeatherAttemptedTarget",0)==target?" · 날씨 확인 불가":" · 날씨 확인 중");
    }
    private static Notification build(Context c,int id,long target,long end,String text){
        Intent stop=new Intent(c,AlarmReceiver.class).setAction("stop_occurrence").setData(Uri.parse("minutevoice://stop/"+id+"/"+target)).putExtra("id",id).putExtra("target",target);
        String weather=weatherText(Scheduler.prefs(c,id),target);
        return new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(text).setContentText(weather).setStyle(new Notification.BigTextStyle().bigText(text+"\n"+weather))
            .setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_ALARM)
            .setTimeoutAfter(Math.max(1000,end+60000-System.currentTimeMillis()))
            .addAction(new Notification.Action.Builder(null,"알람 중지",PendingIntent.getBroadcast(c,id,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build()).build();
    }
    static void clear(Context c,int id){
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        manager.cancel("alarm_window",id);
        manager.cancel(notificationId(id));
        Scheduler.prefs(c,id).edit().remove("visibleTarget").remove("visibleEnd").remove("visibleText").apply();
    }
    static void finish(Context c,int id,long target){if(Scheduler.prefs(c,id).getLong("visibleTarget",0)==target)clear(c,id);}
    static void restore(Context c,int id){
        var p=Scheduler.prefs(c,id);long target=p.getLong("visibleTarget",0),end=p.getLong("visibleEnd",0);
        if(target==0||VoiceService.hasSpeech(id,target))return;
        if(!p.getBoolean("enabled",false)||System.currentTimeMillis()>end){clear(c,id);return;}
        show(c,id,target,end);
    }
}
