package com.cpuconf;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by emir on 3/23/16.
 */
public class Logger {


    final private static boolean DBG = Definitions.DBG;
    final static public String TAG = "Logger";
    private static final String LOG_FILE_NAME = "mHealthProject";
    private static final String LOG_FILE_NAME_PHONE = "mHealthProjectPHONE";
    final static public String UPLOAD_FILE_NAME = "Upload.log";
    private static FileOutputStream mOutputStream = null;
    private static FileOutputStream mOutputStreamPhone = null;

    final private static Object mLogLock = new Object();

    private static final byte[] SPACE  = " ".getBytes();
    private static final byte[] NEWLINE= "\n".getBytes();


    public static void createLogFile(Context c) {
        synchronized (mLogLock) {
            try {
                mOutputStream = c.openFileOutput(LOG_FILE_NAME, Context.MODE_APPEND);
            } catch (Exception e) {
                Log.e(TAG, "Can't open file " + LOG_FILE_NAME + ":" + e);

            }
        }
    }

    public static void createLogFileToUpload(Context c) {
        synchronized (mLogLock) {
            try {
                mOutputStreamPhone = c.openFileOutput(LOG_FILE_NAME_PHONE, Context.MODE_APPEND);
            } catch (Exception e) {
                Log.e(TAG, "Can't open file " + LOG_FILE_NAME_PHONE + ":" + e);

            }
        }
    }




    public void logEntry(String s){
        try {
            mOutputStream.write(s.getBytes());
            mOutputStream.write(NEWLINE);
        } catch (IOException ioe) {
            Log.e(TAG, "ERROR: Can't write string to file: " + ioe);
        }
    }






    public static long logFileSize(Context c) {
        File log_file = new File(c.getFilesDir(), Definitions.LOG_FILE_NAME);
        if (!log_file.exists()) {
            return 0;
        }
        return log_file.length();
    }
}