package kr.co.minutevoice;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Resolves the user-selected approximate location to a short spoken city name. */
final class WeatherLocation {
    static String city(Address address) {
        if(address==null)return "";
        String admin=clean(address.getAdminArea());
        String locality=clean(address.getLocality());
        if(isSeoul(admin)||isSeoul(locality))return "서울";
        if(!locality.isEmpty())return locality;
        String subAdmin=clean(address.getSubAdminArea());
        return !subAdmin.isEmpty()?subAdmin:admin;
    }
    static boolean isSeoul(String city) {
        String normalized=clean(city).toLowerCase(Locale.ROOT);
        return normalized.contains("서울")||normalized.equals("seoul")||normalized.startsWith("seoul ");
    }
    private static String clean(String s){return s==null?"":s.replaceAll("[\\r\\n\\t]"," ").trim();}
    static String resolve(Context context,double latitude,double longitude){
        if(!Geocoder.isPresent())return "";
        try{
            List<Address> results=new Geocoder(context,Locale.KOREA).getFromLocation(latitude,longitude,1);
            return results==null||results.isEmpty()?"":city(results.get(0));
        }catch(Exception ignored){return "";}
    }
    static String resolveWithin(Context context,double latitude,double longitude){
        FutureTask<String> task=new FutureTask<>(()->resolve(context,latitude,longitude));
        Thread worker=new Thread(task,"minutevoice-geocoder");worker.setDaemon(true);worker.start();
        try{return task.get(2500,TimeUnit.MILLISECONDS);}catch(Exception ignored){task.cancel(true);return "";}
    }
    private WeatherLocation(){}
}
