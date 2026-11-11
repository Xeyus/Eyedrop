package com.eyedrop.manager;
import java.time.*;
import java.util.*;
/** Reminder placement only. Does not prescribe dosage or catch-up treatment. */
public final class Schedule {
 public static int window(int wake,int end){int n=(end-wake+1440)%1440;if(n==0)throw new IllegalArgumentException("起床与结束时间不能相同");return n;}
 public static List<LocalDateTime> times(LocalDate day,int wake,int end,int count){
  int span=window(wake,end);if(count<1||count>span+1)throw new IllegalArgumentException("使用次数超出清醒窗口可安排的分钟数");
  List<LocalDateTime> r=new ArrayList<>();LocalDateTime start=day.atStartOfDay().plusMinutes(wake);
  for(int i=0;i<count;i++)r.add(start.plusMinutes(count==1?0:Math.round((double)i*span/(count-1))));return r;
 }
 public static LocalDate day(LocalDateTime now,int wake,int end){return end<wake&&now.toLocalTime().toSecondOfDay()/60<=end?now.toLocalDate().minusDays(1):now.toLocalDate();}
}
