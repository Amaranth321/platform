package platform.analytics;

/**
 * @author Aye Maung
 * @since v4.5
 */
public enum VcaStatus
{
    WAITING,
    RUNNING,
    DISABLED,
    NOT_SCHEDULED,
    ERROR;

    public static VcaStatus parse(String statusString)
    {
        for (VcaStatus vcaStatus : values())
        {
            if (vcaStatus.name().equals(statusString))
            {
                return vcaStatus;
            }
        }

        throw new IllegalArgumentException();
    }

    public static boolean isOldStatus(String statusString)
    {
        return "EXITED".equals(statusString) ||
               "CMDERR".equals(statusString);
    }

    public static VcaStatus migrate(String statusString, boolean isVcaEnabled)
    {
        if (statusString.equals("EXITED"))
        {
            //the actual schedule is not checked here
            //due to the unreliable availability of the timezone info on cloud
            return !isVcaEnabled ? DISABLED : NOT_SCHEDULED;
        }

        if (statusString.equals("CMDERR"))
        {
            return ERROR;
        }

        throw new UnsupportedOperationException("only old statuses can be migrated");
    }
}
