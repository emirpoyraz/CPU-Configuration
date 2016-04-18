package com.cpuconf;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import eu.chainfire.libsuperuser.Shell;

/**
 * Created by emir on 4/1/16.
 */
public class Applications {
    final private static boolean DBG = Definitions.DBG;
    private static final String TAG = "Applications";

    private static final String APP_ID_PATTERN = "u\\d+_a\\d+";
    private static final String PROCESSES_FILE = "ps | grep com.";

    private byte[] procStatCommandCharss;
    private int found = 0;
    private boolean firstLine = true;
    private long userOld =0;
    private long totalTimeInUserOld =0;

    String[] apps_old = new String[30];

    private boolean firstTime = true;

    //  private static final byte[] PROCESSES_FILE_COMMAND = FileRepeatReader.generateReadfileCommand(PROCESSES_FILE);



    public Applications(){
        getApps();
    }

    public void getApps(){
        int totalApps=0;
        long totalTimeInUser =0;
        long deltaTotalUser =0;
        List<Process> processes = new ArrayList<>();
        try {
        //    List<String> stdout = Shell.SH.run("ps | grep");
            List<String> stdout = Shell.SH.run("ps | grep u0");
            int myPid = android.os.Process.myPid();

            String[] apps = new String[30];


            // Log.i(TAG, "Process Manager1: " +line );
            for(String line : stdout) {
                String[] lineArray = line.split("\\s+");
                String user = lineArray[0];

                if (!user.equalsIgnoreCase("u0_a97") || lineArray[8] != null) {

                    int pid = Integer.parseInt(lineArray[1]);
                    int ppid = Integer.parseInt(lineArray[2]);

                    String name = lineArray[8];
                    //  Log.i(TAG, "Process Manager2: " +name + " " + getUser(pid) + " " + pid );
                    // Process processMan = new Process(line);

                    //  Log.i(TAG, "Process Manager3: " +name + " " + getUser(pid) + " " + pid );
                    if (ppid != myPid || !name.equals("toolbox") || !name.contains("cpuconf") || !name.equals("ps") || !name.equals("grep")) {
                        // skip the processes we created to get the running apps.

                        //  processes.add(processMan);

                        long getUserTime = getUser(pid);
                        apps[totalApps] = name + " " + getUserTime + " " + pid;
                        //ServiceClass.getLogger().logEntry(apps[totalApps]);
                        Log.d(TAG, "Process Manager5: " + apps[totalApps]);
                        totalApps++;

                        totalTimeInUser = totalTimeInUser + getUserTime;
                    }
                }
            }





            if(totalTimeInUserOld == 0) {
                totalTimeInUserOld = totalTimeInUser;
            }
            else {
                deltaTotalUser = totalTimeInUser - totalTimeInUserOld;
                totalTimeInUserOld = totalTimeInUser;
            }

            Log.d(TAG, "In here - 1 ########## deltaTotalUser: " + deltaTotalUser);

            if(firstTime){
                apps_old = apps;
                Log.d(TAG, "In here - 2 ##########: " + apps_old[0]);
                firstTime = false;
            } else {
                String line;
                Log.d(TAG, "In here - 3 ##########: " + apps_old[0]);
                //   int i = 0;
                //   int j = 0;
                //  while (i < 35) {
                for (int i = 0; i < 30; i++) {
                    for (int j = 0; j < 30; j++) {
                        // while (j < 35) {
                        Log.d(TAG, "In here - 4 ##########: " + apps_old[j]);
                        if (apps[i] != null && apps_old[j] != null && !apps[i].equalsIgnoreCase("") && !apps_old[j].equalsIgnoreCase("")) {
                            Log.d(TAG, "i and j: " + i + " " + j);
                            String[] lineArrayNew = apps[i].split("\\s+");
                            String[] lineArrayOld = apps_old[j].split("\\s+");
                            Log.d(TAG, "In here - 6 ##########: " + lineArrayNew[0] + " " + lineArrayOld[0]);
                            if (lineArrayNew[0].equalsIgnoreCase(lineArrayOld[0])) {   // same app
                                int userTimeOld = Integer.parseInt(lineArrayOld[1]);
                                int userTimeNew = Integer.parseInt(lineArrayNew[1]);
                                Log.d(TAG, "In here - 5 ##########: userTimeOldTraffic: " + lineArrayNew[0] + " " + lineArrayOld[0] + " " + userTimeOld + " " + userTimeNew);

                                int deltaUserTime = userTimeNew - userTimeOld;


                                Log.d(TAG, "DeltaUserTime: " + apps[i] + " " + deltaUserTime);
                                if (deltaUserTime / deltaTotalUser * 100 >= 10) {
                                    Log.d(TAG, apps[i] + " ListA: " + (deltaUserTime/deltaTotalUser*100));
                                } else {
                                    Log.d(TAG, deltaUserTime + " " + deltaTotalUser + " ListB: " + (deltaUserTime/deltaTotalUser*100));
                                }
                                // j = 34;

                            }
                        }
                        //  j++;

                    }
                    // i++;


                }
            }


                apps_old = apps;
              //  Log.d(TAG, " " + deltaTotalUser + " ListB: " + deltaTotalUser * 100);

/*
                for (int b = 0; b < 35; b++) {
                  //  Log.d(TAG, "Old apps: " + apps_old[b]);

                }

                for (int c = 0; c < 35; c++) {
                   // Log.d(TAG, "New apps1: " + apps[c]);

                }

*/
             //   for (int f = 0; f < 30; f++) {
             //       apps[f] = null;
             //   }



            


        } catch (Exception e) {
            android.util.Log.d(TAG, "Failed parsing line1 " + e);
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String stackTrace = writer.toString();

            // flatten the string to a single line
            stackTrace=stackTrace.replace("\n\r", " / ");
            stackTrace=stackTrace.replace("\r\n", " / ");
            stackTrace=stackTrace.replace("\r", " / ");
            stackTrace=stackTrace.replace("\n", " / ");

            Log.d(TAG, "ERROR: " + stackTrace);

        }
    }


    private long getUser(int pid) {

        long[] data;
        long user =0;
        long kern = 0;
        long userDelta =0;

        procStatCommandCharss = FileRepeatReader.generateReadfileCommand("/proc/" + pid + "/stat");

        try {
            data = getAndParseProcStat(procStatCommandCharss);

            if (data == null) {
                // read is invalid
                return 0;
            } else {

                user = data[0];
                kern = data[1] ;

            }

        }

        catch(Throwable e){}
        return user;
    }

    private static long[] getAndParseProcStat(byte[] fReadCommand) throws Throwable {
        FileRepeatReader mRepeatReader = ServiceClass.getRepeatReader();
        if (mRepeatReader == null) {
                Log.d(TAG, "mRepeatReader is null");
                return null;
        } else {
            long[] ret = null;
            try {
                mRepeatReader.lock();
                mRepeatReader.refresh(fReadCommand);

                FileRepeatReader.SpaceSeparatedLine ssLine;

                if (mRepeatReader.hasNextLine()) {
                    ssLine = mRepeatReader.getSSLine();
                    long user = Long.parseLong(ssLine.getToken(13));
                    long kern = Long.parseLong(ssLine.getToken(14));

                    ret = new long[2];
                    ret[0] = user;
                    ret[1] = kern;
                    //   ret[3] = run;
                } else {
                    ret = null;
                }

            } finally {
                mRepeatReader.unlock();
            }
            return ret;
        }
    }

}

