package models;

import com.google.code.morphia.annotations.Entity;
import platform.time.OperatingSchedule;
import platform.time.RecurrenceRule;
import play.modules.morphia.Model;

/**
 * Author:  Aye Maung
 */
@Entity
public class SchedulePreset extends Model {
    public final long bucketId;
    public final String name;
    public final RecurrenceRule recurrenceRule;

    public SchedulePreset(long bucketId, String name, RecurrenceRule recurrenceRule) {
        this.bucketId = bucketId;
        this.name = name;
        this.recurrenceRule = recurrenceRule;
    }
}

