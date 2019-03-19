/**
 * FileUtil.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */
package com.kaisquare.util;

import org.apache.commons.lang.StringUtils;

import play.Logger;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class FileUtil {

    public static String readFile(File file) {
        StringBuilder sb = new StringBuilder();
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = new byte[8192];
            int read;

            while ((read = bis.read(buf)) > 0) {
                sb.append(new String(buf, 0, read));
            }
        } catch (Exception e) {
            Logger.error(lib.util.Util.getStackTraceString(e));
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
        }

        return sb.toString();
    }

    public static void copyFile(File src, File dst) {
        if (src.exists()) {
            FileChannel srcChannel = null;
            FileChannel dstChannel = null;
            try {
                if (!dst.exists())
                    dst.createNewFile();

                srcChannel = new FileInputStream(src).getChannel();
                dstChannel = new FileOutputStream(dst).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            } catch (Exception e) {
                Logger.error(lib.util.Util.getStackTraceString(e));
            } finally {
                if (srcChannel != null) {
                    try {
                        srcChannel.close();
                    } catch (IOException e) {
                    }
                }
                if (dstChannel != null) {
                    try {
                        dstChannel.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public static Date getCreatedDate(File file) {
        return getCreatedDate(Paths.get(file.getAbsolutePath()));
    }

    public static Date getCreatedDate(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return new Date(attrs.creationTime().toMillis());
        } catch (IOException e) {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
        return null;
    }

    public static String getConvertedUNIXpath(String path) {
        String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        char[] alphachararry = alpha.toCharArray();
        boolean flag = false;
        char[] check = {' ', '(', ')', '{', '}', '[', ']', '!', '^', '$', '~', '@', '&', '='};
        List<Character> checklist = new ArrayList<>();
        for (char checkc : check) {
            checklist.add(checkc);
        }
        String temp = "";
        for (char c : path.toCharArray()) {
            if (c != '\\') {
                for (char al : alphachararry) {
                    if (c == al) {
                        temp += c;
                        flag = true;
                    }
                }
                if (!flag) {
                    if (checklist.contains(c)) {
                        temp += '\\';
                        temp += c;
                    } else {
                        temp += c;
                    }
                }
            } else {
                temp += '/';
            }
            flag = false;
        }
        path = temp;
        return path;
    }

    public static List<String> readTextFile(String textFile) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(textFile));
        List<String> lines = new ArrayList<>();
        try {
            for (String line; (line = br.readLine()) != null; ) {
                lines.add(line);
            }
        } catch (IOException e) {
            Logger.error(e, "");
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Logger.error(e, "");
            }
        }

        return lines;
    }

    public static String readTextFileAsString(String textFile) throws FileNotFoundException {
        List<String> lines = readTextFile(textFile);
        return StringUtils.join(lines, "\n");
    }

	public static void writeTextFile(File file, String text) {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file));
			byte[] b = text.getBytes();
			bos.write(b);
		} catch (IOException e) {
			Logger.error(e, "");
		} finally {
			if (bos != null)
			{
				try {
					bos.flush();
					bos.close();
				} catch (IOException e) {}
			}
		}
	}

    public static void close(InputStream inputStream)
    {
        try
        {
            inputStream.close();
        }
        catch (IOException e)
        {
        }
    }

    public static void close(OutputStream outputStream)
    {
        try
        {
            outputStream.close();
        }
        catch (IOException e)
        {
        }
    }
}
