package kr.co.minutevoice;
import android.content.*;
public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        int id=i.getIntExtra("id",-1);if(!AlarmStore.exists(c,id))return;
        if("disable".equals(i.getAction())){Scheduler.disable(c,id);return;}
        if("stop_occurrence".equals(i.getAction())){Scheduler.stopOccurrence(c,id,i.getLongExtra("target",0));return;}
        var p = Scheduler.prefs(c,id);
        long target = i.getLongExtra("target",0), tick = i.getLongExtra("tick",0);
        if (!p.getBoolean("enabled",false) || target != p.getLong("target",0) || tick != p.getLong("next",0)) return;
        if (!AlarmLogic.eligible(java.time.Instant.ofEpochMilli(target).atZone(java.time.ZoneId.systemDefault()).toLocalDate(), p.getInt("weekdays",127), p.getBoolean("skipHolidays",false))) { Scheduler.schedule(c,id,true); return; }
        long delay = System.currentTimeMillis()-tick;
        long end=target+p.getInt("after",10)*60000L;
        boolean timely=delay>=0&&delay<60000;
        if(timely)AlarmNotifications.show(c,id,target,end);
        else if(System.currentTimeMillis()>=end)AlarmNotifications.finish(c,id,target);
        Scheduler.schedule(c,id, false);
        if (timely) {
            String text=AlarmLogic.phrase(p.getString("title",""),p.getInt("hour",7),p.getInt("minute",0),(tick-target)/60000);
            if(tick<=target)text=AlarmLogic.withBriefings(text,BriefingReceiver.weatherSpoken(c,id,target),tick==target?BriefingReceiver.newsSpoken(c,id,target):"");
            boolean first=p.getLong("announcedStartTarget",0)!=target;
            String soundUri=tick==target?p.getString("soundUri",""):"";
            boolean last=tick>=end, endAfterSound=last&&!soundUri.isEmpty();
            text=AlarmLogic.withWindowMessages(text,first,last&&!endAfterSound);
            if(first)p.edit().putLong("announcedStartTarget",target).apply();
            c.startForegroundService(new Intent(c,VoiceService.class).putExtra("id",id).putExtra("target",target).putExtra("last",last).putExtra("endAfterSound",endAfterSound).putExtra("soundUri",soundUri).putExtra("text",text));
        }
    }
}
