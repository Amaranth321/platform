/*
 * SysInfoUtil.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import lib.util.CmdExecutor;
import lib.util.Util;
import platform.Environment;
import platform.config.readers.ConfigsNode;
import play.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;

/**
 * Utility methods for System Information.
 *
 * @author Kapil Pendse
 * @author Aye Maung
 */
public final class SysInfoUtil
{
    private static TimeZone OS_TIME_ZONE = null;
    private static String MAC_ADDRESS = null;

    /**
     * This class is not intended to be instantiated.
     */
    private SysInfoUtil()
    {
    }

    private static void readTimeZoneFromCmdOutput()
    {
        try
        {
            if (Environment.getInstance().onWindows())
            {
                List<String> cmdParams = new ArrayList<>();
                cmdParams.add("systeminfo");
                List<String> outputs = CmdExecutor.readTillProcessEnds(cmdParams, false);
                if (outputs.isEmpty())
                {
                    Logger.error(Util.whichFn() + "no cmd output");
                    return;
                }

                for (String output : outputs)
                {
                    if (output.contains("Time Zone"))
                    {
                        int start = output.indexOf("UTC") + 3;
                        int end = start + 6;
                        String tzId = "GMT" + output.substring(start, end).replace(":", "");
                        OS_TIME_ZONE = TimeZone.getTimeZone(tzId);
                    }
                }

            }
            else
            {
                List<String> cmdParams = new ArrayList<>();
                cmdParams.add("cat");
                cmdParams.add("/etc/timezone");
                List<String> outputs = CmdExecutor.readTillProcessEnds(cmdParams, false);
                if (outputs.isEmpty())
                {
                    Logger.error(Util.whichFn() + "no cmd output");
                    return;
                }

                OS_TIME_ZONE = TimeZone.getTimeZone(outputs.get(0));
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    /**
     * Get MAC address of current hardware
     *
     * @return the MAC format should be address XX:XX:XX:XX:XX:XX
     */
    private static String readMacAddress()
    {
        String iface = ConfigsNode.getInstance().getNetworkInterface();
        StringBuilder sb = new StringBuilder();
        try
        {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

            loop:
            while (ifaces.hasMoreElements())
            {
                NetworkInterface nic = ifaces.nextElement();
                if (iface != null && !nic.getName().equals(iface))
                {
                    continue;
                }

                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements())
                {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.getHostAddress().startsWith("127.0"))
                    {
                        byte[] mac = nic.getHardwareAddress();
                        for (int i = 0; i < mac.length; i++)
                        {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                        }
                        break loop;
                    }
                }
            }
        }
        catch (SocketException e)
        {
            Logger.error("error reading mac address: %s", e.getMessage());
        }

        return sb.toString().toLowerCase();
    }

    /**
     * Get value of the specified CPU property from the /proc/cpuinfo file. Works
     * only on Linux systems that have this file. Does not work on Windows/MacOS.
     *
     * @param property The name of property to read, e.g. "model name"
     *
     * @return The value string of the specified property, e.g. "Inter(R) Core(TM) i5-3317U CPU @ 1.70GHz"
     */
    public static String getCpuInfo(String property)
    {
        String cpuinfo = "/proc/cpuinfo";
        final String LINE_SEPARATOR = "line.separator";
        final String COLON = ":";
        String[] cpuinfoSplit;

        try
        {
            BufferedReader input = new BufferedReader(new FileReader(new File(cpuinfo)));
            StringBuilder fileContent = new StringBuilder();
            String oneLine;
            try
            {
                while ((oneLine = input.readLine()) != null)
                {
//                	Logger.info("cpuinfo: %s", oneLine);
                    fileContent.append(oneLine);
                    fileContent.append(System.getProperty(LINE_SEPARATOR));
                }
            }
            catch (IOException ex)
            {
                Logger.error(ex.getMessage());
                Logger.error(lib.util.Util.getStackTraceString(ex));
            }
            input.close();

            cpuinfoSplit = fileContent.toString().split(System.getProperty(LINE_SEPARATOR));
            for (String line : cpuinfoSplit)
            {
                if (line.contains(property))
                {
                    return line.split(COLON)[1];
                }
            }
        }
        catch (FileNotFoundException ex)
        {
            Logger.error(ex.getMessage());
            Logger.error(lib.util.Util.getStackTraceString(ex));
        }
        catch (IOException ex)
        {
            Logger.error(ex.getMessage());
            Logger.error(lib.util.Util.getStackTraceString(ex));
        }
        return "";
    }

    public static String runCommand(int lineLimit, String... params)
    {
        StringBuilder sb = new StringBuilder();
        ProcessBuilder builder = new ProcessBuilder(params);
        Process p = null;
        try
        {
            p = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
                if (lineLimit > 0)
                {
                    lines++;
                    if (lines >= lineLimit)
                    {
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
//    		Logger.error(e, "run '%s'", StringCollectionUtil.join(builder.command(), " "));
        }
        finally
        {
            if (p != null)
            {
                p.destroy();
            }
        }
        return sb.toString();
    }

    public static String getMACAddressOnWin()
    {
        try
        {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] macBytes = network.getHardwareAddress();
            String macAddress = "";
            for (int i = 0; i < macBytes.length; i++)
            {
                macAddress += String.format("%02X%s", macBytes[i], (i != (macBytes.length - 1)) ? ":" : "");
            }

            return macAddress;

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    /**
     * @param refresh true to re-read time zone. false to get last-read value
     */
    public static TimeZone getOSTimeZone(boolean refresh)
    {
        if (OS_TIME_ZONE == null || refresh)
        {
            readTimeZoneFromCmdOutput();
        }

        return OS_TIME_ZONE;
    }

    /**
     * @param refresh true to re-read the mac address. false to get last-read value
     */
    public static String getMacAddress(boolean refresh)
    {
        if (MAC_ADDRESS == null || refresh)
        {
            MAC_ADDRESS = readMacAddress();
        }

        return MAC_ADDRESS;
    }
}

