package kr.co.minutevoice;
import android.content.*;
public class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        boolean timeChanged = Intent.ACTION_TIME_CHANGED.equals(i.getAction()) || Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction());
        Scheduler.restoreAll(c,timeChanged);
    }
}
