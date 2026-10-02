package com.example.liveclip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A foreground transfer owns its progress, retry policy and notification. */
public final class DownloadService extends Service {
    static final String PREFS="transfer_v5", QUEUE="queue", EVENT="event";
    private static final String CHANNEL="liveclip_transfers_v5";
    private static final int ONGOING=501;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Object lock=new Object();
    private SharedPreferences prefs;
    private NotificationManager notifications;
    private boolean draining;
    @Override public void onCreate(){super.onCreate();prefs=getSharedPreferences(PREFS,MODE_PRIVATE);notifications=getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"拾影 · 媒体保存",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        synchronized(lock){
            if(intent!=null&&intent.hasExtra("url")){
                try{
                    JSONArray q=queue();JSONObject item=new JSONObject();
                    item.put("id",UUID.randomUUID().toString());item.put("url",intent.getStringExtra("url"));
                    item.put("ext",intent.getStringExtra("ext"));item.put("mime",intent.getStringExtra("mime"));
                    q.put(item);prefs.edit().putString(QUEUE,q.toString()).commit();
                }catch(Exception error){event("下载任务保存失败："+error.getMessage());}
            }
            JSONArray q=queue();if(q.length()==0){stopSelf(startId);return START_NOT_STICKY;}
            try{
                if(Build.VERSION.SDK_INT>=29)startForeground(ONGOING,notification("拾影 · 正在准备下载","即将连接媒体服务器",0,0,true),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                else startForeground(ONGOING,notification("拾影 · 正在准备下载","即将连接媒体服务器",0,0,true));
            }catch(Exception error){event("无法启动下载服务："+error.getMessage());stopSelf(startId);return START_NOT_STICKY;}
            if(!draining){draining=true;executor.execute(this::drain);}
        }
        return START_STICKY;
    }
    private JSONArray queue(){try{return new JSONArray(prefs.getString(QUEUE,"[]"));}catch(Exception ignored){return new JSONArray();}}
    private void drain(){
        while(true){JSONObject item;
            synchronized(lock){JSONArray q=queue();if(q.length()==0){
                if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE);else stopForeground(true);
                draining=false;stopSelf();return;
            }item=q.optJSONObject(0);if(item==null){prefs.edit().putString(QUEUE,"[]").commit();continue;}}
            String kind=item.optString("mime","").startsWith("video/")?"视频":"图片";
            String result="";
            for(int attempt=1;attempt<=3;attempt++){
                try{transfer(item,kind,attempt);result=kind+"下载完成 · 已保存到 下载/LiveClip";break;}
                catch(Exception error){result=kind+"下载失败："+error.getMessage();
                    if(attempt==3||error instanceof PermanentFailure)break;
                    progress(kind+"连接中 · 第 "+(attempt+1)+" 次尝试",0,0,true);
                    try{Thread.sleep(700L*attempt);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
                }
            }
            event(result);
            notifications.notify(1000+Math.abs(item.optString("id").hashCode()%10000),notification("拾影 · "+(result.contains("下载完成")?"保存完成":"保存失败"),result,0,0,false));
            synchronized(lock){JSONArray old=queue(),next=new JSONArray();for(int i=0;i<old.length();i++)if(!item.optString("id").equals(old.optJSONObject(i)==null?"":old.optJSONObject(i).optString("id")))next.put(old.opt(i));prefs.edit().putString(QUEUE,next.toString()).commit();}
        }
    }
    private void transfer(JSONObject item,String kind,int attempt) throws Exception {
        String raw=item.getString("url"),ext=item.getString("ext"),mime=item.getString("mime");
        URL source=new URL(raw);if(!"https".equalsIgnoreCase(source.getProtocol()))throw new PermanentFailure("只支持安全的 HTTPS 媒体地址");
        HttpURLConnection conn=(HttpURLConnection)source.openConnection();Uri destination=null;File oldFile=null;
        try{
            conn.setInstanceFollowRedirects(true);conn.setConnectTimeout(12000);conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36");
            conn.setRequestProperty("Referer","https://www.douyin.com/");
            String host=source.getHost().toLowerCase(java.util.Locale.ROOT);
            if(host.equals("douyin.com")||host.endsWith(".douyin.com")||host.endsWith(".iesdouyin.com")){
                String cookie=CookieManager.getInstance().getCookie(raw);if(cookie!=null)conn.setRequestProperty("Cookie",cookie);
            }
            int code=conn.getResponseCode();
            if(!"https".equalsIgnoreCase(conn.getURL().getProtocol()))throw new PermanentFailure("媒体地址跳转到非 HTTPS 连接");
            if(code!=200){if(code==403||code==401)throw new PermanentFailure("媒体地址已失效（HTTP "+code+"），请重新解析");
                if(code>=400&&code<500&&code!=408&&code!=429)throw new PermanentFailure("服务器拒绝访问（HTTP "+code+"）");
                throw new java.io.IOException("服务器暂不可用（HTTP "+code+"）");}
            String contentType=conn.getContentType();if(contentType!=null&&(contentType.toLowerCase(java.util.Locale.ROOT).contains("text/html")||contentType.toLowerCase(java.util.Locale.ROOT).contains("json")))throw new PermanentFailure("服务器返回了网页，请重新解析链接");
            if(mime.startsWith("image/")&&contentType!=null){String lower=contentType.toLowerCase(java.util.Locale.ROOT);
                if(lower.contains("image/webp")){ext="webp";mime="image/webp";}
                else if(lower.contains("image/png")){ext="png";mime="image/png";}
                else if(lower.contains("image/gif")){ext="gif";mime="image/gif";}
                else if(lower.contains("image/avif")){ext="avif";mime="image/avif";}
            }
            long total=conn.getContentLengthLong();String name="LiveClip-"+System.currentTimeMillis()+"-"+item.optString("id").substring(0,8)+"."+ext;
            if(Build.VERSION.SDK_INT>=29){ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(MediaStore.MediaColumns.MIME_TYPE,mime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/LiveClip");values.put(MediaStore.MediaColumns.IS_PENDING,1);
                destination=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
                if(destination==null)throw new java.io.IOException("无法创建下载文件");
            }else{File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"LiveClip");if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("无法创建下载目录");oldFile=new File(dir,name);}
            try(InputStream in=conn.getInputStream();OutputStream out=destination!=null?getContentResolver().openOutputStream(destination):new FileOutputStream(oldFile)){
                if(out==null)throw new java.io.IOException("无法写入下载文件");
                byte[] buf=new byte[32768];int n=0;while(n<12){int chunk=in.read(buf,n,12-n);if(chunk<0)throw new java.io.IOException("媒体文件为空或不完整");n+=chunk;}
                if(mime.startsWith("video/")&&!(buf[4]=='f'&&buf[5]=='t'&&buf[6]=='y'&&buf[7]=='p'))throw new PermanentFailure("返回的内容不是 MP4，请重新解析");
                if(mime.startsWith("image/")&&buf[0]=='<'&&buf[1]=='!')throw new PermanentFailure("返回的是网页，不是原图");
                long done=0,last=0;while(n!=-1){out.write(buf,0,n);done+=n;long now=System.currentTimeMillis();
                    if(now-last>=500){progress(kind+"下载中 · 第 "+attempt+" 次连接",done,total,false);last=now;}
                    n=in.read(buf);
                }
                if(total>0&&done<total)throw new java.io.IOException("文件未下载完整（"+done+"/"+total+" 字节）");
            }
            if(destination!=null){ContentValues done=new ContentValues();done.put(MediaStore.MediaColumns.IS_PENDING,0);getContentResolver().update(destination,done,null,null);destination=null;}
            oldFile=null;
        }catch(Exception error){if(destination!=null)getContentResolver().delete(destination,null,null);if(oldFile!=null)oldFile.delete();throw error;}
        finally{conn.disconnect();}
    }
    private void progress(String label,long bytes,long total,boolean retry){
        int percent=total>0?(int)Math.min(100,bytes*100/total):0;
        String detail=retry?"网络暂时不稳定，正在重试":total>0?percent+"% · "+size(bytes)+" / "+size(total):size(bytes)+" · 下载中";
        prefs.edit().putString("progress",label+"|"+detail+"|"+percent+"|"+(total>0)).apply();
        notifications.notify(ONGOING,notification("拾影 · "+label,detail,percent,100,total<=0));
    }
    private String size(long bytes){return String.format(java.util.Locale.ROOT,"%.1f MB",bytes/1048576.0);}
    private void event(String message){prefs.edit().putString(EVENT,System.currentTimeMillis()+"|"+message).remove("progress").apply();}
    private Notification notification(String title,String message,int current,int max,boolean indeterminate){
        Intent open=new Intent(this,MainActivity.class);PendingIntent pending=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_stat_download).setColor(Color.rgb(80,75,207))
            .setContentTitle(title).setContentText(message).setContentIntent(pending)
            .setCategory(Notification.CATEGORY_SERVICE).setOnlyAlertOnce(true).setOngoing(max>0||indeterminate)
            .setAutoCancel(max==0&&!indeterminate).setProgress(max,current,indeterminate).build();
    }
    @Override public void onDestroy(){executor.shutdownNow();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
    private static final class PermanentFailure extends Exception{PermanentFailure(String message){super(message);}}
}
