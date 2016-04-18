package com.cpuconf;

/**
 * Created by emir on 4/6/16.
 */
import android.annotation.TargetApi;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by User on 3/2/15.
 */
public class UStats {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("M-d-yyyy HH:mm:ss");
    public static final String TAG = UStats.class.getSimpleName();

    long startTime = System.currentTimeMillis();
    
    
    
    @SuppressWarnings("ResourceType")
    public static void getStats(Context context){
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        int interval = UsageStatsManager.INTERVAL_MONTHLY;
        Calendar calendar = Calendar.getInstance();

       // long endTime = System.currentTimeMillis();

       // long startTime = System.currentTimeMillis() - 5000; // every 2 sec, 2 lists A and B and A is 20% cpu util in foreground, B is for background apps
        //long endTime = calendar.getTimeInMillis();        // in the mean time it needs to collect all other infos. (frame time, avg, std.. etc)

        //long startTime = calendar.getTimeInMillis();

        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_WEEK, 4);
        long startTime = calendar.getTimeInMillis();

        Log.d(TAG, "Range start:" + dateFormat.format(startTime) );
        Log.d(TAG, "Range end:" + dateFormat.format(endTime));

        UsageEvents uEvents = usm.queryEvents(startTime, endTime);
        while (uEvents.hasNextEvent()){
            UsageEvents.Event e = new UsageEvents.Event();
            uEvents.getNextEvent(e);

            if (e != null){
                Log.d(TAG, "Event: " + e.getPackageName() + "\t" +  e.getTimeStamp());
            }
        }
    }


    protected static class LollipopTaskInfo {
        protected String packageName = "";
        protected String lastRecentPackageName = "";
        protected String className = "";
        protected String lastPackageName = "";
        protected long lastUsedStamp;
        protected long timeInFGDelta;
        protected long timeInFG;

        LollipopTaskInfo (String string) {
            packageName = string;
        }
    }



    @TargetApi(21)
    public static LollipopTaskInfo parseUsageStats(List<UsageStats> stats, LollipopTaskInfo lollipopTaskInfo) {
        UsageStats aRunner = stats.get(0);
        UsageStats bRunner = null;

        if (lollipopTaskInfo == null) {
            // setup new lollipopTaskInfo object!

            lollipopTaskInfo = new UStats.LollipopTaskInfo(aRunner.getPackageName());
        } else if (lollipopTaskInfo.packageName.equals(aRunner.getPackageName())) {
//            Tools.HangarLog("Last package same as current top, skipping! [" + lollipopTaskInfo.packageName + "]");
            return lollipopTaskInfo;
        }

        // TODO change this to keep track of all usagestats and compare timeinFg deltas
        // Will need to refactor buildTasks to manage bulk time change to db as well as
        // new runningTask.

        for (UsageStats s : stats) {
            if (s.getPackageName().equals(lollipopTaskInfo.packageName)) {
                bRunner = s;
            }
        }

        lollipopTaskInfo.lastPackageName = lollipopTaskInfo.packageName;
        lollipopTaskInfo.packageName = aRunner.getPackageName();
        if (bRunner == null) {
            Log.d(TAG, "Couldn't find previous task [" + lollipopTaskInfo.packageName + "]");
        } else {
            lollipopTaskInfo.timeInFGDelta = (lollipopTaskInfo.timeInFG > 0) ? bRunner.getTotalTimeInForeground() - lollipopTaskInfo.timeInFG : 0;
        }
        lollipopTaskInfo.timeInFG = aRunner.getTotalTimeInForeground();

        Log.d(TAG, "New [" + lollipopTaskInfo.packageName + "] old [" + lollipopTaskInfo.lastPackageName + "] old FG delta: " + lollipopTaskInfo.timeInFGDelta);

        return lollipopTaskInfo;
    }



    public List<UsageStats> getUsageStatsList(Context context){
        UsageStatsManager usm = getUsageStatsManager(context);
        Calendar calendar = Calendar.getInstance();

        long endTime = System.currentTimeMillis();


     //   long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_WEEK, -1);
    //    long startTime = calendar.getTimeInMillis();

      //  Log.d(TAG, "Range start:" + dateFormat.format(startTime) );
      //  Log.d(TAG, "Range end:" + dateFormat.format(endTime));

        Log.d(TAG, "Range end:" + endTime );
        Log.d(TAG, "Range start:" + startTime);

        List<UsageStats> usageStatsList = usm.queryUsageStats(4,startTime,endTime);

        startTime = endTime;

        return usageStatsList;
    }

    public void printUsageStats(List<UsageStats> usageStatsList){
        long totalForeground=0;
        long timePercentage;
        for (UsageStats u : usageStatsList){
            totalForeground =  totalForeground + u.getTotalTimeInForeground();
            Log.d(TAG, "Pkg: " + u.getPackageName() +  "\t" + "ForegroundTimeListB: "
                    + u.getTotalTimeInForeground()) ;
        }
        Log.d(TAG, "TotalForegroundTime: " + totalForeground);

      //  LollipopTaskInfo lollipopTaskInfo = new LollipopTaskInfo("123");
     //   parseUsageStats(usageStatsList, lollipopTaskInfo);




        for (UsageStats r : usageStatsList){
               //   Log.d(TAG, "Pkg: " + r.getPackageName() + "\t" + "ForegroundTimeListA: "
                //          + r.getTotalTimeInForeground() + " TimeInCPU: " + r.getTotalTimeInForeground()/totalForeground * 100) ;


        }




    }

    public void printCurrentUsageStatus(Context context){
        printUsageStats(getUsageStatsList(context));
    }
    @SuppressWarnings("ResourceType")
    private static UsageStatsManager getUsageStatsManager(Context context){
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        return usm;
    }
}
