package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.Analytics.NodeVcaInstance;
import platform.analytics.IVcaInstance;
import platform.analytics.Program;
import platform.analytics.VcaManager;
import platform.analytics.VcaType;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class vca extends Controller
{
    private static String getVcaListHtml()
    {
        return "/vca/management/vca_list.html";
    }

    public static void drawmask()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/draw_mask.html");
    }

    public static void debug(String id)
    {
        String instanceId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/debugger.html", instanceId);
    }

    public static void allrunning()
    {
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml());
    }

    public static void scheduler()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/schedule/scheduler.html");
    }

    public static void presetloader()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/schedule/preset_loader.html");
    }

    public static void savepreset()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/schedule/save_as.html");
    }

    public static void errorlog(String id)
    {
        try
        {
            String instanceId = id;
            IVcaInstance dbInstance = VcaManager.getInstance().getVcaInstance(instanceId);
            String coreDeviceId = dbInstance.getVcaInfo().getCamera().getCoreDeviceId();
            String channelId = dbInstance.getVcaInfo().getCamera().getChannelId();

            long created = 0;
            if (dbInstance instanceof NodeVcaInstance)
            {
                created = ((NodeVcaInstance) dbInstance)._getCreated();
            }

            renderTemplate(renderArgs.get("HtmlPath") + "/vca/errorlog.html",
                           instanceId,
                           coreDeviceId,
                           channelId,
                           created);

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            notFound();
        }
    }

    public static void gMaskTool()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/gmask/main_win.html");
    }

    public static void gMaskFrame()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/gmask/leaflet_frame.html");
    }

    public static void visualizeschedule()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/schedule/visualizer.html");
    }

    public static void supportedprograms(String nodeId)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/supported_programs.html", nodeId);
    }

    public static void vcaconcurrency(String nodeId, String maxVcaConcurrency)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/schedule/vca_concurrency.html", nodeId, maxVcaConcurrency);
    }

    public static void occupancygrid()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/management/occupancy_grid.html");
    }

    public static void capturesizelimit()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/sizelimiting/capture_size_limit.html");
    }

    public static void chooseprogram()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/choose_program.html");
    }


    public static void addnew(VcaType vcaType, Program program)
    {
        Logger.info("vca.java.add new...");
    	String htmlFile = "";
        switch (vcaType)
        {
            case TRAFFIC_FLOW:
                htmlFile += "/vca/popups/traffic_flow_add_channel.html";
                break;

            case PEOPLE_COUNTING:
                htmlFile += String.format("/vca/peoplecounting/people_counting_add_%s.html", program);
                break;

            case PASSERBY:
                htmlFile += "/vca/popups/passerby_add.html";
                break;

            case CROWD_DETECTION:
                htmlFile += "/vca/popups/crowd_add_channel.html";
                break;

            case AUDIENCE_PROFILING:
            	//update by RenZongKe for audience-profiling gen2
            	//htmlFile += "/vca/popups/profiling_add_channel.html";
            	htmlFile += String.format("/vca/audience-profiling/profiling_add_%s.html", program);
            	Logger.info("Audience-profiling: {}",htmlFile);
            	break;

            case AREA_INTRUSION:
                htmlFile += "/vca/popups/intrusion_add.html";
                break;

            case PERIMETER_DEFENSE:
                htmlFile += "/vca/popups/pdefense_add.html";
                break;

            case AREA_LOITERING:
                htmlFile += "/vca/popups/loitering_add.html";
                break;

            case OBJECT_COUNTING:
                htmlFile += "/vca/popups/objectcounting_add_device.html";
                break;

            case VIDEO_BLUR:
                htmlFile += "/vca/popups/blur_add_channel.html";
                break;

            case FACE_INDEXING:
                htmlFile += "/vca/popups/face_indexing_add_channel.html";
                break;
            //add by renzongke
            case OBJECT_DETECTION:
            	htmlFile += "/vca/popups/object_detection_add_channel.html";
            	break;
            default:
                notFound();
        }
        Logger.info(htmlFile);
        renderTemplate(renderArgs.get("HtmlPath") + htmlFile);
    }

    public static void configure(VcaType vcaType, String instanceId, String readonly, Program program)
    {
        String htmlFile = "";
        switch (vcaType)
        {
            case TRAFFIC_FLOW:
                htmlFile = "/vca/popups/traffic_flow_config.html";
                break;

            case PEOPLE_COUNTING:
                htmlFile = String.format("/vca/peoplecounting/people_counting_config_%s.html", program);
                break;

            case PASSERBY:
                htmlFile = "/vca/popups/passerby_config.html";
                break;

            case CROWD_DETECTION:
                htmlFile = "/vca/popups/crowd_config.html";
                break;

            case AUDIENCE_PROFILING:
            	//update by RenZongKe for Audience-profiling gen2
                //htmlFile = "/vca/popups/profiling_config.html";
                htmlFile = String.format("/vca/audience-profiling/profiling_config_%s.html",program);
            	break;


            case AREA_INTRUSION:
                htmlFile = "/vca/popups/intrusion_config.html";
                break;

            case PERIMETER_DEFENSE:
                htmlFile = "/vca/popups/pdefense_config.html";
                break;

            case AREA_LOITERING:
                htmlFile = "/vca/popups/loitering_config.html";
                break;

            case OBJECT_COUNTING:
                htmlFile = "/vca/popups/objectcounting_config.html";
                break;

            case VIDEO_BLUR:
                htmlFile = "/vca/popups/blur_config.html";
                break;

            case FACE_INDEXING:
                htmlFile = "/vca/popups/face_indexing_config.html";
                break;
            case OBJECT_DETECTION:
            	htmlFile= "/vca/popups/object_detection_config.html";
            	break;
            default:
                notFound();
        }
        Logger.info("htmlPath:::", htmlFile);
        renderTemplate(renderArgs.get("HtmlPath") + htmlFile, instanceId, readonly);
    }

    public static void areaintrusion()
    {
        String vcaTypeName = VcaType.AREA_INTRUSION.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void pdefense()
    {
        String vcaTypeName = VcaType.PERIMETER_DEFENSE.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void loitering()
    {
        String vcaTypeName = VcaType.AREA_LOITERING.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void objectcounting()
    {
        String vcaTypeName = VcaType.OBJECT_COUNTING.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void blur()
    {
        String vcaTypeName = VcaType.VIDEO_BLUR.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void faceindexing()
    {
        String vcaTypeName = VcaType.FACE_INDEXING.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void trafficflow()
    {
        String vcaTypeName = VcaType.TRAFFIC_FLOW.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void peoplecounting()
    {
        String vcaTypeName = VcaType.PEOPLE_COUNTING.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void crowd()
    {
        String vcaTypeName = VcaType.CROWD_DETECTION.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void crowdregionname()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/popups/crowd_add_region_name.html");
    }

    public static void profiling()
    {
        String vcaTypeName = VcaType.AUDIENCE_PROFILING.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }

    public static void passerby()
    {
        String vcaTypeName = VcaType.PASSERBY.getVcaTypeName();
        renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }
    
    public static void objectdetection(){
    	String vcaTypeName = VcaType.OBJECT_DETECTION.getVcaTypeName();
    	renderTemplate(renderArgs.get("HtmlPath") + getVcaListHtml(), vcaTypeName);
    }
    
}