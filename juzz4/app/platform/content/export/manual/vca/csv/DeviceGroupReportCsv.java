package platform.content.export.manual.vca.csv;

import org.joda.time.DateTime;
import platform.analytics.aggregation.AggregateOperator;
import platform.analytics.aggregation.AggregateType;
import platform.analytics.aggregation.AggregatedTicker;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.ReportCsvWriter;
import platform.content.export.VcaExportHelper;
import platform.devices.DeviceGroup;
import platform.devices.DeviceGroupType;
import platform.events.EventType;
import platform.time.UtcPeriod;
import play.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class DeviceGroupReportCsv implements ReportBuilder
{
    private final EventType eventType;
    private final UtcPeriod period;
    private final int tzOffsetMins;
    private final AggregateType aggregateType;
    private final List<DeviceGroup> deviceGroups;

    public DeviceGroupReportCsv(EventType eventType,
                                UtcPeriod period,
                                int offsetMins,
                                AggregateType aggregateType,
                                List<DeviceGroup> deviceGroups)
    {
        this.eventType = eventType;
        this.period = period;
        tzOffsetMins = offsetMins;
        this.aggregateType = aggregateType;
        this.deviceGroups = deviceGroups;
    }

    @Override
    public String getFilename()
    {
        return String.format("report_%s_%s_%s.%s",
                             aggregateType.name().toLowerCase(),
                             eventType.name().toLowerCase(),
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
        AggregateOperator aggOperator = eventType.equals(EventType.VCA_PEOPLE_COUNTING) ?
                                        AggregateOperator.CUSTOM :
                                        AggregateOperator.SUM;

        //aggregated tickers from each group
        TreeSet<AggregatedTicker> sortedSet = new TreeSet<>();
        for (DeviceGroup deviceGroup : deviceGroups)
        {
            if (deviceGroup.getCameraList().isEmpty())
            {
                continue;
            }

            List<AggregatedTicker> aggTickerList = VcaExportHelper.aggregateVcaReports(deviceGroup,
                                                                                       eventType,
                                                                                       period,
                                                                                       tzOffsetMins,
                                                                                       aggregateType,
                                                                                       aggOperator);
            sortedSet.addAll(aggTickerList);
        }

        //create csv
        try (ReportCsvWriter csvWriter = new ReportCsvWriter())
        {
            for (AggregatedTicker aggTicker : sortedSet)
            {
                //skip, if one of the device-group data is empty
                if (aggTicker.getBaseTicker() == null)
                    continue;
                    
                Map<String, Object> dataMap = new LinkedHashMap<>();

                addSpecialColumns(dataMap, aggTicker);
                dataMap.putAll(VcaExportHelper.asExportData(aggTicker.getBaseTicker()));
                removeColumnsIfNecessary(dataMap);

                //column row
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

    private void addSpecialColumns(Map<String, Object> dataMap, AggregatedTicker aggTicker)
    {
        dataMap.put("timestamp", aggregateType.timestamp(aggTicker.getGroupTime(), tzOffsetMins, period));
        dataMap.put("device-group", aggTicker.getDeviceGroup().getGroupName());

        DateTime dtLocal = new DateTime(aggTicker.getGroupTime()).plusMinutes(tzOffsetMins);
        switch (eventType)
        {
            case VCA_PROFILING:
                if (aggregateType.equals(AggregateType.DAY))
                {
                    dataMap.put("day", dtLocal.toString("E"));
                }
        }
    }

    private void removeColumnsIfNecessary(Map<String, Object> dataMap)
    {
        switch (eventType)
        {
            case VCA_PEOPLE_COUNTING:
                //display avg occupancy for label-based groups only
                if (!deviceGroups.get(0).getType().equals(DeviceGroupType.LABELS))
                {
                    dataMap.remove("avg-occupancy");
                }
        }
    }
}
