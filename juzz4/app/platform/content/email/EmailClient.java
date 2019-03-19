package platform.content.email;

import com.mongodb.gridfs.GridFSDBFile;
import models.BannedEmail;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsHelper;
import play.Logger;
import play.Play;
import play.libs.Mail;

import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class EmailClient
{
    private static final long TIME_OUT_SECONDS = 20L;

    private final EmailItem emailItem;

    public EmailClient(EmailItem emailItem)
    {
        this.emailItem = emailItem;
    }

    public boolean send()
    {
        try
        {
            //read sender info from play configuration
            String emailFrom = Play.configuration.getProperty("mail.smtp.from");
            String emailFromText = Play.configuration.getProperty("mail.smtp.fromText");

            //set info
            HtmlEmail email = new HtmlEmail();
            email.setCharset("utf-8");
            email.addTo(emailItem.getRecipientEmail());
            email.setFrom(emailFrom, emailFromText);
            email.setSubject(emailItem.getSubject());
            email.setHtmlMsg(emailItem.getHtmlMsg());

            //set attachments
            for (GridFsDetails gridFsDetails : emailItem.getAttachments())
            {
                GridFSDBFile gridFile = GridFsHelper.getGridFSDBFile(gridFsDetails);
                String contentType = gridFsDetails.getFormat().getContentType();
                ByteArrayDataSource bad = new ByteArrayDataSource(gridFile.getInputStream(), contentType);
                email.attach(bad, gridFile.getFilename(), "description", EmailAttachment.ATTACHMENT);
            }

            //send and wait
            boolean result = Mail.send(email).get(TIME_OUT_SECONDS, TimeUnit.SECONDS);
            BannedEmail.recordResult(emailItem.getRecipientEmail(), result);
            return result;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            BannedEmail.recordResult(emailItem.getRecipientEmail(), false);
            return false;
        }
    }
}
