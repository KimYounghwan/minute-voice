package kr.co.minutevoice;
import android.app.*;
import android.content.*;
import android.os.Bundle;

/** Run only on a test device: initializes the app's alarm preferences. */
public class MultiAlarmTestRunner extends ThemePreviewRunner {
    int checks;boolean preview,demo;
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){preview=args!=null&&"true".equals(args.getString("preview"));playMode=args!=null&&"true".equals(args.getString("play"));demo=args!=null&&"true".equals(args.getString("demo"));start();}
    @Override public void onStart(){
        if(preview){super.onStart();return;}
        if(demo){
            Bundle result=new Bundle();try{
                Context c=getTargetContext();for(int old:AlarmStore.ids(c))AlarmStore.delete(c,old);
                java.util.Calendar target=java.util.Calendar.getInstance();target.add(java.util.Calendar.MINUTE,1);target.set(java.util.Calendar.SECOND,0);target.set(java.util.Calendar.MILLISECOND,0);
                int id=AlarmStore.allocate(c);AlarmStore.prefs(c,id).edit().putInt("hour",target.get(java.util.Calendar.HOUR_OF_DAY)).putInt("minute",target.get(java.util.Calendar.MINUTE)).putString("title","알람 시연").putInt("weekdays",127).putInt("before",0).putInt("after",0).putBoolean("repeat",false).putBoolean("skipHolidays",false).putBoolean("enabled",true).commit();AlarmStore.register(c,id);Scheduler.schedule(c,id,true);AlarmStore.prefs(c,id).edit().commit();
                runOnMainSync(()->c.startActivity(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                result.putString("stream","Demo alarm scheduled for "+new java.util.Date(target.getTimeInMillis())+"\n");finish(Activity.RESULT_OK,result);
            }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}return;
        }
        Bundle result=new Bundle();
        try{
            Context c=getTargetContext();
            for(int id:AlarmStore.ids(c))AlarmStore.delete(c,id);
            c.getSharedPreferences("alarm_index",0).edit().clear().commit();
            c.getSharedPreferences("alarm",0).edit().clear().putInt("hour",7).putInt("minute",15).putString("title","기존 알람").putInt("weekdays",62).putBoolean("enabled",true).commit();
            Scheduler.restoreAll(c,false);
            check(AlarmStore.ids(c).size()==1,"legacy alarm migrated");
            check(AlarmStore.prefs(c,1).getString("title","").equals("기존 알람"),"title retained");
            check(AlarmStore.prefs(c,1).getInt("minute",0)==15,"time retained");
            check(AlarmStore.prefs(c,1).getInt("weekdays",0)==62,"weekdays retained");
            check(AlarmStore.prefs(c,1).getBoolean("enabled",false),"activation retained");
            AlarmStore.migrate(c);check(AlarmStore.ids(c).size()==1,"migration idempotent");
            int id=AlarmStore.allocate(c);check(id!=1,"unique id");
            AlarmStore.prefs(c,id).edit().putInt("hour",7).putInt("minute",15).putInt("weekdays",127).putBoolean("enabled",true).commit();AlarmStore.register(c,id);Scheduler.schedule(c,id,true);
            check(AlarmStore.ids(c).size()==2,"two alarms saved");
            check(!Scheduler.pending(c,1,0,0).equals(Scheduler.pending(c,id,0,0)),"independent pending intents at same time");
            // Restore the extras modified by the identity check.
            Scheduler.restoreAll(c,false);
            long secondTarget=AlarmStore.prefs(c,id).getLong("target",0);
            check(secondTarget>System.currentTimeMillis(),"second alarm scheduled");
            if(android.os.Build.VERSION.SDK_INT<33||c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED){
                long notificationEnd=secondTarget+600_000;
                AlarmNotifications.show(c,id,secondTarget,notificationEnd);
                waitForAlarmNotification(c,id);
                check(activeAlarmNotifications(c,id)==1,"alarm window has one notification");
                AlarmNotifications.show(c,id,secondTarget,notificationEnd);
                waitForAlarmNotification(c,id);
                check(activeAlarmNotifications(c,id)==1,"minute tick does not duplicate the notification");
                check(AlarmNotifications.current(c,id,secondTarget).extras.getString(android.app.Notification.EXTRA_TITLE,"").contains("07:15"),"speech service uses stable alarm time title");
                AlarmStore.prefs(c,id).edit().putBoolean("hasLocation",true).putString("cityName","서울").putLong("currentWeatherTarget",secondTarget).putLong("currentWeatherFetchedAt",System.currentTimeMillis()).putString("currentWeatherText","맑음 · 18°C").commit();
                AlarmNotifications.updateWeather(c,id,secondTarget);
                waitForAlarmNotification(c,id);
                check(AlarmNotifications.current(c,id,secondTarget).extras.getString(android.app.Notification.EXTRA_TEXT,"").contains("서울 · 맑음 · 18°C"),"active notification shows current city, weather and temperature");
                check(activeAlarmNotifications(c,id)==1,"weather refresh updates the existing alarm notification");
                c.startForegroundService(new Intent(c,VoiceService.class).putExtra("id",id).putExtra("target",secondTarget).putExtra("text","알림 중복 검사").putExtra("soundUri",""));
                Thread.sleep(400);
                check(activeAlarmNotifications(c,id)==1&&legacySpeechNotifications(c)==0,"speech service shares the alarm notification");
                runOnMainSync(()->VoiceService.cancelAlarm(id));
                Thread.sleep(300);
                check(activeAlarmNotifications(c,id)==1,"alarm notification remains after speech service stops");
                AlarmNotifications.clear(c,id);
            }
            AlarmStore.prefs(c,id).edit().putBoolean("weather",true).putBoolean("news",true).putString("newsFormat",NewsSources.FORMAT).putBoolean("hasLocation",true).putLong("latitude",37566000).putLong("longitude",126978000).putLong("weatherTarget",secondTarget).putLong("newsTarget",secondTarget).putLong("weatherFetchedAt",System.currentTimeMillis()).putString("briefingWeather","맑음, 최고 20도입니다.").putString("briefingNews","첫 번째 뉴스입니다.").commit();
            BriefingReceiver.schedule(c,id,secondTarget);
            check(AlarmStore.prefs(c,id).getLong("weatherScheduledTarget",0)==secondTarget,"weather prefetch scheduled");
            check(AlarmStore.prefs(c,id).getLong("newsScheduledTarget",0)==secondTarget,"news prefetch scheduled");
            check(BriefingReceiver.spoken(c,id,secondTarget,false).contains("맑음")&&!BriefingReceiver.spoken(c,id,secondTarget,false).contains("뉴스"),"pre-alarm announcement includes weather only");
            check(BriefingReceiver.newsSpoken(c,id,secondTarget).startsWith("다음 뉴스 주요 뉴스입니다"),"default news source is Daum");
            AlarmStore.prefs(c,id).edit().putString("newsSource",NewsSources.YONHAP).putString("newsSourceFetched",NewsSources.YONHAP).commit();
            check(BriefingReceiver.newsSpoken(c,id,secondTarget).startsWith("연합뉴스 주요 뉴스입니다"),"saved Yonhap source is preserved");
            AlarmStore.prefs(c,id).edit().putString("newsSource",NewsSources.NATE).putString("newsSourceFetched",NewsSources.NATE).commit();
            check(BriefingReceiver.newsSpoken(c,id,secondTarget).startsWith("네이트 뉴스 주요 뉴스입니다"),"selected Nate source is announced");
            check(NewsSources.nateTitles("<div class=\"newsMainIssue\"><h2>첫 &quot;제목&quot;</h2><h2>두 번째</h2><h2>세 번째</h2><h2>네 번째</h2><!-- //newsMainIssue : 메인 이슈 -->").size()==4,"Nate main issue is not limited to three headlines");
            check(NewsSources.nateTitles("<div class=\"newsMainIssue\"><h2>첫 &quot;제목&quot;</h2><!-- //newsMainIssue : 메인 이슈 -->").get(0).equals("첫 \"제목\""),"Nate headline entities are parsed");
            check(NewsSources.nateTitles("<div class=\"postRankSubjectList\"><h2 class=\"tit\">인기기사</h2></div>").isEmpty(),"Nate popularity ranking is not used as major news");
            check(NewsSources.yonhapTitles("<section class=\"box-type261 box-juyonews-list01\"><span class=\"title01\">첫 &quot;기사&quot;</span><span class=\"title01\">둘</span><span class=\"title01\">셋</span><span class=\"title01\">넷</span></section>").size()==4,"Yonhap curated major news is not limited to three headlines");
            check(NewsSources.yonhapTitles("<rss><item><title>최신기사</title></item></rss>").isEmpty(),"Yonhap latest RSS is not used as major news");
            check(NewsSources.daumTitles("<div class=\"box_comp box_news_headline2\"><ul class=\"list_newsheadline2\"><li><strong class=\"tit_txt\">첫 &quot;기사&quot;</strong></li><li><strong class=\"tit_txt\">둘</strong></li><li><strong class=\"tit_txt\">셋</strong></li><li><strong class=\"tit_txt\">넷</strong></li></ul></div>").size()==4,"Daum major news titles are parsed without a three-headline limit");
            check(NewsSources.daumTitles("<div class=\"box_comp box_news_headline2\"><ul><li><strong class=\"tit_txt\">첫 &quot;기사&quot;</strong></li></ul></div>").get(0).equals("첫 \"기사\""),"Daum headline entities are parsed");
            java.util.List<String> fourNews=java.util.Arrays.asList("첫째","둘째","셋째","넷째");
            check(NewsSources.forSpeech(fourNews,0).size()==4,"zero news count reads every headline");
            check(NewsSources.forSpeech(fourNews,2).equals(fourNews.subList(0,2)),"chosen news count limits speech");
            check(NewsSources.forSpeech(fourNews,100).size()==4,"news count above available articles reads only available headlines");
            check(NewsSources.countIntro(16,5).contains("16건")&&NewsSources.countIntro(16,5).contains("5건"),"news announcement reports available and selected counts");
            check(NewsSources.countIntro(4,4).contains("모두 읽어드립니다"),"all-news announcement reports the full count");
            check("비 · 13°C".equals(BriefingReceiver.parseCurrentWeather("{\"current\":{\"temperature_2m\":12.6,\"weather_code\":61}}")),"current weather uses current temperature and condition");
            StringBuilder longBriefing=new StringBuilder();for(int n=0;n<100;n++)longBriefing.append("주요 뉴스 ").append(n).append("번째 제목입니다. ");
            java.util.List<String> speechParts=VoiceService.splitSpeech(longBriefing.toString());
            check(speechParts.size()>1&&String.join("",speechParts).equals(longBriefing.toString()),"long news briefing is split without losing text");
            check(speechParts.stream().allMatch(part->part.length()<=450),"each speech part stays within the TTS size limit");
            check(BriefingReceiver.spoken(c,id,secondTarget,true).contains("뉴스"),"target announcement includes news");
            check(BriefingReceiver.spoken(c,id,secondTarget+86400000,true).isEmpty(),"stale briefing is not spoken");
            android.location.Address seoulAddress=new android.location.Address(java.util.Locale.KOREA);seoulAddress.setAdminArea("서울특별시");seoulAddress.setLocality("종로구");
            check("서울".equals(WeatherLocation.city(seoulAddress)),"Seoul district resolves to Seoul city");
            android.location.Address suwonAddress=new android.location.Address(java.util.Locale.KOREA);suwonAddress.setAdminArea("경기도");suwonAddress.setLocality("수원시");
            check("수원시".equals(WeatherLocation.city(suwonAddress)),"non-Seoul city keeps its name");
            String seoulDate=java.time.Instant.ofEpochMilli(secondTarget).atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate().toString();
            String localDate=java.time.Instant.ofEpochMilli(secondTarget).atZone(java.time.ZoneId.of("America/Los_Angeles")).toLocalDate().toString();
            org.json.JSONObject seoulWeather=sampleForecast("Asia/Seoul",seoulDate,11,20,10);
            org.json.JSONObject localWeather=sampleForecast("America/Los_Angeles",localDate,23,30,14);
            java.util.List<WeatherCities.City> seoulOnly=WeatherCities.selected(true,true,37.566,126.978,"서울",java.util.Collections.emptyList());
            String seoulSpoken=BriefingReceiver.parseWeather(seoulWeather.toString(),seoulOnly,secondTarget);
            check(seoulSpoken.contains("서울 날씨는 현재 기온 11도")&&!seoulSpoken.contains("수원"),"Seoul weather speaks once with current temperature");
            WeatherCities.City paris=new WeatherCities.City(2988507,"파리","일드프랑스","프랑스","FR",48.8566,2.3522);
            WeatherCities.City busan=new WeatherCities.City(1838524,"부산광역시","부산광역시","대한민국","KR",35.10168,129.03004);
            java.util.List<WeatherCities.City> saved=WeatherCities.parse(WeatherCities.encode(java.util.Arrays.asList(paris,busan)));
            check(saved.size()==2&&saved.get(0).id==paris.id&&saved.get(1).id==busan.id,"multiple cities retain order after save");
            java.util.List<WeatherCities.City> found=WeatherCities.parseSearch("{\"results\":[{\"id\":1838524,\"name\":\"부산광역시\",\"latitude\":35.10168,\"longitude\":129.03004,\"admin1\":\"부산광역시\",\"country\":\"대한민국\",\"country_code\":\"KR\"}]}");
            check(found.size()==1&&found.get(0).id==busan.id&&found.get(0).display().contains("대한민국"),"city search keeps region and country");
            saved.remove(0);check(WeatherCities.parse(WeatherCities.encode(saved)).size()==1,"removed city stays removed after save");
            saved.add(0,paris);
            java.util.List<WeatherCities.City> selected=WeatherCities.selected(true,true,39.243,-123.152,"Redwood Valley",saved);
            check(selected.size()==3&&"Redwood Valley".equals(selected.get(0).name),"only selected current and added cities are used");
            check(WeatherCities.selected(false,true,39.243,-123.152,"Redwood Valley",saved).size()==2,"current city can be excluded");
            check(WeatherCities.selected(true,true,37.566,126.978,"서울",saved).size()==3,"Seoul is included only if current city is Seoul");
            check(WeatherCities.selected(false,false,Double.NaN,Double.NaN,"",java.util.Collections.emptyList()).isEmpty(),"no weather city is implicit");
            check(WeatherCities.parse(WeatherCities.encode(java.util.Collections.singletonList(WeatherCities.seoul()))).size()==1,"Seoul can be added explicitly");
            check(WeatherCities.samePlace(WeatherCities.current("부산광역시",35.1,129.03),busan),"added current city is deduplicated");
            String parisDate=java.time.Instant.ofEpochMilli(secondTarget).atZone(java.time.ZoneId.of("Europe/Paris")).toLocalDate().toString();
            org.json.JSONObject parisWeather=sampleForecast("Europe/Paris",parisDate,16,22,11);
            org.json.JSONObject busanWeather=sampleForecast("Asia/Seoul",seoulDate,18,24,13);
            String allSpoken=BriefingReceiver.parseWeather(new org.json.JSONArray().put(localWeather).put(parisWeather).put(busanWeather).toString(),selected,secondTarget);
            check(!allSpoken.contains("서울 날씨")&&allSpoken.contains("Redwood Valley 날씨는 현재 기온 23도")&&allSpoken.contains("파리 날씨는 현재 기온 16도")&&allSpoken.contains("부산광역시 날씨는 현재 기온 18도"),"only selected cities include current temperature");
            AlarmStore.prefs(c,id).edit().remove("newsFormat").putLong("newsTarget",secondTarget).putString("briefingNews","이전 버전 기사").commit();
            BriefingReceiver.schedule(c,id,secondTarget);
            check(NewsSources.FORMAT.equals(AlarmStore.prefs(c,id).getString("newsFormat",""))&&AlarmStore.prefs(c,id).getLong("newsTarget",0)==0,"old headline cache is discarded and major news is rescheduled");
            Scheduler.disable(c,1);
            check(!AlarmStore.prefs(c,1).getBoolean("enabled",true),"first disabled");
            check(AlarmStore.prefs(c,id).getBoolean("enabled",false),"second stays enabled");
            check(AlarmStore.prefs(c,id).getLong("target",0)==secondTarget,"second target retained");
            AlarmStore.delete(c,1);check(AlarmStore.ids(c).size()==1&&AlarmStore.exists(c,id),"delete only selected alarm");
            Scheduler.restoreAll(c,false);check(AlarmStore.prefs(c,id).getBoolean("enabled",false),"restore active alarm");
            Scheduler.disable(c,id);Scheduler.restoreAll(c,false);check(!AlarmStore.prefs(c,id).getBoolean("enabled",true),"restore keeps inactive alarm off");
            check(AlarmStore.prefs(c,id).getLong("weatherScheduledTarget",0)==0,"disabled alarm cancels briefing");
            AlarmStore.prefs(c,id).edit().commit(); // Flush asynchronous preference edits before instrumentation exits.
            runOnMainSync(()->{Intent launch=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(launch);});
            result.putString("stream",checks+" multi-alarm assertions passed\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
    org.json.JSONObject sampleForecast(String zone,String day,int current,int high,int low)throws Exception{
        return new org.json.JSONObject().put("timezone",zone).put("current",new org.json.JSONObject().put("temperature_2m",current)).put("daily",new org.json.JSONObject().put("time",new org.json.JSONArray().put(day)).put("temperature_2m_max",new org.json.JSONArray().put(high)).put("temperature_2m_min",new org.json.JSONArray().put(low)).put("weather_code",new org.json.JSONArray().put(0)));
    }
    int activeAlarmNotifications(Context c,int id){
        int count=0;for(android.service.notification.StatusBarNotification notification:c.getSystemService(NotificationManager.class).getActiveNotifications())if(notification.getId()==AlarmNotifications.notificationId(id)&&notification.getTag()==null)count++;
        return count;
    }
    void waitForAlarmNotification(Context c,int id)throws InterruptedException{
        for(int retry=0;retry<30&&activeAlarmNotifications(c,id)==0;retry++)Thread.sleep(50);
    }
    int legacySpeechNotifications(Context c){
        int count=0;for(android.service.notification.StatusBarNotification notification:c.getSystemService(NotificationManager.class).getActiveNotifications())if(notification.getId()==7)count++;
        return count;
    }
}
