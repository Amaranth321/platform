package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import platform.devices.NodeEnv;
import platform.nodesoftware.SoftwareManager;
import play.Logger;
import play.modules.morphia.Model;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class SoftwareUpdateFile extends Model
{
    @Indexed
    private final String fileServerId; //checksum

    @Indexed
    private final long modelId;

    private final String version;
    private final double releaseNumber;
    private final String host;
    private final int port;
    private final long fileSize;
    private final long uploadedTime;

    /**
     * This function is necessary to prevent OTA from jumping releases (e.g. 4.3 => 4.5, instead of 4.3 => 4.4 => 4.5)
     *
     * @param modelId       model Id.
     * @param nodeReleaseNo Use ({@link models.node.NodeObject#getReleaseNumber()})
     *                      It must be the current release no of the node. The closest latest file will be returned.
     *                      e.g. 4.3 node => 4.3 latest file, 4.4 node => 4.4 latest file
     *
     * @return the closest latest update file based on the modelId and release number provided.
     */
    public static SoftwareUpdateFile findEligibleUpdate(Long modelId, double nodeReleaseNo)
    {
        //don't allow OTA for very old nodes
        if (modelId == null || nodeReleaseNo < 4.3)
        {
            return null;
        }

        TreeMap<Double, SoftwareUpdateFile> sortedFileMap = getSortedLatestFileMap(modelId);

        /**
         * Don't let ingrasys nodes update to 4.6 and above because 4.6 requires ubuntu 14.04
         *
         * todo: remove when igrasys nodes are not in production anymore
         */
        if (modelId.equals(115L))
        {
            if (nodeReleaseNo > 4.5)
            {
                Logger.error("model 115 node version should not be higher than 4.5");
                return null;
            }

            if (nodeReleaseNo == 4.5)
            {
                return sortedFileMap.get(4.5);
            }

            return sortedFileMap.ceilingEntry(nodeReleaseNo + 0.1).getValue();
        }

        //check the availability of the next closest release first
        Double newerReleaseLatest = sortedFileMap.ceilingKey(nodeReleaseNo + 0.1);
        if (newerReleaseLatest != null)
        {
            return sortedFileMap.get(newerReleaseLatest);
        }

        //check update file in the same release
        return sortedFileMap.get(nodeReleaseNo);
    }

    /**
     * @param modelId
     *
     * @return the latest available version for the given model
     */
    public static SoftwareUpdateFile getLatestUploaded(long modelId)
    {
        TreeMap<Double, SoftwareUpdateFile> sortedFileMap = getSortedLatestFileMap(modelId);
        if (sortedFileMap.isEmpty())
        {
            return null;
        }
        return sortedFileMap.lastEntry().getValue();
    }

    public static SoftwareUpdateFile findByServerId(String fileServerId)
    {
        return q().filter("fileServerId", fileServerId).first();
    }

    /**
     * This returns a TreeMap of latest update files for each release.
     * The sorting is by the oldest release first (e.g. 4.3, 4.4, ...)
     */
    private static TreeMap<Double, SoftwareUpdateFile> getSortedLatestFileMap(long modelId)
    {
        NodeEnv nodeEnv = NodeEnv.of(modelId);
        if (nodeEnv == null)
        {
            return new TreeMap<>();
        }

        //Currently update files for ubuntu nodes do not have modelId.
        //So, modelId for Ubuntu nodes is always set as 115L for update files
        if (nodeEnv == NodeEnv.UBUNTU)
        {
            modelId = SoftwareManager.NODE_UBUNTU_MODEL_ID;
        }

        SoftwareManager softwareMgr = SoftwareManager.getInstance();
        TreeMap<Double, SoftwareUpdateFile> sortedUniqueMap = new TreeMap<>();
        Set<?> uniqueReleaseSet = SoftwareUpdateFile.q().filter("modelId", modelId).distinct("releaseNumber");
        for (Object obj : uniqueReleaseSet)
        {
            Double releaseNo = ((Double) obj);
            List<SoftwareUpdateFile> dbFiles = SoftwareUpdateFile.q()
                    .filter("modelId", modelId)
                    .filter("releaseNumber", releaseNo)
                    .asList();

            //find the latest file
            SoftwareUpdateFile latestFile = null;
            for (SoftwareUpdateFile currentFile : dbFiles)
            {
                if (latestFile == null ||
                    softwareMgr.compareVersions(currentFile.getVersion(), latestFile.getVersion()) > 0)
                {
                    latestFile = currentFile;
                }
            }

            if (latestFile == null)
            {
                continue;
            }

            sortedUniqueMap.put(releaseNo, latestFile);
        }

        return sortedUniqueMap;
    }

    public SoftwareUpdateFile(String fileServerId,
                              long modelId,
                              String version,
                              String host,
                              int port,
                              long fileSize,
                              long uploadedTime)
    {
        this.fileServerId = fileServerId;
        this.modelId = modelId;
        this.version = version;
        this.releaseNumber = SoftwareManager.getInstance().getReleaseNumber(version);
        this.host = host;
        this.port = port;
        this.fileSize = fileSize;
        this.uploadedTime = uploadedTime;
    }

    public String getFileServerId()
    {
        return fileServerId;
    }

    public long getModelId()
    {
        return modelId;
    }

    public String getVersion()
    {
        return version;
    }

    public double getReleaseNumber()
    {
        return releaseNumber;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public long getUploadedTime()
    {
        return uploadedTime;
    }

}
