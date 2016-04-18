package com.cpuconf;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by emir on 3/23/16.
 */
public class ServiceClass extends Service {


    final private static boolean DBG = Definitions.DBG;
    final public static String TAG = "ServiceClass";

    private static Logger mLogger = new Logger();
    private static FileRepeatReader mRepeatReader;

    private int mScreenBrightness = -1;
  //  private UStats uStats;
    private Applications applications;
    private CPUtil cpUtil;

    static {
        try {
            mRepeatReader = new FileRepeatReader(4096);
        } catch (IOException e) {
            if (DBG) {
                Log.e(TAG, "Error creating FileRepeatReader");
                e.printStackTrace();
            }
            mRepeatReader=null;
        }
    }


    public ActivityManager mActivityManager;

    @Override
    public void onCreate() {



        try {
            Logger.createLogFile(this);
            Logger.createLogFileToUpload(this);
            mLogger.logEntry("Logger On");
            //  mProcessManager.readStats();

            //mStatsProc.reset();
         //   uStats = new UStats();
            applications = new Applications();
            cpUtil = new CPUtil();

            mActivityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );



            registerAll();

            mHandler.postDelayed(mRefresh, 1500);


            try {
                mScreenBrightness = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS);
                if (mScreenBrightness >= 0) {
                    if (DBG) {Log.i(TAG, "Screen brightness now " + mScreenBrightness);}
                    //mLogger.logIntEntry(Logger.EntryType.SCREEN_BRIGHTNESS, mScreenBrightness);
                    //
                    //  mLogger.screenBrightness(mScreenBrightness);
                }
            } catch (Settings.SettingNotFoundException e) {
                if (DBG) {Log.e(TAG, "Brightness setting not found");}
            }
        } catch (Throwable t) {
            Log.d(TAG,"Error Occured in ServiceClass: " + t);
        }
    }

    public static FileRepeatReader getRepeatReader() {
        return mRepeatReader;
    }

    public void registerAll(){
     // screen on/off + wifi manager rssi + airplane mode + date + timezone + headset plugged +
        // applications = all of them (music, browser, game?, texting, radio)
        // how much each app is used, optimal configuration...
        // current app info and current cpu conf? also

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_CAMERA_BUTTON);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        filter.addAction(Intent.ACTION_GTALK_SERVICE_CONNECTED);
        filter.addAction(Intent.ACTION_GTALK_SERVICE_DISCONNECTED);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(Intent.ACTION_MANAGE_PACKAGE_STORAGE);
        // PUT INTO PHONE STUFF? - skipping for now
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);

        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_UMS_CONNECTED);
        filter.addAction(Intent.ACTION_UMS_DISCONNECTED);

        filter.addAction(AudioManager.VIBRATE_SETTING_CHANGED_ACTION);

        registerReceiver(mBroadcastIntentReceiver, filter);


    }


    public static Logger getLogger() {
        return mLogger;
    }








    BroadcastReceiver mBroadcastIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (DBG) {Log.i(TAG,">>>>>>>>>> caught ACTION_SCREEN_OFF <<<<<<<<<<<");	}
                //mLogger.logSimpleEntry(Logger.EntryType.SCREEN_OFF);
                mLogger.logEntry("ScreenOff");
                //  if(wakeLock==null)wakeLock.acquire();

            }
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (DBG) {Log.i(TAG,">>>>>>>>>> caught ACTION_SCREEN_ON <<<<<<<<<<<");}
                //mLogger.logSimpleEntry(Logger.EntryType.SCREEN_ON);
                mLogger.logEntry("ScreenOn");
                //   if (wakeLock!=null ||  wakeLockisOn) wakeLock.release();
                //    wakeLock=null;

            }

            else if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
                Bundle extra = intent.getExtras();
                int strength = extra.getInt(WifiManager.EXTRA_NEW_RSSI);

                if (DBG) {Log.i(TAG,"Wifi signal strength: " + strength);}
                //mLogger.logIntEntry(Logger.EntryType.WIFI_SIGNAL_STRENGTH, strength);
                mLogger.logEntry("Wifi signal strength: " + strength);
            }

            else if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                Bundle extra = intent.getExtras();
                if (true == (Boolean) extra.get("state")) {
                    if (DBG) {Log.i(TAG,">>>>>>>>>> AIRPLANE MODE: on <<<<<<<<<<<");}
                    //mLogger.logSimpleEntry(Logger.EntryType.AIRPLANE_MODE_ON);
                    mLogger.logEntry("Airplane_On");
                } else {
                    if (DBG) {Log.i(TAG,">>>>>>>>>> AIRPLANE MODE: off <<<<<<<<<<<");}
                    //mLogger.logSimpleEntry(Logger.EntryType.AIRPLANE_MODE_OFF);
                    mLogger.logEntry("Airplane_Off");
                }
            }


            else if (intent.getAction().equals(Intent.ACTION_DATE_CHANGED)) {
                if (DBG) {Log.i(TAG,"Date changed");}
                //mLogger.logSimpleEntry(Logger.EntryType.DATE_CHANGED);
                mLogger.logEntry("Date_changed");
            }

            else if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                Bundle extra = intent.getExtras();
                String state = "";
                switch ((Integer) extra.get("state")) {
                    case 0:
                        state = "unplugged";
                        break;
                    case 1:
                        state = "plugged";
                        break;
                    default:
                        state = "invalid";
                        break;
                }
                if (DBG) {Log.i(TAG,"Headset Plug: " + state);}
                //mLogger.logStringEntry(Logger.EntryType.HEADSET_PLUG, state);
                mLogger.logEntry("Headset_Plug: " + state);
            }

            else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
                if (DBG) {Log.i(TAG,"Time changed");}
                //mLogger.logSimpleEntry(Logger.EntryType.TIME_CHANGED);
                mLogger.logEntry("Time_Changed");
            }

            else if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                Bundle extra = intent.getExtras();
                String timezone = (String) extra.get("time-zone");
                TimeZone tz = java.util.TimeZone.getTimeZone(timezone);
                int tz_offset = tz.getRawOffset();
                if (DBG) {Log.i(TAG,"Timezone Change: " + tz_offset);}
                //mLogger.logIntEntry(Logger.EntryType.TIMEZONE_CHANGED, tz_offset);
                mLogger.logEntry("Timezone_Changed: " + tz_offset);
            }

            else if (intent.getAction().equals(Intent.ACTION_GTALK_SERVICE_CONNECTED)) {
                if (DBG) {Log.i(TAG,"GTalk Service Connected");}
                //mLogger.logSimpleEntry(Logger.EntryType.GTALK_SERVICE_CONNECTED);
                mLogger.logEntry("GTalk_Service_Connected");
            }
            else if (intent.getAction().equals(Intent.ACTION_GTALK_SERVICE_DISCONNECTED)) {
                if (DBG) {Log.i(TAG,"GTalk Service Disconnected");}
                //mLogger.logSimpleEntry(Logger.EntryType.GTALK_SERVICE_DISCONNECTED);
                mLogger.logEntry("GTalk_Service_Disconnected");
            }

            // TODO: POSSIBLY STOP LOGGING WHEN STORAGE IS LOW
            else if (intent.getAction().equals(Intent.ACTION_DEVICE_STORAGE_LOW)) {
                if (DBG) {Log.i(TAG,"Device Storage Low");}
                //mLogger.logSimpleEntry(Logger.EntryType.DEVICE_STORAGE_LOW);
                mLogger.logEntry("Device_Storage_Low");
            }
            else if (intent.getAction().equals(Intent.ACTION_DEVICE_STORAGE_OK)) {
                if (DBG) {Log.i(TAG,"Device Storage Ok");}
                //mLogger.logSimpleEntry(Logger.EntryType.DEVICE_STORAGE_OK);
                mLogger.logEntry("Device_Storage_Ok");
            }

            // low memory condition acknowledged by user - start package management
            else if (intent.getAction().equals(Intent.ACTION_MANAGE_PACKAGE_STORAGE)) {
                if (DBG) {Log.i(TAG,"Manage Package Storage");}
                //mLogger.logSimpleEntry(Logger.EntryType.MANAGE_PACKAGE_STORAGE);
                mLogger.logEntry("Manage_Package_Storage");
            }

            else if (intent.getAction().equals(Intent.ACTION_UMS_CONNECTED)) {
                if (DBG) {Log.i(TAG,"UMS Connected");}
                mLogger.logEntry("UMS_Connected");
            }
            else if (intent.getAction().equals(Intent.ACTION_UMS_DISCONNECTED)) {
                if (DBG) {Log.i(TAG,"UMS Connected");}
                mLogger.logEntry("UMS_Disconnected");
            }

            // TODO ensure this vibrate settings thing works
            else if (intent.getAction().equals(AudioManager.VIBRATE_SETTING_CHANGED_ACTION)) {
                if (DBG) {Log.i(TAG,"Vibrate Setting changed");}

            }

            // TODO: maybe add NULL checking to be safe with all extra.get()s
			/*
			// This one will take some more work with configuration DIFF
			filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);

			// PUT INTO PHONE STUFF?
			// REQUIRE PROCESS_OUTGOING_CALLS permission - do we really need this?
			filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
			*/
        }
    };

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service onDestroy", Toast.LENGTH_LONG).show();
        unregisterReceiver(mBroadcastIntentReceiver);
        mHandler.removeCallbacksAndMessages(null);
        stopSelf();
    }



    private String[] getActivePackagesCompat(){
        final List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        final String[] activePackages = new String[1];
        activePackages[0] = componentName.getPackageName();
        return activePackages;
    }


    private String[] getActivePackages(){
        Set<String> activePackages = new HashSet<String>();
        List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                Log.d(TAG, "Process Name: "+ processInfo.processName);
                activePackages.addAll(Arrays.asList(processInfo.pkgList));

            }
        }
        return activePackages.toArray(new String[activePackages.size()]);
    }



 /*

        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                ActivityManager.RunningAppProcessInfo processName
                if (processInfo.equals("com.google.android.calendar")) {
                    activePackages.addAll(Arrays.asList(processInfo.pkgList));
                }
            }
        }
        return activePackages.toArray(new String[activePackages.size()]);
    }

*/

    Handler mHandler = new Handler();   // handler is more convenient for massage passing between objects and also UI friendly.
    // so if we need to put some info or even in notifications we may need handler instead of thread.
    Runnable mRefresh = new Runnable() {


        @Override
        public void run() {


            try {



                mHandler.postDelayed(mRefresh, 1500);


                String[] activePackages;
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) { // so it is lollipop then it is gonna go here!
                    activePackages = getActivePackagesCompat();
                   // Log.d(TAG, activePackages[0]);
                   // Log.d(TAG,"it is in build-1" + activePackages.toString());
                } else {
                    activePackages = getActivePackagesCompat();
                  //  Log.d(TAG,"it is in build-2" + activePackages.toString());
                }
                if (activePackages != null) {
                    for (String activePackage : activePackages) {
                        Log.d(TAG, activePackage);
                        if (activePackage.equals("com.google.android.calendar")) {
                            //Calendar app is launched, do something
                           // Log.d(TAG,"Calender is working");
                        }
                      //  else Log.d(TAG,"Calender is not working");
                    }
                }


             //   UStats.getStats(ServiceClass.this);
             //   uStats.printCurrentUsageStatus(ServiceClass.this);

              //  applications.getApps();
                cpUtil.readStats();






              //  mHandler.postDelayed(this, 1000);




/*
                try {



                } catch (Exception e) {
                    Log.i(TAG, "Error occured while collecting sensors " + e);
                }

                // deletes 1 sec after sending and creates new log file


                //Send to phone in every 10 sec

*/


            } catch (Exception e) {
                Log.i(TAG, "Error occured " + e);

            }
        }
    };





    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
