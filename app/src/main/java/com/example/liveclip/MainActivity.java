package com.example.liveclip;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;
import android.graphics.BitmapFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int INK=Color.rgb(28,45,49),MUTED=Color.rgb(104,120,121),TEAL=Color.rgb(17,129,120),SURFACE=Color.WHITE,BG=Color.rgb(247,249,247);
    private final ExecutorService work=Executors.newFixedThreadPool(2);
    private EditText input; private TextView status; private LinearLayout results; private WebView webView,loginView;
    private int generation=0;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);render();String shared=getIntent().getStringExtra(Intent.EXTRA_TEXT);if(shared!=null)input.setText(shared);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(INK);v.setPadding(0,dp(5),0,dp(5));return v;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackground(shape(TEAL,14));b.setOnClickListener(v->action.run());return b;}
    private LinearLayout card(LinearLayout parent){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(15),dp(16),dp(16));box.setBackground(shape(SURFACE,20));box.setElevation(dp(2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(15);parent.addView(box,lp);return box;}
    private void render(){getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(28),dp(20),dp(38));page.setBackgroundColor(BG);scroll.addView(page);setContentView(scroll);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams headerLp=new LinearLayout.LayoutParams(-1,-2);headerLp.bottomMargin=dp(18);page.addView(header,headerLp);
        ImageView mark=new ImageView(this);mark.setImageResource(com.example.liveclip.R.drawable.ic_launcher_foreground);mark.setBackground(shape(TEAL,16));mark.setPadding(dp(5),dp(5),dp(5),dp(5));header.addView(mark,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.setPadding(dp(14),0,0,0);header.addView(titles);TextView title=text("拾影",27);title.setTypeface(null,Typeface.BOLD);titles.addView(title);TextView subtitle=text("留住每一帧喜欢",13);subtitle.setTextColor(MUTED);titles.addView(subtitle);
        LinearLayout inputCard=card(page);TextView heading=text("粘贴作品链接",19);heading.setTypeface(null,Typeface.BOLD);inputCard.addView(heading);TextView hint=text("支持视频、图文和可获取动态短片的实况作品",13);hint.setTextColor(MUTED);inputCard.addView(hint);
        input=new EditText(this);input.setMinLines(3);input.setMaxLines(6);input.setGravity(Gravity.TOP);input.setTextSize(15);input.setTextColor(INK);input.setPadding(dp(12),dp(12),dp(12),dp(12));input.setHint("在这里粘贴抖音分享内容…");input.setBackground(shape(BG,12));LinearLayout.LayoutParams inputLp=new LinearLayout.LayoutParams(-1,dp(128));inputLp.topMargin=dp(10);inputLp.bottomMargin=dp(12);inputCard.addView(input,inputLp);
        LinearLayout row=new LinearLayout(this);inputCard.addView(row);Button paste=button("粘贴",()->{ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);ClipData clip=cb.getPrimaryClip();if(clip!=null&&clip.getItemCount()>0)input.setText(clip.getItemAt(0).coerceToText(this));});paste.setBackground(shape(Color.rgb(228,241,237),13));paste.setTextColor(TEAL);LinearLayout.LayoutParams pasteLp=new LinearLayout.LayoutParams(0,dp(48),1);pasteLp.rightMargin=dp(10);row.addView(paste,pasteLp);row.addView(button("开始解析",this::parse),new LinearLayout.LayoutParams(0,dp(48),2));
        LinearLayout statusCard=card(page);status=text("准备就绪 · 粘贴分享链接开始解析",14);status.setTextColor(TEAL);statusCard.addView(status);
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);page.addView(results,new LinearLayout.LayoutParams(-1,-2));
        TextView foot=text("仅保存你有权保存的内容 · 文件存入 下载/LiveClip",12);foot.setTextColor(MUTED);page.addView(foot);
    }
    private void message(String s){status.setText(s);}
    private static HttpURLConnection connection(String raw) throws Exception {URL url=new URL(raw);HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36");return c;}
    private static final class Page {final String html,url;Page(String html,String url){this.html=html;this.url=url;}}
    private Page fetchPage(String raw) throws Exception {for(int i=0;i<6;i++){if(!MediaParser.allowed(raw))throw new Exception("跳转到非抖音域名，已停止解析");HttpURLConnection c=connection(raw);try{int code=c.getResponseCode();if(code>=300&&code<400){String location=c.getHeaderField("Location");if(location==null)throw new Exception("跳转缺少目标地址");raw=new URL(new URL(raw),location).toString();continue;}if(code!=200)throw new Exception("分享页返回 HTTP "+code);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;try(java.io.InputStream in=c.getInputStream()){while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>6_000_000)throw new Exception("页面数据过大");}}return new Page(out.toString(StandardCharsets.UTF_8.name()),raw);}finally{c.disconnect();}}throw new Exception("分享链接跳转次数过多");}
    private JSONObject fetchFeed(String host,String id) throws Exception {
        HttpURLConnection c=connection("https://"+host+"/aweme/v1/feed/?aweme_id="+id+"&aid=1128");
        c.setReadTimeout(10000);
        try{
            if(c.getResponseCode()!=200)throw new Exception("备用作品接口不可用");
            try(java.io.InputStream in=c.getInputStream()){
                byte[] bytes=in.readNBytes(6_000_001);
                if(bytes.length>6_000_000)throw new Exception("作品数据过大");
                return new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            }
        }finally{c.disconnect();}
    }
    private MediaParser.Result feedFallback(String id) throws Exception {
        if(!id.matches("[0-9]{5,30}"))throw new Exception("无法识别作品 ID");
        for(String host:new String[]{"api5-normal-c-hl.amemv.com","aweme.snssdk.com"}){
            try{
                JSONObject data=fetchFeed(host,id);
                if(!id.equals(MediaParser.itemId(data)))continue;
                return enrich(data,MediaParser.parse(data));
            }catch(Exception ignored){}
        }
        throw new Exception("分享页和备用接口均未提供作品数据");
    }
    private JSONObject fetchDetail(String id) throws Exception {
        String query="device_platform=webapp&aid=6383&channel=channel_pc_web&aweme_id="+id;
        String cookie=CookieManager.getInstance().getCookie("https://www.douyin.com");
        String uifid="";
        if(cookie!=null)for(String part:cookie.split(";")){
            String[] pair=part.trim().split("=",2);
            if(pair.length==2&&pair[0].equalsIgnoreCase("UIFID")){uifid=pair[1];break;}
        }
        if(!uifid.isEmpty()){
            long timestamp=System.currentTimeMillis()/1000;
            query+="&uifid="+java.net.URLEncoder.encode(uifid,StandardCharsets.UTF_8)+"&timestamp="+timestamp;
            String plain=uifid+"_"+timestamp+"_A96D855A08C0A9707F8BEF0D9A527E4E_"+query;
            byte[] digest=MessageDigest.getInstance("MD5").digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            query+="&x-secsdk-web-signature="+hex;
        }
        HttpURLConnection c=connection("https://www.douyin.com/aweme/v1/web/aweme/detail/?"+query);
        c.setRequestProperty("Referer","https://www.douyin.com/");
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
        if(cookie!=null&&!cookie.isEmpty())c.setRequestProperty("Cookie",cookie);
        if(!uifid.isEmpty())c.setRequestProperty("uifid",uifid);
        try {
            if(c.getResponseCode()!=200)throw new Exception("作品详情不可用");
            try(java.io.InputStream in=c.getInputStream()){
                byte[] bytes=in.readNBytes(6_000_001);
                if(bytes.length>6_000_000)throw new Exception("作品详情过大");
                return new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            }
        } finally {c.disconnect();}
    }
    private MediaParser.Result enrich(JSONObject data, MediaParser.Result base) {
        if(base.images.isEmpty()||base.images.stream().allMatch(a->!a.live.isEmpty()))return base;
        String id=MediaParser.itemId(data);
        if(id.isEmpty())return base;
        try{return MediaParser.mergeMotion(base,MediaParser.parse(fetchDetail(id)));}
        catch(Exception ignored){}
        for(String host:new String[]{"api5-normal-c-hl.amemv.com","aweme.snssdk.com"}){
            try{
                JSONObject feed=fetchFeed(host,id);
                if(id.equals(MediaParser.itemId(feed)))return MediaParser.mergeMotion(base,MediaParser.parse(feed));
            }catch(Exception ignored){}
        }
        return base;
    }
    private void parse(){String text=input.getText().toString();final String url;try{url=MediaParser.shareUrl(text);}catch(Exception e){message(e.getMessage());return;}int current=++generation;results.removeAllViews();if(loginView!=null){loginView.destroy();loginView=null;}message("正在解析…");work.execute(()->{Page page=null;try{page=fetchPage(url);JSONObject data=MediaParser.extractJson(page.html);MediaParser.Result result=enrich(data,MediaParser.parse(data));runOnUiThread(()->{if(current==generation)show(result);});}catch(Exception error){String id=page==null?"":MediaParser.shareItemId(page.url);if(!id.isEmpty()){try{MediaParser.Result result=feedFallback(id);runOnUiThread(()->{if(current==generation)show(result);});return;}catch(Exception ignored){}}runOnUiThread(()->{if(current==generation)browserFallback(url,current,error.getMessage());});}});}
    private void browserFallback(String url,int current,String prior){message("网页直读失败，正在尝试浏览器模式…");if(webView!=null)webView.destroy();webView=new WebView(this);webView.getSettings().setJavaScriptEnabled(true);webView.getSettings().setDomStorageEnabled(true);webView.getSettings().setAllowFileAccess(false);webView.getSettings().setAllowContentAccess(false);webView.setWebViewClient(new WebViewClient(){boolean done=false;
        @Override public void onPageFinished(WebView view,String loaded){if(done||current!=generation)return;view.evaluateJavascript("(function(){try{if(window._ROUTER_DATA)return JSON.stringify(window._ROUTER_DATA);var e=document.getElementById('RENDER_DATA');if(e)return decodeURIComponent(e.textContent);return ''}catch(e){return ''}})()",value->{if(done||current!=generation)return;try{String decoded=new JSONArray("["+value+"]").getString(0);if(decoded.isEmpty())throw new Exception("浏览器页面未包含作品数据");JSONObject data=new JSONObject(decoded);MediaParser.Result result=MediaParser.parse(data);done=true;message("正在查找实况视频…");work.execute(()->{MediaParser.Result complete=enrich(data,result);runOnUiThread(()->{if(current==generation)show(complete);if(webView==view){view.destroy();webView=null;}});});}catch(Exception ex){message("解析失败："+prior+"；浏览器模式："+ex.getMessage());if(results.getChildCount()==0){LinearLayout card=card(results);card.addView(text("作品详情暂不可用。可以登录抖音网页后重试。",14));card.addView(button("打开抖音网页登录",this::showLogin));}}});}
    });webView.loadUrl(url);}
    private void show(MediaParser.Result r){message("解析成功 · "+r.type);results.removeAllViews();LinearLayout info=card(results);TextView title=text(r.title,20);title.setTypeface(null,Typeface.BOLD);info.addView(title);if(!r.author.isEmpty()){TextView author=text("作者 · "+r.author,13);author.setTextColor(MUTED);info.addView(author);}
        if(!r.video.isEmpty()){LinearLayout videoCard=card(results);videoCard.addView(text("视频",18));if(!r.videoCover.isEmpty())loadPreview(videoCard,r.videoCover);videoCard.addView(button("下载无水印视频",()->save(r.video,"mp4","video/mp4")),new LinearLayout.LayoutParams(-1,dp(48)));Button preview=button("播放视频预览",()->{});preview.setBackground(shape(Color.rgb(228,241,237),13));preview.setTextColor(TEAL);preview.setOnClickListener(v->{preview.setEnabled(false);VideoView player=new VideoView(this);MediaController controls=new MediaController(this);player.setMediaController(controls);videoCard.addView(player,new LinearLayout.LayoutParams(-1,dp(240)));player.setOnPreparedListener(mp->player.start());player.setOnErrorListener((mp,what,extra)->{message("预览加载失败，可直接下载视频");return true;});player.setVideoURI(Uri.parse(r.video));});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.topMargin=dp(9);videoCard.addView(preview,lp);}
        int i=0;for(MediaParser.Asset a:r.images){final int index=++i;LinearLayout photoCard=card(results);TextView photoTitle=text("第 "+index+" 张"+(a.live.isEmpty()?" · 原图":" · 实况"),17);photoTitle.setTypeface(null,Typeface.BOLD);photoCard.addView(photoTitle);loadPreview(photoCard,a.image);photoCard.addView(button("下载原图",()->save(a.image,extension(a.image,"jpg"),"image/*")),new LinearLayout.LayoutParams(-1,dp(48)));if(!a.live.isEmpty()){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.topMargin=dp(9);photoCard.addView(button("下载动态视频 MP4",()->save(a.live,"mp4","video/mp4")),lp);}}
        if(!r.images.isEmpty()){LinearLayout actions=card(results);actions.addView(button("全部保存 · "+r.images.size()+" 张",()->{for(MediaParser.Asset a:r.images){save(a.image,extension(a.image,"jpg"),"image/*");if(!a.live.isEmpty())save(a.live,"mp4","video/mp4");}}),new LinearLayout.LayoutParams(-1,dp(48)));if(r.images.stream().anyMatch(a->a.live.isEmpty())){TextView detail=text("有些图片只返回了静态原图。若作品含实况，可登录抖音网页后重试；能否获取 MP4 取决于作品详情数据。",13);detail.setTextColor(MUTED);actions.addView(detail);Button login=button("打开抖音网页登录",this::showLogin);login.setBackground(shape(Color.rgb(228,241,237),13));login.setTextColor(TEAL);actions.addView(login,new LinearLayout.LayoutParams(-1,dp(48)));}}}
    private void showLogin(){if(loginView!=null)return;LinearLayout panel=card(results);panel.addView(text("抖音官网登录",18));TextView tip=text("下方打开 www.douyin.com。登录完成后点“重新解析”；网页会话仅用于请求抖音作品详情。",13);tip.setTextColor(MUTED);panel.addView(tip);panel.addView(button("登录完成 · 重新解析",this::parse),new LinearLayout.LayoutParams(-1,dp(48)));loginView=new WebView(this);loginView.getSettings().setJavaScriptEnabled(true);loginView.getSettings().setDomStorageEnabled(true);CookieManager.getInstance().setAcceptCookie(true);loginView.setWebViewClient(new WebViewClient());panel.addView(loginView,new LinearLayout.LayoutParams(-1,dp(480)));loginView.loadUrl("https://www.douyin.com/");}
    private String extension(String url,String fallback){try{String p=new URL(url).getPath().toLowerCase();for(String x:new String[]{"jpg","jpeg","png","webp","gif"})if(p.endsWith("."+x))return x;}catch(Exception ignored){}return fallback;}
    private void loadPreview(LinearLayout parent,String url){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(shape(BG,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(220));lp.topMargin=dp(9);lp.bottomMargin=dp(12);parent.addView(image,lp);work.execute(()->{try{HttpURLConnection c=connection(url);c.setInstanceFollowRedirects(true);c.setReadTimeout(10000);try(java.io.InputStream in=c.getInputStream()){byte[] b=in.readNBytes(2_000_000);android.graphics.Bitmap bitmap=BitmapFactory.decodeByteArray(b,0,b.length);runOnUiThread(()->{if(bitmap!=null)image.setImageBitmap(bitmap);});}finally{c.disconnect();}}catch(Exception ignored){}});}
    private void save(String url,String extension,String mime){try{Uri uri=Uri.parse(url);if(!"https".equals(uri.getScheme())||uri.getHost()==null)throw new Exception("不支持该媒体地址");DownloadManager.Request request=new DownloadManager.Request(uri);request.setTitle("拾影作品");request.setMimeType(mime);request.addRequestHeader("Referer","https://www.douyin.com/");request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"LiveClip/"+System.currentTimeMillis()+"_"+Math.abs(url.hashCode())+"."+extension);((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);Toast.makeText(this,"已加入下载队列",Toast.LENGTH_SHORT).show();}catch(Exception e){message("保存失败："+e.getMessage());}}
    @Override protected void onDestroy(){generation++;if(webView!=null)webView.destroy();if(loginView!=null)loginView.destroy();work.shutdownNow();super.onDestroy();}
}
