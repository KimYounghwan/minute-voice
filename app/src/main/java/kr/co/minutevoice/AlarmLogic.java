package kr.co.minutevoice;
import java.time.*;
public final class AlarmLogic {
    public static String withBriefing(String timePhrase,String briefing){
        String detail=briefing==null?"":briefing.trim();
        return detail.isEmpty()?timePhrase:timePhrase+" "+detail+" "+timePhrase;
    }
    public static String withBriefings(String timePhrase,String... briefings){
        String result=timePhrase;
        for(String briefing:briefings)if(briefing!=null&&!briefing.trim().isEmpty())result+=" "+briefing.trim()+" "+timePhrase;
        return result;
    }
    public static String withWindowMessages(String announcement,boolean first,boolean last){
        String result=announcement;
        if(first)result="알림을 시작합니다. "+result;
        if(last)result+=(result.endsWith(".")?" ":". ")+"알림을 종료합니다.";
        return result;
    }
    public static String weekdayLabel(int mask){
        if((mask&127)==127)return "매일";
        String[] days={"일","월","화","수","목","금","토"};StringBuilder label=new StringBuilder();
        for(int i=0;i<7;i++)if((mask&(1<<i))!=0){if(label.length()>0)label.append(" ");label.append(days[i]);}
        return label.toString();
    }
    public static String phrase(String title, int hour, int minute, long offset) {
        String name = title == null ? "" : title.trim();
        if (name.isEmpty()) return phrase(hour, minute, offset);
        if (offset < 0) return name + " " + (-offset) + "분 남았습니다";
        if (offset > 0) return name + " " + offset + "분 지났습니다";
        return name + " 시간입니다";
    }
    public static String phrase(int hour, int minute, long offset) {
        String time = (hour % 12 == 0 ? 12 : hour % 12) + "시" + (minute == 0 ? "" : " " + minute + "분");
        if (offset < 0) return time + " " + (-offset) + "분 전입니다";
        if (offset > 0) return time + " " + offset + "분 지났습니다";
        return time + "입니다. 알람 시간입니다";
    }
    public static long nextTarget(long now, int hour, int minute, ZoneId zone) {
        return nextTarget(now, hour, minute, zone, 127, false);
    }
    public static boolean eligible(LocalDate date, int weekdays, boolean skipHolidays) {
        int bit = date.getDayOfWeek().getValue() % 7; // Sunday = 0
        return (weekdays & (1 << bit)) != 0 && (!skipHolidays || !KoreanHolidays.isHoliday(date));
    }
    public static long nextTarget(long now, int hour, int minute, ZoneId zone, int weekdays, boolean skipHolidays) {
        if ((weekdays & 127) == 0) throw new IllegalArgumentException("Select at least one weekday");
        LocalDate first = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        for (int i = 0; i < 370; i++) {
            LocalDate date = first.plusDays(i);
            long candidate = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli();
            if (candidate > now && eligible(date, weekdays, skipHolidays)) return candidate;
        }
        throw new IllegalStateException("No eligible alarm date");
    }
    public static long nextTargetAfterStop(long now,long occurrence,int hour,int minute,ZoneId zone,int weekdays,boolean skipHolidays){
        return nextTarget(Math.max(now,occurrence),hour,minute,zone,weekdays,skipHolidays);
    }
    public static long nextTick(long now, long target, int before) {
        long start = target - before * 60000L;
        if (start > now) return start;
        return target + (Math.floorDiv(now - target, 60000L) + 1) * 60000L;
    }
}
