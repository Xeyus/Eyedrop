package com.eyedrop.manager;
import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.time.Instant;
/** Foreground care timer. Wall-clock deadlines persist across Activity/process restarts. */
public class CareService extends Service {
 public static final String CHANNEL="care",PHASE="care_phases";final Handler handler=new Handler(Looper.getMainLooper());PowerManager.WakeLock lock;
 public static void channels(Context c){NotificationManager n=c.getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel(CHANNEL,"护理进行中",NotificationManager.IMPORTANCE_LOW));n.createNotificationChannel(new NotificationChannel(PHASE,"护理阶段提示",NotificationManager.IMPORTANCE_HIGH));}
 Notification notification(String channel,String text){PendingIntent p=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,channel).setSmallIcon(com.eyedrop.manager.R.drawable.ic_drop).setContentTitle("滴答护眼 · 护理计时").setContentText(text).setContentIntent(p).setOngoing(channel.equals(CHANNEL)).setAutoCancel(!channel.equals(CHANNEL)).build();}
 final Runnable tick=new Runnable(){public void run(){try{
  Store s=AppState.get(CareService.this);long now=Instant.now().getEpochSecond();if(AppState.tick(CareService.this))AppState.save(CareService.this);String ongoing="护理进行中";boolean active=false;
  for(int i=0;i<s.cares().length();i++){JSONObject c=s.cares().getJSONObject(i);now=AppState.careNow(CareService.this,c);String phase=c.optString("status").equals("Completed")||now>=c.getLong("end_time")?"done":now>=c.getLong("press_end")?"rest":"press";
   if(!phase.equals(c.optString("notified_phase"))){if(!phase.equals("press"))getSystemService(NotificationManager.class).notify(100+i,notification(PHASE,c.optString("name")+(phase.equals("done")?"：护理计时已结束":"：按压阶段结束，请继续闭眼休息")));c.put("notified_phase",phase);AppState.save(CareService.this);}
   if(!c.optString("status").equals("Completed")){active=true;long left=Math.max(0,c.getLong("end_time")-now);ongoing=c.optString("name")+" · "+(phase.equals("press")?"按压泪囊区":"继续闭眼休息")+" · 剩余 "+left/60+":"+String.format(java.util.Locale.ROOT,"%02d",left%60);}
  }
  if(!active){stopForeground(true);stopSelf();return;}getSystemService(NotificationManager.class).notify(1,notification(CHANNEL,ongoing));handler.postDelayed(this,1000);
 }catch(Exception e){getSystemService(NotificationManager.class).notify(2,notification(PHASE,"护理服务中断，请打开应用检查计时状态"));stopSelf();}}};
 @Override public int onStartCommand(Intent i,int flags,int startId){channels(this);startForeground(1,notification(CHANNEL,"正在恢复护理计时"));if(lock==null){lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"EyeDrop:care");lock.acquire(65*60*1000L);}handler.removeCallbacks(tick);handler.post(tick);return START_STICKY;}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){handler.removeCallbacks(tick);if(lock!=null&&lock.isHeld())lock.release();super.onDestroy();}
}
