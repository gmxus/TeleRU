package org.teleru.utils;

import org.telegram.messenger.ApplicationLoader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("WeakerAccess")
public class FileUtils
{
    public static class Zipper
    {
        private Thread thread;
        private boolean isRunning = false;
        private boolean isDisposed = false;

        public interface IZipperListener
        {
            void onProgress(Zipper zipper, int percent);
            void onComplete(Zipper zipper, String filename);
            void onError(Zipper zipper, Exception ex);
        }

        public final String path;
        public final String filename;
        private int totalFiles;
        private int currentProcessed;
        private int currentPercent;
        private ZipOutputStream zipOutputStream;
        private IZipperListener listener;


        public Zipper(String path, String filename, IZipperListener listener)
        {
            this.path = path;
            this.filename = filename;
            this.listener = listener;
        }

        public void compress()
        {
            if (isRunning || isDisposed)
                return;

            isRunning = true;
            thread = new Thread(() ->
            {
                if (!isRunning || Thread.interrupted())
                    return;

                try
                {
                    File sourcePath = new File(path);
                    totalFiles = getNumberOfFiles(sourcePath, 0);
                    FileOutputStream outputStream = new FileOutputStream(filename);
                    zipOutputStream = new ZipOutputStream(new BufferedOutputStream(outputStream));
                    compressInternal(sourcePath, zipOutputStream);
                    if (isRunning && !isDisposed)
                    {
                        cancel();
                        ApplicationLoader.applicationHandler.post(()-> listener.onComplete(this, filename));
                    }
                }
                catch (Exception ex)
                {
                    boolean isAlive = isRunning && !isDisposed;
                    cancel();
                    if (isAlive)
                        ApplicationLoader.applicationHandler.post(() -> listener.onError(this, ex));
                }
            });
            thread.start();
        }

        private void compressInternal(File path, ZipOutputStream zipOutputStream) throws Exception
        {
            if (!isRunning)
                return;

            if (path.isDirectory())
            {
                File[] files = path.listFiles();
                if (files != null && files.length > 0)
                {
                    for (File file : files)
                        compressInternal(file, zipOutputStream);
                }
            }
            else
            {
                String filename = path.getPath().substring(this.path.length() + 1);
                zipOutputStream.putNextEntry(new ZipEntry(filename));
                FileInputStream fileInputStream = new FileInputStream(path);
                byte[] buffer = new byte[2048];
                int read;
                while ((read = fileInputStream.read(buffer, 0, buffer.length)) > 0)
                    zipOutputStream.write(buffer, 0, read);

                zipOutputStream.closeEntry();
                fileInputStream.close();
                currentProcessed++;
                int percent = (currentProcessed / totalFiles) * 100;
                if (percent > currentPercent)
                {
                    currentPercent = percent;
                    ApplicationLoader.applicationHandler.post(() -> listener.onProgress(this, currentPercent));
                }
            }
        }

        public void cancel()
        {
            isDisposed = true;
            isRunning = false;
            if (thread != null)
            {
                try
                {
                    thread.interrupt();
                }
                catch (Exception ignored) {}
                thread = null;
            }

            if (zipOutputStream != null)
            {
                try
                {
                    zipOutputStream.close();
                }
                catch (Exception ignored) {}
                zipOutputStream = null;
            }
        }
    }

    public static void deleteRecursive(File fileOrDirectory, File parent) throws Exception
    {
        if (fileOrDirectory.isDirectory())
        {
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child, parent);
        }

        if (parent == null || !parent.getPath().equals(fileOrDirectory.getPath()))
        {
            if (!fileOrDirectory.delete())
                throw new Exception("can't delete file");
        }
    }

    public static String getFixedFilename(String path, String name, String extension)
    {
        int num = 0;
        String newName;
        File file;

        do
        {
            if (num > 0)
                newName = String.format("%s(%s)", name, num);
            else
                newName = name;

            if (!StringUtils.isNullOrEmpty(extension))
                file = new File(path, String.format("%s.%s", newName, extension));
            else
                file = new File(path, newName);

            num++;
        }
        while (file.exists());
        return file.getPath();
    }

    public static int getNumberOfFiles(File path, int count)
    {
        File[] list = path.listFiles();
        if (list != null && list.length > 0)
        {
            for (File file : list)
            {
                if (file.isDirectory())
                    getNumberOfFiles(file, count);
                else
                    count++;
            }
        }

        return count;
    }
}
