package com.eyedrop.manager;
import android.content.Context;
import org.json.*;
import android.os.SystemClock;
import android.provider.Settings;
import java.time.Instant;
public final class AppState {
 private static Store store;
 public static synchronized Store get(Context context)throws JSONException{if(store==null)store=new Store(context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("data",null));return store;}
 public static synchronized void use(Context context,Store candidate){if(!context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).edit().putString("data",candidate.data.toString()).commit())throw new IllegalStateException("本地存储失败，原数据未覆盖");store=candidate;}
 public static long careNow(Context context,JSONObject c){int boot=Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,-1);if(boot>=0&&c.optInt("boot_count",-2)==boot&&c.has("elapsed_start")&&SystemClock.elapsedRealtime()>=c.optLong("elapsed_start"))return c.optLong("start_time")+(SystemClock.elapsedRealtime()-c.optLong("elapsed_start"))/1000;return Instant.now().getEpochSecond();}
 public static void stamp(Context context,JSONObject c)throws JSONException{c.put("elapsed_start",SystemClock.elapsedRealtime()).put("boot_count",Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,-1));}
 public static boolean tick(Context context)throws JSONException{Store s=get(context);boolean changed=false;for(int i=0;i<s.cares().length();i++){JSONObject c=s.cares().getJSONObject(i);changed|=s.tickCare(c,careNow(context,c));}return changed;}
 public static synchronized void save(Context context){use(context,store);}
}
