package kr.co.minutevoice;
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.speech.tts.*;
import java.util.*;

public class VoiceService extends Service {
    static VoiceService running;
    static final String CHANNEL="voice";
    static final long SPEECH_TIMEOUT=120_000L;
    static class Speech {final boolean last,endAfterSound;final int alarmId;final long target;final String text,soundUri;final String token=UUID.randomUUID().toString();final List<String> parts;int partIndex;String utteranceId;boolean soundFinished,closing;Speech(int id,long target,String text,String soundUri,boolean last,boolean endAfterSound){this.last=last;this.endAfterSound=endAfterSound;alarmId=id;this.target=target;this.text=text;this.parts=splitSpeech(text);this.soundUri=soundUri==null?"":soundUri;}}
    static List<String> splitSpeech(String text){
        List<String> parts=new ArrayList<>();
        if(text==null||text.isEmpty()){parts.add("");return parts;}
        int start=0;
        while(start<text.length()){
            int end=Math.min(start+450,text.length());
            if(end<text.length()){
                int sentence=text.lastIndexOf(". ",end-1);
                int space=text.lastIndexOf(' ',end-1);
                if(sentence>=start+225)end=sentence+2;
                else if(space>=start+225)end=space+1;
            }
            parts.add(text.substring(start,end));start=end;
        }
        return parts;
    }
    final ArrayDeque<Speech> queue=new ArrayDeque<>();Speech current;
    int foregroundAlarmId=-1;long foregroundTarget;boolean foregroundDetached;
    MediaPlayer player;boolean playingSound;
    TextToSpeech tts;boolean ready,voiceAvailable;PowerManager.WakeLock wake;
    Handler handler=new Handler();AudioManager audio;AudioFocusRequest focus;
    static boolean hasSpeech(int id,long target){
        if(running==null)return false;
        Speech s=running.current;if(s!=null&&s.alarmId==id&&s.target==target)return true;
        for(Speech pending:running.queue)if(pending.alarmId==id&&pending.target==target)return true;
        return false;
    }
    static void cancelAlarm(int id){if(running!=null)running.cancel(id);}
    void cancel(int id){queue.removeIf(s->s.alarmId==id);if(current!=null&&current.alarmId==id){tts.stop();releaseSound();current=null;handler.removeCallbacksAndMessages(null);next();}else if(current==null&&queue.isEmpty())stopSelf();}
    @Override public void onCreate(){super.onCreate();running=this;getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"음성 시간 안내",NotificationManager.IMPORTANCE_LOW));wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"minutevoice:speech");wake.setReferenceCounted(false);audio=getSystemService(AudioManager.class);}
    void foreground(Speech speech){
        foregroundAlarmId=speech.alarmId;foregroundTarget=speech.target;foregroundDetached=false;
        startForeground(AlarmNotifications.notificationId(speech.alarmId),AlarmNotifications.current(this,speech.alarmId,speech.target));
    }
    void releaseForeground(){
        if(foregroundDetached)return;
        boolean visible=foregroundAlarmId>=0&&Scheduler.prefs(this,foregroundAlarmId).getLong("visibleTarget",0)==foregroundTarget;
        stopForeground(visible?STOP_FOREGROUND_DETACH:STOP_FOREGROUND_REMOVE);
        foregroundDetached=true;
        if(visible)AlarmNotifications.keepAfterSpeech(this,foregroundAlarmId,foregroundTarget);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||intent.getStringExtra("text")==null){if(current==null&&queue.isEmpty())stopSelf();return START_NOT_STICKY;}
        Speech speech=new Speech(intent.getIntExtra("id",-1),intent.getLongExtra("target",0),intent.getStringExtra("text"),intent.getStringExtra("soundUri"),intent.getBooleanExtra("last",false),intent.getBooleanExtra("endAfterSound",false));queue.add(speech);wake.acquire(SPEECH_TIMEOUT+45000);foreground(current==null?speech:current);
        if(ready)next();else if(tts==null){handler.postDelayed(this::failure,30000);tts=new TextToSpeech(this,status->{
            if(ready)return;
            voiceAvailable=status==TextToSpeech.SUCCESS&&tts.setLanguage(Locale.KOREAN)>=0;
            if(!voiceAvailable){failure();return;}
            tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){handler.post(()->completed(id));}public void onError(String id){handler.post(()->completed(id));}});
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener(change->{}).build();audio.requestAudioFocus(focus);ready=true;handler.removeCallbacksAndMessages(null);next();
        });}
        return START_NOT_STICKY;
    }
    void next(){
        if(!ready||current!=null)return;
        current=queue.poll();if(current==null){releaseForeground();stopSelf();return;}
        foreground(current);speakPart();
    }
    void speakPart(){
        if(current==null)return;
        if(!voiceAvailable){current.partIndex=current.parts.size();completed(current.utteranceId=UUID.randomUUID().toString());return;}
        String part=current.parts.get(current.partIndex++);
        String id=current.utteranceId=UUID.randomUUID().toString();wake.acquire(SPEECH_TIMEOUT+45000);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(()->{if(current!=null&&id.equals(current.utteranceId)){tts.stop();completed(id);}},SPEECH_TIMEOUT);
        if(tts.speak(part,TextToSpeech.QUEUE_FLUSH,null,id)==TextToSpeech.ERROR)completed(id);
    }
    void completed(String id){
        if(current==null||!id.equals(current.utteranceId))return;
        if(playingSound)return;
        handler.removeCallbacksAndMessages(null);
        if(!current.closing&&current.partIndex<current.parts.size()){speakPart();return;}
        String token=current.token;
        if(!current.soundFinished&&!current.soundUri.isEmpty()){playSound(token);return;}
        finishSound(token);
    }
    void releaseSound(){if(player!=null){player.release();player=null;}playingSound=false;}
    void finishSound(String token){
        if(current==null||!current.token.equals(token))return;
        handler.removeCallbacksAndMessages(null);releaseSound();current.soundFinished=true;
        if(current.endAfterSound&&!current.closing){
            current.closing=true;wake.acquire(SPEECH_TIMEOUT+45000);
            String id=current.utteranceId=UUID.randomUUID().toString();
            handler.postDelayed(()->{if(current!=null&&id.equals(current.utteranceId)){tts.stop();completed(id);}},SPEECH_TIMEOUT);
            if(!voiceAvailable||tts.speak("알림을 종료합니다.",TextToSpeech.QUEUE_FLUSH,null,id)==TextToSpeech.ERROR)completed(id);
            return;
        }
        if(current.last)AlarmNotifications.finish(this,current.alarmId,current.target);
        current=null;next();
    }
    void playSound(String token){
        playingSound=true;wake.acquire(45000);
        handler.postDelayed(()->finishSound(token),30000);
        try{
            MediaPlayer media=new MediaPlayer();player=media;
            media.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            media.setDataSource(this,android.net.Uri.parse(current.soundUri));
            media.setOnPreparedListener(m->{if(player==m)m.start();});media.setOnCompletionListener(m->finishSound(token));
            media.setOnErrorListener((m,what,extra)->{soundFailure();finishSound(token);return true;});media.prepareAsync();
        }catch(Exception e){soundFailure();finishSound(token);}
    }
    void soundFailure(){getSystemService(NotificationManager.class).notify(9,new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_alarm).setContentTitle("선택한 알람 소리를 재생할 수 없어요").setContentText("알람 설정에서 소리를 다시 선택해 주세요. 음성 안내는 계속됩니다.").setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).build());}
    void failure(){getSystemService(NotificationManager.class).notify(8,new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_alarm).setContentTitle("한국어 음성을 확인해 주세요").setContentText("휴대폰의 텍스트 음성 변환 설정에서 한국어 음성을 설치해 주세요.").setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).build());voiceAvailable=false;ready=true;handler.removeCallbacksAndMessages(null);next();}
    @Override public void onDestroy(){if(running==this)running=null;releaseForeground();releaseSound();handler.removeCallbacksAndMessages(null);if(tts!=null){tts.stop();tts.shutdown();}if(focus!=null)audio.abandonAudioFocusRequest(focus);if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
