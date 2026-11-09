package kr.co.minutevoice;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class AlarmEditActivity extends Activity {
    int ink,green,muted,appliedTheme;
    LinearLayout root,cityList; TextView clock, period, state, locationLabel; EditText title, before, after, newsReadCount; Switch repeat, skipHolidays, weather, includeCurrent, news; RadioButton daumNews,yonhapNews,nateNews; ToggleButton[] days = new ToggleButton[7];
    int hour, minute, alarmId;
    final int[] newsCounts={-1,-1,-1};boolean newsCountsRequested;int newsCountGeneration;
    double latitude=Double.NaN,longitude=Double.NaN;String cityName="";int cityLookupVersion;
    final ArrayList<WeatherCities.City> addedCities=new ArrayList<>();
    String soundUri="",soundName="음성 안내만";TextView soundLabel;
    int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
    @Override public void onCreate(Bundle b){
        appliedTheme=AppStyle.apply(this);ink=AppStyle.WHITE;green=AppStyle.WHITE;muted=AppStyle.MUTED;
        super.onCreate(b); setVolumeControlStream(AudioManager.STREAM_ALARM);
        alarmId=b==null?getIntent().getIntExtra("id",-1):b.getInt("alarmId",-1);if(alarmId<0)alarmId=AlarmStore.allocate(this);
        var p=Scheduler.prefs(this,alarmId);Calendar now=Calendar.getInstance();
        hour=b==null?p.getInt("hour",now.get(Calendar.HOUR_OF_DAY)):b.getInt("hour");
        minute=b==null?p.getInt("minute",now.get(Calendar.MINUTE)):b.getInt("minute");
        soundUri=b==null?p.getString("soundUri",""):b.getString("soundUri","");soundName=b==null?p.getString("soundName","음성 안내만"):b.getString("soundName","음성 안내만");
        if(b==null){if(p.getBoolean("hasLocation",false)){latitude=p.getLong("latitude",0)/1_000_000d;longitude=p.getLong("longitude",0)/1_000_000d;cityName=p.getString("cityName","");}}
        else{latitude=b.getDouble("latitude",Double.NaN);longitude=b.getDouble("longitude",Double.NaN);cityName=b.getString("cityName","");}
        addedCities.addAll(WeatherCities.parse(b==null?p.getString("weatherCities","[]"):b.getString("weatherCities","[]")));
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setBackgroundColor(AppStyle.BLUE);setContentView(screen);
        screen.setOnApplyWindowInsetsListener((v,insets)->{screen.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(16),dp(8),dp(16),dp(8));screen.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(4),dp(16),dp(24));scroll.addView(root);
        TextView back=iconAction("‹","알람 목록");toolbar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));back.setOnClickListener(v->finish());
        TextView toolbarTitle=new TextView(this);toolbarTitle.setText(AlarmStore.exists(this,alarmId)?"알람 수정":"알람 등록");toolbarTitle.setTextColor(ink);toolbarTitle.setTextSize(21);toolbarTitle.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));toolbar.addView(toolbarTitle,new LinearLayout.LayoutParams(0,-2,1));
        TextView topSave=AppStyle.action(this,"저장",true);toolbar.addView(topSave,new LinearLayout.LayoutParams(-2,dp(48)));topSave.setOnClickListener(v->save());
        TextView menu=iconAction("⋮","메뉴");toolbar.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));menu.setOnClickListener(v->AppStyle.menu(this,v));
        LinearLayout time=card();section(time,"알람 시각");period=label(time,"",14,AppStyle.PINK,true);clock=label(time,"",54,ink,true);clock.setGravity(Gravity.CENTER);period.setGravity(Gravity.CENTER);clock.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        View.OnClickListener changeTime=v->new TimePickerDialog(this,(view,h,m)->{hour=h;minute=m;updateClock();},hour,minute,false).show();
        clock.setOnClickListener(changeTime);period.setOnClickListener(changeTime);clock.setContentDescription("알람 시각 변경");
        label(time,"제목 등록 · 선택",16,ink,true);
        title=new EditText(this);title.setSingleLine(true);title.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);title.setTextSize(20);title.setTextColor(ink);title.setHint("제목을 입력하세요");title.setHintTextColor(muted);title.setContentDescription("알람 제목");title.setText(b==null?p.getString("title",""):b.getString("title",""));time.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));
        label(time,"제목을 비워두면 지정한 시각으로 안내해요",12,muted,false);
        LinearLayout settings=card();section(settings,"매분 안내 범위");
        LinearLayout wheels=new LinearLayout(this);wheels.setOrientation(LinearLayout.HORIZONTAL);settings.addView(wheels);
        before=minuteInput(wheels,"알람 전", "몇 분 전부터",b==null?String.valueOf(p.getInt("before",20)):b.getString("beforeInput",String.valueOf(b.getInt("before",20))));
        after=minuteInput(wheels,"알람 후", "몇 분 후까지",b==null?String.valueOf(p.getInt("after",10)):b.getString("afterInput",String.valueOf(b.getInt("after",10))));
        label(settings,"각각 0~180분 · 0분으로 설정하면 해당 안내 끄기",12,muted,false);
        LinearLayout schedule=card();section(schedule,"반복 일정");
        repeat=new Switch(this);AppStyle.tint(repeat);repeat.setTextColor(ink);repeat.setPadding(0,dp(8),0,dp(8));repeat.setChecked(b==null?p.getBoolean("repeat",true):b.getBoolean("repeat"));markSwitch(repeat,"반복 알람");repeat.setOnCheckedChangeListener((v,checked)->markSwitch(repeat,"반복 알람"));schedule.addView(repeat);
        divider(schedule);
        label(schedule,"알람 요일",14,muted,true);
        LinearLayout dayRow=new LinearLayout(this);dayRow.setOrientation(LinearLayout.HORIZONTAL);schedule.addView(dayRow,new LinearLayout.LayoutParams(-1,dp(52)));
        String[] dayNames={"일","월","화","수","목","금","토"};
        int mask=b==null?p.getInt("weekdays",127):b.getInt("weekdays");
        for(int i=0;i<7;i++){
            ToggleButton day=new ToggleButton(this);days[i]=day;day.setTag(i);day.setTextOn(dayNames[i]+"✓");day.setTextOff(dayNames[i]);day.setTextSize(15);day.setSingleLine(true);day.setPadding(0,0,0,0);day.setMinWidth(0);day.setMinimumWidth(0);day.setChecked((mask&(1<<i))!=0);styleDay(day);
            LinearLayout.LayoutParams dayParams=new LinearLayout.LayoutParams(0,dp(48),1);dayParams.setMargins(dp(1),0,dp(1),0);dayRow.addView(day,dayParams);day.setOnCheckedChangeListener((view,checked)->styleDay(day));
        }
        divider(schedule);
        skipHolidays=new Switch(this);AppStyle.tint(skipHolidays);skipHolidays.setTextColor(ink);skipHolidays.setPadding(0,dp(16),0,dp(8));skipHolidays.setChecked(b==null?p.getBoolean("skipHolidays",!AlarmStore.exists(this,alarmId)):b.getBoolean("skipHolidays"));markSwitch(skipHolidays,"공휴일에는 끄기");skipHolidays.setOnCheckedChangeListener((v,checked)->markSwitch(skipHolidays,"공휴일에는 끄기"));schedule.addView(skipHolidays);
        LinearLayout briefing=card();section(briefing,"음성 안내 콘텐츠");
        weather=new Switch(this);AppStyle.tint(weather);weather.setTextColor(ink);weather.setChecked(b==null?p.getBoolean("weather",!AlarmStore.exists(this,alarmId)):b.getBoolean("weather"));markSwitch(weather,"오늘 날씨 알려주기");weather.setOnCheckedChangeListener((v,checked)->markSwitch(weather,"오늘 날씨 알려주기"));briefing.addView(weather);
        includeCurrent=new Switch(this);AppStyle.tint(includeCurrent);includeCurrent.setTextColor(ink);includeCurrent.setChecked(b==null?p.getBoolean("includeCurrent",p.getBoolean("weather",true)):b.getBoolean("includeCurrent"));markSwitch(includeCurrent,"현재 도시 포함");briefing.addView(includeCurrent);
        locationLabel=label(briefing,"",13,muted,false);updateLocationLabel();
        Button findLocation=button("현재 위치 다시 찾기",false);briefing.addView(findLocation);findLocation.setOnClickListener(v->requestLocation());
        includeCurrent.setOnCheckedChangeListener((v,checked)->{markSwitch(includeCurrent,"현재 도시 포함");updateLocationLabel();if(checked&&Double.isNaN(latitude))requestLocation();});
        label(briefing,"날씨 읽을 도시",16,ink,true);
        cityList=new LinearLayout(this);cityList.setOrientation(LinearLayout.VERTICAL);briefing.addView(cityList);renderCityList();
        Button addCity=button("＋ 도시 검색·추가",false);briefing.addView(addCity);addCity.setOnClickListener(v->searchCity());
        divider(briefing);
        news=new Switch(this);AppStyle.tint(news);news.setTextColor(ink);news.setChecked(b==null?p.getBoolean("news",false):b.getBoolean("news"));markSwitch(news,"오늘의 주요 뉴스 듣기");briefing.addView(news);
        LinearLayout newsOptions=new LinearLayout(this);newsOptions.setOrientation(LinearLayout.VERTICAL);briefing.addView(newsOptions);
        RadioGroup newsSources=new RadioGroup(this);newsSources.setOrientation(RadioGroup.VERTICAL);newsOptions.addView(newsSources);
        daumNews=new RadioButton(this);daumNews.setId(View.generateViewId());AppStyle.tint(daumNews);newsSources.addView(daumNews,new RadioGroup.LayoutParams(-1,dp(44)));
        yonhapNews=new RadioButton(this);yonhapNews.setId(View.generateViewId());AppStyle.tint(yonhapNews);newsSources.addView(yonhapNews,new RadioGroup.LayoutParams(-1,dp(44)));
        nateNews=new RadioButton(this);nateNews.setId(View.generateViewId());AppStyle.tint(nateNews);newsSources.addView(nateNews,new RadioGroup.LayoutParams(-1,dp(44)));
        String selectedNews=NewsSources.normalize(b==null?p.getString("newsSource",NewsSources.DAUM):b.getString("newsSource",NewsSources.DAUM));
        (NewsSources.NATE.equals(selectedNews)?nateNews:NewsSources.YONHAP.equals(selectedNews)?yonhapNews:daumNews).setChecked(true);markNewsSource();newsSources.setOnCheckedChangeListener((group,checkedId)->markNewsSource());
        LinearLayout countRow=new LinearLayout(this);countRow.setGravity(Gravity.CENTER_VERTICAL);newsOptions.addView(countRow,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView countLabel=new TextView(this);countLabel.setText("읽어줄 뉴스 개수\n0은 전체");countLabel.setTextColor(ink);countLabel.setTextSize(14);countRow.addView(countLabel,new LinearLayout.LayoutParams(0,-2,1));
        newsReadCount=new EditText(this);newsReadCount.setSingleLine(true);newsReadCount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);newsReadCount.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});newsReadCount.setTextSize(18);newsReadCount.setTextColor(ink);newsReadCount.setGravity(Gravity.CENTER);newsReadCount.setSelectAllOnFocus(true);newsReadCount.setBackground(AppStyle.fill(AppStyle.CONTROL,dp(14)));newsReadCount.setContentDescription("읽어줄 뉴스 개수, 0은 전체");newsReadCount.setText(b==null?String.valueOf(p.getInt("newsReadCount",0)):b.getString("newsReadCountInput",String.valueOf(b.getInt("newsReadCount",0))));countRow.addView(newsReadCount,new LinearLayout.LayoutParams(dp(88),dp(48)));
        newsOptions.setVisibility(news.isChecked()?View.VISIBLE:View.GONE);if(news.isChecked())loadNewsCounts();news.setOnCheckedChangeListener((v,checked)->{markSwitch(news,"오늘의 주요 뉴스 듣기");newsOptions.setVisibility(checked?View.VISIBLE:View.GONE);if(checked)loadNewsCounts();else{newsCountGeneration++;newsCountsRequested=false;Arrays.fill(newsCounts,-1);markNewsSource();}});
        LinearLayout sounds=card();section(sounds,"알람 소리");soundLabel=label(sounds,soundName,15,muted,true);soundLabel.setMaxLines(2);soundLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        Button choose=button("소리 목록 / 노래 검색",false);sounds.addView(choose);choose.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("알람 소리 선택").setItems(new String[]{"기본 알람 소리 목록","휴대폰 노래 검색 / 선택","음성 안내만"},(d,which)->chooseSound(which)).show());
        state=label(root,"",14,AppStyle.PINK,true);state.setPadding(dp(8),dp(20),dp(8),dp(12));
        label(root,"음량 버튼으로 알람 음량을 조절할 수 있어요.",12,muted,false);
        Button tts=button("음성 / 알람 권한 설정",false);root.addView(tts);tts.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("설정").setItems(new String[]{"텍스트 음성 변환 설정","정확한 알람 권한","앱 알림 설정"},(d,n)->{try{if(n==0)startActivity(new Intent("com.android.settings.TTS_SETTINGS"));else if(n==1&&Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}).show());
        updateClock();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},4);
        else if(b==null&&!AlarmStore.exists(this,alarmId)&&weather.isChecked()&&includeCurrent.isChecked()&&Double.isNaN(latitude))requestLocation();
    }
    void renderCityList(){
        if(cityList==null)return;
        cityList.removeAllViews();
        if(addedCities.isEmpty()){label(cityList,"추가한 도시가 없어요",13,muted,false);return;}
        for(WeatherCities.City city:new ArrayList<>(addedCities)){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);cityList.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));
            TextView name=new TextView(this);name.setText(city.display());name.setTextColor(ink);name.setTextSize(14);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            TextView remove=new TextView(this);remove.setText("×");remove.setTextColor(AppStyle.PINK);remove.setTextSize(26);remove.setGravity(Gravity.CENTER);remove.setContentDescription(city.name+" 삭제");row.addView(remove,new LinearLayout.LayoutParams(dp(44),-1));remove.setOnClickListener(v->{addedCities.remove(city);renderCityList();});
            View line=new View(this);line.setBackgroundColor(AppStyle.LINE);LinearLayout.LayoutParams lineParams=new LinearLayout.LayoutParams(-1,dp(1));lineParams.bottomMargin=dp(4);cityList.addView(line,lineParams);
        }
    }
    void searchCity(){
        EditText query=new EditText(this);query.setSingleLine(true);query.setHint("도시 이름 입력 · 예: 부산, 파리");query.setInputType(android.text.InputType.TYPE_CLASS_TEXT);query.setTextColor(ink);query.setHintTextColor(muted);
        new AlertDialog.Builder(this).setTitle("날씨 읽을 도시 검색").setView(query).setPositiveButton("검색",(dialog,which)->{
            String text=query.getText().toString().trim();if(text.length()<2){Toast.makeText(this,"도시 이름을 2자 이상 입력해 주세요",Toast.LENGTH_LONG).show();return;}
            Toast.makeText(this,"도시를 찾는 중이에요",Toast.LENGTH_SHORT).show();
            new Thread(()->{
                try{List<WeatherCities.City> results=WeatherCities.search(text);runOnUiThread(()->showCityResults(results));}
                catch(Exception e){runOnUiThread(()->Toast.makeText(this,"도시를 검색할 수 없어요. 인터넷 연결을 확인해 주세요.",Toast.LENGTH_LONG).show());}
            },"minutevoice-city-search").start();
        }).setNegativeButton("취소",null).show();
    }
    void showCityResults(List<WeatherCities.City> results){
        if(isFinishing())return;
        if(results.isEmpty()){Toast.makeText(this,"검색된 도시가 없어요",Toast.LENGTH_LONG).show();return;}
        String[] choices=new String[results.size()];for(int i=0;i<results.size();i++)choices[i]=results.get(i).display();
        new AlertDialog.Builder(this).setTitle("도시 선택").setItems(choices,(dialog,which)->{
            WeatherCities.City city=results.get(which);
            if(includeCurrent.isChecked()&&!Double.isNaN(latitude)&&WeatherCities.samePlace(WeatherCities.current(cityName,latitude,longitude),city)){Toast.makeText(this,"현재 도시가 이미 포함돼 있어요",Toast.LENGTH_SHORT).show();return;}
            for(WeatherCities.City existing:addedCities)if(WeatherCities.samePlace(existing,city)){Toast.makeText(this,"이미 추가한 도시예요",Toast.LENGTH_SHORT).show();return;}
            if(addedCities.size()>=WeatherCities.MAX_ADDED){Toast.makeText(this,"도시는 최대 "+WeatherCities.MAX_ADDED+"개까지 추가할 수 있어요",Toast.LENGTH_LONG).show();return;}
            addedCities.add(city);renderCityList();
        }).setNegativeButton("취소",null).show();
    }
    void updateLocationLabel(){if(locationLabel!=null)locationLabel.setText(includeCurrent!=null&&!includeCurrent.isChecked()?"현재 도시 읽기 꺼짐":Double.isNaN(latitude)?"현재 도시를 읽으려면 위치를 설정해 주세요":"저장된 위치: "+(cityName.isEmpty()?"도시 확인 중":cityName));}
    void requestLocation(){
        if(checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},27);return;}
        try{
            LocationManager manager=getSystemService(LocationManager.class);
            String provider=manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?LocationManager.NETWORK_PROVIDER:LocationManager.GPS_PROVIDER;
            Location last=manager.getLastKnownLocation(provider);if(last!=null)rememberLocation(last);
            if(Build.VERSION.SDK_INT>=30)manager.getCurrentLocation(provider,null,command->new Handler(Looper.getMainLooper()).post(command),location->{if(location!=null)rememberLocation(location);else if(Double.isNaN(latitude))Toast.makeText(this,"현재 위치를 확인할 수 없어요. 기기 위치 서비스를 켜 주세요.",Toast.LENGTH_LONG).show();});
            else manager.requestSingleUpdate(provider,this::rememberLocation,Looper.getMainLooper());
        }catch(Exception e){Toast.makeText(this,"현재 위치를 확인할 수 없어요. 기기 위치 서비스를 켜 주세요.",Toast.LENGTH_LONG).show();}
    }
    void rememberLocation(Location location){
        latitude=location.getLatitude();longitude=location.getLongitude();cityName="";updateLocationLabel();
        int lookup=++cityLookupVersion;double lat=latitude,lon=longitude;
        new Thread(()->{String resolved=WeatherLocation.resolve(this,lat,lon);runOnUiThread(()->{
            if(lookup!=cityLookupVersion||isFinishing())return;
            cityName=resolved;updateLocationLabel();
        });},"minutevoice-city").start();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==27&&grants.length>0&&grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)requestLocation();else if(code==27){includeCurrent.setChecked(false);Toast.makeText(this,"현재 도시는 제외하고 추가한 도시의 날씨를 안내해요",Toast.LENGTH_LONG).show();}else if(code==4&&weather.isChecked()&&includeCurrent.isChecked()&&Double.isNaN(latitude))requestLocation();}
    void chooseSound(int which){
        try{
            if(which==0){Intent pick=new Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,android.media.RingtoneManager.TYPE_ALARM).putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE,"알람 소리").putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true).putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,Settings.System.DEFAULT_ALARM_ALERT_URI).putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,soundUri.isEmpty()?null:Uri.parse(soundUri));startActivityForResult(pick,20);}
            else if(which==1)startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),21);
            else{soundUri="";soundName="음성 안내만";soundLabel.setText(soundName);}
        }catch(ActivityNotFoundException e){Toast.makeText(this,"소리 선택 앱을 찾을 수 없어요",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||(request!=20&&request!=21))return;
        Uri uri=request==20?data.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI):data.getData();
        if(uri==null){if(request==20){soundUri="";soundName="음성 안내만";soundLabel.setText(soundName);}return;}
        try{
            if(request==21)getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String name="선택한 소리";
            if(request==20){android.media.Ringtone r=android.media.RingtoneManager.getRingtone(this,uri);if(r!=null)name=r.getTitle(this);}
            else try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
            soundUri=uri.toString();soundName=name;soundLabel.setText(soundName);
        }catch(Exception e){Toast.makeText(this,"파일 접근 권한을 유지할 수 없어요. 휴대폰에 저장한 다른 파일을 선택해 주세요.",Toast.LENGTH_LONG).show();}
    }
    TextView iconAction(String symbol,String description){TextView view=new TextView(this);view.setText(symbol);view.setTextColor(ink);view.setTextSize(28);view.setGravity(Gravity.CENTER);view.setContentDescription(description);view.setBackground(AppStyle.fill(AppStyle.CONTROL,dp(24)));return view;}
    void markSwitch(Switch control,String title){control.setText(title+" · "+(control.isChecked()?"켜짐":"꺼짐"));}
    void markNewsSource(){
        RadioButton[] buttons={daumNews,yonhapNews,nateNews};
        String[] labels={"다음 뉴스","연합뉴스","네이트 뉴스"};
        for(int i=0;i<buttons.length;i++){
            RadioButton button=buttons[i];
            String count=newsCounts[i]>=0?newsCounts[i]+"건":newsCounts[i]==-2?"확인 불가":"확인 중";
            button.setText(labels[i]+" · "+count+(button.isChecked()?" ✓":""));
            button.setTextColor(button.isChecked()?ink:muted);
        }
    }
    void loadNewsCounts(){
        if(newsCountsRequested)return;
        newsCountsRequested=true;
        final int generation=++newsCountGeneration;
        String[] sources={NewsSources.DAUM,NewsSources.YONHAP,NewsSources.NATE};
        for(int i=0;i<sources.length;i++){
            final int index=i;final String source=sources[i];
            new Thread(()->{
                int count;try{count=NewsSources.fetchTitles(source).size();}catch(Exception e){count=-2;}
                final int result=count;
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&news.isChecked()&&generation==newsCountGeneration){newsCounts[index]=result;markNewsSource();}});
            },"minutevoice-news-count-"+source).start();
        }
    }
    String selectedNewsSource(){return nateNews.isChecked()?NewsSources.NATE:yonhapNews.isChecked()?NewsSources.YONHAP:NewsSources.DAUM;}
    void section(LinearLayout parent,String heading){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(44));rp.bottomMargin=dp(8);parent.addView(row,rp);View marker=new View(this);marker.setBackground(AppStyle.fill(AppStyle.PINK,dp(2)));row.addView(marker,new LinearLayout.LayoutParams(dp(4),dp(20)));TextView title=AppStyle.sectionTitle(this,heading);title.setPadding(dp(12),0,0,0);row.addView(title,new LinearLayout.LayoutParams(0,-1,1));}
    void divider(LinearLayout parent){View line=new View(this);line.setBackgroundColor(AppStyle.LINE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(18);lp.bottomMargin=dp(12);parent.addView(line,lp);}
    TextView label(LinearLayout parent,String text,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(4),0,dp(4));if(bold)v.setTypeface(null,Typeface.BOLD);parent.addView(v);return v;}
    LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(18),dp(16),dp(18),dp(18));v.setBackground(AppStyle.surface(this));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);root.addView(v,lp);return v;}
    EditText minuteInput(LinearLayout parent,String heading,String description,String value){
        LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams columnParams=new LinearLayout.LayoutParams(0,-2,1);columnParams.rightMargin=dp(8);parent.addView(column,columnParams);
        label(column,heading,16,ink,true);label(column,description,12,muted,false);
        LinearLayout field=new LinearLayout(this);field.setGravity(Gravity.CENTER_VERTICAL);field.setPadding(dp(12),0,dp(12),0);field.setBackground(AppStyle.fill(AppStyle.CONTROL,dp(16)));LinearLayout.LayoutParams fieldParams=new LinearLayout.LayoutParams(-1,dp(56));fieldParams.topMargin=dp(10);column.addView(field,fieldParams);
        EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});input.setTextSize(22);input.setTextColor(ink);input.setSelectAllOnFocus(true);input.setPadding(0,0,0,0);input.setBackgroundColor(Color.TRANSPARENT);input.setContentDescription(heading+" 안내 분");input.setText(value);field.addView(input,new LinearLayout.LayoutParams(0,-1,1));
        TextView unit=new TextView(this);unit.setText("분");unit.setTextSize(14);unit.setTextColor(muted);field.addView(unit);return input;
    }
    int minuteValue(EditText input){
        String value=input.getText().toString().trim();int minutes;
        try{minutes=Integer.parseInt(value);}catch(NumberFormatException e){minutes=-1;}
        if(minutes<0||minutes>180){input.setError("0~180 사이의 숫자를 입력해 주세요");input.requestFocus();return -1;}
        input.setError(null);return minutes;
    }
    int newsCountValue(){
        int count;try{count=Integer.parseInt(newsReadCount.getText().toString().trim());}catch(NumberFormatException e){count=-1;}
        if(!news.isChecked())return count>=0&&count<=100?count:0;
        if(count<0||count>100){newsReadCount.setError("0~100 사이의 숫자를 입력해 주세요");newsReadCount.requestFocus();return -1;}
        newsReadCount.setError(null);return count;
    }
    Button button(String title,boolean primary){Button b=new Button(this);b.setText(title);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setAllCaps(false);b.setTextColor(primary?AppStyle.ON_ACCENT:AppStyle.PINK);b.setPadding(dp(18),0,dp(18),0);b.setMinWidth(0);b.setMinimumWidth(0);b.setBackground(AppStyle.fill(primary?AppStyle.PINK:AppStyle.CONTROL,dp(24)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(48));lp.topMargin=dp(10);lp.bottomMargin=dp(4);b.setLayoutParams(lp);return b;}
    void styleDay(ToggleButton day){
        int weekday=(Integer)day.getTag();
        boolean checked=day.isChecked();
        GradientDrawable bg=new GradientDrawable();bg.setColor(AppStyle.dayColor(appliedTheme,weekday,checked,false));bg.setCornerRadius(dp(12));
        day.setBackground(bg);day.setTextColor(AppStyle.dayColor(appliedTheme,weekday,checked,true));String name=new String[]{"일","월","화","수","목","금","토"}[weekday];day.setContentDescription(name+"요일 "+(checked?"선택됨":"선택 안 됨"));
    }
    int weekdayMask(){int mask=0;for(int i=0;i<7;i++)if(days[i].isChecked())mask|=1<<i;return mask;}
    void updateClock(){
        period.setText(hour<12?"오전":"오후");clock.setText(String.format(Locale.KOREA,"%02d:%02d",hour%12==0?12:hour%12,minute));
    }
    void save(){int pre=minuteValue(before);if(pre<0)return;int post=minuteValue(after);if(post<0)return;int readCount=newsCountValue();if(readCount<0)return;
        if(weather.isChecked()&&includeCurrent.isChecked()&&Double.isNaN(latitude)){Toast.makeText(this,"현재 도시를 포함하려면 위치를 먼저 설정해 주세요",Toast.LENGTH_LONG).show();requestLocation();return;}
        if(weather.isChecked()&&!includeCurrent.isChecked()&&addedCities.isEmpty()){Toast.makeText(this,"날씨를 읽을 도시를 추가하거나 현재 도시를 선택해 주세요",Toast.LENGTH_LONG).show();return;}
        int weekdays=weekdayMask();if(weekdays==0){Toast.makeText(this,"알람 요일을 하나 이상 선택해 주세요",Toast.LENGTH_LONG).show();return;}
        if(!Scheduler.allowed(this)){new AlertDialog.Builder(this).setTitle("정확한 알람 권한이 필요해요").setMessage("매분 제시간에 안내하려면 ‘알람 및 리마인더’를 허용해 주세요. 돌아온 뒤 저장 버튼을 눌러 주세요.").setPositiveButton("설정 열기",(d,w)->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())))).setNegativeButton("취소",null).show();return;}
        Scheduler.disable(this,alarmId);Scheduler.prefs(this,alarmId).edit().putString("soundUri",soundUri).putString("soundName",soundName).putString("title",title.getText().toString().trim()).putInt("hour",hour).putInt("minute",minute).putInt("before",pre).putInt("after",post).putInt("weekdays",weekdays).putBoolean("skipHolidays",skipHolidays.isChecked()).putBoolean("repeat",repeat.isChecked()).putBoolean("weather",weather.isChecked()).putBoolean("includeCurrent",includeCurrent.isChecked()).putString("weatherCities",WeatherCities.encode(addedCities)).putBoolean("news",news.isChecked()).putString("newsSource",selectedNewsSource()).putInt("newsReadCount",readCount).putBoolean("hasLocation",!Double.isNaN(latitude)).putLong("latitude",Double.isNaN(latitude)?0:Math.round(latitude*1_000_000)).putLong("longitude",Double.isNaN(longitude)?0:Math.round(longitude*1_000_000)).putString("cityName",cityName).putBoolean("enabled",true).apply();AlarmStore.register(this,alarmId);Scheduler.schedule(this,alarmId,true);refresh();Toast.makeText(this,"알람을 저장했어요",Toast.LENGTH_SHORT).show();finish();
    }
    void refresh(){if(state==null)return;var p=Scheduler.prefs(this,alarmId);boolean on=p.getBoolean("enabled",false);state.setText(on?(Scheduler.allowed(this)?"● 알람 켜짐 · 다음 안내\n"+new SimpleDateFormat("M월 d일 (E) a h:mm",Locale.KOREA).format(new Date(p.getLong("next",0))):"정확한 알람 권한을 허용해 주세요"):"○ 알람이 꺼져 있어요");if(on&&p.getBoolean("skipHolidays",false)&&!KoreanHolidays.supported(java.time.Instant.ofEpochMilli(p.getLong("target",0)).atZone(java.time.ZoneId.systemDefault()).toLocalDate()))state.append("\n이 예약은 공휴일 자료 범위 밖이에요. 공휴일 제외를 적용하려면 앱 업데이트가 필요해요.");}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("alarmId",alarmId);out.putString("soundUri",soundUri);out.putString("soundName",soundName);out.putInt("hour",hour);out.putInt("minute",minute);out.putString("title",title.getText().toString());out.putString("beforeInput",before.getText().toString());out.putString("afterInput",after.getText().toString());out.putBoolean("repeat",repeat.isChecked());out.putInt("weekdays",weekdayMask());out.putBoolean("skipHolidays",skipHolidays.isChecked());out.putBoolean("weather",weather.isChecked());out.putBoolean("includeCurrent",includeCurrent.isChecked());out.putString("weatherCities",WeatherCities.encode(addedCities));out.putBoolean("news",news.isChecked());out.putString("newsSource",selectedNewsSource());out.putString("newsReadCountInput",newsReadCount.getText().toString());out.putDouble("latitude",latitude);out.putDouble("longitude",longitude);out.putString("cityName",cityName);}
    @Override protected void onResume(){super.onResume();if(appliedTheme!=AppStyle.selected(this)){recreate();return;}refresh();}
}
