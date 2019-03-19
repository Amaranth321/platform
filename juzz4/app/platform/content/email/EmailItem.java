package platform.content.email;

import platform.db.gridfs.GridFsDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class EmailItem
{
    private final String recipientEmail;
    private final String subject;
    private final String htmlMsg;
    private final List<GridFsDetails> attachments;

    public EmailItem(String recipientEmail, String subject, String htmlMsg, List<GridFsDetails> attachments)
    {
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.htmlMsg = htmlMsg;
        this.attachments = attachments;
    }

    public String getRecipientEmail()
    {
        return recipientEmail;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getHtmlMsg()
    {
        return htmlMsg;
    }

    public List<GridFsDetails> getAttachments()
    {
        return attachments == null ? new ArrayList<GridFsDetails>() : attachments;
    }

    @Override
    public String toString()
    {
        return String.format("To %s (%s attachments) : %s", recipientEmail, getAttachments().size(), subject);
    }
}
