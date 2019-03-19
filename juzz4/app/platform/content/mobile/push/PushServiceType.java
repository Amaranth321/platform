package platform.content.mobile.push;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum PushServiceType
{
    GCM,
    APNS;

    public MobilePushClient getClient()
    {
        switch (this)
        {
            case GCM:
                return new GCMClient();

            case APNS:
                return new APNSClient();

            default:
                throw new IllegalArgumentException(this.toString());
        }
    }
}
