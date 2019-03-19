package platform.content;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import play.Logger;
import play.jobs.Job;
import play.libs.F;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class ZipHelper
{
    private static final ZipHelper instance = new ZipHelper();

    public static ZipHelper getInstance()
    {
        return instance;
    }

    public static ZipParameters getDefaultParameters()
    {
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        return parameters;
    }

    /**
     * @param zipFilename output file (with absolute path)
     * @param localFiles  files to be zipped
     */
    public File zip(String zipFilename, List<File> localFiles) throws ZipException
    {
        ZipFile zipFile = new ZipFile(zipFilename);
        ZipParameters parameters = getDefaultParameters();
        zipFile.addFiles(new ArrayList<>(localFiles), parameters);
        return zipFile.getFile();
    }

    /**
     * @param zipFilename output file (with absolute path)
     * @param localFiles  files to be zipped
     */
    public F.Promise<File> zipAsync(final String zipFilename, final List<File> localFiles)
    {
        Job<File> zipJob = new Job<File>()
        {
            @Override
            public File doJobWithResult()
            {
                try
                {
                    return zip(zipFilename, localFiles);
                }
                catch (ZipException e)
                {
                    Logger.error(e, "");
                    return null;
                }
            }
        };

        return zipJob.now();
    }

    private ZipHelper()
    {
    }

}
