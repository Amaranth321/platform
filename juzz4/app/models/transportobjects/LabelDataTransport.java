package models.transportobjects;

import models.Analytics.TickerReport;
import models.labels.DeviceLabel;
import platform.events.EventType;
import platform.label.LabelType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kapil Pendse
 * @since v4.5
 */
public class LabelDataTransport
{
    public final String labelName;
    public final LabelType labelType;
    public final List<TickerWithType> records;

    public LabelDataTransport(String labelName)
    {
        this.labelName = labelName;
        this.labelType = LabelType.OTHERS;
        this.records = new ArrayList<>();
    }

    public LabelDataTransport(DeviceLabel label)
    {
        this.labelName = label.getLabelName();
        this.labelType = label.getType();
        this.records = new ArrayList<>();
    }

    public class TickerWithType
    {
        public EventType type;
        public TickerReport record;

        public TickerWithType(EventType t, TickerReport tr)
        {
            type = t;
            record = tr;
        }
    }
}
