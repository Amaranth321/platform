package platform.content.mobile.sms;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class SMSItem
{
    private final String senderName;
    private final String recipientNumber;
    private final String text;

    public SMSItem(String senderName, String recipientNumber, String text)
    {
        if (!recipientNumber.startsWith("+"))
        {
            recipientNumber = "+" + recipientNumber;
        }

        this.senderName = senderName;
        this.recipientNumber = recipientNumber;
        this.text = text;
    }

    public String getSenderName()
    {
        return senderName;
    }

    public String getRecipientNumber()
    {
        return recipientNumber;
    }

    public String getText()
    {
        return text;
    }

    @Override
    public String toString()
    {
        return String.format("to %s from %s)", recipientNumber, senderName);
    }
}
