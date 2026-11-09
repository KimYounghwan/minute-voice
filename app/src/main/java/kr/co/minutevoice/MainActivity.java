package kr.co.minutevoice;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root;int appliedTheme;
    int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
    @Override public void onCreate(Bundle b){appliedTheme=AppStyle.apply(this);super.onCreate(b);Scheduler.restoreAll(this,false);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},4);}
    @Override protected void onResume(){super.onResume();if(appliedTheme!=AppStyle.selected(this)){recreate();return;}AppStyle.load(this);render();}
    TextView text(LinearLayout box,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(AppStyle.WHITE);t.setTextSize(size);t.setPadding(0,dp(4),0,dp(4));box.addView(t);return t;}
    TextView action(String label,String description){TextView b=new TextView(this);b.setText(label);b.setTextSize(30);b.setTextColor(AppStyle.WHITE);b.setGravity(Gravity.CENTER);b.setContentDescription(description);b.setFocusable(true);b.setBackground(AppStyle.fill(Color.TRANSPARENT,dp(12)));return b;}
    void edit(int id){startActivity(new Intent(this,AlarmEditActivity.class).putExtra("id",id));}
    void render(){
        if(appliedTheme==5){renderChrono();return;}
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setBackgroundColor(AppStyle.BLUE);setContentView(screen);
        screen.setOnApplyWindowInsetsListener((v,i)->{screen.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(22),dp(8),dp(12),dp(8));screen.addView(header);
        ImageView alarm=new ImageView(this);alarm.setImageResource(R.drawable.ic_alarm_outline);alarm.setColorFilter(AppStyle.WHITE);alarm.setContentDescription("알람");header.addView(alarm,new LinearLayout.LayoutParams(dp(30),dp(30)));
        TextView heading=text(header,"  알람",20);heading.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu=action("⋮","메뉴");header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));menu.setOnClickListener(v->AppStyle.menu(this,v));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),0,dp(20),dp(12));scroll.addView(root);
        List<Integer> ids=AlarmStore.ids(this);ids.sort(Comparator.comparingInt(id->{var p=AlarmStore.prefs(this,id);return p.getInt("hour",7)*60+p.getInt("minute",0);}));
        TextView section=AppStyle.sectionTitle(this,"내 알람  ·  "+ids.size());root.addView(section);
        if(ids.isEmpty()){LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setPadding(dp(20),dp(32),dp(20),dp(32));empty.setBackground(AppStyle.surface(this));root.addView(empty);text(empty,"아직 알람이 없어요",21);text(empty,"아래 추가 버튼으로 첫 알람을 만들어 보세요",14).setTextColor(AppStyle.MUTED);}
        for(int id:ids){
            var p=AlarmStore.prefs(this,id);
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(18),dp(20),dp(18));card.setBackground(AppStyle.surface(this));LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);cardParams.topMargin=dp(12);root.addView(card,cardParams);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row);
            int hour=p.getInt("hour",7),minute=p.getInt("minute",0);String time=String.format(Locale.KOREA,"%d:%02d",hour%12==0?12:hour%12,minute);
            LinearLayout timeBox=new LinearLayout(this);timeBox.setGravity(Gravity.BOTTOM);row.addView(timeBox,new LinearLayout.LayoutParams(0,-2,1));
            TextView period=text(timeBox,hour<12?"오전":"오후",16);period.setPadding(0,0,dp(6),dp(16));
            TextView clock=text(timeBox,time,52);clock.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));clock.setSingleLine(true);clock.setAutoSizeTextTypeUniformWithConfiguration(32,52,2,android.util.TypedValue.COMPLEX_UNIT_SP);clock.setLayoutParams(new LinearLayout.LayoutParams(0,dp(78),1));
            Switch active=new Switch(this);AppStyle.tint(active);active.setChecked(p.getBoolean("enabled",false));active.setContentDescription(time+" 알람 활성화");row.addView(active,new LinearLayout.LayoutParams(dp(64),dp(48)));
            clock.setTextColor(active.isChecked()?AppStyle.WHITE:AppStyle.MUTED);period.setTextColor(clock.getCurrentTextColor());timeBox.setOnClickListener(v->edit(id));
            String title=p.getString("title","");if(!title.isEmpty()){TextView titleView=text(card,title,16);titleView.setMaxLines(2);titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);titleView.setOnClickListener(v->edit(id));}
            LinearLayout detailsRow=new LinearLayout(this);detailsRow.setGravity(Gravity.CENTER_VERTICAL);card.addView(detailsRow);
            TextView days=text(detailsRow,AlarmLogic.weekdayLabel(p.getInt("weekdays",127))+(p.getBoolean("repeat",true)?"":" · 한 번"),14);days.setTextColor(AppStyle.MUTED);days.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));days.setOnClickListener(v->edit(id));
            TextView expand=action("⌄","알람 상세 펼치기");detailsRow.addView(expand,new LinearLayout.LayoutParams(dp(48),dp(48)));
            text(card,"알람 전 "+p.getInt("before",20)+"분 · 알람 후 "+p.getInt("after",10)+"분",14).setTextColor(AppStyle.MUTED);
            LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);details.setVisibility(View.GONE);card.addView(details);
            if(p.getBoolean("skipHolidays",false))text(details,"공휴일에는 끄기",14);
            TextView modify=text(details,"수정",16);modify.setMinHeight(dp(48));modify.setOnClickListener(v->edit(id));
            TextView delete=text(details,"삭제",16);delete.setMinHeight(dp(48));delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("알람 삭제").setMessage(time+" 알람을 삭제할까요?").setPositiveButton("삭제",(d,w)->{AlarmStore.delete(this,id);render();}).setNegativeButton("취소",null).show());
            expand.setOnClickListener(v->{boolean open=details.getVisibility()!=View.VISIBLE;details.setVisibility(open?View.VISIBLE:View.GONE);expand.setText(open?"⌃":"⌄");expand.setContentDescription(open?"알람 상세 접기":"알람 상세 펼치기");});
            TextView permission=text(card,"정확한 알람 권한 필요",13);permission.setVisibility(active.isChecked()&&!Scheduler.allowed(this)?View.VISIBLE:View.GONE);
            active.setOnCheckedChangeListener((view,on)->{
                if(on&&!Scheduler.allowed(this)){active.setChecked(false);new AlertDialog.Builder(this).setTitle("정확한 알람 권한").setMessage("권한을 허용한 뒤 이 알람을 다시 켜 주세요.").setPositiveButton("설정",(d,w)->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())))).setNegativeButton("취소",null).show();return;}
                if(on){p.edit().putBoolean("enabled",true).apply();Scheduler.schedule(this,id,true);}else Scheduler.disable(this,id);permission.setVisibility(View.GONE);clock.setTextColor(on?AppStyle.WHITE:AppStyle.MUTED);period.setTextColor(clock.getCurrentTextColor());
            });
        }
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(8),0,dp(16));screen.addView(footer);
        TextView add=action("+","알람 추가");add.setTextSize(36);add.setTextColor(AppStyle.ON_ACCENT);add.setBackground(AppStyle.fill(AppStyle.PINK,dp(28)));footer.addView(add,new LinearLayout.LayoutParams(dp(56),dp(56)));add.setOnClickListener(v->edit(-1));
    }

    TextView chronoText(String value,int size,int color,boolean bold){
        TextView label=new TextView(this);label.setText(value);label.setTextSize(size);label.setTextColor(color);label.setPadding(0,dp(2),0,dp(2));if(bold)label.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return label;
    }
    LinearLayout chronoPanel(LinearLayout parent,int top){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(20),dp(22),dp(20));panel.setBackground(AppStyle.fill(AppStyle.SURFACE,dp(24)));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(top);parent.addView(panel,params);return panel;
    }
    String chronoTime(int hour,int minute){return String.format(Locale.KOREA,"%02d:%02d",hour%12==0?12:hour%12,minute);}
    void renderChrono(){
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setBackgroundColor(AppStyle.BLUE);setContentView(screen);
        screen.setOnApplyWindowInsetsListener((v,i)->{screen.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(24),dp(14),dp(16),dp(10));screen.addView(header);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_alarm_outline);icon.setColorFilter(AppStyle.PINK);header.addView(icon,new LinearLayout.LayoutParams(dp(34),dp(34)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setPadding(dp(12),0,0,0);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        brand.addView(chronoText("Minute Voice",20,AppStyle.WHITE,true));brand.addView(chronoText("Alarm",13,AppStyle.MUTED,false));
        TextView menu=action("⋮","메뉴");header.addView(menu,new LinearLayout.LayoutParams(dp(44),dp(48)));menu.setOnClickListener(v->AppStyle.menu(this,v));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(8),dp(20),dp(30));scroll.addView(content);
        LinearLayout addRow=new LinearLayout(this);addRow.setGravity(Gravity.END);content.addView(addRow);TextView add=AppStyle.action(this,"＋  알람 추가",true);add.setContentDescription("알람 추가");LinearLayout.LayoutParams addParams=new LinearLayout.LayoutParams(-2,dp(48));addParams.bottomMargin=dp(16);addRow.addView(add,addParams);add.setOnClickListener(v->edit(-1));
        List<Integer> ids=AlarmStore.ids(this);ids.sort(Comparator.comparingInt(id->{var p=AlarmStore.prefs(this,id);return p.getInt("hour",7)*60+p.getInt("minute",0);}));
        long now=System.currentTimeMillis(),next=Long.MAX_VALUE;int nextId=-1;
        for(int id:ids){var p=AlarmStore.prefs(this,id);if(!p.getBoolean("enabled",false))continue;
            long target=AlarmLogic.nextTarget(now,p.getInt("hour",7),p.getInt("minute",0),java.time.ZoneId.systemDefault(),p.getInt("weekdays",127),p.getBoolean("skipHolidays",false));
            if(target<next){next=target;nextId=id;}
        }
        LinearLayout hero=chronoPanel(content,0);
        hero.addView(chronoText("◉  다음 알람",14,Color.rgb(125,207,244),true));
        if(nextId<0){hero.addView(chronoText("알람을 추가해 보세요",27,AppStyle.WHITE,true));hero.addView(chronoText("설정한 알람이 여기에 표시됩니다",14,AppStyle.MUTED,false));}
        else {var p=AlarmStore.prefs(this,nextId);int hour=p.getInt("hour",7),minute=p.getInt("minute",0);
            LinearLayout heroTime=new LinearLayout(this);heroTime.setGravity(Gravity.BOTTOM);LinearLayout.LayoutParams timeParams=new LinearLayout.LayoutParams(-1,dp(72));timeParams.topMargin=dp(10);hero.addView(heroTime,timeParams);
            heroTime.addView(chronoText(chronoTime(hour,minute),48,AppStyle.WHITE,true));TextView heroPeriod=chronoText(hour<12?"오전":"오후",20,AppStyle.PINK,true);LinearLayout.LayoutParams periodParams=new LinearLayout.LayoutParams(-2,-2);periodParams.leftMargin=dp(8);periodParams.bottomMargin=dp(9);heroTime.addView(heroPeriod,periodParams);
            long remaining=Math.max(0,next-now)/60000;String countdown=remaining>=1440?(remaining/1440)+"일 "+((remaining%1440)/60)+"시간 후":remaining>=60?(remaining/60)+"시간 "+(remaining%60)+"분 후":remaining+"분 후";
            hero.addView(chronoText(countdown+" 울림",17,Color.rgb(255,187,105),true));String title=p.getString("title","");if(!title.isEmpty())hero.addView(chronoText(title,15,AppStyle.MUTED,false));
        }
        TextView section=chronoText("알람  "+ids.size()+"개",18,AppStyle.WHITE,true);LinearLayout.LayoutParams sectionParams=new LinearLayout.LayoutParams(-1,-2);sectionParams.topMargin=dp(28);content.addView(section,sectionParams);
        if(ids.isEmpty())content.addView(chronoText("위의 알람 추가를 눌러 시작하세요",15,AppStyle.MUTED,false));
        for(int id:ids){var p=AlarmStore.prefs(this,id);boolean enabled=p.getBoolean("enabled",false);int hour=p.getInt("hour",7),minute=p.getInt("minute",0);String time=chronoTime(hour,minute);
            LinearLayout card=chronoPanel(content,dp(12));
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row);
            LinearLayout timeBox=new LinearLayout(this);timeBox.setGravity(Gravity.BOTTOM);row.addView(timeBox,new LinearLayout.LayoutParams(0,dp(66),1));
            TextView period=chronoText(hour<12?"오전":"오후",16,enabled?AppStyle.PINK:AppStyle.MUTED,true);LinearLayout.LayoutParams periodParams=new LinearLayout.LayoutParams(-2,-2);periodParams.rightMargin=dp(5);periodParams.bottomMargin=dp(10);timeBox.addView(period,periodParams);
            TextView clock=chronoText(time,44,enabled?AppStyle.WHITE:AppStyle.MUTED,true);clock.setSingleLine(true);clock.setAutoSizeTextTypeUniformWithConfiguration(27,44,2,android.util.TypedValue.COMPLEX_UNIT_SP);timeBox.addView(clock,new LinearLayout.LayoutParams(0,-2,1));timeBox.setOnClickListener(v->edit(id));
            Switch active=new Switch(this);AppStyle.tint(active);active.setChecked(enabled);active.setContentDescription(time+" 알람 활성화");row.addView(active,new LinearLayout.LayoutParams(dp(64),dp(48)));
            String title=p.getString("title","");if(!title.isEmpty()){TextView titleView=chronoText(title,15,enabled?AppStyle.WHITE:AppStyle.MUTED,false);card.addView(titleView);titleView.setOnClickListener(v->edit(id));}
            LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams metaParams=new LinearLayout.LayoutParams(-1,-2);metaParams.topMargin=dp(8);card.addView(meta,metaParams);
            int mask=p.getInt("weekdays",127);LinearLayout days=chronoDays(mask,p.getBoolean("repeat",true),enabled);meta.addView(days,new LinearLayout.LayoutParams(0,-2,1));days.setOnClickListener(v->edit(id));
            TextView expand=chronoText("⌄",24,AppStyle.MUTED,false);expand.setGravity(Gravity.CENTER);expand.setContentDescription("알람 상세 펼치기");meta.addView(expand,new LinearLayout.LayoutParams(dp(40),dp(40)));
            card.addView(chronoText("알람 전 "+p.getInt("before",20)+"분 · 알람 후 "+p.getInt("after",10)+"분",13,AppStyle.MUTED,false));
            LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);details.setVisibility(View.GONE);card.addView(details);
            if(p.getBoolean("skipHolidays",false))details.addView(chronoText("공휴일에는 끄기",14,AppStyle.MUTED,false));
            TextView modify=chronoText("수정",16,AppStyle.WHITE,false);modify.setMinHeight(dp(48));details.addView(modify);modify.setOnClickListener(v->edit(id));
            TextView delete=chronoText("삭제",16,AppStyle.WHITE,false);delete.setMinHeight(dp(48));details.addView(delete);delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("알람 삭제").setMessage(time+" 알람을 삭제할까요?").setPositiveButton("삭제",(d,w)->{AlarmStore.delete(this,id);renderChrono();}).setNegativeButton("취소",null).show());
            expand.setOnClickListener(v->{boolean open=details.getVisibility()!=View.VISIBLE;details.setVisibility(open?View.VISIBLE:View.GONE);expand.setText(open?"⌃":"⌄");expand.setContentDescription(open?"알람 상세 접기":"알람 상세 펼치기");});
            TextView permission=chronoText("정확한 알람 권한 필요",13,Color.rgb(255,187,105),false);permission.setVisibility(enabled&&!Scheduler.allowed(this)?View.VISIBLE:View.GONE);card.addView(permission);
            active.setOnCheckedChangeListener((view,on)->{
                if(on&&!Scheduler.allowed(this)){active.setChecked(false);new AlertDialog.Builder(this).setTitle("정확한 알람 권한").setMessage("권한을 허용한 뒤 이 알람을 다시 켜 주세요.").setPositiveButton("설정",(d,w)->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())))).setNegativeButton("취소",null).show();return;}
                if(on){p.edit().putBoolean("enabled",true).apply();Scheduler.schedule(this,id,true);}else Scheduler.disable(this,id);renderChrono();
            });
        }
    }
    LinearLayout chronoDays(int mask,boolean repeat,boolean enabled){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        if(!repeat){TextView once=chronoText("한 번",12,AppStyle.MUTED,false);LinearLayout.LayoutParams onceParams=new LinearLayout.LayoutParams(-2,-2);onceParams.rightMargin=dp(5);row.addView(once,onceParams);}
        if(mask==127){TextView daily=chronoText("매일",13,enabled?AppStyle.dayColor(5,1,false,true):AppStyle.MUTED,true);daily.setPadding(dp(8),dp(3),dp(8),dp(3));daily.setBackground(AppStyle.fill(enabled?AppStyle.dayColor(5,1,false,false):AppStyle.CONTROL,dp(8)));row.addView(daily);return row;}
        String[] names={"일","월","화","수","목","금","토"};
        for(int i=0;i<7;i++){boolean chosen=(mask&(1<<i))!=0;TextView day=chronoText(names[i],13,chosen&&enabled?AppStyle.dayColor(5,i,false,true):AppStyle.MUTED,chosen);day.setGravity(Gravity.CENTER);day.setPadding(dp(5),dp(3),dp(5),dp(3));if(chosen)day.setBackground(AppStyle.fill(enabled?AppStyle.dayColor(5,i,false,false):AppStyle.CONTROL,dp(7)));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.rightMargin=dp(3);row.addView(day,params);}
        return row;
    }
}
