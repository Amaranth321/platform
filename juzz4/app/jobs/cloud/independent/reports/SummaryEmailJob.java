package jobs.cloud.independent.reports;

import com.google.code.morphia.query.Query;
import jobs.cloud.CloudCronJob;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.*;
import models.cloud.UIConfigurableCloudSettings;
import models.labels.DeviceLabel;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.AuditManager;
import platform.DeviceManager;
import platform.analytics.VcaFeature;
import platform.content.delivery.Deliverable;
import platform.content.delivery.DeliveryManager;
import platform.content.delivery.DeliveryMethod;
import platform.content.email.EmailItem;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.label.LabelManager;
import platform.reports.AnalyticsReport;
import platform.reports.AudienceProfilingAnalyticsReport.AudienceProfilingReport;
import platform.reports.EventReport;
import platform.reports.PeopleCountingAnalyticsReport.PeopleCountingReport;
import play.Logger;
import play.Play;
import play.i18n.Messages;
import play.jobs.On;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Periodic emails with summary reports.
 *
 * @author edward.wu@kaisquare.com.tw
 */
@On("cron.ReportSummary.weekly")
public class SummaryEmailJob extends CloudCronJob
{
    @Override
    public void doJob()
    {
        try
        {
            if (!UIConfigurableCloudSettings.server().featureControls().allowWeeklySummaryEmails)
            {
                return;
            }

            Logger.info("Generate weekly summary ::");
            int start_minus = 8;
            int end_minus = 2;
            Date start = DateTime.now()
                    .minusDays(start_minus)
                    .withTimeAtStartOfDay()
                    .withZone(DateTimeZone.UTC)
                    .toDate();
            Date end = DateTime.now()
                    .minusDays(end_minus)
                    .withTime(23, 59, 59, 999)
                    .withZone(DateTimeZone.UTC)
                    .toDate();
            StringBuilder timeRange = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            timeRange.append(sdf.format(start)).append(" - ").append(sdf.format(end));
            timeRange.append(" UTC");
            Logger.info("WeeklySummaryJob: start = %s , end = %s", start, end);

            List<MongoBucket> buckets = MongoBucket.q().fetchAll();
            for (MongoBucket bucket : buckets)
            {
                DeviceManager dm = DeviceManager.getInstance();
                List<MongoDevice> bucketDevices = dm.getDevicesOfBucket(bucket.getBucketId());
                if (bucket.isSuspended() || bucket.isDeleted() || bucketDevices.isEmpty())
                {
                    continue;
                }

                List<DeviceChannelPair> cameraList = new ArrayList<>();
                for (MongoDevice device : bucketDevices)
                {
                    cameraList.add(new DeviceChannelPair(device.getCoreDeviceId(), ""));
                }

                List<DeviceLabel> bucketLabels = LabelManager.getInstance()
                        .getBucketLabels(Long.parseLong(bucket.getBucketId()));

                AnalyticsReport peoAnalyticReport = EventReport.getReport(EventType.VCA_PEOPLE_COUNTING);
                if (peoAnalyticReport == null)
                {
                    throw new ApiException("no people counting report object");
                }
                Query pepQuery = peoAnalyticReport.query(start, end).addDevice(cameraList).getQuery();
                Iterable peopleCountingReportList = pepQuery.fetch();

                AnalyticsReport audiAnalyticReport = EventReport.getReport(EventType.VCA_PROFILING);
                if (audiAnalyticReport == null)
                {
                    throw new ApiException("no audience profiling report object");
                }
                Query audiQuery = audiAnalyticReport.query(start, end).addDevice(cameraList).getQuery();
                Iterable audienceProfilingReportList = audiQuery.fetch();

                //People counting reports group everyday data at this week.
                Map<String, List<PeopleCountingReport>> PepCountingGroup = new TreeMap<>();
                for (Object obj : peopleCountingReportList)
                {
                    PeopleCountingReport report = (PeopleCountingReport) obj;
                    String dateKey = report.date.substring(0, 10);  //according date to group data('2015/11/18').
                    List<PeopleCountingReport> list;
                    if (PepCountingGroup.containsKey(dateKey))
                    {
                        list = (List) PepCountingGroup.get(dateKey);
                    }
                    else
                    {
                        list = new ArrayList<>();
                    }
                    list.add(report);
                    PepCountingGroup.put(dateKey, list);
                }

                //Audience profiling reports group everyday data at this week.
                Map<String, List<AudienceProfilingReport>> audiProfilingGroup = new TreeMap<>();
                for (Object obj : audienceProfilingReportList)
                {
                    AudienceProfilingReport report = (AudienceProfilingReport) obj;
                    String dateKey = report.date.substring(0, 10);
                    List<AudienceProfilingReport> list;
                    if (audiProfilingGroup.containsKey(dateKey))
                    {
                        list = (List) audiProfilingGroup.get(dateKey);
                    }
                    else
                    {
                        list = new ArrayList<>();
                    }
                    list.add(report);
                    audiProfilingGroup.put(dateKey, list);
                }

                List<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", bucket.getBucketId()).fetchAll();
                for (MongoUser user : bucketUsers)
                {
                    List<MongoDevice> userDevices = dm.getDevicesOfUser(user.getUserId());
                    List<String> userDeviceIds = new ArrayList<String>();
                    for (MongoDevice device : userDevices)
                    {
                        userDeviceIds.add(device.getCoreDeviceId());
                    }

                    List<Long> weekPeopleIn = new ArrayList<Long>();
                    List<Long> weekMales = new ArrayList<Long>();
                    List<Long> weekFemales = new ArrayList<Long>();
                    long totalPeopleIn = 0L;
                    long totalMales = 0L;
                    long totalFemales = 0L;

                    for (Map.Entry<String, List<PeopleCountingReport>> entry : PepCountingGroup.entrySet())
                    {
                        List<PeopleCountingReport> list = (List) entry.getValue();
                        long peoplein = 0L;
                        for (PeopleCountingReport pepReport : list)
                        {
                            if (userDeviceIds.contains(pepReport.deviceId))
                            {
                                peoplein += pepReport.in;
                            }
                        }
                        weekPeopleIn.add(peoplein);
                    }

                    for (Map.Entry<String, List<AudienceProfilingReport>> entry : audiProfilingGroup.entrySet())
                    {
                        List<AudienceProfilingReport> list = (List) entry.getValue();
                        long male = 0L;
                        long female = 0L;
                        for (AudienceProfilingReport audiReport : list)
                        {
                            if (userDeviceIds.contains(audiReport.deviceId))
                            {
                                male += audiReport.male;
                                female += audiReport.female;
                            }
                        }
                        weekMales.add(male);
                        weekFemales.add(female);
                    }

                    totalPeopleIn = countListAllElement(weekPeopleIn);
                    totalMales = countListAllElement(weekMales);
                    totalFemales = countListAllElement(weekFemales);

                    int malePercent = 0;
                    int femalePercent = 0;
                    if ((totalMales + totalFemales) != 0)
                    {
                        malePercent = Math.round((float) totalMales / (totalMales + totalFemales) * 100);
                        femalePercent = Math.round((float) totalFemales / (totalMales + totalFemales) * 100);
                    }

                    //Top 5 Most visited store.
                    List<RankedLabel> rankedList = new ArrayList<>();
                    for (DeviceLabel label : bucketLabels)
                    {
                        List<DeviceChannelPair> assignedCameras = new ArrayList<>();
                        for (DeviceChannelPair labelCam : label.getCameraList())
                        {
                            if (userDeviceIds.contains(labelCam.getCoreDeviceId()))
                            {
                                assignedCameras.add(labelCam);
                            }
                        }

                        if (!assignedCameras.isEmpty())
                        {
                            //check label's device is assigned user.
                            long totalPeopleInOfLabel = 0L;
                            peopleCountingReportList = pepQuery.fetch();
                            for (Object obj : peopleCountingReportList)
                            {
                                PeopleCountingReport pepReport = (PeopleCountingReport) obj;
                                if (assignedCameras.contains(new DeviceChannelPair(pepReport.deviceId,
                                                                                   pepReport.channelId)))
                                {
                                    //check each p.c. report is generated by label's device.
                                    totalPeopleInOfLabel += pepReport.in;
                                }
                            }
                            rankedList.add(new RankedLabel(label.getLabelName(), totalPeopleInOfLabel));
                        }
                    }

                    //Sort by total people in, becoming ranked.
                    Collections.sort(rankedList, new Comparator<RankedLabel>()
                    {
                        @Override
                        public int compare(RankedLabel r1, RankedLabel r2)
                        {
                            long res = (r1.totalPeopleIn - r2.totalPeopleIn) * (-1);
                            return (int) res;
                        }
                    });

                    for (RankedLabel rLab : rankedList)
                    {
                        rLab.rank = rankedList.indexOf(rLab) + 1;
                    }

                    //Compare last week data.
                    Map<String, Object> emailArgs = new LinkedHashMap<String, Object>();
                    WeeklySummary summary = WeeklySummary.find("userId", user.getUserId()).first();
                    if (summary == null)
                    {  //new user.
                        summary = new WeeklySummary(bucket.getBucketId(),
                                                    user.getUserId(),
                                                    Util.convertToUtc(start),
                                                    Util.convertToUtc(end),
                                                    totalPeopleIn,
                                                    totalMales,
                                                    totalFemales,
                                                    rankedList);

                        emailArgs.put("sameData", "");
                        emailArgs.put("totalPeopleIn", NumberFormat.getNumberInstance().format(totalPeopleIn));
                        emailArgs.put("growthRatePeoIN", 0);
                        emailArgs.put("malePercent", malePercent);
                        emailArgs.put("growthRateMale", 0);
                        emailArgs.put("femalePercent", femalePercent);
                        emailArgs.put("growthRateFemale", 0);
                        emailArgs.put("rankLabels", rankedList);
                    }
                    else
                    {
                        long lastSummaryPeoIN = summary.totalPeopleIn;
                        long lastSummaryMales = summary.totalMales;
                        long lastSummaryFemales = summary.totalFemales;
                        List<RankedLabel> lastSummaryRankedList = summary.rankLabels;

                        //Growth rate formula : (present - past) / past * 100.
                        int growthRatePeoIN = 0;
                        if (lastSummaryPeoIN != 0)
                        {
                            growthRatePeoIN = (int) ((totalPeopleIn - lastSummaryPeoIN) / (float) lastSummaryPeoIN * 100);
                            growthRatePeoIN = growthRatePeoIN > 100 ? 100 : growthRatePeoIN;
                        }
                        int growthRateMale = 0;
                        if (lastSummaryMales != 0)
                        {
                            growthRateMale = (int) ((totalMales - lastSummaryMales) / (float) lastSummaryMales * 100);
                            growthRateMale = growthRateMale > 100 ? 100 : growthRateMale;
                        }
                        int growthRateFemale = 0;
                        if (lastSummaryFemales != 0)
                        {
                            growthRateFemale = (int) ((totalFemales - lastSummaryFemales) / (float) lastSummaryFemales * 100);
                            growthRateFemale = growthRateFemale > 100 ? 100 : growthRateFemale;
                        }

                        StringBuffer sameData = new StringBuffer();
                        if (lastSummaryPeoIN == totalPeopleIn)
                        {
                            sameData.append(", peoIn");
                        }
                        if (lastSummaryMales == totalMales)
                        {
                            sameData.append(", males");
                        }
                        if (lastSummaryFemales == totalFemales)
                        {
                            sameData.append(", females");
                        }

                        for (RankedLabel rankLab : rankedList)
                        {
                            if (lastSummaryRankedList == null)
                            {
                                break;
                            }
                            for (RankedLabel lastRankLab : lastSummaryRankedList)
                            {
                                if (rankLab.name.equals(lastRankLab.name))
                                {
                                    if (rankLab.rank == lastRankLab.rank)
                                    {
                                        rankLab.rankStatus = RankedLabel.STATUS_NORMAL;
                                    }
                                    else if (rankLab.rank > lastRankLab.rank)
                                    {
                                        rankLab.rankStatus = RankedLabel.STATUS_DOWN;
                                    }
                                    else
                                    {
                                        rankLab.rankStatus = RankedLabel.STATUS_TOP;
                                    }
                                    if (lastRankLab.totalPeopleIn != 0)
                                    {
                                        rankLab.growthRate = (int) ((rankLab.totalPeopleIn - lastRankLab.totalPeopleIn) / (float) lastRankLab.totalPeopleIn * 100);
                                    }
                                }
                            }
                        }

                        summary.start = Util.convertToUtc(start);
                        summary.end = Util.convertToUtc(end);
                        summary.totalPeopleIn = totalPeopleIn;
                        summary.totalMales = totalMales;
                        summary.totalFemales = totalFemales;
                        summary.rankLabels = rankedList;

                        emailArgs.put("sameData", sameData);
                        emailArgs.put("totalPeopleIn", NumberFormat.getNumberInstance().format(totalPeopleIn));
                        emailArgs.put("growthRatePeoIN", growthRatePeoIN);
                        emailArgs.put("malePercent", malePercent);
                        emailArgs.put("growthRateMale", growthRateMale);
                        emailArgs.put("femalePercent", femalePercent);
                        emailArgs.put("growthRateFemale", growthRateFemale);
                        emailArgs.put("rankLabels", rankedList);
                    }
                    summary.save();

                    boolean pepCountPermission = user.hasAccessToVcaFeature(VcaFeature.REPORT_PEOPLE_COUNTING);
                    boolean audiProfilingPermission = user.hasAccessToVcaFeature(VcaFeature.REPORT_PROFILING);
                    if ((!pepCountPermission && !audiProfilingPermission) ||
                        Util.isNullOrEmpty(user.getEmail()) ||
                        !user.isActivated())
                    {
                        continue;
                    }

                    String baseUrl = Play.configuration.getProperty("application.baseUrl");  //baseurl for images.
                    emailArgs.put("pepCountPermission", pepCountPermission);
                    emailArgs.put("audiProfilingPermission", audiProfilingPermission);
                    emailArgs.put("baseUrl", baseUrl);
                    emailArgs.put("date", timeRange);
                    emailArgs.put("userName", user.getName());
                    emailArgs.put("userLogin", user.getLogin());
                    emailArgs.put("bucket", bucket.getName());

                    //Render email content.
                    Template emailTmpl = TemplateLoader.load("kaisquare/common/templates/summary_email_tmpl.html");
                    String emailHtmlContent = emailTmpl.render(emailArgs);

                    EmailItem emailItem = new EmailItem(user.getEmail(),
                                                        Messages.get("weekly-report-generated"),
                                                        emailHtmlContent,
                                                        null);
                    DeliveryManager.getInstance().queue(DeliveryMethod.EMAIL, new Deliverable<>(emailItem));

                    AuditManager am = AuditManager.getInstance();
                    am.generateAuditlog(user, "SummaryEmailJob.doJob", null);

                    Logger.info("Weekly report email send to : %s , address : %s", user.getName(), user.getEmail());
                }
            }
        }
        catch (Exception exp)
        {
            Logger.error("Exception in SummaryEmailJob : %s", exp.getMessage());
            Logger.error(lib.util.Util.getStackTraceString(exp));
        }
    }

    private long countListAllElement(List<Long> list)
    {
        long result = 0L;
        for (Long val : list)
        {
            result = result + val;
        }
        return result;
    }
}
