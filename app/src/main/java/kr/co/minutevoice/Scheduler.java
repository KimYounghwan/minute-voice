package kr.co.minutevoice;
import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.*;
public final class Scheduler {
    static SharedPreferences prefs(Context c,int id) { return AlarmStore.prefs(c,id); }
    static boolean allowed(Context c) { return Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    static PendingIntent pending(Context c, int id, long tick, long target) {
        return PendingIntent.getBroadcast(c, id, new Intent(c, AlarmReceiver.class).setData(android.net.Uri.parse("minutevoice://alarm/"+id)).putExtra("id",id).putExtra("tick", tick).putExtra("target", target), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c,int id) { c.getSystemService(AlarmManager.class).cancel(pending(c, id, 0, 0)); }
    static void disable(Context c,int id) { AlarmNotifications.clear(c,id); prefs(c,id).edit().putBoolean("enabled", false).apply(); cancel(c,id); BriefingReceiver.cancel(c,id); VoiceService.cancelAlarm(id); }
    static void stopOccurrence(Context c,int id,long occurrence) {
        SharedPreferences p=prefs(c,id);
        AlarmNotifications.finish(c,id,occurrence);
        VoiceService.cancelAlarm(id);
        // The final announcement may already have scheduled the next occurrence.
        if(occurrence<=0 || p.getLong("target",0)!=occurrence || !p.getBoolean("enabled",false))return;
        cancel(c,id);
        if(!p.getBoolean("repeat",true)){p.edit().putBoolean("enabled",false).apply();return;}
        long target=AlarmLogic.nextTargetAfterStop(System.currentTimeMillis(),occurrence,p.getInt("hour",7),p.getInt("minute",0),ZoneId.systemDefault(),p.getInt("weekdays",127),p.getBoolean("skipHolidays",false));
        p.edit().putLong("target",target).apply();
        BriefingReceiver.schedule(c,id,target);
        arm(c,id,AlarmLogic.nextTick(System.currentTimeMillis(),target,p.getInt("before",20)),target);
    }
    static void restoreAll(Context c,boolean fresh){AlarmStore.migrate(c);for(int id:AlarmStore.ids(c)){if(fresh)AlarmNotifications.clear(c,id);else AlarmNotifications.restore(c,id);schedule(c,id,fresh);}}
    static void arm(Context c, int id, long tick, long target) {
        if (!allowed(c)) return;
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        c.getSystemService(AlarmManager.class).setAlarmClock(new AlarmManager.AlarmClockInfo(tick, show), pending(c, id, tick, target));
        prefs(c,id).edit().putLong("next", tick).apply();
    }
    static void schedule(Context c, int id, boolean fresh) {
        SharedPreferences p = prefs(c,id);
        if (!p.getBoolean("enabled", false)) return;
        long now = System.currentTimeMillis();
        long target = p.getLong("target", 0);
        int weekdays = p.getInt("weekdays", 127);
        boolean skipHolidays = p.getBoolean("skipHolidays", false);
        if ((weekdays & 127) == 0) { disable(c,id); return; }
        if (target != 0 && !AlarmLogic.eligible(Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault()).toLocalDate(), weekdays, skipHolidays)) fresh = true;
        if (fresh || target == 0) target = AlarmLogic.nextTarget(now, p.getInt("hour",7), p.getInt("minute",0), ZoneId.systemDefault(), weekdays, skipHolidays);
        long tick = AlarmLogic.nextTick(now, target, p.getInt("before",20));
        if (tick > target + p.getInt("after",10)*60000L) {
            if (!p.getBoolean("repeat", true)) { p.edit().putBoolean("enabled",false).apply(); cancel(c,id); return; }
            target = AlarmLogic.nextTarget(now, p.getInt("hour",7), p.getInt("minute",0), ZoneId.systemDefault(), weekdays, skipHolidays);
            tick = AlarmLogic.nextTick(now, target, p.getInt("before",20));
        }
        p.edit().putLong("target", target).apply();
        arm(c, id, tick, target);
        BriefingReceiver.schedule(c,id,target);
    }
}
