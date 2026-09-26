package com.example.liveclip;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService work=Executors.newFixedThreadPool(2);
    private EditText input; private TextView status; private LinearLayout results; private WebView webView;
    private int generation=0;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);render();String shared=getIntent().getStringExtra(Intent.EXTRA_TEXT);if(shared!=null)input.setText(shared);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(Color.rgb(42,43,48));v.setPadding(0,dp(8),0,dp(8));return v;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
    private void render(){ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(28),dp(20),dp(30));page.setBackgroundColor(Color.rgb(249,249,249));scroll.addView(page);setContentView(scroll);
        TextView title=text("拾影 · 媒体保存",27);title.setTextColor(Color.rgb(160,64,78));page.addView(title);page.addView(text("视频 / 图文 / 实况原图与短片",14));
        input=new EditText(this);input.setMinLines(3);input.setGravity(Gravity.TOP);input.setTextSize(16);input.setHint("粘贴抖音分享内容或从抖音分享到本应用");page.addView(input,new LinearLayout.LayoutParams(-1,dp(120)));
        LinearLayout row=new LinearLayout(this);page.addView(row);Button paste=button("粘贴",()->{ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);ClipData clip=cb.getPrimaryClip();if(clip!=null&&clip.getItemCount()>0)input.setText(clip.getItemAt(0).coerceToText(this));});row.addView(paste,new LinearLayout.LayoutParams(0,-2,1));Button parse=button("解析作品",this::parse);row.addView(parse,new LinearLayout.LayoutParams(0,-2,2));
        status=text("仅保存你有权保存的内容。解析依赖分享页数据，页面更新后可能需要升级应用。",14);page.addView(status);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);page.addView(results);
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
        HttpURLConnection c=connection("https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id="+id);
        c.setRequestProperty("Referer","https://www.douyin.com/");
        c.setRequestProperty("Accept","application/json");
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
    private void parse(){String text=input.getText().toString();final String url;try{url=MediaParser.shareUrl(text);}catch(Exception e){message(e.getMessage());return;}int current=++generation;results.removeAllViews();message("正在解析…");work.execute(()->{Page page=null;try{page=fetchPage(url);JSONObject data=MediaParser.extractJson(page.html);MediaParser.Result result=enrich(data,MediaParser.parse(data));runOnUiThread(()->{if(current==generation)show(result);});}catch(Exception error){String id=page==null?"":MediaParser.shareItemId(page.url);if(!id.isEmpty()){try{MediaParser.Result result=feedFallback(id);runOnUiThread(()->{if(current==generation)show(result);});return;}catch(Exception ignored){}}runOnUiThread(()->{if(current==generation)browserFallback(url,current,error.getMessage());});}});}
    private void browserFallback(String url,int current,String prior){message("网页直读失败，正在尝试浏览器模式…");if(webView!=null)webView.destroy();webView=new WebView(this);webView.getSettings().setJavaScriptEnabled(true);webView.getSettings().setDomStorageEnabled(true);webView.getSettings().setAllowFileAccess(false);webView.getSettings().setAllowContentAccess(false);webView.setWebViewClient(new WebViewClient(){boolean done=false;
        @Override public void onPageFinished(WebView view,String loaded){if(done||current!=generation)return;view.evaluateJavascript("(function(){try{if(window._ROUTER_DATA)return JSON.stringify(window._ROUTER_DATA);var e=document.getElementById('RENDER_DATA');if(e)return decodeURIComponent(e.textContent);return ''}catch(e){return ''}})()",value->{if(done||current!=generation)return;try{String decoded=new JSONArray("["+value+"]").getString(0);if(decoded.isEmpty())throw new Exception("浏览器页面未包含作品数据");JSONObject data=new JSONObject(decoded);MediaParser.Result result=MediaParser.parse(data);done=true;message("正在查找实况视频…");work.execute(()->{MediaParser.Result complete=enrich(data,result);runOnUiThread(()->{if(current==generation)show(complete);if(webView==view){view.destroy();webView=null;}});});}catch(Exception ex){message("解析失败："+prior+"；浏览器模式："+ex.getMessage());}});}
    });webView.loadUrl(url);}
    private void show(MediaParser.Result r){message("解析成功 · "+r.type);results.removeAllViews();results.addView(text(r.title,21));if(!r.author.isEmpty())results.addView(text("作者："+r.author,14));if(!r.video.isEmpty()){results.addView(button("保存无水印视频",()->save(r.video,"mp4","video/mp4")));if(!r.videoCover.isEmpty())loadPreview(r.videoCover,0);Button preview=button("播放视频预览",()->{});preview.setOnClickListener(v->{preview.setEnabled(false);VideoView player=new VideoView(this);MediaController controls=new MediaController(this);player.setMediaController(controls);results.addView(player,new LinearLayout.LayoutParams(-1,dp(240)));player.setOnPreparedListener(mp->player.start());player.setOnErrorListener((mp,what,extra)->{message("预览加载失败，可直接点击保存视频");return true;});player.setVideoURI(Uri.parse(r.video));});results.addView(preview);}
        int i=0;for(MediaParser.Asset a:r.images){final int index=++i;results.addView(text("第 "+index+" 张",17));results.addView(button("保存原图",()->save(a.image,extension(a.image,"jpg"),"image/*")));if(!a.live.isEmpty())results.addView(button("保存第 "+index+" 张的动态视频 MP4",()->save(a.live,"mp4","video/mp4")));loadPreview(a.image,index);}
        if(!r.images.isEmpty())results.addView(button("全部保存（共 "+r.images.size()+" 张）",()->{for(MediaParser.Asset a:r.images){save(a.image,extension(a.image,"jpg"),"image/*");if(!a.live.isEmpty())save(a.live,"mp4","video/mp4");}}));
        if("实况图文".equals(r.type))results.addView(text("动态视频以 MP4 保存到下载目录；原图和视频分别保存。",13));
        else if("图文".equals(r.type))results.addView(text("当前作品数据只提供了静态图片。如作品确有动态内容，请将分享链接发来以便适配。",13));}
    private String extension(String url,String fallback){try{String p=new URL(url).getPath().toLowerCase();for(String x:new String[]{"jpg","jpeg","png","webp","gif"})if(p.endsWith("."+x))return x;}catch(Exception ignored){}return fallback;}
    private void loadPreview(String url,int id){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);results.addView(image,new LinearLayout.LayoutParams(-1,dp(230)));work.execute(()->{try{HttpURLConnection c=connection(url);c.setInstanceFollowRedirects(true);c.setReadTimeout(10000);try(java.io.InputStream in=c.getInputStream()){byte[] b=in.readNBytes(2_000_000);android.graphics.Bitmap bitmap=BitmapFactory.decodeByteArray(b,0,b.length);runOnUiThread(()->{if(bitmap!=null)image.setImageBitmap(bitmap);});}finally{c.disconnect();}}catch(Exception ignored){}});}
    private void save(String url,String extension,String mime){try{Uri uri=Uri.parse(url);if(!"https".equals(uri.getScheme())||uri.getHost()==null)throw new Exception("不支持该媒体地址");DownloadManager.Request request=new DownloadManager.Request(uri);request.setTitle("拾影作品");request.setMimeType(mime);request.addRequestHeader("Referer","https://www.douyin.com/");request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"LiveClip/"+System.currentTimeMillis()+"_"+Math.abs(url.hashCode())+"."+extension);((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);Toast.makeText(this,"已加入下载队列",Toast.LENGTH_SHORT).show();}catch(Exception e){message("保存失败："+e.getMessage());}}
    @Override protected void onDestroy(){generation++;if(webView!=null)webView.destroy();work.shutdownNow();super.onDestroy();}
}
