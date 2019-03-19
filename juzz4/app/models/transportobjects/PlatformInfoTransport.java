package models.transportobjects;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class PlatformInfoTransport
{
    public final long serverStartTime;
    public final String applicationType;
    public final double releaseNumber;
    public final String commitHash;
    public final String releaseTag;

    public PlatformInfoTransport(long serverStartTime,
                                 String applicationType,
                                 double releaseNumber,
                                 String commitHash,
                                 String releaseTag)
    {
        this.serverStartTime = serverStartTime;
        this.applicationType = applicationType;
        this.releaseNumber = releaseNumber;
        this.commitHash = commitHash;
        this.releaseTag = releaseTag;
    }
}
