package platform.content.delivery;

import models.BannedEmail;
import platform.content.email.EmailItem;
import platform.content.ftp.FTPItem;
import platform.content.mobile.push.PushNotificationItem;
import platform.content.mobile.sms.SMSItem;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.3
 */
public enum DeliveryMethod
{
    EMAIL,
    FTP,
    SMS,
    MOBILE_PUSH;

    public boolean hasValidItem(Deliverable deliverable)
    {
        if (deliverable == null)
        {
            return false;
        }

        Object item = deliverable.getDetails();
        switch (this)
        {
            case EMAIL:
                return isValidEmailItem(item);

            case FTP:
                return item instanceof FTPItem;

            case SMS:
                return item instanceof SMSItem;

            case MOBILE_PUSH:
                return item instanceof PushNotificationItem;

            default:
                return false;
        }
    }

    private boolean isValidEmailItem(Object item)
    {
        if (!(item instanceof EmailItem))
        {
            return false;
        }

        EmailItem emailItem = (EmailItem) item;
        if (BannedEmail.isBanned(emailItem.getRecipientEmail()))
        {
            Logger.error("'%s' email is banned (Subject:%s).",
                         emailItem.getRecipientEmail(),
                         emailItem.getSubject());
            return false;
        }

        return true;
    }
}
