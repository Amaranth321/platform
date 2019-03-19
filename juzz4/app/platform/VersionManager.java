package platform;

import com.kaisquare.util.FileUtil;
import lib.util.Util;
import models.SystemVersion;
import play.Logger;

import java.io.File;

public class VersionManager
{
    public static final Double BEFORE_VERSIONING = 4.0;

    /**
     * Manually update majorReleaseNo and minorReleaseNo
     * Increment minorReleaseNo only when migration is required
     */
    private static final int majorReleaseNo = 4;
    private static final int minorReleaseNo = 6;

    private static VersionManager versionManager = null;

    private String codeCommitHash = null;
    private String releaseTag = null;

    private VersionManager()
    {
        File commitHashFile = new File("conf/platformcodeversion.txt");
        if (commitHashFile.exists())
        {
            codeCommitHash = FileUtil.readFile(commitHashFile);
        }

        File releaseTagFile = new File("conf/platformversion.txt");
        if (releaseTagFile.exists())
        {
            releaseTag = FileUtil.readFile(releaseTagFile);
        }
    }

    public static VersionManager getInstance()
    {
        synchronized (VersionManager.class)
        {
            if (versionManager == null)
            {
                versionManager = new VersionManager();
            }
        }

        return versionManager;
    }

    public void setPlatformVersion(Double platformVersion)
    {
        SystemVersion.setPlatformVersion(platformVersion);
    }

    /**
     * @return current version of the platform
     */
    public Double getPlatformVersion()
    {
        return SystemVersion.getPlatformVersion();
    }

    /**
     * @return latest version of the platform. This will be different from getPlatformVersion()
     * when platform is not migrated yet
     */
    public Double getLatestPlatformVersion()
    {
        String strVer = String.format("%s.%s", majorReleaseNo, minorReleaseNo);
        return Double.parseDouble(strVer);
    }

    public String getCompatibleNodeVersion()
    {
        return String.format("%s.%s.0.0", majorReleaseNo, minorReleaseNo);
    }

    public String getCodeCommitHash()
    {
        return codeCommitHash == null ? "" : codeCommitHash.trim();
    }

    public String getReleaseTag()
    {
        return releaseTag == null ? "" : releaseTag.trim();
    }

    public void printVersions()
    {
        Double platformVersion = getPlatformVersion();
        Logger.info("Platform version : %s", (platformVersion == null) ? "" : platformVersion);

        if (!Util.isNullOrEmpty(codeCommitHash))
        {
            Logger.info("Code Hash : %s", codeCommitHash.trim());
        }
    }
}
