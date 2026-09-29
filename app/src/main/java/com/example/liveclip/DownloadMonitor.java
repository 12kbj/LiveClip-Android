package com.example.liveclip;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.util.Map;
import java.util.UUID;

/** The system owns downloads and their progress notifications, even after this Activity exits. */
final class DownloadMonitor {
    private final Activity activity;
    private final DownloadManager manager;
    private final SharedPreferences tasks;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;
    DownloadMonitor(Activity activity){
        this.activity=activity;
        manager=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
        tasks=activity.getSharedPreferences("download_tasks",Context.MODE_PRIVATE);
    }
    void enqueue(String url,String extension,String mime){
        String kind=mime.startsWith("video/")?"视频":"图片";
        if(Build.VERSION.SDK_INT<=28&&activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},40);
            toast("请允许存储权限后，再点击下载");return;
        }
        try{
            Uri uri=Uri.parse(url);
            if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null)throw new IllegalArgumentException("媒体地址无效");
            String filename="LiveClip-"+UUID.randomUUID()+"."+extension;
            DownloadManager.Request request=new DownloadManager.Request(uri)
                .setTitle("拾影 · "+kind)
                .setDescription(kind+"正在下载中 · 下载/LiveClip/"+filename)
                .setMimeType(mime)
                .addRequestHeader("Referer","https://www.douyin.com/")
                .addRequestHeader("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"LiveClip/"+filename);
            long id=manager.enqueue(request);
            tasks.edit().putString(Long.toString(id),kind).apply();
            toast(kind+"正在下载中，可在通知栏查看进度");
            if(active){handler.removeCallbacks(poll);handler.post(poll);}
        }catch(Exception error){toast(kind+"下载未开始："+error.getMessage());}
    }
    void start(){active=true;handler.removeCallbacks(poll);handler.post(poll);}
    void stop(){active=false;handler.removeCallbacks(poll);}
    private final Runnable poll=new Runnable(){
        @Override public void run(){
            if(!active)return;
            Map<String,?> pending=tasks.getAll();
            if(!pending.isEmpty()){
                long[] ids=new long[pending.size()];int n=0;
                for(String id:pending.keySet())ids[n++]=Long.parseLong(id);
                try(Cursor cursor=manager.query(new DownloadManager.Query().setFilterById(ids))){
                    java.util.HashSet<String> found=new java.util.HashSet<>();
                    if(cursor!=null)while(cursor.moveToNext()){
                        String id=Long.toString(cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)));found.add(id);
                        int status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        String kind=String.valueOf(pending.get(id));
                        if(status==DownloadManager.STATUS_SUCCESSFUL){
                            tasks.edit().remove(id).apply();toast(kind+"下载完成，已保存到 下载/LiveClip");
                        }else if(status==DownloadManager.STATUS_FAILED){
                            int reason=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                            tasks.edit().remove(id).apply();toast(kind+"下载失败（"+reason+"），请重新解析后重试");
                        }
                    }
                    if(cursor!=null)for(String id:pending.keySet())if(!found.contains(id)){
                        tasks.edit().remove(id).apply();toast(pending.get(id)+"下载任务已取消或移除");
                    }
                }catch(Exception ignored){/* Keep IDs: a temporary query failure is not a failed download. */}
            }
            if(active)handler.postDelayed(this,1500);
        }
    };
    private void toast(String text){Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
}
