package platform.notification;

import models.cloud.CloudServerConfigs;
import models.cloud.UIConfigurableCloudSettings;
import platform.Environment;
import play.i18n.Messages;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum NotifyMethod
{
    ON_SCREEN,
    EMAIL,
    SMS,
    MOBILE_PUSH;

    public static Set<NotifyMethod> getServerEnabledMethods()
    {
        if (Environment.getInstance().onKaiNode())
        {
            return new LinkedHashSet<>(Arrays.asList(NotifyMethod.ON_SCREEN));
        }

        CloudServerConfigs.Delivery deliveryConfigs = UIConfigurableCloudSettings.server().delivery();
        Set<NotifyMethod> enabledList = new LinkedHashSet<>();
        for (NotifyMethod notifyMethod : values())
        {
            switch (notifyMethod)
            {
                case ON_SCREEN:
                    enabledList.add(notifyMethod);
                    continue;

                case EMAIL:
                    if (deliveryConfigs.allowEmail)
                    {
                        enabledList.add(notifyMethod);
                    }
                    continue;

                case SMS:
                    if (deliveryConfigs.allowSms)
                    {
                        enabledList.add(notifyMethod);
                    }
                    continue;

                case MOBILE_PUSH:
                    if (deliveryConfigs.allowMobilePush)
                    {
                        enabledList.add(notifyMethod);
                    }
                    continue;
            }
        }

        return enabledList;
    }

    public String localizedName()
    {
        String messageKey = "";
        switch (this)
        {
            case ON_SCREEN:
                messageKey = "method-on-screen";
                break;
            case EMAIL:
                messageKey = "method-email";
                break;
            case SMS:
                messageKey = "method-sms";
                break;
            case MOBILE_PUSH:
                messageKey = "method-mobile-push";
                break;
        }
        return messageKey.isEmpty() ? "" : Messages.get(messageKey);
    }
}
