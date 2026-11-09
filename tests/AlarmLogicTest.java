package kr.co.minutevoice;
import java.time.*;
public class AlarmLogicTest {
    static int checks;
    static void equal(Object a,Object b){checks++;if(!a.equals(b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        equal(AlarmLogic.phrase(7,0,-20),"7시 20분 전입니다");equal(AlarmLogic.phrase(7,0,1),"7시 1분 지났습니다");equal(AlarmLogic.phrase(0,30,0),"12시 30분입니다. 알람 시간입니다");
        equal(AlarmLogic.phrase("출근",7,0,-20),"출근 20분 남았습니다");
        equal(AlarmLogic.phrase("출근",7,0,1),"출근 1분 지났습니다");
        equal(AlarmLogic.phrase("회의",7,30,0),"회의 시간입니다");
        equal(AlarmLogic.phrase("  약 먹기  ",7,0,-1),"약 먹기 1분 남았습니다");
        equal(AlarmLogic.phrase("",7,0,-20),"7시 20분 전입니다");
        equal(AlarmLogic.phrase("   ",7,0,1),"7시 1분 지났습니다");
        equal(AlarmLogic.phrase(null,7,30,0),"7시 30분입니다. 알람 시간입니다");
        equal(AlarmLogic.withBriefing("출근 20분 남았습니다","서울 날씨는 맑음입니다"),"출근 20분 남았습니다 서울 날씨는 맑음입니다 출근 20분 남았습니다");
        equal(AlarmLogic.withBriefing("출근 20분 남았습니다",""),"출근 20분 남았습니다");
        equal(AlarmLogic.withBriefings("출근 20분 남았습니다","서울 날씨","주요 뉴스"),"출근 20분 남았습니다 서울 날씨 출근 20분 남았습니다 주요 뉴스 출근 20분 남았습니다");
        equal(AlarmLogic.withWindowMessages("출근 20분 남았습니다",true,false),"알림을 시작합니다. 출근 20분 남았습니다");
        equal(AlarmLogic.withWindowMessages("출근 1분 지났습니다",false,true),"출근 1분 지났습니다. 알림을 종료합니다.");
        equal(AlarmLogic.withWindowMessages("출근 시간입니다",true,true),"알림을 시작합니다. 출근 시간입니다. 알림을 종료합니다.");
        equal(AlarmLogic.withWindowMessages("이미 마침표가 있습니다.",false,true),"이미 마침표가 있습니다. 알림을 종료합니다.");
        equal(AlarmLogic.withWindowMessages("출근 1분 남았습니다",false,false),"출근 1분 남았습니다");
        equal(AlarmLogic.weekdayLabel(127),"매일");
        equal(AlarmLogic.weekdayLabel(62),"월 화 수 목 금");
        equal(AlarmLogic.weekdayLabel(65),"일 토");
        ZoneId z=ZoneId.of("Asia/Seoul");long target=ZonedDateTime.of(2026,9,24,7,0,0,0,z).toInstant().toEpochMilli();
        equal(AlarmLogic.nextTarget(target-1,7,0,z),target);equal(AlarmLogic.nextTarget(target,7,0,z),target+86400000);
        equal(AlarmLogic.nextTick(target-21*60000,target,20),target-20*60000);
        equal(AlarmLogic.nextTick(target-5*60000+3000,target,20),target-4*60000);
        equal(AlarmLogic.nextTick(target,target,20),target+60000);equal(AlarmLogic.nextTick(target-1000,target,0),target);
        equal(AlarmLogic.nextTarget(target-1,7,0,z,62,true), target+4*86400000L); // Chuseok -> Monday
        equal(AlarmLogic.nextTarget(target-1,7,0,z,127,true), target+3*86400000L); // ordinary Sunday stays enabled
        equal(AlarmLogic.nextTarget(target-1,7,0,z,62,false), target);
        equal(KoreanHolidays.isHoliday(LocalDate.of(2026,6,3)),true);
        equal(KoreanHolidays.isHoliday(LocalDate.of(2026,10,5)),true);
        equal(KoreanHolidays.isHoliday(LocalDate.of(2027,2,9)),true);
        equal(KoreanHolidays.isHoliday(LocalDate.of(2027,5,3)),true);
        equal(KoreanHolidays.isHoliday(LocalDate.of(2027,7,19)),true);
        equal(KoreanHolidays.isHoliday(LocalDate.of(2027,12,27)),true);
        equal(KoreanHolidays.supported(LocalDate.of(2028,1,1)),false);
        long midnight=LocalDate.of(2026,9,28).atStartOfDay(z).toInstant().toEpochMilli();
        equal(AlarmLogic.nextTick(midnight-3600000,midnight,20),midnight-20*60000); // target date governs pre-midnight speech
        for(int mask=1;mask<=127;mask++)for(int day=0;day<7;day++){
            long now=LocalDate.of(2026,11,1).plusDays(day).atTime(8,0).atZone(z).toInstant().toEpochMilli();
            long next=AlarmLogic.nextTarget(now,7,0,z,mask,true);
            var date=Instant.ofEpochMilli(next).atZone(z);
            equal(next>now,true);
            equal((mask & (1 << (date.getDayOfWeek().getValue()%7))) != 0,true);
            equal(date.getHour(),7);
            equal(next-now<=7*86400000L,true);
        }
        try { AlarmLogic.nextTarget(target,7,0,z,0,false);throw new AssertionError("Empty weekdays accepted"); }
        catch(IllegalArgumentException expected){checks++;}
        equal(AlarmLogic.nextTargetAfterStop(target-20*60000,target,7,0,z,127,false),target+86400000L);
        equal(AlarmLogic.nextTargetAfterStop(target+60000,target,7,0,z,127,false),target+86400000L);
        equal(AlarmLogic.nextTargetAfterStop(target-60000,target,7,0,z,62,true),target+4*86400000L);
        System.out.println(checks+" alarm logic assertions passed");
    }
}
