package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.BucketSetting;
import models.MongoBucket;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import platform.BucketManager;
import platform.db.gridfs.GridFsHelper;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;

import java.io.FileInputStream;
import java.io.InputStream;

@With(WebInterceptor.class)
public class bucket extends Controller
{
    public static void add()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/bucket/add.html");
    }

    public static void edit(String id)
    {
        String targetBucketId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/bucket/edit.html", targetBucketId);
    }

    public static void assignfeatures(String id)
    {
        try
        {
            String targetBucketId = id;
            MongoBucket targetBucket = MongoBucket.getById(targetBucketId);
            String parentBucketId = targetBucket.getParentId();
            renderTemplate(renderArgs.get("HtmlPath") + "/bucket/assign_features.html", targetBucketId, parentBucketId);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            notFound();
        }
    }

    public static void listUsers(String id)
    {
        String bucketId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/bucket/listUsers.html", bucketId);
    }

    public static void settings(String id)
    {

        String path = Play.applicationPath + (String) renderArgs.get("CdnPath") + "/common/images/logo.png";
        try
        {
            String binaryData = "";
            boolean customLogo = false;
            BucketSetting bucketSetting = BucketManager.getInstance().getBucketSetting(id);

            //Get Default bucket logo
            InputStream defaultLogo = new FileInputStream(path);
            String defaultBinaryData = Base64.encodeBase64String(IOUtils.toByteArray(defaultLogo));

            //Get custom bucket logo
            if (bucketSetting.logoBlobId != null && !bucketSetting.logoBlobId.isEmpty())
            {
                customLogo = true;
                binaryData = GridFsHelper.getBlobAsBase64String(bucketSetting, BucketSetting.BUCKET_LOGO_BLOB_GRIDFS, bucketSetting.logoBlobId);
            }
            renderTemplate(renderArgs.get("HtmlPath") +
                           "/bucket/settings.html", bucketSetting, binaryData, defaultBinaryData, customLogo);
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
            notFound();
        }
    }

    public static void selector()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/bucket/bucket_selector.html");
    }

    public static void manager()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/bucket/manager.html");
    }
}
