package platform.notification;

/**
 * @author Aye Maung
 * @since v4.5
 */
public enum NotificationSource
{
    CAMERA,
    LABEL;

    public static NotificationSource parse(String typeName)
    {
        for (NotificationSource source : values())
        {
            if (source.name().equalsIgnoreCase(typeName))
            {
                return source;
            }
        }

        throw new IllegalArgumentException(typeName);
    }
}
