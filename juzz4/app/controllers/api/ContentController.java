package controllers.api;

import controllers.interceptors.APIInterceptor;
import models.reports.ExportedFile;
import play.mvc.With;

/**
 * @author Aye Maung
 * @since v4.4
 */
@With(APIInterceptor.class)
public class ContentController extends APIController
{

    public static void downloadexportedfile(String identifier)
    {
        ExportedFile exportedFile = ExportedFile.findByIdentifier(identifier);
        if (exportedFile == null)
        {
            notFound();
        }

        String bucket = renderArgs.get("bucket").toString();
        if (!exportedFile.getBucketName().equals(bucket))
        {
            forbidden();
        }

        respondDownloadFile(exportedFile.getFileDetails());
    }
}
