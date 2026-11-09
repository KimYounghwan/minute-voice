package kr.co.minutevoice;

import android.text.Html;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** News sources available for each alarm's spoken headlines. */
final class NewsSources {
    static final String DAUM="daum",YONHAP="yonhap",NATE="nate";
    static final String FORMAT="major-v3";
    static final String DAUM_URL="https://news.daum.net/home";
    static final String YONHAP_URL="https://www.yna.co.kr/theme/headlines-history";
    static final String NATE_URL="https://news.nate.com/";
    private static final Pattern YONHAP_TITLE=Pattern.compile("<span\\b[^>]*class\\s*=\\s*['\"][^'\"]*\\btitle01\\b[^'\"]*['\"][^>]*>(.*?)</span>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
    private static final Pattern NATE_TITLE=Pattern.compile("<h2\\b[^>]*>(.*?)</h2>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
    private static final Pattern DAUM_TITLE=Pattern.compile("<strong\\b[^>]*class\\s*=\\s*['\"][^'\"]*\\btit_txt\\b[^'\"]*['\"][^>]*>(.*?)</strong>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

    static String normalize(String source){return NATE.equals(source)?NATE:YONHAP.equals(source)?YONHAP:DAUM;}
    static String label(String source){String normalized=normalize(source);return NATE.equals(normalized)?"네이트 뉴스":YONHAP.equals(normalized)?"연합뉴스":"다음 뉴스";}
    static List<String> daumTitles(String html){
        return titles(html,"box_news_headline2","</ul>",DAUM_TITLE);
    }
    static List<String> yonhapTitles(String html){
        return titles(html,"box-juyonews-list01","</section>",YONHAP_TITLE);
    }
    static List<String> nateTitles(String html){
        return titles(html,"<div class=\"newsMainIssue\">","<!-- //newsMainIssue",NATE_TITLE);
    }
    static List<String> forSpeech(List<String> titles,int count){
        return count<=0?titles:titles.subList(0,Math.min(count,titles.size()));
    }
    static String countIntro(int available,int selected){
        if(available==0)return "현재 주요 뉴스는 없습니다. ";
        return "현재 주요 뉴스는 "+available+"건입니다. "+(selected<available?selected+"건을 읽어드립니다. ":"모두 읽어드립니다. ");
    }
    static List<String> fetchTitles(String source)throws Exception{
        String selected=normalize(source);
        String url=NATE.equals(selected)?NATE_URL:YONHAP.equals(selected)?YONHAP_URL:DAUM_URL;
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent","MinuteVoice/"+BuildConfig.VERSION_NAME);
        try{
            if(connection.getResponseCode()!=200)throw new IllegalStateException("HTTP error");
            try(InputStream in=connection.getInputStream()){
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int read;
                while(out.size()<300_000&&(read=in.read(buffer,0,Math.min(buffer.length,300_000-out.size())))>0)out.write(buffer,0,read);
                String html=new String(out.toByteArray(),NATE.equals(selected)?java.nio.charset.Charset.forName("EUC-KR"):StandardCharsets.UTF_8);
                return NATE.equals(selected)?nateTitles(html):YONHAP.equals(selected)?yonhapTitles(html):daumTitles(html);
            }
        }finally{connection.disconnect();}
    }
    private static List<String> titles(String html,String sectionStart,String sectionEnd,Pattern pattern){
        List<String> titles=new ArrayList<>();
        int start=html.indexOf(sectionStart);
        if(start<0)return titles;
        int end=html.indexOf(sectionEnd,start);
        if(end<0)return titles;
        Matcher matcher=pattern.matcher(html.substring(start,end));
        while(matcher.find()){
            String title=Html.fromHtml(matcher.group(1),Html.FROM_HTML_MODE_LEGACY).toString().replaceAll("\\s+"," ").trim();
            if(!title.isEmpty())titles.add(title);
        }
        return titles;
    }
    private NewsSources(){}
}
