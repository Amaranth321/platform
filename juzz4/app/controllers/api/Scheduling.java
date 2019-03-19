package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import models.SchedulePreset;
import models.labels.LabelStore;
import platform.label.LabelType;
import platform.time.RecurrenceRule;
import play.mvc.With;

import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Scheduling
 * @sectiondesc APIs for CURD schedules
 */
@With(APIInterceptor.class)
public class Scheduling extends APIController
{

    /**
     * @param name            Name of schedule.e.g Intrusion detection schedule. Mandatory
     * @param recurrence-rule Json string of recurrence rule for schedule. Mandatory
     *                        i.e periods of day with start minutes and end minutes e.g
     *                        {
     *                        summary:name of preset,
     *                        periodsOfDays:
     *                        {
     *                        1:[{startMinutes:0,endMinutes:1439}],
     *                        2:[{startMinutes:0,endMinutes:1439}],
     *                        4:[{startMinutes:0,endMinutes:1439}],
     *                        5:[{startMinutes:0,endMinutes:1439}],
     *                        6:[{startMinutes:0,endMinutes:1439}]
     *                        }
     *                        }
     *
     * @servtitle Adds schedules preset to current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/addschedulepreset
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void addschedulepreset() throws ApiException
    {
        try
        {
            String name = readApiParameter("name", true);
            String ruleString = readApiParameter("recurrence-rule", true);
            Long currentBucketId = Long.parseLong(renderArgs.get("caller-bucket-id").toString());

            //check name duplicates
            SchedulePreset dbPreset = SchedulePreset.q()
                    .filter("bucketId", currentBucketId)
                    .filter("name", name)
                    .first();
            if (dbPreset != null)
            {
                throw new ApiException("same-name-preset-exists");
            }

            RecurrenceRule objRule = RecurrenceRule.parse(ruleString);
            if (objRule == null)
            {
                throw new ApiException("failed-to-parse-schedule");
            }

            Long bucketId = Long.parseLong(renderArgs.get("caller-bucket-id").toString());
            SchedulePreset newPreset = new SchedulePreset(bucketId, name, objRule);
            newPreset.save();

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param preset-id The id of preset to delete. Mandatory
     *
     * @servtitle Deletes schedule preset of current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/removeschedulepreset
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removeschedulepreset() throws ApiException
    {
        try
        {
            Map<String, String[]> input = params.all();
            String presetId = input.get("preset-id") == null ? "" : input.get("preset-id")[0];

            SchedulePreset target = SchedulePreset.findById(presetId);
            if (target == null)
            {
                throw new ApiException("cannot-remove-preset");
            }
            target.delete();
            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Returns list of schedule preset of current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/getschedulepresets
     * @responsejson {
     * "result": "ok",
     * "presets":[
     * {
     * "bucketId":1,
     * "name":"Every Day",
     * "recurrenceRule":
     * {
     * "summary":"Every Day"
     * },
     * "_id":"5379c38e8da1d6e3e8111f14",
     * "_created":1400488846866,
     * "_modified":1400488846866
     * },
     * {
     * "bucketId":1,
     * "name":"Nischal",
     * "recurrenceRule":
     * {
     * "summary":"testing schedule",
     * "periodsOfDays":
     * {
     * "1":[{"startMinutes":0,"endMinutes":1439}],
     * "2":[{"startMinutes":0,"endMinutes":1439}],
     * "4":[{"startMinutes":0,"endMinutes":1439}],
     * "5":[{"startMinutes":0,"endMinutes":1439}],
     * "6":[{"startMinutes":0,"endMinutes":1439}]}},
     * "_id":"53ad4b3960ed8d245ece7992",
     * "_created":1403865913406,
     * "_modified":1403865913406
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getschedulepresets() throws ApiException
    {
        try
        {
            Long bucketId = Long.parseLong(renderArgs.get("caller-bucket-id").toString());
            List<SchedulePreset> schedules = SchedulePreset.find("bucketId", bucketId).asList();

//            Iterable<LabelStore> stores = LabelType.STORE.getQuery().filter("bucketId", bucketId).fetch();
//            for (LabelStore label : stores)
//            {
//                String presetName = String.format("(%s) %s", label.getType(), label.getLabelName());
//                schedules.add(new SchedulePreset(
//                        label.getBucketId(),
//                        presetName,
//                        label.getSchedule().toRecurrenceRule(presetName)));
//            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("presets", schedules);
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

}
