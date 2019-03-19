package platform.content.export.manual.vca.csv;

import lib.util.exceptions.ApiException;
import models.Analytics.TickerReport;
import models.MongoDevice;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.Environment;
import platform.analytics.VcaDataRequest;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.ReportCsvWriter;
import platform.content.export.VcaExportHelper;
import platform.devices.DeviceChannelPair;
import play.Logger;
import play.i18n.Messages;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Aye Maung
 * @author Keith
 * @since v4.4
 */
public class VcaDataLogCsv implements ReportBuilder
{
    private final VcaDataRequest dataRequest;
    private final int tzOffsetMins;

    public VcaDataLogCsv(VcaDataRequest dataRequest, int tzOffsetMins) throws ApiException
    {
        if (dataRequest.getQuery().countAll() == 0)
        {
            throw new ApiException("no-data-logs");
        }

        this.dataRequest = dataRequest;
        this.tzOffsetMins = tzOffsetMins;
    }

    @Override
    public String getFilename()
    {
        return String.format("data_logs_%s_%s.%s",
                             dataRequest.getEventType().name().toLowerCase(),
                             VcaExportHelper.getGeneratedTime(tzOffsetMins),
                             getFileFormat().getExtension());
    }

    @Override
    public FileFormat getFileFormat()
    {
        return FileFormat.CSV;
    }

    @Override
    public InputStream generate()
    {
        try (ReportCsvWriter csvWriter = new ReportCsvWriter())
        {
            //get device name
            Map<String, DeviceName> deviceNameMap = getDeviceName();
            
            //query data and write
            Iterable<TickerReport> iterable = dataRequest.getQuery().fetch();
            for (TickerReport tickerReport : iterable)
            {
                Map<String, Object> dataMap = new LinkedHashMap<>();
                dataMap.put("timestamp", VcaExportHelper.getLocalTimeStamp(tickerReport.time, tzOffsetMins));
                
                DeviceName deviceName = deviceNameMap.get(tickerReport.deviceId + "-" + tickerReport.channelId);
                if (deviceName == null)
                    deviceName = deviceNameMap.get(tickerReport.deviceId);
                
                dataMap.put("device-name", deviceName.deviceName);
                
                if (Environment.getInstance().onCloud())
                {
                    dataMap.put("camera-name", deviceName.channelName);
                }

                dataMap.putAll(VcaExportHelper.asExportData(tickerReport));

                //columns row
                if (csvWriter.lineCount() == 0)
                {
                    csvWriter.writeRow(new ArrayList<>(dataMap.keySet()));
                }

                csvWriter.writeRow(new ArrayList<>(dataMap.values()));
            }

            return new FileInputStream(csvWriter.getCsvFile());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
    
    private Map<String, DeviceName> getDeviceName()
    {
        Map<String, DeviceName> deviceNameMap = new LinkedHashMap<String, DeviceName>();
        for (DeviceChannelPair camera : dataRequest.getCameraList())
        {
            MongoDevice device = MongoDevice.getByCoreId(camera.getCoreDeviceId());

            if(device != null)
            {
                deviceNameMap.put(camera.getCoreDeviceId(), new DeviceName(device.getName(), Messages.get("deleted-db-entry")));
                
                if (device.isKaiNode())
                {
                    NodeObject node = NodeObject.findByPlatformId(device.getDeviceId());
                    for (NodeCamera c : node.getCameras())
                    {
                        deviceNameMap.put(camera.getCoreDeviceId() + "-" + c.nodeCoreDeviceId, new DeviceName(device.getName(), c.name));
                    }
                }
            }
            else
            {
                deviceNameMap.put(camera.getCoreDeviceId(),new DeviceName(Messages.get("deleted-db-entry"), Messages.get("deleted-db-entry")));
            }
        }
        return deviceNameMap;
    }
    
    class DeviceName 
    {
        String deviceName = "";
        String channelName = "";
        
        public DeviceName (String deviceName, String channelName)
        {
            this.deviceName = deviceName;
            this.channelName = channelName;
        }
    }
}
