package jobs.cloud;

import lib.util.exceptions.ApiException;
import models.BucketSetting;
import platform.BucketManager;
import play.Logger;
import play.jobs.Job;
import play.templates.Template;
import play.templates.TemplateLoader;

public class EmailVerificationOfUsers extends Job
{
    private String bucketId;
    private String userId;
    private String hostServer;

    public EmailVerificationOfUsers(String bkId, String usrId, String hostServer)
    {
        bucketId = bkId;
        userId = usrId;
        this.hostServer = hostServer;
    }

    @Override
    public void doJob() throws ApiException
    {
        BucketSetting bs = BucketManager.getInstance().getBucketSetting(bucketId);
        if (bs.emailVerificationOfUsersEnabled)
        {
            Template emailTmpl = TemplateLoader.load("kaisquare/common/templates/reset_password_email.html");
            platform.UserProvisioning.sendPasswordResetEmail(emailTmpl, userId, hostServer);
            Logger.debug("Bucket %s, User %s: new user verification email sent", bucketId, userId);
        }
        else
        {
            Logger.debug("Bucket %s not configured for new user email verification", bucketId);
        }
    }
}
