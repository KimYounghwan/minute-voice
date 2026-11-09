package kr.co.minutevoice;
import android.content.*;
import java.util.*;

public final class AlarmStore {
    static SharedPreferences index(Context c){return c.getSharedPreferences("alarm_index",0);}
    static SharedPreferences prefs(Context c,int id){return c.getSharedPreferences("alarm_"+id,0);}
    static synchronized void migrate(Context c){
        if(index(c).getBoolean("migrated",false))return;
        SharedPreferences old=c.getSharedPreferences("alarm",0);
        if(old.contains("hour")){
            SharedPreferences.Editor e=prefs(c,1).edit();
            for(var entry:old.getAll().entrySet()){
                Object v=entry.getValue();String k=entry.getKey();
                if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof String)e.putString(k,(String)v);
            }
            e.commit();
            index(c).edit().putStringSet("ids",new HashSet<>(Arrays.asList("1"))).putInt("nextId",2).putBoolean("migrated",true).commit();
        }else index(c).edit().putBoolean("migrated",true).commit();
        c.getSystemService(android.app.AlarmManager.class).cancel(android.app.PendingIntent.getBroadcast(c,1,new Intent(c,AlarmReceiver.class),android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE));
    }
    static List<Integer> ids(Context c){migrate(c);List<Integer> ids=new ArrayList<>();for(String s:index(c).getStringSet("ids",Collections.emptySet()))ids.add(Integer.parseInt(s));Collections.sort(ids);return ids;}
    static boolean exists(Context c,int id){return ids(c).contains(id);}
    static synchronized int allocate(Context c){migrate(c);int id=index(c).getInt("nextId",1);index(c).edit().putInt("nextId",id+1).commit();return id;}
    static synchronized void register(Context c,int id){Set<String> ids=new HashSet<>(index(c).getStringSet("ids",Collections.emptySet()));ids.add(String.valueOf(id));index(c).edit().putStringSet("ids",ids).commit();}
    static synchronized void delete(Context c,int id){Scheduler.disable(c,id);Set<String> ids=new HashSet<>(index(c).getStringSet("ids",Collections.emptySet()));ids.remove(String.valueOf(id));index(c).edit().putStringSet("ids",ids).commit();prefs(c,id).edit().clear().commit();}
}
