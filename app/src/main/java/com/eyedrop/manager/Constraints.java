package com.eyedrop.manager;
import org.json.*;
import java.time.*;
import java.util.*;
/** Constraint scheduling. No dosage decisions, catch-up doses, or silent relaxation. */
public final class Constraints {
 public static long epoch(String local){return LocalDateTime.parse(local).atZone(ZoneId.systemDefault()).toEpochSecond();}
 public static String local(long epoch){return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch),ZoneId.systemDefault()).toString();}
 public static long at(JSONObject t)throws JSONException{return t.optLong("scheduled_epoch",epoch(t.getString("scheduled")));}
 public static String profile(JSONObject t){return t.optString("profile","GENERAL");}
 public static int care(JSONObject t){return t.optInt("care_seconds",0);}
 public static int fixed(JSONObject t){return t.optInt("fixed_interval",0)*60;}
 public static String pair(JSONObject a,long x,JSONObject b,long y){
  String ap=profile(a),bp=profile(b),names=a.optString("name")+" / "+b.optString("name");
  if((ap.equals("CYCLO")&&bp.equals("TEARS"))||(bp.equals("CYCLO")&&ap.equals("TEARS")))if(Math.abs(x-y)<900)return names+"：环孢素与人工泪液须前后间隔至少15分钟";
  if(care(a)>0&&y>=x&&y<x+care(a))return names+"：与10分钟（或配置时长）护理独占窗口冲突";
  if(care(b)>0&&x>=y&&x<y+care(b))return names+"：与护理独占窗口冲突";
  if(a.optString("drugId").equals(b.optString("drugId"))){
   int interval=Math.max(fixed(a),fixed(b));int delta=Math.abs(a.optInt("index")-b.optInt("index"));
   // Only consecutive sequence members must be exactly the configured interval apart.
   if(interval>0&&a.optString("day").equals(b.optString("day"))&&Math.abs(x-y)!=(long)interval*delta)return names+"：固定间隔必须为"+(interval/60)+"分钟（相邻次数）";
   if(interval>0&&!a.optString("day").equals(b.optString("day"))&&Math.abs(x-y)<interval)return names+"：与相邻日实际用药间隔不足"+(interval/60)+"分钟";
   if(x==y)return names+"：同一药物的两次任务不能重合";
  }
  return "";
 }
 public static final class Result {public JSONArray plans=new JSONArray(),conflicts=new JSONArray();public int visited;}
 private static final class Search {
  List<JSONObject> work,pins,best=new ArrayList<>(),placed=new ArrayList<>();long start,end;int budget=5000,visited=0;boolean solved;
  Search(List<JSONObject>w,List<JSONObject>p,long s,long e){work=w;pins=p;start=s;end=e;}
  String reason(JSONObject t,long at)throws JSONException{if(at<start||at+care(t)>end)return "超出清醒窗口（含护理时长）";
   for(JSONObject p:pins){String r=pair(t,at,p,p.getLong("actual_epoch"));if(!r.isEmpty())return r;}
   for(JSONObject p:placed){String r=pair(t,at,p,Constraints.at(p));if(!r.isEmpty())return r;}return "";
  }
  List<Long> options(JSONObject t)throws JSONException{List<Long> out=new ArrayList<>();long desired=t.getLong("desired_epoch"),lower=Math.max(start,t.optLong("not_before",start));
   if(fixed(t)>0){out.add(desired);return out;}
   long first=(lower+59)/60*60;for(long q=first;q+care(t)<=end;q+=60)out.add(q);
   Collections.sort(out,(a,b)->{int c=Long.compare(Math.abs(a-desired),Math.abs(b-desired));return c==0?Long.compare(a,b):c;});return out;
  }
  void go(int index)throws JSONException{if(solved||visited>=budget)return;if(placed.size()>best.size())best=new ArrayList<>(placed);if(index==work.size()){solved=true;return;}
   JSONObject t=work.get(index);for(long q:options(t)){if(++visited>budget)return;if(q<t.optLong("not_before",start)||!reason(t,q).isEmpty())continue;
    JSONObject copy=new JSONObject(t.toString()).put("scheduled",local(q)).put("scheduled_epoch",q);placed.add(copy);go(index+1);if(solved)return;placed.remove(placed.size()-1);
   }
  }
 }
 public static Result solve(JSONArray tasks,JSONArray records,long start,long end)throws JSONException{
  List<JSONObject> pins=new ArrayList<>(),work=new ArrayList<>();Result out=new Result();
  for(int i=0;i<records.length();i++){JSONObject p=records.getJSONObject(i);if(!p.optString("actual").isEmpty()&&!p.optString("status").equals("Missed")){JSONObject copy=new JSONObject(p.toString());copy.put("actual_epoch",p.optLong("actual_epoch",epoch(p.getString("actual"))));pins.add(copy);}}
  for(int i=0;i<tasks.length();i++){JSONObject p=new JSONObject(tasks.getJSONObject(i).toString());if(p.optBoolean("done")||!p.optString("status").equals("Pending")){out.plans.put(p);continue;}work.add(p);}
  work.sort((a,b)->Integer.compare(fixed(a)>0?0:care(a)>0?1:profile(a).equals("TEARS")?2:3,fixed(b)>0?0:care(b)>0?1:profile(b).equals("TEARS")?2:3));
  Search search=new Search(work,pins,start,end);search.go(0);out.visited=search.visited;
  List<JSONObject> assigned=search.solved?search.placed:search.best;Set<String> ids=new HashSet<>();for(JSONObject p:assigned){p.remove("conflict");out.plans.put(p);ids.add(p.getString("id"));}
  for(JSONObject p:work)if(!ids.contains(p.getString("id"))){String why=search.reason(p,p.getLong("desired_epoch"));if(why.isEmpty())why="当前搜索未找到可行位置；请调整作息或偏好，未放宽任何约束";p.put("status","Unresolved").put("conflict",why).remove("scheduled_epoch");p.put("scheduled",JSONObject.NULL);out.plans.put(p);out.conflicts.put(p.optString("name")+" 第"+p.optInt("index")+"次："+why);}
  for(int i=0;i<out.plans.length();i++){JSONObject p=out.plans.getJSONObject(i);if(p.optString("status").equals("NeedsReview"))out.conflicts.put(p.optString("name")+"：原计划已过期，需补记实际使用或标记漏滴；未自动补滴");}
  return out;
 }
 public static JSONArray validate(JSONArray tasks,JSONArray records,long start,long end)throws JSONException{
  JSONArray errors=new JSONArray();List<JSONObject> all=new ArrayList<>();List<Long> times=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(int i=0;i<records.length();i++){JSONObject r=records.getJSONObject(i);if(r.optString("actual").isEmpty())continue;all.add(r);times.add(r.optLong("actual_epoch",epoch(r.getString("actual"))));ids.add(r.getString("id"));}
  for(int i=0;i<tasks.length();i++){JSONObject t=tasks.getJSONObject(i);if(ids.contains(t.getString("id"))||t.isNull("scheduled")||!t.optString("status").equals("Pending"))continue;long q=at(t);if(fixed(t)>0&&q!=t.optLong("desired_epoch",q))errors.put(t.optString("name")+"：固定12小时计划须成对调整，请修改药物规则中的首剂时间并预览");if(q<start||q+care(t)>end)errors.put(t.optString("name")+"：超出清醒窗口");
   for(int j=0;j<all.size();j++){String r=pair(t,q,all.get(j),times.get(j));if(!r.isEmpty())errors.put(local(q)+" / "+local(times.get(j))+"  "+r);}all.add(t);times.add(q);
  }return errors;
 }
}
