package com.obsez.android.lib.filechooser.internals;

import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.InputFilter;
import android.text.Spanned;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

/**
 * Created by coco on 6/7/15.
 */
public class FileUtil {


    public static String getExtension(File file) {
        if (file == null) {
            return null;
        }

        int dot = file.getName().lastIndexOf(".");
        if (dot >= 0) {
            return file.getName().substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    public static String getExtensionWithoutDot(File file) {
        String ext = getExtension(file);
        if (ext.length() == 0) {
            return ext;
        }
        return ext.substring(1);
    }

    public static String getReadableFileSize(long size) {
        final int BYTES_IN_KILOBYTES = 1024;
        final DecimalFormat dec = new DecimalFormat("###.#");
        final String KILOBYTES = " KB";
        final String MEGABYTES = " MB";
        final String GIGABYTES = " GB";
        float fileSize = 0;
        String suffix = KILOBYTES;

        if (size > BYTES_IN_KILOBYTES) {
            fileSize = size / BYTES_IN_KILOBYTES;
            if (fileSize > BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES;
                if (fileSize > BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES;
                    suffix = GIGABYTES;
                } else {
                    suffix = MEGABYTES;
                }
            }
        }
        return String.valueOf(dec.format(fileSize) + suffix);
    }

    public static String getStoragePath(Context context, boolean isRemovable) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovableMtd = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(storageManager);
            final int length = Array.getLength(result);
            //Timber.d("---length--" + length);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                //Timber.d("---Object--" + storageVolumeElement + "i==" + i);
                String path = (String) getPath.invoke(storageVolumeElement);
                //Timber.d("---path_total--" + path);
                boolean removable = (Boolean) isRemovableMtd.invoke(storageVolumeElement);
                if (isRemovable == removable) {
                    //Timber.d("---path--" + path);
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long readSDCard(Context context, Boolean isRemovable) {
        return readSDCard(context, isRemovable, false);
    }

    public static long readSDCard(Context context, Boolean isRemovable, Boolean freeOrTotal) {
        DecimalFormat df = new DecimalFormat("0.00");
        if (getStoragePath(context, isRemovable) != null) {
            StatFs sf = new StatFs(getStoragePath(context, isRemovable));
            long blockSize;
            long blockCount;
            long availCount;
            if (Build.VERSION.SDK_INT > 18) {
                blockSize = sf.getBlockSizeLong(); //文件存储时每一个存储块的大小为4KB
                blockCount = sf.getBlockCountLong();//存储区域的存储块的总个数
                availCount = sf.getFreeBlocksLong();//存储区域中可用的存储块的个数（剩余的存储大小）
            } else {
                blockSize = sf.getBlockSize();
                blockCount = sf.getBlockCount();
                availCount = sf.getFreeBlocks();
            }
            //Log.d("sss", "总的存储空间大小:" + blockSize * blockCount / 1073741824 + "GB" + ",剩余空间:"
            //    + availCount * blockSize / 1073741824 + "GB"
            //    + "--存储块的总个数--" + blockCount + "--一个存储块的大小--" + blockSize / 1024 + "KB");
            //return df.format((freeOrTotal ? availCount : blockCount) * blockSize / 1073741824.0);
            return (long) (freeOrTotal ? availCount : blockCount) * blockSize;
        }
        //return "-1";
        return -1;
    }


    public static class NewFolderFilter implements InputFilter {
        private final int maxLength;
        private final String regex;

        public NewFolderFilter() {
            this.maxLength = 255;
            this.regex = "[<>|\\\\:&;#\n\r\t?*~\0-\37]";
        }

        public NewFolderFilter(int max) {
            this.maxLength = max;
            this.regex = "[<>|\\\\:&;#\n\r\t?*~\0-\37]";
        }

        public NewFolderFilter(String regex) {
            this.maxLength = 255;
            this.regex = regex;
        }

        public NewFolderFilter(int max, String regex) {
            this.maxLength = max;
            this.regex = regex;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String filtered;

            int keep = maxLength - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                filtered = "";
            } else if (keep >= end - start) {
                filtered = source.subSequence(start, end).toString();
            } else {
                keep += start;
                if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                    --keep;
                    if (keep == start) {
                        return "";
                    }
                }
                filtered = source.subSequence(start, keep).toString();
            }

            return filtered.replaceAll(regex, "");
        }
    }
}
