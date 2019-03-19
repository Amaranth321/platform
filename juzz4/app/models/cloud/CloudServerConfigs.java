package models.cloud;

import com.google.code.morphia.annotations.Embedded;
import platform.config.RetentionDays;

/**
 * Add Cloud configurations only
 * <p/>
 * Add more internal classes if needed, but try to group similar items together
 * <p/>
 * All configs here must be at the server level, not for each bucket.
 * <p/>
 * Do not modify existing field names. Only add new ones.
 * <p/>
 * Do not forget to add new keys in message files. You must append "config-" to the field name. (e.g. config-delivery)
 *
 * @author Aye Maung
 * @since v4.5
 */
@Embedded
public class CloudServerConfigs
{
    private Delivery delivery;
    private FeatureControls featureControls;
    private RetentionDays retentionDays;
    private Access access;
    private ExternalAccounts externalAccounts;
    private KeyValues keyValues;
    private Miscellaneous miscellaneous;

    public CloudServerConfigs()
    {
        initializeNullFields();
    }

    public boolean initializeNullFields()
    {
        boolean changed = false;
        if (delivery == null)
        {
            delivery = new Delivery();
            changed = true;
        }
        if (featureControls == null)
        {
            featureControls = new FeatureControls();
            changed = true;
        }
        if (retentionDays == null)
        {
            retentionDays = RetentionDays.forNewCloudServer();
            changed = true;
        }
        if (access == null)
        {
            access = new Access();
        }
        if (externalAccounts == null)
        {
            externalAccounts = new ExternalAccounts();
        }
        if (keyValues == null)
        {
            keyValues = new KeyValues();
        }
        if (miscellaneous == null)
        {
            miscellaneous = new Miscellaneous();
        }

        return changed;
    }

    //keep all updates in one place for consistency
    public void update(CloudServerConfigs newConfigs)
    {
        this.delivery = newConfigs.delivery;
        this.featureControls = newConfigs.featureControls;
        this.retentionDays = newConfigs.retentionDays;
        this.access = newConfigs.access;
        this.externalAccounts = newConfigs.externalAccounts;
        this.keyValues = newConfigs.keyValues;
        this.miscellaneous = newConfigs.miscellaneous;
    }

    public Delivery delivery()
    {
        return delivery;
    }

    public FeatureControls featureControls()
    {
        return featureControls;
    }

    public RetentionDays retentionDays()
    {
        return retentionDays;
    }

    public Access access()
    {
        return access;
    }

    public ExternalAccounts externalAccounts()
    {
        return externalAccounts;
    }

    public KeyValues keyValues()
    {
        return keyValues;
    }

    public Miscellaneous miscellaneous()
    {
        return miscellaneous;
    }

    public static class Delivery
    {
        public final boolean allowEmail;
        public final boolean allowSms;
        public final boolean allowMobilePush;
        public final boolean allowFtp;

        public Delivery()
        {
            //default values
            this.allowEmail = false;
            this.allowSms = false;
            this.allowMobilePush = false;
            this.allowFtp = false;
        }
    }

    public static class FeatureControls
    {
        public final boolean allowWeeklySummaryEmails;

        public FeatureControls()
        {
            //default values
            this.allowWeeklySummaryEmails = false;
        }
    }

    public static class Access
    {
        public final int incorrectLoginLockMins;
        public final int defaultSessionTimeOutMins;
        public final boolean forceHttps;

        public Access()
        {
            //default values
            this.forceHttps = false;
            this.incorrectLoginLockMins = 15;
            this.defaultSessionTimeOutMins = 1440;
        }
    }

    public static class ExternalAccounts
    {
        public final String machSmsUserName;
        public final String machSmsPassword;
        public final String apnsPassword;

        public ExternalAccounts()
        {
            //default values
            this.machSmsUserName = "";
            this.machSmsPassword = "";
            this.apnsPassword = "kaisquare";
        }
    }

    public static class KeyValues
    {
        public final String gcmApiKey;
        public final int piwikServerId;

        public KeyValues()
        {
            //default values
            this.gcmApiKey = "AIzaSyBBEdIFfsc8Cqt7BFG1ZoxflGfefhNL5fc";
            this.piwikServerId = 0;
        }
    }

    public static class Miscellaneous
    {
        public final boolean simulateVcaData;

        public Miscellaneous()
        {
            //default values
            this.simulateVcaData = false;
        }
    }
}

