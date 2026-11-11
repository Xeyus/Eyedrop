package com.eyedrop.manager;
import org.json.*;
import java.time.*;
import java.util.*;
public final class Store {
 public JSONObject data;
 public Store(String json)throws JSONException{
  data=json==null?new JSONObject("{\"wake\":480,\"end\":1320,\"drugs\":[],\"plans\":[],\"records\":[]}"):new JSONObject(json);
  if(!data.has("cares"))data.put("cares",new JSONArray());data.put("schema_version",2);
  normalize(data.getJSONArray("plans"));normalize(data.getJSONArray("records"));
 }
 private void normalize(JSONArray list)throws JSONException{for(int i=0;i<list.length();i++){JSONObject p=list.getJSONObject(i);if(!p.has("status"))p.put("status",p.optBoolean("done")?"Completed":"Pending");if(!p.has("original_scheduled"))p.put("original_scheduled",p.opt("scheduled"));if(!p.isNull("scheduled")&&!p.has("scheduled_epoch"))p.put("scheduled_epoch",Constraints.epoch(p.getString("scheduled")));if(p.has("actual")&&!p.isNull("actual")&&!p.has("actual_epoch"))p.put("actual_epoch",Constraints.epoch(p.getString("actual")));}}
 public int wake(){return data.optInt("wake",480);} public int end(){return data.optInt("end",1320);}
 public JSONArray drugs(){return data.optJSONArray("drugs");} public JSONArray plans(){return data.optJSONArray("plans");} public JSONArray records(){return data.optJSONArray("records");}public JSONArray cares(){return data.optJSONArray("cares");}
 public String day(){return Schedule.day(LocalDateTime.now(),wake(),end()).toString();}
 public long start(){return LocalDate.parse(day()).atStartOfDay().plusMinutes(wake()).atZone(ZoneId.systemDefault()).toEpochSecond();}
 public long endEpoch(){return LocalDate.parse(day()).atStartOfDay().plusMinutes(wake()+Schedule.window(wake(),end())).atZone(ZoneId.systemDefault()).toEpochSecond();}
 public Store copy()throws JSONException{return new Store(data.toString());}
 public static void configure(JSONObject d,String profile,int careMinutes,int pressMinutes)throws JSONException{
  d.put("profile",profile).put("frequency_per_day",d.getInt("count")).put("schedule_type",profile.equals("CYCLO")?"FIXED_INTERVAL":"UNIFORM").put("fixed_interval",profile.equals("CYCLO")?720:0);
  if(profile.equals("CYCLO")){if(d.getInt("count")!=2)throw new IllegalArgumentException("环孢素配置须每日2次、间隔12小时");careMinutes=0;pressMinutes=0;}
  if(profile.equals("CARE")){if(d.getInt("count")!=3)throw new IllegalArgumentException("施图伦配置须每日3次");careMinutes=10;pressMinutes=3;}
  if(careMinutes<0||careMinutes>60||pressMinutes<0||pressMinutes>careMinutes)throw new IllegalArgumentException("护理总时长须0–60分钟，按压时长不能超过总时长");
  d.put("care_seconds",careMinutes*60).put("press_seconds",pressMinutes*60);
  JSONArray constraints=new JSONArray();if(profile.equals("CYCLO")||profile.equals("TEARS"))constraints.put(new JSONObject().put("type","SEPARATION").put("target_profile",profile.equals("CYCLO")?"TEARS":"CYCLO").put("duration",900));
  if(careMinutes>0)constraints.put(new JSONObject().put("type","EXCLUSIVE_WINDOW").put("duration",careMinutes*60));d.put("interval_constraints",constraints);
  JSONArray actions=new JSONArray();if(pressMinutes>0)actions.put(new JSONObject().put("action_type","PRESS_LACRIMAL").put("duration",pressMinutes*60).put("offset",0));if(careMinutes>pressMinutes)actions.put(new JSONObject().put("action_type","EYES_CLOSED").put("duration",(careMinutes-pressMinutes)*60).put("offset",pressMinutes*60));d.put("post_dose_actions",actions);
 }
 public JSONArray pins()throws JSONException{JSONArray pins=new JSONArray();for(int i=0;i<records().length();i++){JSONObject p=new JSONObject(records().getJSONObject(i).toString());for(int j=0;j<drugs().length();j++){JSONObject d=drugs().getJSONObject(j);if(d.getString("id").equals(p.optString("drugId"))){p.put("profile",d.optString("profile","GENERAL")).put("fixed_interval",d.optInt("fixed_interval"));break;}}pins.put(p);}return pins;}
 public Constraints.Result generate(boolean repair,long now)throws JSONException{
  String day=day();Map<String,JSONObject> finished=new HashMap<>(),old=new HashMap<>();JSONArray next=new JSONArray();
  for(int i=0;i<plans().length();i++){JSONObject p=plans().getJSONObject(i);old.put(p.getString("id"),p);}
  for(int i=0;i<records().length();i++){JSONObject p=records().getJSONObject(i);if(p.getString("day").equals(day)){JSONObject snapshot=new JSONObject(p.toString()).put("done",true);finished.put(p.getString("id"),snapshot);next.put(snapshot);}}
  JSONArray pins=pins();
  for(int n=0;n<drugs().length();n++){JSONObject d=drugs().getJSONObject(n);if(!d.optBoolean("active",true))continue;int count=d.getInt("count");Schedule.times(LocalDate.parse(day),wake(),end(),count);
   int completed=0;for(JSONObject p:finished.values())if(p.optString("drugId").equals(d.getString("id")))completed++;int remaining=Math.max(0,count-completed);
   long anchor=Constraints.epoch(LocalDate.parse(day()).atStartOfDay().plusMinutes(d.optInt("first_time",wake())).toString());if(anchor<start())anchor+=86400;
   if(d.optInt("fixed_interval")>0){long latest=Long.MIN_VALUE;JSONObject last=null;for(int i=0;i<pins.length();i++){JSONObject p=pins.getJSONObject(i);if(p.optString("drugId").equals(d.getString("id"))&&p.has("actual_epoch")&&p.getLong("actual_epoch")>latest&&p.getLong("actual_epoch")<=now){latest=p.getLong("actual_epoch");last=p;}}if(last!=null){if(last.optString("day").equals(day))anchor=latest-(last.optInt("index")-1)*720L*60;else{anchor=latest+720*60;while(anchor<start())anchor+=720*60;}}}
   List<LocalDateTime> uniform=Schedule.times(LocalDate.parse(day),wake(),end(),count);
   for(int i=0;i<count&&remaining>0;i++){String id=day+":"+d.getString("id")+":"+i;if(finished.containsKey(id))continue;remaining--;
    JSONObject previous=old.get(id);long preferred=d.optInt("fixed_interval")>0?anchor+i*720L*60:Constraints.epoch(uniform.get(i).toString());if(repair&&d.optInt("fixed_interval")==0&&previous!=null&&!previous.isNull("scheduled"))preferred=Constraints.at(previous);
    JSONObject p=new JSONObject().put("id",id).put("drugId",d.getString("id")).put("day",day).put("name",d.getString("name")).put("dose",d.optString("dose")).put("index",i+1).put("total",count).put("done",false).put("status","Pending").put("desired_epoch",preferred).put("scheduled",Constraints.local(preferred)).put("scheduled_epoch",preferred).put("profile",d.optString("profile","GENERAL")).put("fixed_interval",d.optInt("fixed_interval")).put("care_seconds",d.optInt("care_seconds")).put("press_seconds",d.optInt("press_seconds")).put("post_dose_actions",d.optJSONArray("post_dose_actions")==null?new JSONArray():new JSONArray(d.getJSONArray("post_dose_actions").toString()));
    p.put("original_scheduled",previous==null?Constraints.local(preferred):previous.opt("original_scheduled"));
    if(previous!=null&&(!previous.optString("status").equals("Unresolved")&&!previous.isNull("scheduled")&&Constraints.at(previous)<now||previous.optString("status").equals("NeedsReview")))p.put("status","NeedsReview").put("scheduled",previous.opt("scheduled")).put("scheduled_epoch",Constraints.at(previous));
    else if(repair||previous!=null)p.put("not_before",now);
    next.put(p);
   }
  }
  Constraints.Result result=Constraints.solve(next,pins,start(),endEpoch());for(int i=0;i<result.plans.length();i++){JSONObject p=result.plans.getJSONObject(i);if(!old.containsKey(p.getString("id"))&&!p.isNull("scheduled")&&!p.optBoolean("done"))p.put("original_scheduled",p.getString("scheduled"));}data.put("plans",result.plans).put("conflicts",result.conflicts).put("plan_day",day);return result;
 }
 public void generate()throws JSONException{generate(false,Instant.now().getEpochSecond());}
 public JSONArray validation()throws JSONException{return Constraints.validate(plans(),pins(),start(),endEpoch());}
 public JSONArray actualConflicts(JSONObject task,long actual)throws JSONException{JSONArray errors=new JSONArray();JSONArray pins=pins();for(int i=0;i<pins.length();i++){JSONObject p=pins.getJSONObject(i);if(!p.has("actual_epoch"))continue;String r=Constraints.pair(task,actual,p,p.getLong("actual_epoch"));if(!r.isEmpty())errors.put(r+"（实际 "+Constraints.local(p.getLong("actual_epoch"))+"）");}return errors;}
 public void complete(String id,LocalDateTime actual)throws JSONException{completeAt(id,actual.atZone(ZoneId.systemDefault()).toEpochSecond());}
 public void completeAt(String id,long actual)throws JSONException{
  for(int i=0;i<plans().length();i++){JSONObject p=plans().getJSONObject(i);if(p.getString("id").equals(id)&&!p.optBoolean("done")){
   String status=!p.isNull("scheduled")&&actual>Constraints.at(p)+60?"Delayed":"Completed";
   JSONObject record=new JSONObject(p.toString()).put("actual",Constraints.local(actual)).put("actual_epoch",actual).put("status",status).put("violations",actualConflicts(p,actual));records().put(record);p.put("done",true).put("actual",Constraints.local(actual)).put("actual_epoch",actual).put("status",status);
   if(Constraints.care(p)>0){JSONObject c=new JSONObject().put("id",id).put("drugId",p.getString("drugId")).put("name",p.getString("name")).put("original_scheduled",p.opt("original_scheduled")).put("start_time",actual).put("end_time",actual+Constraints.care(p)).put("press_end",actual+p.optInt("press_seconds")).put("status","Running").put("notified_phase",p.optInt("press_seconds")>0?"press":"rest");JSONArray actions=new JSONArray();JSONArray defs=p.optJSONArray("post_dose_actions");if(defs!=null)for(int j=0;j<defs.length();j++){JSONObject a=new JSONObject(defs.getJSONObject(j).toString());a.put("start_time",actual+a.optInt("offset")).put("planned_completion_time",actual+a.optInt("offset")+a.optInt("duration")).put("completion_time",JSONObject.NULL).put("status","Waiting");actions.put(a);}c.put("actions",actions);cares().put(c);tick(actual);}
   for(int j=0;j<plans().length();j++){JSONObject future=plans().getJSONObject(j);if(future.optBoolean("done")||future.isNull("scheduled"))continue;String reason=Constraints.pair(future,Constraints.at(future),record,actual);if(!reason.isEmpty())future.put("status","Unresolved").put("conflict",reason).put("scheduled",JSONObject.NULL).remove("scheduled_epoch");}return;
  }}
 }
 public void missed(String id)throws JSONException{for(int i=0;i<plans().length();i++){JSONObject p=plans().getJSONObject(i);if(p.getString("id").equals(id)&&!p.optBoolean("done")){JSONObject r=new JSONObject(p.toString()).put("status","Missed").put("actual",JSONObject.NULL);records().put(r);p.put("done",true).put("status","Missed");return;}}}
 public boolean tick(long now)throws JSONException{boolean changed=false;for(int i=0;i<cares().length();i++)changed|=tickCare(cares().getJSONObject(i),now);return changed;}
 public boolean tickCare(JSONObject c,long now)throws JSONException{if(c.optString("status").equals("Completed"))return false;boolean changed=false;JSONArray actions=c.getJSONArray("actions");for(int i=0;i<actions.length();i++){JSONObject a=actions.getJSONObject(i);if(a.optString("status").equals("Completed"))continue;String status=now>=a.getLong("planned_completion_time")?"Completed":now>=a.getLong("start_time")?"Running":"Waiting";if(!status.equals(a.getString("status"))){a.put("status",status);if(status.equals("Completed"))a.put("completion_time",a.getLong("planned_completion_time"));changed=true;}}if(now>=c.getLong("end_time")){c.put("status","Completed").put("completion_time",c.getLong("end_time"));changed=true;}return changed;}
 public boolean hasCare(){for(int i=0;i<cares().length();i++)if(!cares().optJSONObject(i).optString("status").equals("Completed"))return true;return false;}
}
