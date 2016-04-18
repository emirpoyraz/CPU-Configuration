package com.cpuconf;

import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created by emir on 4/13/16.
 */
public class CPUtil {

    final private static boolean DBG = Definitions.DBG;
    final private static String TAG = "CPUtil";
    final private static String STAT_FILE = "/proc/stat";
    final private static String CPU_FREQ_FILE = "/proc/cpuinfo";
    final private static String CPU_FREQ_FILE_SCALE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    final private DecimalFormat mPercentFmt = new DecimalFormat("#0.0");


    private long mUser;
    private long mSystem;
    private long mTotal;
    private double user_sys_perc2;
    private double user_sys_perc3;
    private Vector<TextView> mDisplay;

    private int putItToDataHolder =0;
    private int user_sys_perc2Counter =0;



    public CPUtil() {
        readStats();
    }


    private static final byte[] FREQ_COMMAND = FileRepeatReader.generateReadfileCommand(CPU_FREQ_FILE);
    private static final String FREQ_KEY = "BogoMIPS";
    private static final byte[] FREQ_COMMAND_SCALE = FileRepeatReader.generateReadfileCommand(CPU_FREQ_FILE_SCALE);
    private static final String FREQ_KEY_SCALE = "CPU_Freq_Scale";



    public boolean readStats() {
        FileReader fstream;

        //   this.readCpuFreqScale();

        try {
            fstream = new FileReader(STAT_FILE);
        } catch (FileNotFoundException e) {
            if (DBG) {Log.e("MonNet", "Could not read " + STAT_FILE);}
            return false;
        }
        BufferedReader in = new BufferedReader(fstream, 500);
        String line;
        try {
            while ((line = in.readLine()) != null ) {
                String[] firstLine = line.split("\\s+");
                if (firstLine[0].equalsIgnoreCase("cpu")) {
                    updateStats(line.trim().split("[ ]+"), 00);
                   // return true;
                }
                 else if (firstLine[0].equalsIgnoreCase("cpu0")) {
                    updateStats(line.trim().split("[ ]+"), 0);
                  //  return true;
                }
                else if (firstLine[0].equalsIgnoreCase("cpu1")) {
                    updateStats(line.trim().split("[ ]+"), 1);
                   // return true;
                }
                else if (firstLine[0].equalsIgnoreCase("cpu2")) {
                    updateStats(line.trim().split("[ ]+"), 2);
                    //return true;
                }
                else if (firstLine[0].equalsIgnoreCase("cpu3")) {
                    updateStats(line.trim().split("[ ]+"),3);
                    //return true;
                }
                else if (firstLine[0].equalsIgnoreCase("cpu4")) {
                    updateStats(line.trim().split("[ ]+"), 4);
                   /// return true;
                }
                else if (firstLine[0].equalsIgnoreCase("cpu5")) {
                    updateStats(line.trim().split("[ ]+"), 5);
                   // return true;
                }
                else if (firstLine[0].equalsIgnoreCase("cpu6")) {
                    updateStats(line.trim().split("[ ]+"), 6);
                    //return true;
                }
                else if (firstLine[0].equalsIgnoreCase("cpu7")) {
                    updateStats(line.trim().split("[ ]+"), 7);
                    return true;
                }






            }
        } catch (IOException e) {
            if (DBG) {Log.e("MonNet", e.toString());}
        }
        return false;
    }

    private void updateStats(String[] segs, int core) {
        // user = user + nice
        long user = Long.parseLong(segs[1]) + Long.parseLong(segs[2]);
        // system = system + intr + soft_irq
        long system = Long.parseLong(segs[3]) +
                Long.parseLong(segs[6]) + Long.parseLong(segs[7]);
        // total = user + system + idle + io_wait
        long total = user + system + Long.parseLong(segs[4]) + Long.parseLong(segs[5]);

        if (mTotal != 0 || total >= mTotal) {
            long duser = user - mUser;
            long dsystem = system - mSystem;
            long dtotal = total - mTotal;
            broadcast(duser, dsystem, dtotal);
            double user_sys_perc = (double)(duser+dsystem)*100.0/dtotal;
            double user_perc = (double)(duser)*100.0/dtotal;
            double sys_perc = (double)(dsystem)*100.0/dtotal;

            if (mDisplay != null) {
                mDisplay.get(0).setText(mPercentFmt.format(user_sys_perc) + "% ("
                        + mPercentFmt.format(user_perc) + "/"
                        + mPercentFmt.format(sys_perc) + ")");
            }

            if (DBG) {
              //  Log.i(TAG, "CPU Util: " + mPercentFmt.format(user_sys_perc) + "% ("
               //         + mPercentFmt.format(user_perc) + "/"
               //         + mPercentFmt.format(sys_perc) + ")");
            }

            //   mHistory.add((int)((duser + dsystem) * 100 / dtotal));

            //JamLoggerService.getLogger().logDoubleDoubleDoubleEntry(Logger.EntryType.CPU_UTILIZATION, user_sys_perc, user_perc, sys_perc);
         //   ServiceClass.getLogger().logEntry("CPU"+ core +" "+user_sys_perc+ " " + user_perc + " " + sys_perc);
            Log.d(TAG, "CPU"+ core +" "+user_sys_perc+ " " + user_perc + " " + sys_perc);



/*  It is the way to transfer data to other classes
            if(String.valueOf(user_sys_perc3)==null) user_sys_perc3=0;
            if(DataHolder.getInstance().getReset()) resetCpuReading();
            user_sys_perc3 =user_sys_perc3 + (double)(duser+dsystem)*100.0/dtotal;
            user_sys_perc2Counter++;
            putItToDataHolder ++;
            if(putItToDataHolder > 10){
                user_sys_perc3 = Double.parseDouble(new DecimalFormat("##.##").format(user_sys_perc3));
                DataHolder.getInstance().setCpuUtil(user_sys_perc3/user_sys_perc2Counter);
                putItToDataHolder =0;
            }
*/
        }

        mUser = user;
        mSystem = system;
        mTotal = total;

    }

    private void resetCpuReading(){
        user_sys_perc3 =0;
        user_sys_perc2Counter =0;
        user_sys_perc2 =0;
        putItToDataHolder =0;
    }

    private Set<CpuMonListener> mListeners =
            Collections.synchronizedSet(
                    new HashSet<CpuMonListener>());

    public void registerListener(CpuMonListener l) {
        mListeners.add(l);
    }
    public void unregisterListener(CpuMonListener l) {
        mListeners.remove(l);
    }
    private void broadcast(long dUser, long dSystem, long dTotal) {
        for (CpuMonListener l : mListeners) {
            l.CpuUtilUpdated(dUser, dSystem, dTotal);
        }
    }

    public interface CpuMonListener {
        public void CpuUtilUpdated(long deltaUser, long deltaSystem, long deltaTotal);
    }
};



