package core;

import com.kaisquare.core.thrift.*;
import com.kaisquare.util.ThriftUtil;
import lib.util.Util;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.joda.time.DateTime;
import platform.config.readers.ConfigsServers;
import platform.coreengine.CoreUtils;
import platform.coreengine.MediaType;
import platform.coreengine.RecordedLocalFile;
import platform.coreengine.UploadedRecordingFile;
import platform.devices.DeviceChannelPair;
import platform.time.UtcPeriod;
import play.Logger;

import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StreamControlClient
{
    private static StreamControlClient instance = null;

    private ThriftUtil.Client<StreamControlService.Iface> streamControlServiceClient;

    private StreamControlClient()
    {
        try
        {
            initClient();
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }

    public static StreamControlClient getInstance()
    {
        if (instance == null)
        {
            instance = new StreamControlClient();
        }

        return instance;
    }

    private void initClient()
    {
        InetSocketAddress serverAddress = ConfigsServers.getInstance().coreStreamControlServer();
        Logger.info("Initializing StreamControlClient (%s)", serverAddress);
        try
        {
            //retry 3 times, with an interval of 1000 milliseconds between each try
            this.streamControlServiceClient = ThriftUtil.newServiceClient(StreamControlService.Iface.class,
                                                                          StreamControlService.Client.class,
                                                                          serverAddress.getHostName(),
                                                                          serverAddress.getPort(),
                                                                          ThriftUtil.DEFAULT_TIMEOUT_MILLIS);
        }
        catch (TTransportException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }

    public List<String> beginStreamSession(String sessionKey,
                                           int ttl,
                                           String streamType,
                                           List<String> clientIpAddresses,
                                           String deviceId,
                                           String channelId,
                                           DateTime startTimestamp,
                                           DateTime endTimestamp) throws CoreException
    {
        StreamControlService.Iface streamControlServiceIface = this.streamControlServiceClient.getIface();
        List<String> result;
        SimpleDateFormat ddMMyyyyHHmmss = new SimpleDateFormat("ddMMyyyyHHmmss");
        String from, to;

        try
        {
            if (startTimestamp == null && endTimestamp == null)
            {
                //live video requested
                from = "";
                to = "";
            }
            else
            {
                //playback of recorded video requested
                from = ddMMyyyyHHmmss.format(startTimestamp.toDate());
                to = ddMMyyyyHHmmss.format(endTimestamp.toDate());
            }
            result = streamControlServiceIface.beginStreamSession(sessionKey,
                                                                  ttl,
                                                                  streamType,
                                                                  clientIpAddresses,
                                                                  deviceId,
                                                                  channelId,
                                                                  from,
                                                                  to
            );
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return null;
        }
    }

    public boolean keepStreamSessionAlive(String sessionKey,
                                          int ttl,
                                          List<String> clientIpAddresses) throws CoreException
    {
        StreamControlService.Iface streamControlServiceIface = this.streamControlServiceClient.getIface();
        boolean result;
        try
        {
            result = streamControlServiceIface.keepStreamSessionAlive(sessionKey, ttl, clientIpAddresses);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean endStreamSession(String sessionKey) throws CoreException
    {
        StreamControlService.Iface streamControlServiceIface = this.streamControlServiceClient.getIface();
        boolean result;
        try
        {
            result = streamControlServiceIface.endStreamSession(sessionKey);
            return result;
        }
        catch (TException e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }

    public List<StorageInfo> getCoreStorageStatus()
    {
        StreamControlService.Iface streamService = this.streamControlServiceClient.getIface();
        try
        {
            List<StorageInfo> infoList = streamService.getStorageStatus();
            if (infoList == null)
            {
                return new ArrayList<>();
            }
            return infoList;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return new ArrayList<>();
        }
    }

    public List<RecordedLocalFile> getRecordedLocalFiles(String deviceId,
                                                         String channelId,
                                                         UtcPeriod period)
    {
        StreamControlService.Iface streamService = this.streamControlServiceClient.getIface();
        try
        {
            List<RecordedMediaInfo> mediaInfoList = streamService.getRecordedMediaList(
                    deviceId,
                    channelId,
                    MediaType.VIDEO.toString(),
                    CoreUtils.convertToTimestamp(period.getFromMillis()),
                    CoreUtils.convertToTimestamp(period.getToMillis())
            );

            if (mediaInfoList == null)
            {
                throw new NullPointerException();
            }

            List<RecordedLocalFile> fileInfoList = new ArrayList<>();
            for (RecordedMediaInfo mediaInfo : mediaInfoList)
            {
                try
                {
                    fileInfoList.add(new RecordedLocalFile(mediaInfo));
                }
                catch (FileNotFoundException fnf)
                {
                    Logger.error("File not found (%s)", mediaInfo);
                }
            }

            return fileInfoList;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return new ArrayList<>();
        }
    }

    public List<UploadedRecordingFile> getCloudStreamFileList(DeviceChannelPair camera, UtcPeriod period)
    {
        StreamControlService.Iface streamService = this.streamControlServiceClient.getIface();
        try
        {
            String coreChannelId = CoreUtils.getCameraCoreChannelId(camera);
            List<StreamFileDetails> fileDetailsList = streamService.getStreamFileDetails(
                    "", //unused
                    camera.getCoreDeviceId(),
                    coreChannelId,
                    MediaType.VIDEO.toString(),
                    CoreUtils.convertToTimestamp(period.getFromMillis()),
                    CoreUtils.convertToTimestamp(period.getToMillis())
            );

            if (fileDetailsList == null)
            {
                throw new NullPointerException();
            }

            List<UploadedRecordingFile> fileInfoList = new ArrayList<>();
            for (StreamFileDetails details : fileDetailsList)
            {
                if (Util.isNullOrEmpty(details.getStatus()))
                {
                    Logger.error(Util.whichFn() + details);
                }
                fileInfoList.add(new UploadedRecordingFile(details));
            }

            return fileInfoList;

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return new ArrayList<>();
        }
    }

    /**
     * Requests the recordings from the given period to be uploaded to Cloud
     *
     * @param camera
     * @param period
     */
    public boolean requestRecordingUpload(DeviceChannelPair camera, UtcPeriod period)
    {
        StreamControlService.Iface streamService = this.streamControlServiceClient.getIface();
        try
        {
            String sessionKey = UUID.randomUUID().toString();
            String coreChannelId = CoreUtils.getCameraCoreChannelId(camera);

            boolean result = streamService.requestStreamForPlayback(
                    sessionKey,
                    camera.getCoreDeviceId(),
                    coreChannelId,
                    MediaType.VIDEO.toString(),
                    CoreUtils.convertToTimestamp(period.getFromMillis()),
                    CoreUtils.convertToTimestamp(period.getToMillis())
            );

            return result;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    /**
     * @param camera
     * @param toCancelList List of start timestamps of recording files to cancel
     */
    public boolean deleteCloudRecordings(DeviceChannelPair camera, List<String> toCancelList)
    {
        StreamControlService.Iface streamService = this.streamControlServiceClient.getIface();
        try
        {
            String coreChannelId = CoreUtils.getCameraCoreChannelId(camera);
            return streamService.cancelStreamForPlayback(
                    camera.getCoreDeviceId(),
                    coreChannelId,
                    MediaType.VIDEO.toString(),
                    toCancelList
            );
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public boolean resetRecordingFiles(DeviceChannelPair camera)
    {
        StreamControlService.Iface streamService = this.streamControlServiceClient.getIface();
        try
        {
            String coreChannelId = CoreUtils.getCameraCoreChannelId(camera);
            return streamService.resetMediaFiles(Long.parseLong(camera.getCoreDeviceId()),
                                                 Integer.parseInt(coreChannelId));
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

}
