package com.example.liveclip;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/** Keeps foreground progress in sync with the persistent download service. */
final class DownloadMonitor {
    interface Listener{void onProgress(String title,String detail,int percent,boolean determinate);}
    private final Activity activity;private final Listener listener;
    private final SharedPreferences state;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;private String lastEvent="",pendingUrl,pendingExt,pendingMime;
    DownloadMonitor(Activity activity,Listener listener){this.activity=activity;this.listener=listener;state=activity.getSharedPreferences(DownloadService.PREFS,Activity.MODE_PRIVATE);}
    void enqueue(String url,String extension,String mime){
        if(Build.VERSION.SDK_INT<=28&&activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            pendingUrl=url;pendingExt=extension;pendingMime=mime;
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},40);return;
        }
        if(Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED&&!state.getBoolean("notification_asked",false)){
            pendingUrl=url;pendingExt=extension;pendingMime=mime;
            state.edit().putBoolean("notification_asked",true).apply();
            activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},41);return;
        }
        try{Intent intent=new Intent(activity,DownloadService.class);intent.putExtra("url",url);intent.putExtra("ext",extension);intent.putExtra("mime",mime);
            if(Build.VERSION.SDK_INT>=26)activity.startForegroundService(intent);else activity.startService(intent);
            Toast.makeText(activity,(mime.startsWith("video/")?"视频":"图片")+"正在下载中",Toast.LENGTH_SHORT).show();
            handler.removeCallbacks(poll);if(active)handler.post(poll);
        }catch(Exception error){Toast.makeText(activity,"下载未启动："+error.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void permissionResult(int requestCode,boolean granted){
        if(pendingUrl==null)return;
        String url=pendingUrl,ext=pendingExt,mime=pendingMime;pendingUrl=null;
        if(requestCode==41){if(!granted)Toast.makeText(activity,"通知未开启，下载仍会进行，可在应用内查看进度",Toast.LENGTH_LONG).show();enqueue(url,ext,mime);}
        else if(granted)enqueue(url,ext,mime);
        else Toast.makeText(activity,"需要存储权限才能保存文件",Toast.LENGTH_LONG).show();
    }
    void start(){active=true;handler.removeCallbacks(poll);handler.post(poll);
        if(state.getString(DownloadService.QUEUE,"[]").length()>2){try{Intent intent=new Intent(activity,DownloadService.class);
            if(Build.VERSION.SDK_INT>=26)activity.startForegroundService(intent);else activity.startService(intent);}catch(Exception ignored){}}
    }
    void stop(){active=false;handler.removeCallbacks(poll);}
    private final Runnable poll=new Runnable(){@Override public void run(){if(!active)return;
        String progress=state.getString("progress","");
        if(!progress.isEmpty()){String[] p=progress.split("\\|",4);if(p.length==4)try{listener.onProgress(p[0],p[1],Integer.parseInt(p[2]),Boolean.parseBoolean(p[3]));}catch(Exception ignored){}}
        else listener.onProgress("","",0,false);
        String event=state.getString(DownloadService.EVENT,"");if(!event.isEmpty()&&!event.equals(lastEvent)){lastEvent=event;int at=event.indexOf('|');if(at>=0)Toast.makeText(activity,event.substring(at+1),Toast.LENGTH_LONG).show();}
        handler.postDelayed(this,450);
    }};
}
