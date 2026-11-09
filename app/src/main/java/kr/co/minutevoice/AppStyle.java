package kr.co.minutevoice;
import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.*;
import android.widget.*;
final class AppStyle {
    static final String[] NAMES={"클래식 블루","미드나이트","클린 화이트","세이지","라벤더","크로노"};
    // Background, surface, text, secondary text, accent, on-accent, divider, control surface.
    static final String[][] COLORS={
        {"#F3F6FA","#FFFFFF","#17263B","#526277","#315E91","#FFFFFF","#DCE5EE","#EAF0F7"},
        {"#101318","#1C2028","#F0F2F6","#ADB7C4","#A8C7E8","#17324C","#353C47","#29313B"},
        {"#F5F6F8","#FFFFFF","#1B2430","#586575","#365F9C","#FFFFFF","#E1E6ED","#EDF1F6"},
        {"#F4F7F2","#FFFFFF","#24352D","#596B60","#436B52","#FFFFFF","#DFE7DD","#E9F0E8"},
        {"#F7F5FA","#FFFFFF","#30283E","#695F78","#715296","#FFFFFF","#E8E0ED","#F0EAF5"},
        {"#10131D","#202431","#EEF0FC","#AEB5CA","#C2BEFF","#24205A","#373D4C","#2B3041"}
    };
    // Theme x (Sunday, weekday, Saturday) x (selected background/text, unselected background/text).
    static final String[][][] DAY_COLORS={
        {{"#A93665","#FFFFFF","#FBE8F0","#89506A"},{"#287454","#FFFFFF","#E8F4EC","#4B6D59"},{"#28699F","#FFFFFF","#E7F2FB","#4C6980"}},
        {{"#A84D72","#FFFFFF","#342631","#B78CA0"},{"#367B60","#FFFFFF","#26352F","#A8C7B3"},{"#39739C","#FFFFFF","#253342","#A6C4D8"}},
        {{"#A93665","#FFFFFF","#FBEAF1","#89506A"},{"#287454","#FFFFFF","#EAF5ED","#4B6D59"},{"#28699F","#FFFFFF","#E9F3FB","#4C6980"}},
        {{"#A34B66","#FFFFFF","#F8ECEE","#875B63"},{"#31704A","#FFFFFF","#E7F2E8","#4D6B52"},{"#326D91","#FFFFFF","#E8F1F5","#4E6C7D"}},
        {{"#A34170","#FFFFFF","#F9EAF2","#875269"},{"#337450","#FFFFFF","#EAF4EC","#4D6E57"},{"#386A9A","#FFFFFF","#EAF1FA","#536D87"}},
        {{"#A65380","#FFFFFF","#3A2B3A","#C6A3B8"},{"#3A786B","#FFFFFF","#263733","#B3CFC6"},{"#516EA4","#FFFFFF","#293448","#B5C5E0"}}
    };
    // Checked controls carry the visual weight; unchecked controls recede into the surface.
    static final String[][] SWITCH_COLORS={
        {"#315E91","#DDEAF7","#E2E9F0","#AAB6C4"},
        {"#466E92","#F5F8FC","#323943","#768292"},
        {"#365F9C","#E0EBF8","#E4E9F0","#AFB9C7"},
        {"#436B52","#E1EDE3","#E2E9E2","#ACB9AF"},
        {"#715296","#E9DCF5","#E9E3EF","#B8ABC8"},
        {"#655DA7","#F9F8FF","#343847","#7B8298"}
    };
    static int dayColor(int theme,int weekday,boolean selected,boolean foreground){int group=weekday==0?0:weekday==6?2:1;return Color.parseColor(DAY_COLORS[theme][group][(selected?0:2)+(foreground?1:0)]);}
    static int BLUE,PINK,WHITE,MUTED,SURFACE,ON_ACCENT,LINE,CONTROL;
    static int selected(Context c){return Math.max(0,Math.min(NAMES.length-1,c.getSharedPreferences("appearance",0).getInt("theme",0)));}
    static void load(Context c){String[] v=COLORS[selected(c)];BLUE=Color.parseColor(v[0]);SURFACE=Color.parseColor(v[1]);WHITE=Color.parseColor(v[2]);MUTED=Color.parseColor(v[3]);PINK=Color.parseColor(v[4]);ON_ACCENT=Color.parseColor(v[5]);LINE=Color.parseColor(v[6]);CONTROL=Color.parseColor(v[7]);}
    static int apply(Activity a){int id=selected(a);load(a);int[] styles={R.style.AppTheme,R.style.MidnightTheme,R.style.WhiteTheme,R.style.SageTheme,R.style.LavenderTheme,R.style.ChronoTheme};a.setTheme(styles[id]);a.getWindow().setStatusBarColor(BLUE);a.getWindow().setNavigationBarColor(BLUE);boolean light=id!=1&&id!=5;int flags=light?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0;if(Build.VERSION.SDK_INT>=27&&light)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;a.getWindow().getDecorView().setSystemUiVisibility(flags);return id;}
    static void tint(Switch s){
        String[] colors=SWITCH_COLORS[selected(s.getContext())];
        int trackWidth=dp(s.getContext(),42),trackHeight=dp(s.getContext(),24),thumbSize=dp(s.getContext(),20);
        StateListDrawable track=new StateListDrawable();
        track.addState(new int[]{android.R.attr.state_checked},controlShape(colors[0],trackWidth,trackHeight));
        track.addState(new int[]{},controlShape(colors[2],trackWidth,trackHeight));
        StateListDrawable thumb=new StateListDrawable();
        thumb.addState(new int[]{android.R.attr.state_checked},controlShape(colors[1],thumbSize,thumbSize));
        thumb.addState(new int[]{},controlShape(colors[3],thumbSize,thumbSize));
        s.setTrackDrawable(track);s.setThumbDrawable(thumb);s.setShowText(false);
        s.setSwitchMinWidth(dp(s.getContext(),48));s.setMinHeight(dp(s.getContext(),48));
    }
    static GradientDrawable controlShape(String color,int width,int height){GradientDrawable shape=fill(Color.parseColor(color),height/2);shape.setSize(width,height);return shape;}
    static void tint(RadioButton r){load(r.getContext());int[][] states={{android.R.attr.state_checked},{}};r.setButtonTintList(new ColorStateList(states,new int[]{PINK,MUTED}));}
    static int dp(Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n);}
    static TextView sectionTitle(Context c,String title){TextView t=new TextView(c);t.setText(title);t.setTextColor(WHITE);t.setTextSize(19);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setMinHeight(dp(c,40));t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    static GradientDrawable surface(Context c){return fill(SURFACE,dp(c,24));}
    static TextView action(Context c,String title,boolean primary){TextView t=new TextView(c);t.setText(title);t.setTextSize(14);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setTextColor(primary?ON_ACCENT:PINK);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,48));t.setPadding(dp(c,16),0,dp(c,16),0);t.setBackground(fill(primary?PINK:CONTROL,dp(c,24)));return t;}
    static void menu(Activity a,View anchor){PopupMenu menu=new PopupMenu(a,anchor);menu.getMenu().add("테마");menu.getMenu().add("앱 소개");menu.getMenu().add("개인정보 처리방침");menu.setOnMenuItemClickListener(item->{String title=item.getTitle().toString();if(title.equals("테마")){int current=selected(a);String[] choices=new String[NAMES.length];for(int i=0;i<NAMES.length;i++)choices[i]=NAMES[i]+(i==current?" · 사용 중 ✓":"");new AlertDialog.Builder(a).setTitle("테마 선택 · 모든 알람에 적용").setSingleChoiceItems(choices,current,(d,which)->{a.getSharedPreferences("appearance",0).edit().putInt("theme",which).apply();d.dismiss();a.recreate();}).setNegativeButton("취소",null).show();}else if(title.equals("개인정보 처리방침")){new AlertDialog.Builder(a).setTitle("개인정보 처리방침").setMessage(AppPrivacy.TEXT).setPositiveButton("닫기",null).show();}else new AlertDialog.Builder(a).setTitle("Minute Voice").setMessage("버전 "+BuildConfig.VERSION_NAME+"\n\n매분 음성으로 시간을 안내합니다.\n공휴일 제외 지원: 대한민국 2026~2027년").setPositiveButton("확인",null).show();return true;});menu.show();}
    static GradientDrawable fill(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
}
