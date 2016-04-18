package com.cpuconf;

/**
 * Created by emir on 4/5/16.
 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

//import eu.chainfire.libsuperuser.Shell;

public class FileRepeatReader {





    /**
     * Read a file into memory for quick parsing;
     * Expects the *same* file to be read repeatedly
     *
     */
    //private String mPath;
    private char[] mBuffer;
    private Reader mReader;
    private char[] mErrBuffer;
    private Reader mErrorReader;

    //private static final long READ_DELAY_STEP = 5; // in ms, approx.
    private static final long READ_DELAY_MAX = 80; // in ms, approx.

    private int mReadCount;
    private static final String TAG = "FileRepeatReader";

    private final ReentrantLock accessLock = new ReentrantLock();

    public FileRepeatReader(int bufSize) throws IOException {
        mBuffer = new char[bufSize];
        mErrBuffer = new char[bufSize];

        setupThreadsafeBuffers(bufSize);
    }

    public static byte[] generateReadfileCommand(String path) {
        // Byte Array

        return ("cat " + path + System.getProperty("line.separator")).getBytes();
        // Char Array
        //return ("cat "+path+System.getProperty("line.separator")).toCharArray();

    }






    public boolean lock() throws InterruptedException {
        if (accessLock.isHeldByCurrentThread()) {
            return true;
        }
        boolean ret;
        ret = (accessLock.tryLock() || accessLock.tryLock(50L, TimeUnit.MILLISECONDS));

        if (! ret) {
            throw new InterruptedException("Unable to acquire lock");
        }
        return ret;
    }

    public void unlock() {
        if (accessLock.isHeldByCurrentThread()) {
            accessLock.unlock();
        }
    }


	/*   // Version 1: FileReader
	public void refresh() throws IOException, FileNotFoundException {
		mReader = new FileReader(mFile);
		mReadCount = mReader.read(mBuffer);


		mStart = 0;
		mEnd = 0;
	}
	*/

    // Version 2: active SH session
    private Process mProcess;
    // Byte Writer
    private OutputStream mStdIn;
    // Char Writer
    //private OutputStreamWriter mStdIn;
    {
        try {
            mProcess = Runtime.getRuntime().exec("sh");
            // Byte Writer
            mStdIn = mProcess.getOutputStream();
            // Char Writer
            //mStdIn = new OutputStreamWriter(mProcess.getOutputStream());
            mReader = new InputStreamReader(mProcess.getInputStream());
            mErrorReader = new InputStreamReader(mProcess.getErrorStream());
        } catch (IOException e) {
            Log.d(TAG, "Could not spawn sh process");
            e.printStackTrace();
        }
    }


    /**
     * Refresh the FileRepeatReader with the given command.
     * Blocks until either
     *   A) something is written to STDOUT or STDERR or
     *   B) time timeout is reached (internally set to 300 ms).
     * If anything spits out StdErr, this throws 'FileNotFoundException'
     *
     * @param command to be executed (ex. "cat /proc/loadavg")
     * @throws IOException if the calling thread did not obtain the
     * FileRepeatReader lock before calling
     * @throws FileNotFoundException if anything is written on STDERR
     * @throws InterruptedException if something interrupts this
     */


    // Implementation 2.6: TODO runtime?  bugs?
    public void refresh(byte[] command) throws IOException, FileNotFoundException {
        if (! stdErrRead.isAlive()) {
            stdErrRead.start();
        }
        if (! stdOutRead.isAlive()) {
            stdOutRead.start();
        }
        if (!accessLock.isHeldByCurrentThread()) {
            throw new IOException("Lock not acquired by calling thread");
        } else {
            //if (Defs.DBG){
            //	Log.d(TAG, "Running command \""+ (new String(command)).trim());
            //}
            synchronized(inCommand) {
                mErrorOccurred.set(false);
                mNewInput.set(false);
                inCommand.set(true);
            }
            // TODO run the command, wait for a notify

            mStdIn.write(command);
            mStdIn.flush();

            IOException timeoutErr = null;

            try {
                synchronized(mNewInput) {
                    mNewInput.wait(READ_DELAY_MAX);
                }
                synchronized(inCommand) {
                    if (mNewInput.get()) {
                        if (mErrorOccurred.get()) {
                            mErrBuffer = getCurrentErrBuffer();
                            mReadCount = 0;
                            throw new FileNotFoundException("Error in command \""+new String(command).trim()+"\": \""+new String(mErrBuffer, 0, mErrorBufferRead.get())+"\"");
                        } else {
                            // No error!
                            mBuffer = getCurrentBuffer();
                            mReadCount = mBufferRead.get();
                            //if (Defs.DBG){
                            //	Log.d(TAG, "Contents: \""+new String(mBuffer, 0, mReadCount)+"\"");
                            //}
                        }
                    } else {
                        timeoutErr = new IOException("Timeout on command: \""+new String(command)+"\"");
                        //JamLoggerService.getLogger().errorOccurred(new Throwable("Timeout on command: \""+new String(command)+"\""));
                    }
                }
            } catch (InterruptedException e) {
                //JamLoggerService.getLogger().errorOccurred(e);
                timeoutErr = new IOException(e.getLocalizedMessage());
            }
            synchronized(inCommand) {
                inCommand.set(false);
            }
            if (timeoutErr != null) {
                throw timeoutErr;
            }
            mStart = 0;
            mEnd = 0;
        }
    }

    private AtomicBoolean inCommand = new AtomicBoolean(false);
    private AtomicBoolean mNewInput = new AtomicBoolean(false);
    private AtomicBoolean mErrorOccurred = new AtomicBoolean(false);

    private char[][] mErrBuffers;
    private char[][] mBuffers;

    private void setupThreadsafeBuffers(int bufSize) {
        mErrBuffers = new char[2][];
        mErrBuffers[0] = new char[bufSize];
        mErrBuffers[1] = new char[bufSize];
        mBuffers = new char[2][];
        mBuffers[0] = new char[bufSize];
        mBuffers[1] = new char[bufSize];
    }

    private char[] getCurrentErrBuffer() {
        if (useSecondErrBuffer.get()) {
            return mErrBuffers[1];
        } else {
            return mErrBuffers[0];
        }
    }

    private char[] getCurrentBuffer() {
        if (useSecondBuffer.get()) {
            return mBuffers[1];
        } else {
            return mBuffers[0];
        }
    }

    private AtomicBoolean useSecondErrBuffer = new AtomicBoolean(false);
    private AtomicInteger mErrorBufferRead = new AtomicInteger(0);
    private Thread stdErrRead = new Thread(new Runnable() {
        @Override
        public void run() {
            int nRead, i;
            boolean nonWhitespace;
            char[] r;
            try {
                while (true) {
                    nRead = mErrorReader.read(privCurrentErrBuffer());
                    // TODO only keep going if non-whitespace character read
                    nonWhitespace = false;
                    r = privCurrentErrBuffer();
                    for (i=0; i<nRead; i++) {
                        if (! Character.isWhitespace(r[i])) {
                            nonWhitespace = true;
                            break;
                        }
                    }
                    if (nonWhitespace) {
                        synchronized(mNewInput) {
                            // if in a command, switch buffers and notify that there is new STDERR output
                            if (inCommand.get()) {
                                useSecondErrBuffer.set(secondErrBuffer);
                                switchErrBuffers();
                                mErrorOccurred.set(true);
                                mErrorBufferRead.set(nRead);
                                mNewInput.set(true);
                                mNewInput.notifyAll();
                            }
                        }
                    }
                }
            } catch (IOException e) {
               // LoggerService.getLogger().errorOccurred(e);
            }
        }

        private void switchErrBuffers() {
            if (secondErrBuffer) {
                secondErrBuffer = false;
            } else {
                secondErrBuffer = true;
            }
        }
        private char[] privCurrentErrBuffer() {
            if (secondErrBuffer) {
                return mErrBuffers[1];
            } else {
                return mErrBuffers[0];
            }
        }
        private boolean secondErrBuffer = false;
    });

    private AtomicBoolean useSecondBuffer = new AtomicBoolean(false);
    private AtomicInteger mBufferRead = new AtomicInteger(0);
    private Thread stdOutRead = new Thread(new Runnable() {
        @Override
        public void run() {
            int nRead;
            try {
                while (true) {
                    nRead = mReader.read(privCurrentBuffer());
                    // TODO if in a command, switch buffers and notify that there is new STDOUT output
                    synchronized(mNewInput) {
                        if (inCommand.get()) {
                            useSecondBuffer.set(secondBuffer);
                            switchBuffers();
                            mBufferRead.set(nRead);
                            mNewInput.set(true);
                            mNewInput.notifyAll();
                        }
                    }
                }
            } catch (IOException e) {
                //LoggerService.getLogger().errorOccurred(e);
            }
        }

        private void switchBuffers() {
            if (secondBuffer) {
                secondBuffer = false;
            } else {
                secondBuffer = true;
            }
        }
        private char[] privCurrentBuffer() {
            if (secondBuffer) {
                return mBuffers[1];
            } else {
                return mBuffers[0];
            }
        }
        private boolean secondBuffer = false;
    });

	/*
	// Implementation 2.5: runs in 25.6 ms in the emulator, occasionally reads overlap and/or fail
	public void refresh(char[] command) throws IOException, FileNotFoundException, InterruptedException {
		if (accessLock.isHeldByCurrentThread()) {

			//Log.d(TAG, "refresh(\""+String.valueOf(command).trim()+"\")");

			// clear buffers
			//while (mErrorReader.ready()) {
			//	mErrorReader.read(mErrBuffer);
			//}
			//while (mReader.ready()) {
			//	mReader.read(mBuffer);
			//}


			mStdIn.write(command);
			mStdIn.flush();

			// Blocking read; waits until something is read from stdOut
			// This is bad practice, as errors will cause the whole app to freeze here
			//mReadCount = mReader.read(mBuffer);


			// Error-checking read; waits until timeout, or something is read from either stdOut or stdErr

			boolean somethingToRead = false;
			long start = SystemClock.uptimeMillis();
			long now = start;

			// sleep-wait loop, with timeout
			while ((!somethingToRead) && ((now - start) < READ_DELAY_MAX)) {
				if (mErrorReader.ready() || mReader.ready()) {
					somethingToRead = true;
				} else {
					Thread.sleep(READ_DELAY_STEP);
					now = SystemClock.uptimeMillis();
				}
			}
			if (mReader.ready()) {
				//Log.d(TAG, "Reading output");
				mReadCount = mReader.read(mBuffer);
			} else {
				mReadCount = 0;
			}
			if (mErrorReader.ready()) {
				//Log.d(TAG, "Has errors");
				//int bRead = mErrorReader.read(mErrBuffer);
				//Log.d(TAG, "Error: \""+String.copyValueOf(mErrBuffer, 0, bRead).trim()+"\"");

				mErrorReader.read(mErrBuffer);
				throw new FileNotFoundException("\""+String.copyValueOf(command).trim()+"\" failed");
			}

			mStart = 0;
			mEnd = 0;
		} else {
			throw new IOException("Lock not acquired by calling thread");
		}
	}
	*/



    // Version 1 used a Scanner to scan for a regular expression
    // its times were: 7 ms for hasNextLine(), 16 ms for nextLine()

    // Version 2: manual character detection
    // 0.372 ms hasNextLine(),  0.287 ms nextLine()
    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final char SPACE = ' ';
    private static final char TAB = '\t';

    private int mStart = 0;
    private int mEnd = 0;

    private boolean findNextLine() {
        mStart = mEnd;
        char c = mBuffer[mStart];
        while (((c == CR) || (c == LF)) && (mStart < mReadCount) ) {
            mStart++;
            c = mBuffer[mStart];
        }
        mEnd = mStart;
        if (mStart >= mReadCount) {
            return false;
        } else {
            try {
                c = mBuffer[mEnd];
                while (((c != CR) && (c != LF)) && (mEnd < mReadCount) ) {
                    mEnd++;
                    c = mBuffer[mEnd];
                }
                if (mEnd >= mReadCount) {
                    return false;
                } else {
                    return true;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }
    }

    public boolean hasNextLine() throws IOException, FileNotFoundException {
        if (accessLock.isHeldByCurrentThread()) {
            return findNextLine();
        } else {
            throw new IOException("Lock not acquired by calling thread");
        }
    }

    public String getLine() {
        return new String(mBuffer, mStart, (mEnd-mStart));
    }

    public SpaceSeparatedLine getSSLine() {
        return new SpaceSeparatedLine(mBuffer, mStart, mEnd);
    }



    public class SpaceSeparatedLine {
        private char[] sslBuffer;
        private int sslStart;
        private int sslEnd;


        public SpaceSeparatedLine(char[] buffer, int start, int end) {
            sslBuffer = buffer;
            sslStart = start;
            sslEnd = end;

            //Log.d(TAG, "SSL: \""+getLine()+"\"");

            resetTokens();
        }

        public String getLine() {
            return new String(sslBuffer, sslStart, (sslEnd-sslStart));
        }

        public String getRestOfLine() {
            return new String(sslBuffer, tokEnd, (sslEnd-tokEnd));
        }

        public SpaceSeparatedLine getRestOfLineAsSSL() {
            return new SpaceSeparatedLine(sslBuffer, tokEnd, sslEnd);
        }

        // Space-separated token parsing
        private int tokStart;
        private int tokEnd;
        private int tokCount;

        // reset the internal stuff for counting tokens
        private void resetTokens() {
            tokStart = sslStart;
            tokEnd = tokStart;
            tokCount = -1;
        }

        // find the next token in the buffer
        private boolean nextToken() {
            tokCount++;

            //Log.d(TAG, "nextToken() START: "+tokStart+", "+tokEnd+"  ("+sslStart+", "+sslEnd+")");

            tokStart = tokEnd;
            char c = sslBuffer[tokStart];
            while (((c == SPACE)||(c == TAB)) && (tokStart < sslEnd) ) {
                tokStart++;
                c = sslBuffer[tokStart];
            }

            tokEnd = tokStart;
            if (tokStart >= sslEnd) {
                return false;
            } else {
                c = sslBuffer[tokEnd];
                while (((c != SPACE)&&(c != TAB)) && (tokEnd < sslEnd) ) {
                    tokEnd++;
                    c = sslBuffer[tokEnd];
                }
                return true;
            }
        }

        // get token 'n', as a String
        // returns 'null' if: 1. n<0, 2. token 'n' not found
        public String getToken(int n) {
            if (n < 0) {
                return null;
            }

            if (tokCount > n) {
                resetTokens();
            }
            String ret;
            if (tokCount == n) {
                ret = new String(sslBuffer, tokStart, (tokEnd-tokStart));
            } else {
                if (tokCount > n) {
                  //  LoggerService.getLogger().errorOccurred(new AssertionError("Something very strange happened to the FileRepeatReader.SpaceSeparatedLine token parser..."));
                    ret = null;
                } else {
                    boolean found = nextToken();
                    while ((n > tokCount) && found) {
                        found = nextToken();
                    }
                    ret = new String(sslBuffer, tokStart, (tokEnd-tokStart));
                }
            }

            return ret;
        }
    }



}

