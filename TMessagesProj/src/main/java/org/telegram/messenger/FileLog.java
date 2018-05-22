package org.telegram.messenger;

import android.util.Log;
import android.widget.Toast;
import org.telegram.messenger.time.FastDateFormat;
import org.teleru.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;

public class FileLog
{
    private OutputStreamWriter streamWriter = null;
    private FastDateFormat dateFormat = null;
    private DispatchQueue logQueue = null;
    private File currentFile = null;
    private File networkFile = null;

    private static volatile FileLog Instance = null;

    public static FileLog getInstance()
    {
        FileLog localInstance = Instance;
        if (localInstance == null)
        {
            synchronized (FileLog.class)
            {
                localInstance = Instance;
                if (localInstance == null)
                {
                    Instance = localInstance = new FileLog();
                }
            }
        }
        return localInstance;
    }

    public FileLog()
    {
        if (!BuildVars.DEBUG_VERSION)
        {
            return;
        }
        dateFormat = FastDateFormat.getInstance("dd_MM_yyyy_HH_mm_ss", Locale.US);
        try
        {
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            if (sdCard == null)
            {
                return;
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            currentFile = new File(dir, dateFormat.format(System.currentTimeMillis()) + ".txt");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            logQueue = new DispatchQueue("logQueue");
            currentFile.createNewFile();
            FileOutputStream stream = new FileOutputStream(currentFile);
            streamWriter = new OutputStreamWriter(stream);
            streamWriter.write("-----start log " + dateFormat.format(System.currentTimeMillis()) + "-----\n");
            streamWriter.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String getNetworkLogPath()
    {
        if (!BuildVars.DEBUG_VERSION)
        {
            return "";
        }
        try
        {
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            if (sdCard == null)
            {
                return "";
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            getInstance().networkFile = new File(dir, getInstance().dateFormat.format(System.currentTimeMillis()) + "_net.txt");
            return getInstance().networkFile.getAbsolutePath();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        return "";
    }

    public static void e(final String message, final Throwable exception)
    {
        if (!BuildVars.DEBUG_VERSION)
        {
            return;
        }
        Log.e("tmessages", message, exception);
        if (getInstance().streamWriter != null)
        {
            getInstance().logQueue.postRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + message + "\n");
                        getInstance().streamWriter.write(exception.toString());
                        getInstance().streamWriter.flush();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void e(final String message)
    {
        if (!BuildVars.DEBUG_VERSION)
        {
            return;
        }
        Log.e("tmessages", message);
        if (getInstance().streamWriter != null)
        {
            getInstance().logQueue.postRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + message + "\n");
                        getInstance().streamWriter.flush();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void e(final Throwable e)
    {
        if (!BuildVars.DEBUG_VERSION)
        {
            return;
        }
        e.printStackTrace();
        if (getInstance().streamWriter != null)
        {
            getInstance().logQueue.postRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + e + "\n");
                        StackTraceElement[] stack = e.getStackTrace();
                        for (int a = 0; a < stack.length; a++)
                        {
                            getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + stack[a] + "\n");
                        }
                        getInstance().streamWriter.flush();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        else
        {
            e.printStackTrace();
        }
    }

    public static void d(final String message)
    {
        if (!BuildVars.DEBUG_VERSION)
        {
            return;
        }
        Log.d("tmessages", message);
        if (getInstance().streamWriter != null)
        {
            getInstance().logQueue.postRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " D/tmessages: " + message + "\n");
                        getInstance().streamWriter.flush();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void w(final String message)
    {
        if (!BuildVars.DEBUG_VERSION)
        {
            return;
        }
        Log.w("tmessages", message);
        if (getInstance().streamWriter != null)
        {
            getInstance().logQueue.postRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " W/tmessages: " + message + "\n");
                        getInstance().streamWriter.flush();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void cleanupLogs()
    {
        File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (sdCard == null)
        {
            return;
        }
        File dir = new File(sdCard.getAbsolutePath() + "/logs");
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (int a = 0; a < files.length; a++)
            {
                File file = files[a];
                if (getInstance().currentFile != null && file.getAbsolutePath().equals(getInstance().currentFile.getAbsolutePath()))
                {
                    continue;
                }
                if (getInstance().networkFile != null && file.getAbsolutePath().equals(getInstance().networkFile.getAbsolutePath()))
                {
                    continue;
                }
                file.delete();
            }
        }
        //plus
        final int i = files.length - 1;
        AndroidUtilities.runOnUIThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast toast = Toast.makeText(ApplicationLoader.applicationContext, i + " " + LocaleController.getString("ClearLogsMsg", R.string.ClearLogsMsg), Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

}
