package kr.co.minutevoice;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Per-alarm city selection and Open-Meteo geocoding results. */
final class WeatherCities {
    static final int MAX_ADDED=8;
    static final double SEOUL_LAT=37.5665,SEOUL_LON=126.9780;

    static final class City {
        final long id;final String name,region,country,countryCode;final double latitude,longitude;
        City(long id,String name,String region,String country,String countryCode,double latitude,double longitude){
            this.id=id;this.name=name;this.region=region;this.country=country;this.countryCode=countryCode;this.latitude=latitude;this.longitude=longitude;
        }
        String display(){
            StringBuilder b=new StringBuilder(name);
            if(!region.isEmpty()&&!region.equalsIgnoreCase(name))b.append(" · ").append(region);
            if(!country.isEmpty())b.append(" · ").append(country);
            return b.toString();
        }
    }
    static City seoul(){return new City(-1,"서울","","대한민국","KR",SEOUL_LAT,SEOUL_LON);}
    static City current(String name,double latitude,double longitude){return new City(0,WeatherLocation.isSeoul(name)?"서울":name.isEmpty()?"현재 위치":name,"","","",latitude,longitude);}
    static boolean isSeoul(City city){return "KR".equalsIgnoreCase(city.countryCode)&&(WeatherLocation.isSeoul(city.name)||WeatherLocation.isSeoul(city.region));}
    static List<City> selected(boolean includeCurrent,boolean hasLocation,double lat,double lon,String currentName,List<City> added){
        List<City> result=new ArrayList<>();
        if(includeCurrent&&hasLocation)result.add(current(currentName,lat,lon));
        for(City city:added){
            boolean duplicate=false;
            for(City existing:result)if(samePlace(existing,city)){duplicate=true;break;}
            if(!duplicate)result.add(city);
        }
        return result;
    }
    static boolean samePlace(City a,City b){
        if(a.id>0&&b.id>0&&a.id==b.id)return true;
        return a.name.equalsIgnoreCase(b.name)&&Math.abs(a.latitude-b.latitude)<0.3&&Math.abs(a.longitude-b.longitude)<0.3;
    }
    static List<City> parse(String json){
        List<City> cities=new ArrayList<>();
        try{
            JSONArray array=new JSONArray(json);
            for(int i=0;i<array.length()&&cities.size()<MAX_ADDED;i++){
                City city=fromJson(array.getJSONObject(i));
                if(city!=null)cities.add(city);
            }
        }catch(Exception ignored){}
        return cities;
    }
    static String encode(List<City> cities){
        JSONArray array=new JSONArray();
        for(int i=0;i<cities.size()&&i<MAX_ADDED;i++){
            City city=cities.get(i);
            try{array.put(new JSONObject().put("id",city.id).put("name",city.name).put("region",city.region).put("country",city.country).put("countryCode",city.countryCode).put("latitude",city.latitude).put("longitude",city.longitude));}catch(Exception ignored){}
        }
        return array.toString();
    }
    private static City fromJson(JSONObject object){
        long id=object.optLong("id",0);
        String name=object.optString("name","").trim();
        double lat=object.optDouble("latitude",Double.NaN),lon=object.optDouble("longitude",Double.NaN);
        if(name.isEmpty()||name.length()>80||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)return null;
        return new City(id,name,object.optString("region",object.optString("admin1","")),object.optString("country",""),object.optString("countryCode",object.optString("country_code","")),lat,lon);
    }
    static List<City> search(String query)throws Exception{
        String url="https://geocoding-api.open-meteo.com/v1/search?name="+URLEncoder.encode(query,"UTF-8")+"&count=10&language=ko";
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(4000);connection.setReadTimeout(4000);connection.setRequestProperty("User-Agent","MinuteVoice/1.11");
        try{
            if(connection.getResponseCode()!=200)throw new IllegalStateException("City search failed");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int read;
            try(InputStream in=connection.getInputStream()){
                while(out.size()<100_000&&(read=in.read(buffer,0,Math.min(buffer.length,100_000-out.size())))>0)out.write(buffer,0,read);
            }
            return parseSearch(new String(out.toByteArray(),StandardCharsets.UTF_8));
        }finally{connection.disconnect();}
    }
    static List<City> parseSearch(String json)throws Exception{
        JSONArray results=new JSONObject(json).optJSONArray("results");List<City> cities=new ArrayList<>();
        if(results!=null)for(int i=0;i<results.length();i++){
            City city=fromJson(results.getJSONObject(i));if(city!=null)cities.add(city);
        }
        return cities;
    }
    private WeatherCities(){}
}
