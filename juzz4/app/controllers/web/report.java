package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class report extends Controller
{

    public static void index()
    {
        renderArgs.put("reportPage", true);
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/default.html");
    }

    public static void crowd()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/crowd.html");
    }

    public static void trafficflow()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/trafficflow.html");
    }

    public static void profiling()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/profiling.html");
    }

    public static void faceindexing()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/faceindexing.html");
    }

    public static void peoplecounting()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/peoplecounting.html");
    }
    
    public static void passerby()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/passerby.html");
    }

    public static void attention()
    {
        renderArgs.put("audienceAttentionPage", true);
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/audienceattention.html");
    }

    /**
     * VCA security
     */

    public static void intrusion()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/intrusion.html");
    }

    public static void pdefense()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/pdefense.html");
    }

    public static void videoblur()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/video_blur.html");
    }

    public static void loitering()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/loitering.html");
    }

    public static void objcounting()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/objcounting.html");
    }

    /**
     * add by RenZongke for ObjectDetection
     */
    public static void objdetect() {
    	renderTemplate(renderArgs.get("HtmlPath") + "/vca/reports/objdetection.html");
    }
    /**
     * POS Management
     */

    public static void possettings()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/reports/pos_settings.html");
    }

    public static void posedit()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/reports/pos_edit.html");
    }
}

