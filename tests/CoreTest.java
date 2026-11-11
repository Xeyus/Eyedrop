import com.eyedrop.manager.*;
import java.time.*;
import java.util.*;
import org.json.*;
public class CoreTest {
 static int checks=0;
 static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  LocalDate date=LocalDate.of(2026,11,11);
  List<LocalDateTime> t=Schedule.times(date,480,1320,4);
  check(t.toString().equals("[2026-11-11T08:00, 2026-11-11T12:40, 2026-11-11T17:20, 2026-11-11T22:00]"),"4 evenly distributed times");
  check(Schedule.times(date,480,1320,1).get(0).equals(date.atTime(8,0)),"single at wake");
  check(Schedule.times(date,1200,120,3).get(2).equals(date.plusDays(1).atTime(2,0)),"overnight ending");
  check(Schedule.day(date.atTime(1,0),1200,120).equals(date.minusDays(1)),"overnight day attribution");
  for(int count:new int[]{0,-1,842}){boolean rejected=false;try{Schedule.times(date,480,1320,count);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid frequency");}
  boolean rejected=false;try{Schedule.window(480,480);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"equal times rejected");
  Random random=new Random(7);for(int n=0;n<500;n++){int wake=random.nextInt(1440),span=1+random.nextInt(1439),end=(wake+span)%1440,count=1+random.nextInt(Math.min(24,span+1));List<LocalDateTime> a=Schedule.times(date,wake,end,count);check(a.size()==count,"count");check(new HashSet<>(a).size()==count,"unique minute slots");check(a.get(0).equals(date.atStartOfDay().plusMinutes(wake)),"start");if(count>1)check(a.get(count-1).equals(date.atStartOfDay().plusMinutes(wake+span)),"end");}
  Store s=new Store(null);JSONObject a=new JSONObject("{\"id\":\"a\",\"name\":\"药A\",\"count\":4,\"active\":true,\"dose\":\"医嘱剂量\"}");JSONObject b=new JSONObject("{\"id\":\"b\",\"name\":\"药B\",\"count\":2,\"active\":true}");s.drugs().put(a).put(b);s.generate();check(s.plans().length()==6,"multiple medications");String id=s.plans().getJSONObject(0).getString("id"),original=s.plans().getJSONObject(0).getString("scheduled");s.complete(id,LocalDateTime.now());s.complete(id,LocalDateTime.now());check(s.records().length()==1,"duplicate completion blocked");
  a.put("count",3).put("name","新药名");s.generate();check(s.plans().length()==5,"edit regenerates pending");check(s.records().getJSONObject(0).getString("name").equals("药A"),"record snapshot immutable");check(s.records().getJSONObject(0).getString("scheduled").equals(original),"original schedule preserved");
  s.drugs().remove(0);s.generate();check(s.plans().length()==3,"delete retains completed only");check(s.records().length()==1,"delete keeps records");b.put("active",false);s.generate();check(s.plans().length()==1,"pause removes pending");Store restored=new Store(s.data.toString());restored.generate();check(restored.records().length()==1&&restored.plans().length()==1,"serialization round trip");
  s.data.put("plans",new JSONArray());s.generate();check(s.plans().length()==1&&s.plans().getJSONObject(0).optBoolean("done"),"completed reconstructed from immutable records");
  Store reduced=new Store(null);JSONObject rx=new JSONObject("{\"id\":\"r\",\"name\":\"Test\",\"count\":4,\"active\":true}");reduced.drugs().put(rx);reduced.generate();reduced.complete(reduced.plans().getJSONObject(3).getString("id"),LocalDateTime.now());rx.put("count",2);reduced.generate();check(reduced.plans().length()==2,"reducing frequency after a later slot was completed does not add excess tasks");
  System.out.println("PASS: "+checks+" assertions (500 generated schedule scenarios, CRUD, duplicate completion, immutable records, persistence).");
 }
}
