package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.BucketSetting;
import platform.BucketManager;
import platform.analytics.VcaType;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;

import java.io.File;

@With(WebInterceptor.class)
public class content extends Controller
{

    public static void csvSample()
    {
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=\"inventory_template.csv\"");
        renderBinary(new File("public/files/samples/inventory_template.csv"));
    }

    public static void downloadGuide(String guideName)
    {
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + guideName + "\"");
        renderBinary(new File("public/files/guides/" + guideName + ""));
    }

    public static void helpvideo(String id)
    {

        VcaType requestType = VcaType.valueOf(id);
        String videoUrl = "";

        if (requestType.equals(VcaType.FACE_INDEXING))
        {
            videoUrl = "http://www.youtube.com/embed/gKYBKpfNjqM?rel=0";
        }

        else if (requestType.equals(VcaType.TRAFFIC_FLOW))
        {
            videoUrl = "http://www.youtube.com/embed/ufAYMTBeXqk?rel=0";
        }

        else if (requestType.equals(VcaType.PEOPLE_COUNTING))
        {
            videoUrl = "http://www.youtube.com/embed/84h9IztCyDI?rel=0";
        }

        else if (requestType.equals(VcaType.CROWD_DETECTION))
        {
            videoUrl = "http://www.youtube.com/embed/aWkuWqfrpLo?rel=0";
        }

        else if (requestType.equals(VcaType.AUDIENCE_PROFILING))
        {
            videoUrl = "http://www.youtube.com/embed/s1gfxXVNw7A?rel=0";
        }

        else if (requestType.equals(VcaType.AREA_INTRUSION))
        {
            videoUrl = "http://www.youtube.com/embed/iNSyvH1yVwI?rel=0";
        }

        else if (requestType.equals(VcaType.PERIMETER_DEFENSE))
        {
            videoUrl = "http://www.youtube.com/embed/CxJjK610nPI?rel=0";
        }

        else if (requestType.equals(VcaType.AREA_LOITERING))
        {
            videoUrl = "http://www.youtube.com/embed/fWdjGTP4fG8?rel=0";
        }

        else if (requestType.equals(VcaType.OBJECT_COUNTING))
        {
            videoUrl = "http://www.youtube.com/embed/QLbgYU3CNgQ?rel=0";
        }

        else if (requestType.equals(VcaType.VIDEO_BLUR))
        {
            videoUrl = "http://www.youtube.com/embed/JoR7hBTpGyQ?rel=0";
        }


        if (videoUrl.isEmpty())
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/content/no_content.html", videoUrl);
        }
        else
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/content/help_video.html", videoUrl);
        }
    }

    /**
     * According to bucket id and return the custom bucket logo from bucket setting.
     *
     * @param id Bucket id
     *
     * @responseBinary return image in binary type
     */
    public static void bucketlogo(String id)
    {

        File defaultLogoFile = new File(Play.applicationPath +
                                        (String) renderArgs.get("CdnPath") +
                                        "/common/images/logo.png");
        try
        {
            if (id == null || id.isEmpty())
            {
                renderBinary(defaultLogoFile);
            }

            BucketSetting bktSettings = BucketManager.getInstance().getBucketSetting(id);
            File logoFile = bktSettings.getBucketLogo();
            renderBinary(logoFile == null ? defaultLogoFile : logoFile);

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            renderBinary(defaultLogoFile);
        }
    }

}
