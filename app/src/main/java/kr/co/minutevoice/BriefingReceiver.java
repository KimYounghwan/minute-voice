package kr.co.minutevoice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Fetches optional, short-lived briefing data before an alarm; alarm delivery never waits for network. */
public class BriefingReceiver extends BroadcastReceiver {
    private static final long HOUR=3_600_000L;
    private static final long WEATHER_REFRESH=10*60_000L;
    private static PendingIntent pending(Context c,int id,long target,String kind){
        Intent i=new Intent(c,BriefingReceiver.class).setData(Uri.parse("minutevoice://briefing/"+kind+"/"+id)).putExtra("id",id).putExtra("target",target).putExtra("kind",kind);
        return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c,int id){
        SharedPreferences p=Scheduler.prefs(c,id);
        c.getSystemService(AlarmManager.class).cancel(pending(c,id,0,"weather"));
        c.getSystemService(AlarmManager.class).cancel(pending(c,id,0,"news"));
        p.edit().remove("weatherScheduledTarget").remove("newsScheduledTarget").remove("weatherTarget").remove("newsTarget").remove("weatherFetchedAt").remove("cityLookupAttemptedTarget").remove("briefingWeather").remove("briefingNews").remove("newsSourceFetched").remove("currentWeatherTarget").remove("currentWeatherFetchedAt").remove("currentWeatherText").remove("currentWeatherAttemptedTarget").apply();
    }
    static void schedule(Context c,int id,long target){
        SharedPreferences p=Scheduler.prefs(c,id);
        if(!p.getBoolean("weather",false)&&!p.getBoolean("news",false)&&!p.getBoolean("hasLocation",false)){cancel(c,id);return;}
        if(p.getBoolean("news",false)&&!NewsSources.FORMAT.equals(p.getString("newsFormat",""))){
            c.getSystemService(AlarmManager.class).cancel(pending(c,id,0,"news"));
            p.edit().putString("newsFormat",NewsSources.FORMAT).remove("newsScheduledTarget").remove("newsTarget").remove("briefingNews").remove("newsSourceFetched").apply();
        }
        long now=System.currentTimeMillis();
        if(target<=now)return;
        if(p.getBoolean("weather",false)||p.getBoolean("hasLocation",false))arm(c,id,target,"weather",Math.max(now+1000,target-p.getInt("before",20)*60_000L-5*60_000L),false);
        if(p.getBoolean("news",false))arm(c,id,target,"news",Math.max(now+1000,target-HOUR),false);
    }
    private static void arm(Context c,int id,long target,String kind,long when,boolean refresh){
        SharedPreferences p=Scheduler.prefs(c,id);
        String key=kind+"ScheduledTarget";
        if(!refresh&&p.getLong(key,0)==target)return;
        c.getSystemService(AlarmManager.class).cancel(pending(c,id,0,kind));
        try{
            c.getSystemService(AlarmManager.class).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(c,id,target,kind));
            p.edit().putLong(key,target).apply();
        }catch(SecurityException ignored){p.edit().remove(key).apply();}
    }
    static String weatherSpoken(Context c,int id,long target){
        SharedPreferences p=Scheduler.prefs(c,id);
        String weather=p.getString("briefingWeather","");
        return p.getBoolean("weather",false)&&p.getLong("weatherTarget",0)==target&&System.currentTimeMillis()-p.getLong("weatherFetchedAt",0)<30*60_000L&&!weather.isEmpty()?"오늘의 날씨입니다. "+weather:"";
    }
    static String newsSpoken(Context c,int id,long target){
        SharedPreferences p=Scheduler.prefs(c,id);
        String news=p.getString("briefingNews","");
        String source=NewsSources.normalize(p.getString("newsSource",NewsSources.DAUM));
        return p.getBoolean("news",false)&&NewsSources.FORMAT.equals(p.getString("newsFormat",""))&&p.getLong("newsTarget",0)==target&&source.equals(p.getString("newsSourceFetched",source))&&!news.isEmpty()?NewsSources.label(source)+" 주요 뉴스입니다. "+news:"";
    }
    static String spoken(Context c,int id,long target,boolean includeNews){
        String weather=weatherSpoken(c,id,target),news=includeNews?newsSpoken(c,id,target):"";
        return weather+(weather.isEmpty()||news.isEmpty()?"":" ")+news;
    }
    @Override public void onReceive(Context c,Intent i){
        int id=i.getIntExtra("id",-1);long target=i.getLongExtra("target",0);
        if(id<0||!AlarmStore.exists(c,id))return;
        SharedPreferences p=Scheduler.prefs(c,id);
        if(!p.getBoolean("enabled",false)||p.getLong("target",0)!=target||target<=0)return;
        String kind=i.getStringExtra("kind");if(!"weather".equals(kind)&&!"news".equals(kind))return;
        PendingResult result=goAsync();
        new Thread(()->{
            try{
                String speech="";
                if("weather".equals(kind)&&(p.getBoolean("weather",false)||p.getBoolean("hasLocation",false)))try{
                    boolean hasLocation=p.getBoolean("hasLocation",false),includeCurrent=p.getBoolean("includeCurrent",true);
                    double lat=p.getLong("latitude",0)/1_000_000d,lon=p.getLong("longitude",0)/1_000_000d;
                    String city=p.getString("cityName","");
                    if(hasLocation&&city.isEmpty()&&p.getLong("cityLookupAttemptedTarget",0)!=target){
                        city=WeatherLocation.resolveWithin(c,lat,lon);
                        p.edit().putLong("cityLookupAttemptedTarget",target).putString("cityName",city).apply();
                    }
                    if(p.getBoolean("weather",false)){
                        List<WeatherCities.City> cities=WeatherCities.selected(includeCurrent,hasLocation,lat,lon,city,WeatherCities.parse(p.getString("weatherCities","[]")));
                        try{speech=weather(cities,target);}catch(Exception ignored){}
                    }
                    if(hasLocation){
                        String current="";
                        try{current=currentWeather(lat,lon);}catch(Exception ignored){}
                        if(p.getBoolean("enabled",false)&&p.getLong("target",0)==target){
                            SharedPreferences.Editor editor=p.edit().putLong("currentWeatherAttemptedTarget",target);
                            if(!current.isEmpty())editor.putLong("currentWeatherTarget",target).putLong("currentWeatherFetchedAt",System.currentTimeMillis()).putString("currentWeatherText",current);
                            editor.apply();
                            AlarmNotifications.updateWeather(c,id,target);
                        }
                    }
                }catch(Exception ignored){}
                String newsSource=NewsSources.normalize(p.getString("newsSource",NewsSources.DAUM));
                if("news".equals(kind)&&p.getBoolean("news",false))try{speech=news(newsSource,p.getInt("newsReadCount",0));}catch(Exception ignored){}
                if(p.getBoolean("enabled",false)&&p.getLong("target",0)==target){
                    if(!speech.isEmpty()){
                        SharedPreferences.Editor editor=p.edit().putLong(kind+"Target",target).putString("weather".equals(kind)?"briefingWeather":"briefingNews",speech);
                        if("weather".equals(kind))editor.putLong("weatherFetchedAt",System.currentTimeMillis());
                        else editor.putString("newsSourceFetched",newsSource).putString("newsFormat",NewsSources.FORMAT);
                        editor.apply();
                    }
                    long next=System.currentTimeMillis()+WEATHER_REFRESH;
                    if("weather".equals(kind)&&next<target+p.getInt("after",10)*60_000L)arm(c,id,target,"weather",next,true);
                }
            }finally{result.finish();}
        },"minutevoice-briefing").start();
    }
    private static HttpURLConnection connect(String url)throws Exception{
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(3500);connection.setReadTimeout(3500);
        connection.setRequestProperty("User-Agent","MinuteVoice/1.12.2");
        if(connection.getResponseCode()!=200){connection.disconnect();throw new IllegalStateException("HTTP error");}
        return connection;
    }
    private static String weather(List<WeatherCities.City> cities,long target)throws Exception{
        if(cities.isEmpty())return "";
        StringBuilder latitudes=new StringBuilder(),longitudes=new StringBuilder();
        for(WeatherCities.City city:cities){
            if(latitudes.length()>0){latitudes.append(',');longitudes.append(',');}
            latitudes.append(String.format(Locale.US,"%.5f",city.latitude));
            longitudes.append(String.format(Locale.US,"%.5f",city.longitude));
        }
        String url="https://api.open-meteo.com/v1/forecast?latitude="+latitudes+"&longitude="+longitudes+"&current=temperature_2m&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto&forecast_days=3";
        HttpURLConnection connection=connect(url);
        try(InputStream in=connection.getInputStream()){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int read;
            while(out.size()<64_000&&(read=in.read(buffer,0,Math.min(buffer.length,64_000-out.size())))>0)out.write(buffer,0,read);
            byte[] bytes=out.toByteArray();
            return parseWeather(new String(bytes,StandardCharsets.UTF_8),cities,target);
        }finally{connection.disconnect();}
    }
    private static String currentWeather(double latitude,double longitude)throws Exception{
        String url="https://api.open-meteo.com/v1/forecast?latitude="+String.format(Locale.US,"%.5f",latitude)+"&longitude="+String.format(Locale.US,"%.5f",longitude)+"&current=temperature_2m,weather_code&timezone=auto";
        HttpURLConnection connection=connect(url);
        try(InputStream in=connection.getInputStream()){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int read;
            while(out.size()<16_000&&(read=in.read(buffer,0,Math.min(buffer.length,16_000-out.size())))>0)out.write(buffer,0,read);
            return parseCurrentWeather(new String(out.toByteArray(),StandardCharsets.UTF_8));
        }finally{connection.disconnect();}
    }
    static String parseCurrentWeather(String json)throws Exception{
        JSONObject current=new JSONObject(json).getJSONObject("current");
        double temperature=current.getDouble("temperature_2m");
        if(!Double.isFinite(temperature))return "";
        return condition(current.getInt("weather_code"))+" · "+Math.round(temperature)+"°C";
    }
    static String parseWeather(String json,List<WeatherCities.City> cities,long target)throws Exception{
        if(cities.isEmpty())return "";
        if(cities.size()==1)return forecast(cities.get(0).name,new JSONObject(json),target);
        JSONArray locations=new JSONArray(json);
        StringBuilder speech=new StringBuilder();
        for(int i=0;i<cities.size()&&i<locations.length();i++){
            try{String part=forecast(cities.get(i).name,locations.getJSONObject(i),target);if(!part.isEmpty()){if(speech.length()>0)speech.append(' ');speech.append(part);}}catch(Exception ignored){}
        }
        return speech.toString();
    }
    private static String forecast(String city,JSONObject response,long target)throws Exception{
        JSONObject daily=response.getJSONObject("daily");
        ZoneId zone=ZoneId.of(response.getString("timezone"));
        String date=Instant.ofEpochMilli(target).atZone(zone).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        JSONArray dates=daily.getJSONArray("time");
        for(int n=0;n<dates.length();n++)if(date.equals(dates.getString(n))){
            int low=(int)Math.round(daily.getJSONArray("temperature_2m_min").getDouble(n));
            int high=(int)Math.round(daily.getJSONArray("temperature_2m_max").getDouble(n));
            int code=daily.getJSONArray("weather_code").getInt(n);
            JSONObject current=response.getJSONObject("current");
            int now=(int)Math.round(current.getDouble("temperature_2m"));
            return city+" 날씨는 현재 기온 "+now+"도, 하늘은 "+condition(code)+", 최저 "+low+"도, 최고 "+high+"도입니다.";
        }
        return "";
    }
    private static String condition(int code){
        if(code==0)return "맑음";
        if(code<=3)return "구름";
        if(code<=48)return "안개";
        if(code<=67||code==80||code==81||code==82)return "비";
        if(code<=77||code==85||code==86)return "눈";
        if(code>=95)return "천둥 번개";
        return "흐림";
    }
    private static String news(String source,int count)throws Exception{
        List<String> available=NewsSources.fetchTitles(source);
        List<String> chosen=NewsSources.forSpeech(available,count);
        return NewsSources.countIntro(available.size(),chosen.size())+headlines(chosen);
    }
    private static String headlines(List<String> titles){
        StringBuilder speech=new StringBuilder();for(int n=0;n<titles.size();n++)speech.append(n+1).append("번째, ").append(titles.get(n)).append(". ");
        return speech.toString();
    }
}
