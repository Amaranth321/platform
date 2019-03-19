package platform.content.mobile.push;

import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class PushMessage
{
    private final String subject;
    private final PushMessageType messageType;
    private final String[] dataValues;

    public PushMessage(String subject, PushMessageType messageType, String... dataValues)
    {
        this.subject = subject;
        this.messageType = messageType;
        this.dataValues = dataValues;
    }

    public PushMessageType getMessageType()
    {
        return messageType;
    }

    public String getSubject()
    {
        return subject;
    }

    public Map<String, String> getFormattedMap()
    {
        return messageType.formatPushData(dataValues);
    }

}
