package platform;

import lib.util.ListUtil;
import lib.util.Util;
import models.*;
import models.licensing.NodeLicense;
import platform.access.DefaultBucket;
import platform.access.FeatureRestriction;
import platform.analytics.VcaFeature;
import platform.node.NodeManager;
import play.Logger;

import java.util.*;

public class FeatureManager
{
    // region fields & constants

    public static final String TYPE_MONITORING = "monitoring";
    public static final String TYPE_RECORDING = "recording";
    public static final String TYPE_REPORTING = "reports";
    public static final String TYPE_ANALYTICS = "video-analytics";
    public static final String TYPE_NOTIFICATIONS = "notification-management";
    public static final String TYPE_ADMIN_SETTINGS = "admin-settings";
    public static final String TYPE_CUSTOMER_SUPPORT = "customer-support";
    public static final String TYPE_CLOUD_SERVER_MGMT = "cloud-server-management";

    private HashSet<String> fullFeatureSet;                     // for removing unused features
    private HashMap<String, HashSet<String>> fullSvcFeatureMap; // for removing unused services and shared Feature lookup
    private HashSet<MongoFeature> newFeatureSet;                // for updating superadmin
    private HashSet<MongoFeature> updatedFeatureSet;            // for updating buckets
    private Map<String, Integer> positionTracker;               // for keeping track of feature positions
    private HashSet<MongoService> commonServiceSet;
    private HashSet<String> commonServiceNames;

    private static FeatureManager instance = null;

    // endregion

    private FeatureManager()
    {
        fullFeatureSet = new LinkedHashSet<>();
        fullSvcFeatureMap = new LinkedHashMap<>();
        newFeatureSet = new LinkedHashSet<>();
        updatedFeatureSet = new LinkedHashSet<>();
        positionTracker = new LinkedHashMap<>();
        commonServiceSet = new LinkedHashSet<>();
        commonServiceNames = new LinkedHashSet<>();
    }

    public static FeatureManager getInstance()
    {
        synchronized (FeatureManager.class)
        {
            if (instance == null)
            {
                instance = new FeatureManager();
            }
        }

        return instance;
    }

    // region public methods

    public List<MongoService> getCommonServices()
    {
        return new ArrayList<>(commonServiceSet);
    }

    public List<String> getCommonServiceNames()
    {
        List<String> serviceNames = new ArrayList<>();
        for (MongoService service : commonServiceSet)
        {
            serviceNames.add(service.getName());
        }
        return serviceNames;
    }

    public HashSet<String> getFeaturesSharingApi(String apiName)
    {
        return fullSvcFeatureMap.get(apiName);
    }

    public void checkAndUpdateFeatures()
    {

        /*******************************
         *  Common services
         *******************************/
        setCommonServices(
                "login",
                "logout",
                "keepalive",
                "isusernameavailable",
                "getuserprofile",
                "updateuserprofile",
                "getuserprefs",
                "setuserprefs",
                "changepassword",
                "getuserfeatures",
                "getuserdevices",
                "generatesyncfile",
                "getusermobiledevices",
                "removemobiledeviceofuser",
                "registerapnsdevice",
                "registergcmdevice",
                "unregistergcmdevice",
                "unregisterapnsdevice",
                "recvcometnotification",
                "geteventvideo",
                "listrunninganalytics",
                "getvcacommands",
                "checklicensestatus",
                "getdashboard",
                "resetregistration",
                "reversegeocode",
                "getuseraccessiblelabels",
                "exportalerts",
                "getbucketsetting",
                "getusernotificationsettings",
                "updateusernotificationsettings",
                "getallowednotifymethods",
                "getlabels",
                "downloadexportedfile",
                "trackrunningtasks",
                "monitorvcainstancechange",
                "getdata",
                "getplatforminformation",
                "updatemobiledeviceinfo",
                "checkplatformstatus",
                "getalertdetails",

                //debugging
                //don't move these elsewhere. Node platform also uses these
                "getplaystatus",
                "getcommandqueues",
                "getcommandlogs",
                "getsynctasksstatus",
                "getrejectedevents",
                "browseeventvideos",
                "getunsyncedevents",
                "getunsyncedeventvideos",
                "geteventvideorequests",
                "deleteeventvideo",
                "getallnotifications",
                "getdeliveryjobsstatus",
                "getmigrationerrorlogs",
                "getserverstatus"
        );


        /*******************************
         *  Monitoring
         *******************************/
        putFeature("live-view", TYPE_MONITORING, FeatureRestriction.NONE,
                   "getlivevideourl",
                   "keepalivelivevideourl",
                   "expirelivevideourl",
                   "saveuserprefs"
        );
        putFeature("current-occupancy", TYPE_MONITORING, FeatureRestriction.CLOUD_ONLY,
                   "getanalyticsreport",
                   "listrunninganalytics",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "monitorOccupancyChange"
        );
        putFeature("live-track", TYPE_MONITORING, FeatureRestriction.CLOUD_ONLY,
                   "getlivelocation",
                   "getbucketpois"
        );
        putFeature("point-of-interests", TYPE_MONITORING, FeatureRestriction.CLOUD_ONLY,
                   "getbucketpois",
                   "addpoi",
                   "updatepoi",
                   "removepoi"
        );


        /*******************************
         *  Recording
         *******************************/
        putFeature("cloud-playback", TYPE_RECORDING, FeatureRestriction.CLOUD_ONLY,
                   "getbucketusers",
                   "searchcloudrecordings",
                   "requestcloudrecordings",
                   "deletecloudrecordings",
                   "findpendinguploadrequests",
                   "getrecordinguploadrequests",
                   "getnodecamerastorage"
        );
        putFeature("node-playback", TYPE_RECORDING, FeatureRestriction.NODE_ONLY,
                   "getplaybackvideourl",
                   "keepaliveplaybackvideourl",
                   "expireplaybackvideourl",
                   "getrecordedfilelist",
                   "downloadzippedrecordings",
                   "getusbdrives",
                   "usbexportrecordings"
        );

        /*******************************
         *  Video Analytics - BI
         *******************************/
        putFeature(VcaFeature.CONFIG_TRAFFIC_FLOW.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "trafficflowfeed"
        );
        putFeature(VcaFeature.CONFIG_PEOPLE_COUNTING.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "peoplecountingfeed",
                   "monitorOccupancyChange"
        );
        putFeature(VcaFeature.CONFIG_CROWD_DENSITY.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "crowddensityfeed"
        );
        putFeature(VcaFeature.CONFIG_PROFILING.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "profilingfeed"
        );
        putFeature(VcaFeature.CONFIG_PASSERBY.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "passerbyfeed",
                   "getvcaconcurrencystatus"
        );


        /*******************************
         *  Video Analytics - Security
         *******************************/
        putFeature(VcaFeature.CONFIG_AREA_INTRUSION.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "intrusionfeed"
        );
        putFeature(VcaFeature.CONFIG_PERIMETER_DEFENSE.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "perimeterdefensefeed"
        );
        putFeature(VcaFeature.CONFIG_LOITERING.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "loiteringfeed"
        );
        putFeature(VcaFeature.CONFIG_OBJ_COUNTING.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "objectcountingfeed"
        );
        putFeature(VcaFeature.CONFIG_VIDEO_BLUR.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "videoblurfeed"
        );
        putFeature(VcaFeature.CONFIG_FACE_INDEXING.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                   "addvca",
                   "updatevca",
                   "activatevca",
                   "deactivatevca",
                   "removevca",
                   "exportdatalogs",
                   "getanalyticsreport",
                   "addschedulepreset",
                   "removeschedulepreset",
                   "getschedulepresets",
                   "getvcaerrors",
                   "geteventswithbinary",
                   "faceindexingfeed"
        );
        
        putFeature(VcaFeature.CONFIG_OBJECT_DETECTION.toString(), TYPE_ANALYTICS, FeatureRestriction.NONE,
                "addvca",
                "updatevca",
                "activatevca",
                "deactivatevca",
                "removevca",
                "exportdatalogs",
                "getanalyticsreport",
                "addschedulepreset",
                "removeschedulepreset",
                "getschedulepresets",
                "getvcaerrors",
                "geteventswithbinary",
                "objectdetectionfeed"
     );


        /*******************************
         *  Reporting - BI
         *******************************/
        putFeature(VcaFeature.REPORT_TRAFFIC_FLOW.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "gettrafficflowmap",
                   "exporttrafficflowpdf",
                   "exportdatalogs",
                   "trafficflowfeed"
        );
        putFeature(VcaFeature.REPORT_PEOPLE_COUNTING.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "exportpeoplecountingpdf",
                   "getpossalesreport",
                   "updatepossalesdata",
                   "listposnames",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaggregatedcsvreport",
                   "peoplecountingfeed"
        );
        putFeature(VcaFeature.REPORT_CROWD_DENSITY.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "getheatmap",
                   "exportcrowddensitypdf",
                   "exportdatalogs",
                   "crowddensityfeed"
        );
        putFeature(VcaFeature.REPORT_PROFILING.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaudienceprofilingpdf",
                   "exportaggregatedcsvreport",
                   "profilingfeed"
        );
        putFeature("report-audience-attention", TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "getreportqueryhistory",
                   "savereportqueryhistory"
        );
        putFeature(VcaFeature.REPORT_PASSERBY.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaggregatedcsvreport",
                   "passerbyfeed"
        );
        
       


        /*******************************
         *  Reporting - Security
         *******************************/
        putFeature(VcaFeature.REPORT_AREA_INTRUSION.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "exportvcasecuritypdf",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaggregatedcsvreport",
                   "intrusionfeed"
        );
        putFeature(VcaFeature.REPORT_PERIMETER_DEFENSE.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "exportvcasecuritypdf",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaggregatedcsvreport",
                   "perimeterdefensefeed"
        );
        putFeature(VcaFeature.REPORT_LOITERING.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "exportvcasecuritypdf",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaggregatedcsvreport",
                   "loiteringfeed"
        );
        putFeature(VcaFeature.REPORT_OBJ_COUNTING.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "exportvcasecuritypdf",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaggregatedcsvreport",
                   "objectcountingfeed"
        );
        putFeature(VcaFeature.REPORT_VIDEO_BLUR.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "getanalyticsreport",
                   "exportvcasecuritypdf",
                   "getreportqueryhistory",
                   "savereportqueryhistory",
                   "exportaggregatedcsvreport",
                   "videoblurfeed"
        );
        putFeature(VcaFeature.REPORT_FACE_INDEXING.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                   "exportdatalogs",
                   "getanalyticsreport",
                   "geteventbinarydata",
                   "exportfaceindexingpdf",
                   "exportdatalogs",
                   "geteventswithbinary",
                   "faceindexingfeed"
        );
        
        putFeature(VcaFeature.REPORT_OBJECT_DETECTION.toString(), TYPE_REPORTING, FeatureRestriction.NONE,
                "getanalyticsreport",
                "getreportqueryhistory",
                "savereportqueryhistory",
                "exportaggregatedcsvreport",
                "objectdetectionfeed"
       );

        /*******************************
         *  Admin Settings
         *******************************/
        putFeature("bucket-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "getbuckets",
                   "addbucket",
                   "updatebucket",
                   "updatebucketsettings",
                   "removebucket",
                   "getbucketfeatures",
                   "updatebucketfeatures",
                   "activatebucket",
                   "deactivatebucket",
                   "getbucketusersbybucketid",
                   "getbucketdevicesbybucketid",
                   "listanalyticsbybucketid",
                   "restorebucket",
                   "getbucketlogs",
                   "getbucketnotificationsettings",
                   "updatebucketnotificationsettings",
                   "restorebucketnotificationsettings",
                   "getnodeinfo",
                   "getbucketpasswordpolicy",
                   "updatebucketpasswordpolicy"
        );
        putFeature("user-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.NONE,
                   "adduser",
                   "updateuser",
                   "removeuser",
                   "activateuser",
                   "deactivateuser",
                   "getbucketusers",
                   "adddeviceuser",
                   "removedeviceuser",
                   "getuserdevicesbyuserid",
                   "getbucketroles",
                   "getuserrolesbyuserid",
                   "updateuserroles",
                   "addvehicleuser",
                   "removevehicleuser",
                   "getuservehicles",
                   "getuservehiclesbyuserid",
                   "getbucketuserlabels",
                   "exportuserlist",
                   "uploadlogobinarydata"
        );
        putFeature("role-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.NONE,
                   "getbucketroles",
                   "addbucketrole",
                   "editbucketrole",
                   "removerole",
                   "getrolefeatures",
                   "getbucketfeatures",
                   "updaterolefeatures",
                   "getassignablerolefeatures"
        );
        putFeature("device-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.NONE,
                   "getbucketdevices",
                   "adddevicetobucket",
                   "removedevicefrombucket",
                   "updatedevice",
                   "getdevicemodels",
                   "getbucketdevicelabels",
                   "adddeviceuser",
                   "editnodecamera",
                   "startautodiscovery",
                   "stopautodiscovery",
                   "getdiscovereddevices",
                   "getnodesettings",
                   "getnodecameralist",
                   "getnodeanalyticslist",
                   "getdevicelogs",
                   "getnodeinfooncloud",
                   "updatenodesoftware",
                   "getvcaconcurrencystatus",
                   "getlivevideourl"
        );
        putFeature("kai-nodes", TYPE_ADMIN_SETTINGS, FeatureRestriction.NONE,
                   "updatedevice",
                   "getlivevideourl",
                   "getdevicemodels"
        );
        putFeature("inventory-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "addinventory",
                   "uploadinventory",
                   "getinventorylist",
                   "removeallinventory",
                   "removeinventory",
                   "updateinventory"
        );
        putFeature("software-update-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "uploadSoftwareUpdate",
                   "getsoftwareupdatelist",
                   "removesoftwareupdate",
                   "downloadsoftwareupdate"
        );
        putFeature("access-key-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "getbuckets",
                   "getaccesskeylist",
                   "generateaccesskey",
                   "removeaccesskey"
        );
        putFeature("announcement-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "getannouncementlist",
                   "addannouncement",
                   "updateannouncement",
                   "removeannouncement"
        );
        putFeature("audit-log", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "getauditlog",
                   "getauditlogdetails",
                   "exportauditlog"
        );
        putFeature("pos-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "getpossettings",
                   "updatepossettings",
                   "updatepossalesdata"
        );
        putFeature("local-license-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "getbucketfeatures",
                   "getassignablenodefeatures",
                   "getnodelicenses",
                   "getnodelicenselogs",
                   "exportlicenselist"
        );
        putFeature("label-management", TYPE_ADMIN_SETTINGS, FeatureRestriction.CLOUD_ONLY,
                   "addlabel",
                   "updatelabel",
                   "removelabel",
                   "assignchannellabel",
                   "unassignchannellabel"
        );

        /*******************************
         * Notification Management
         *******************************/
        putFeature("historical-alerts", TYPE_NOTIFICATIONS, FeatureRestriction.NONE,
                   "getalerts",
                   "acknowledgenotification"
        );
        putFeature("label-settings", TYPE_NOTIFICATIONS, FeatureRestriction.CLOUD_ONLY,
                   "getlabeloccupancysettings",
                   "updatelabeloccupancysettings"
        );
        putFeature("label-notifications", TYPE_NOTIFICATIONS, FeatureRestriction.CLOUD_ONLY,
                   "getlabelnotifications",
                   "acknowledgenotification"
        );

        /*******************************
         * Customer Support
         *******************************/
        putFeature("remote-shell", TYPE_CUSTOMER_SUPPORT, FeatureRestriction.CLOUD_ONLY,
                   "startremoteshell",
                   "stopremoteshell",
                   "getremoteshelllist",
                   "getbuckets",
                   "getbucketdevices"
        );
        putFeature("node-log-management", TYPE_CUSTOMER_SUPPORT, FeatureRestriction.CLOUD_ONLY,
                   "getbuckets",
                   "pullnodelog",
                   "getnodeloglist",
                   "downloadnodelogfile"
        );
        putFeature("global-license-management", TYPE_CUSTOMER_SUPPORT, FeatureRestriction.CLOUD_ONLY,
                   "getbuckets",
                   "getassignablenodefeatures",
                   "getnodelicenses",
                   "addnodelicense",
                   "updatenodelicense",
                   "deletenodelicense",
                   "suspendnodelicense",
                   "unsuspendnodelicense",
                   "getnodelicenselogs",
                   "exportlicenselist"
        );
        putFeature("account-statements", TYPE_CUSTOMER_SUPPORT, FeatureRestriction.CLOUD_ONLY,
                   "getbuckets",
                   "getbucketusersbybucketid",
                   "getbucketdevicesbybucketid",
                   "exportusersfilebybucketid",
                   "exportnodesbybucketid",
                   "getnodelicenses",
                   "exportlicenselist",
                   "getnodeinfo"
        );
        putFeature("node-information", TYPE_CUSTOMER_SUPPORT, FeatureRestriction.CLOUD_ONLY,
                   "getbuckets",
                   "getbucketdevicesbybucketid",
                   "exportnodesbybucketid",
                   "getnodeinfooncloud",
                   "getlabelsbybucketid"
        );
        putFeature("holiday-calendar", TYPE_CUSTOMER_SUPPORT, FeatureRestriction.CLOUD_ONLY,
                   "addholiday",
                   "getholidays",
                   "deleteholiday",
                   "updateholiday",
                   "getcountrylist"
        );
        putFeature("schedule-tasks", TYPE_CUSTOMER_SUPPORT, FeatureRestriction.CLOUD_ONLY,
                   "listnodeupdateschedules",
                   "schedulenodeupdates",
                   "deletenodeupdateschedule",
                   "getnodeupdateschedule"
        );

        /*******************************
         * Cloud Server Management
         *******************************/
        putFeature("node-browser", TYPE_CLOUD_SERVER_MGMT, FeatureRestriction.SUPERADMIN_ONLY,
                   "searchregisterednodes"
        );
        putFeature("server-internal-settings", TYPE_CLOUD_SERVER_MGMT, FeatureRestriction.SUPERADMIN_ONLY,
                   "getserverconfigurations",
                   "updateserverconfigurations"
        );
        putFeature("site-monitoring", TYPE_CLOUD_SERVER_MGMT, FeatureRestriction.SUPERADMIN_ONLY,
                   "fetchnodesformonitoring"
        );

        Logger.info("----------");
        removeOrphanedServices();
        removeOrphanedFeatures();

        Logger.info("Features check completed (Total: %s features, %s APIs)",
                    fullFeatureSet.size(),
                    fullSvcFeatureMap.keySet().size());
    }

    public void refreshModifiedBucketFeatures()
    {
        BucketManager bucketManager = BucketManager.getInstance();

        //update superadmin for new features
        if (newFeatureSet.size() > 0)
        {
            Logger.info("%s new feature(s) found %s", newFeatureSet.size(), newFeatureSet);
            try
            {
                if (Environment.getInstance().onCloud())
                {
                    MongoBucket superBucket = MongoBucket.getByName(DefaultBucket.SUPERADMIN.getBucketName());
                    List<String> newFeatureNames = new ArrayList<>();
                    for (MongoFeature newFeature : newFeatureSet)
                    {
                        newFeatureNames.add(newFeature.getName());
                    }
                    bucketManager.addFeatures(superBucket.getBucketId(), newFeatureNames);
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }

        // update all buckets that were previously assigned modified features
        if (updatedFeatureSet.size() > 0)
        {
            Logger.info("%s feature(s) modified %s", updatedFeatureSet.size(), updatedFeatureSet);
            try
            {
                List<String> updatedFeatureNames = new ArrayList<>();
                for (MongoFeature updatedFeature : updatedFeatureSet)
                {
                    updatedFeatureNames.add(updatedFeature.getName());
                }
                List<MongoBucket> allBuckets = MongoBucket.q().fetchAll();
                for (MongoBucket targetBucket : allBuckets)
                {
                    List<String> affectedBucketFeatureNames = ListUtil.hasInCommon(targetBucket.getFeatureNames(),
                                                                                   updatedFeatureNames);
                    if (affectedBucketFeatureNames.isEmpty())
                    {
                        continue;
                    }

                    // update roles with this feature
                    List<MongoRole> bucketRoles = MongoRole.q()
                            .filter("bucketId", targetBucket.getBucketId())
                            .fetchAll();

                    for (MongoRole bRole : bucketRoles)
                    {
                        List<String> affectedRoleFeatureNames = ListUtil.hasInCommon(bRole.getFeatureNames(),
                                                                                     affectedBucketFeatureNames);
                        if (affectedRoleFeatureNames.isEmpty())
                        {
                            continue;
                        }

                        RoleManager.getInstance()
                                .updateFeatures(bRole.getRoleId(), new ArrayList<>(bRole.getFeatureNames()));
                    }

                    Logger.info("(%s) Updated modified features: %s",
                                targetBucket.getName(),
                                affectedBucketFeatureNames);
                }

            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }

    public void verifyCommonServicesOnAllUsers()
    {
        List<MongoUser> allUsers = MongoUser.q().fetchAll();
        List<String> commonServiceNames = getCommonServiceNames();

        for (MongoUser user : allUsers)
        {
            for (String serviceName : commonServiceNames)
            {
                if (!user.getServiceNames().contains(serviceName))
                {
                    user.addServiceName(serviceName);
                    user.save();
                    Logger.info("Added missing common api (%s) to user [%s]", serviceName, user.getLogin());
                }
            }
            user.save();
        }
    }

    public void checkNodeLicenseFeatures()
    {
        if (Environment.getInstance().onCloud())
        {
            // remove features that are not supposed to be in node licenses
            List<NodeLicense> allLicenses = NodeLicense.q().fetchAll();
            for (NodeLicense license : allLicenses)
            {
                try
                {
                    MongoBucket bucket = MongoBucket.getById(license.cloudBucketId.toString());
                    List<String> removeFeatureNames = new ArrayList<>();
                    for (String featureName : license.featureNameList)
                    {
                        MongoFeature f = MongoFeature.getByName(featureName);
                        if (f == null)
                        {
                            removeFeatureNames.add(featureName);
                            Logger.info("[%s:%s] removed invalid feature (%s) from node license.",
                                        bucket,
                                        license.licenseNumber,
                                        featureName);
                            continue;
                        }
                        if (!f.isAssignableToNodes())
                        {
                            removeFeatureNames.add(featureName);
                            Logger.info("[%s:%s] removed cloud-only feature (%s) from node license.",
                                        bucket,
                                        license.licenseNumber,
                                        featureName);
                            continue;
                        }
                    }

                    if (!removeFeatureNames.isEmpty())
                    {
                        license.featureNameList.removeAll(removeFeatureNames);
                        license.save();
                    }
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                }
            }
        }
        else if (Environment.getInstance().onKaiNode())
        {
            if (!NodeManager.getInstance().isRegisteredOnCloud())
            {
                return;
            }
            // remove features that are not supposed to be on nodes
            try
            {
                MongoBucket nodeBucket = NodeManager.getInstance().getBucket();
                List<String> removeFeatureNames = new ArrayList<>();
                for (String featureName : nodeBucket.getFeatureNames())
                {
                    MongoFeature feature = MongoFeature.getByName(featureName);
                    if (!feature.isAssignableToNodes())
                    {
                        removeFeatureNames.add(featureName);
                    }
                }

                BucketManager.getInstance().removeFeatures(nodeBucket.getBucketId(), removeFeatureNames);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }

    // endregion

    // region private methods

    private void updateSvcFeatureMap(Set<String> serviceNames, String featureName)
    {
        for (String serviceName : serviceNames)
        {
            if (!fullSvcFeatureMap.containsKey(serviceName))
            {
                fullSvcFeatureMap.put(serviceName, new LinkedHashSet<String>());
            }

            fullSvcFeatureMap.get(serviceName).add(featureName);
        }
    }

    private void setCommonServices(String... requiredApis)
    {
        try
        {
            for (String svcName : requiredApis)
            {
                MongoService service = MongoService.getByName(svcName);
                if (service != null)
                {
                    commonServiceSet.add(service);
                    commonServiceNames.add(svcName);
                }
            }

            updateSvcFeatureMap(commonServiceNames, "common");
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    /**
     * At server restart, these fields will be updated, except for "name"
     * Name is not allowed to change and must be unique.
     */
    private void putFeature(String name, String type, FeatureRestriction restriction, String... requiredServiceNames)
    {
        try
        {
            if (fullFeatureSet.contains(name))
            {
                throw new Exception(String.format("Feature name must be unique (%s)", name));
            }

            if (requiredServiceNames == null || requiredServiceNames.length == 0)
            {
                throw new Exception(String.format("Missing required APIs (%s > %s)", type, name));
            }

            // Create or find services in db
            Set<String> assignedServiceNames = new LinkedHashSet<>();
            for (String requiredServiceName : requiredServiceNames)
            {
                MongoService apiService = MongoService.getByName(requiredServiceName);
                if (apiService != null)
                {
                    assignedServiceNames.add(requiredServiceName);
                }
            }

            // create or update feature
            MongoFeature dbFeature = MongoFeature.getByName(name);
            Logger.info("featureName:"+name+" exsits in db?"+(dbFeature==null));
            
            if (dbFeature == null)
            {
                dbFeature = new MongoFeature();
                dbFeature.setName(name);
                dbFeature.save();
                newFeatureSet.add(dbFeature);
                Logger.info("New Feature Added (%s)", dbFeature);
            }
            dbFeature.setType(type);
            dbFeature.setRestriction(restriction);
            dbFeature.setLevelOnePosition(getLevelOnePosition(type));
            dbFeature.setLevelTwoPosition(getLevelTwoPosition(type));

            //find newly assigned and removed services
            List<String> newServices = ListUtil.getExtraItems(dbFeature.getServiceNames(), assignedServiceNames);
            List<String> removedServices = ListUtil.getExtraItems(assignedServiceNames, dbFeature.getServiceNames());
            if (newServices.size() > 0 || removedServices.size() > 0)
            {
                //modified features
                updatedFeatureSet.add(dbFeature);
            }
            dbFeature.setServiceNames(new ArrayList<>(assignedServiceNames));
            dbFeature.save();

            //update internal records
            updateSvcFeatureMap(assignedServiceNames, dbFeature.getName());
            fullFeatureSet.add(dbFeature.getName());
            positionTracker.put(type, dbFeature.getLevelTwoPosition());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void deleteFeature(String featureName)
    {
        try
        {
            MongoFeature feature = MongoFeature.getByName(featureName);
            if (feature == null)
            {
                Logger.warn("%s feature not found", featureName);
                return;
            }

            //remove from buckets
            List<MongoBucket> buckets = MongoBucket.q().fetchAll();
            for (MongoBucket bucket : buckets)
            {
                List<String> duplicateFeatureNames = new ArrayList<>();
                for (String aFeatureName : bucket.getFeatureNames())
                {
                    if (aFeatureName.equals(featureName))
                    {
                        duplicateFeatureNames.add(aFeatureName);
                    }
                }

                if (duplicateFeatureNames.isEmpty())
                {
                    continue;
                }

                Logger.info("Removing features %s from %s bucket", duplicateFeatureNames, bucket);
                bucket.getFeatureNames().removeAll(duplicateFeatureNames);
                bucket.save();
            }

            //remove from roles
            List<MongoRole> roles = MongoRole.q().fetchAll();
            for (MongoRole role : roles)
            {
                List<String> duplicateFeatureNames = new ArrayList<>();
                for (String aFeatureName : role.getFeatureNames())
                {
                    if (aFeatureName.equals(featureName))
                    {
                        duplicateFeatureNames.add(aFeatureName);
                    }
                }

                if (duplicateFeatureNames.isEmpty())
                {
                    continue;
                }

                Logger.info("Removing features %s from %s role", duplicateFeatureNames, role);
                role.getFeatureNames().removeAll(duplicateFeatureNames);
                role.save();
            }

            feature.delete();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void deleteService(String serviceName)
    {
        try
        {
            MongoService targetService = MongoService.getByName(serviceName);
            if (targetService == null)
            {
                Logger.warn("%s api not found", serviceName);
            }

            // remove from features
            List<MongoFeature> features = MongoFeature.q().fetchAll();
            for (MongoFeature feature : features)
            {
                List<String> removeServiceNames = new ArrayList<>();
                for (String aServiceName : feature.getServiceNames())
                {
                    if (aServiceName.equals(serviceName))
                    {
                        removeServiceNames.add(aServiceName);
                    }
                }

                feature.getServiceNames().removeAll(removeServiceNames);
                feature.save();
            }

            // remove from users
            List<MongoUser> users = MongoUser.q().fetchAll();
            for (MongoUser user : users)
            {
                List<String> removeServiceNames = new ArrayList<>();
                for (String aServiceName : user.getServiceNames())
                {
                    if (aServiceName.equals(serviceName))
                    {
                        removeServiceNames.add(aServiceName);
                    }
                }

                user.getServiceNames().removeAll(removeServiceNames);
                user.save();
            }

            targetService.delete();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void removeOrphanedServices()
    {
        try
        {
            List<MongoService> services = MongoService.q().fetchAll();
            for (MongoService service : services)
            {
                if (fullSvcFeatureMap.containsKey(service.getName()))
                {
                    continue;
                }
                Logger.info("Deleting orphaned API (%s)", service);
                deleteService(service.getName());
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    private void removeOrphanedFeatures()
    {
        try
        {
            List<MongoFeature> features = MongoFeature.q().fetchAll();

            // find orphans
            for (MongoFeature feature : features)
            {
                if (fullFeatureSet.contains(feature.getName()))
                {
                    continue;
                }
                Logger.info("Deleting orphaned feature (%s)", feature);
                deleteFeature(feature.getName());
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
        }
    }

    // Level 1 shows main feature groups
    // The position is determined by the insertion order under checkAndUpdateFeatures()
    private int getLevelOnePosition(String featureType)
    {
        if (!positionTracker.containsKey(featureType))
        {
            positionTracker.put(featureType, 0);
        }

        List<String> types = new ArrayList<>(positionTracker.keySet());
        return types.indexOf(featureType);
    }

    // Level 2 shows the features.
    // The position is determined by the insertion order under checkAndUpdateFeatures()
    private int getLevelTwoPosition(String featureType)
    {
        Integer pos = positionTracker.get(featureType);
        if (pos == null)
        {
            return 0;
        }

        pos++;
        return pos;
    }

    // endregion
}
