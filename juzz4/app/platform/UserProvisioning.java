package platform;

import com.kaisquare.util.Hash;
import models.*;
import platform.config.readers.ConfigsShared;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.access.LoginAttempt;
import models.access.PasswordResetKey;
import models.access.UserSession;
import org.apache.commons.mail.HtmlEmail;
import platform.access.UserSessionManager;
import play.Logger;
import play.Play;
import play.i18n.Messages;
import play.libs.Mail;
import play.templates.Template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UserProvisioning
{
    /**
     * @param bucket   bucket name
     * @param username user name
     * @param password password
     * @param validity The session validity period (in seconds). Zero to use default.
     *
     * @return Session
     */
    public static UserSession login(String bucket, String username, String password, String remoteIP, String userAgent, boolean rememberMe) throws ApiException
    {
        // validate
        MongoBucket targetBucket = MongoBucket.getByName(bucket);
        if (targetBucket == null || targetBucket.isDeleted())
        {
            return null;
        }
        Pattern usernamePattern = Pattern.compile("^" + username + "$", Pattern.CASE_INSENSITIVE);
        MongoUser user = MongoUser.q()
                .filter("bucketId", targetBucket.getBucketId())
                .filter("login", usernamePattern)
                .get();
        if (user == null)
        {
            return null;
        }

        // check if suspended
        if (targetBucket.isSuspended() || !user.isActivated())
        {
            throw new ApiException(Messages.get("msg-account-suspended"));
        }

        //check password
        String salt = user.getCreationTimestamp();
        String passwordHash = Hash.sha256(password + salt);
        if (!user.getPassword().equals(passwordHash))
        {
            return null;
        }
        return UserSessionManager.getInstance().createSession(user.getUserId(), remoteIP, userAgent, rememberMe);
    }

    /**
     * Verifies the forgot password information.
     *
     * @param bucket   The bucket name
     * @param username The user login name
     * @param email    The user's email address
     *
     * @return On successful verification, returns the verified User object list. On
     * failure, returns empty list.
     */
    public static List<MongoUser> verifyForgotPasswordInfo(String bucket, String username, String email) throws ApiException
    {
    	try
        {
            if (email == null || email.isEmpty())
            {
                MongoBucket targetBucket = MongoBucket.getByName(bucket);
                if (targetBucket == null)
                {
                    Logger.warn("Bucket doesn't exist");
                    return new ArrayList();
                }

                return MongoUser.q()
                        .filter("bucketId", targetBucket.getBucketId())
                        .filter("login", username)
                        .fetchAll();
            }
            else
            {
                return MongoUser.q().filter("email", email).fetchAll();
            }
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
            e.printStackTrace();
            return new ArrayList();
        }
    }

    /**
     * Sends out password reset email using the configured email agent.
     *
     * @param user The user object whose password is to be reset.
     *
     * @return true on success, false otherwise.
     */
    public static boolean sendPasswordResetEmail(Template emailTemplate, String userId, String baseUrl) throws ApiException
    {
        try
        {
            MongoUser user = MongoUser.getById(userId);
            MongoBucket bucket = MongoBucket.getById(user.getBucketId());

            // 1. create password reset key
            String resetKey = PasswordResetManager.getInstance().createPasswordResetKey(Long.parseLong(user.getUserId()), Long.parseLong(bucket.getBucketId()));

            // 2. construct password reset link
            baseUrl = Util.isNullOrEmpty(baseUrl)
                    ? Play.configuration.getProperty("application.baseUrl")
                    : baseUrl;
            String resetLink = baseUrl + "/api/verifyresetkey/" + resetKey;

            // 3. render email content
            Map emailArgs = new LinkedHashMap();
            emailArgs.put("recipient", user.getName());
            emailArgs.put("sender", "System Admin");
            emailArgs.put("resetLink", resetLink);
            emailArgs.put("baseUrl", baseUrl);
            emailArgs.put("username", user.getLogin());
            emailArgs.put("bucket", bucket.getName());
            String emailHtmlContent = emailTemplate.render(emailArgs);

            //4. configure and send email
            String emailFrom = Play.configuration.getProperty("mail.smtp.from");
            String emailFromText = Play.configuration.getProperty("mail.smtp.fromText");
            HtmlEmail email = new HtmlEmail();
            email.setCharset("utf-8");
            email.addTo(user.getEmail(), user.getName());
            email.setFrom(emailFrom, emailFromText);
            email.setSubject("(" + bucket.getName() + ") Password Reset");
            email.setHtmlMsg(emailHtmlContent);

            Mail.send(email);
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifies if the provided reset key is valid or not.
     *
     * @param resetKey The reset key to verify.
     *
     * @return true if key is valid, false otherwise.
     */
    public static boolean verifyResetKey(String resetKey) throws ApiException
    {
        try
        {
            platform.PasswordResetManager prm = platform.PasswordResetManager.getInstance();
            return prm.isKeyValid(resetKey);
        }
        catch (Exception e)
        {
            Logger.warn(e.getMessage());
            return false;
        }
    }

    /**
     * Resets the password of a user account if the provided reset key is
     * authentic.
     *
     * @param resetKey    The reset key.
     * @param newPassword The new password.
     *
     * @return true on success, false otherwise.
     *
     * @note The reset key is a one time use key. It is discarded after 1
     * successful use.
     */
    public static boolean resetPasswordWithKey(String resetKey, String newPassword) throws ApiException
    {
        try
        {
            PasswordResetKey keyObj = PasswordResetManager.getInstance().getKeyDetails(resetKey);
            if (keyObj == null)
            {
                return false;
            }

            MongoUser user = MongoUser.getById(keyObj.userId.toString());
            String salt = user.getCreationTimestamp();
            user.setPassword(Hash.sha256(newPassword + salt));

            //Password policy check
            new BucketPasswordPolicy(Long.parseLong(user.getBucketId())).findOrCreate().validatePasswordPolicyByUser(Long.parseLong(user.getUserId()), newPassword, user.getPassword());
            user.save();

            //Save password history
            UserPasswordHistory userPasswordHistory = new UserPasswordHistory(Long.parseLong(user.getUserId()), user.getPassword(), false);
            userPasswordHistory.save();

            //one time key is used, invalidate it
            PasswordResetManager.getInstance().invalidateKey(resetKey);

            return true;
        }
        catch (Exception e)
        {
            Logger.warn(e.getMessage());
            throw new ApiException(e.getMessage());
        }
    }

    /**
     * Resets the password of a user account if the provided reset key is
     * authentic.
     *
     * @param userId      The user ID.
     * @param oldPassword The reset key.
     * @param newPassword The new password.
     *
     * @return true on success, false otherwise.
     *
     * @note The reset key is a one time use key. It is discarded after 1
     * successful use.
     */
    public static boolean resetPasswordWithOldPassword(String userId, String oldPassword, String newPassword) throws ApiException
    {
        try
        {
            MongoUser user = MongoUser.getById(userId);

            String salt = user.getCreationTimestamp();
            String oldPasswordHash = Hash.sha256(oldPassword + salt);
            if (!user.getPassword().equals(oldPasswordHash))
            {
                return false;
            }

            user.setPassword(Hash.sha256(newPassword + salt));

            //Password policy check
            new BucketPasswordPolicy(Long.parseLong(user.getBucketId())).findOrCreate().validatePasswordPolicyByUser(Long.parseLong(user.getUserId()), newPassword, user.getPassword());
            user.save();

            //Save password history
            UserPasswordHistory userPasswordHistory = new UserPasswordHistory(Long.parseLong(user.getUserId()), user.getPassword(), false);
            userPasswordHistory.save();

            return true;
        }
        catch (Exception e)
        {
            Logger.warn(e.getMessage());
            throw new ApiException(e.getMessage());
        }
    }

    /**
     * Returns the specified user's profile.
     *
     * @param userId The user ID.
     *
     * @return User object on success, null otherwise.
     */
    public static MongoUser getUserProfile(String userId) throws ApiException
    {
        try
        {
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("invalid-user-id"); //this should not happen
            }
            return user;
        }
        catch (Exception e)
        {
            Logger.warn(e.getMessage() + " (%s)", userId);
            Logger.error(Util.getStackTraceString(e));
            throw e;
        }
    }

    /**
     * Returns the specified user's preferences object. If preferences object is
     * not found in database, it is created with default values.
     *
     * @param userId The user ID.
     *
     * @return UserPrefs object on success, null otherwise.
     */
    public static MongoUserPreference getUserPreference(String userId) throws ApiException
    {
        MongoUserPreference userPreference = MongoUserPreference.q().filter("userId", userId).get();

        // create new if not exist
        if (userPreference == null)
        {
            userPreference = new MongoUserPreference();
            userPreference.setUserId(userId);
            userPreference.save();
        }

        return userPreference;
    }

    public static boolean setUserPreference(String userId, String slotSettingAssignments, String duration, String autoRotation, String autoRotationTime, String POSFakeDataEnabled, String theme) throws ApiException
    {
        try
        {
            MongoUserPreference userPref = MongoUserPreference.q().filter("userId", userId).get();

            if (userPref == null)
            {
                throw new ApiException("User preferences not found");
            }
            if (!Util.isNullOrEmpty(slotSettingAssignments))
            {
                userPref.setSlotSettingAssignments(slotSettingAssignments);
            }
            if (!Util.isNullOrEmpty(duration))
            {
                userPref.setDuration(Long.parseLong(duration) * 24 * 60);
            }
            if (!Util.isNullOrEmpty(autoRotation))
            {
                userPref.setAutoRotation(Boolean.parseBoolean(autoRotation));
            }
            if (!Util.isNullOrEmpty(autoRotationTime))
            {
                userPref.setAutoRotationTime(Long.parseLong(autoRotationTime));
            }
            if (!Util.isNullOrEmpty(POSFakeDataEnabled))
            {
                userPref.setPosFakeDataEnabled(Boolean.parseBoolean(POSFakeDataEnabled));
            }
            if (!Util.isNullOrEmpty(theme))
            {
                userPref.setTheme(theme);
            }
            userPref.save();
            return true;
        }
        catch (NumberFormatException e)
        {
            Logger.warn(e.getMessage());
            throw new ApiException("Invalid parameter");
        }
    }

    public static Map checkLockedHistory(String ipAddress)
    {
        try
        {
            Long nowMillis = Environment.getInstance().getCurrentUTCTimeMillis();
            Long lockDuration = getLoginLockDuration();

            LoginAttempt prevAttempt = LoginAttempt.find("remoteIp", ipAddress).first();
            if (prevAttempt == null || !prevAttempt.locked)
            {
                return new LinkedHashMap();
            }

            //check if lock expired
            Long lockExpiry = prevAttempt.lastTried + lockDuration;
            Map attemptInfo = new LinkedHashMap();
            if (nowMillis > lockExpiry)
            {
                prevAttempt.delete();
            }
            else
            {
                attemptInfo.put("reason", "msg-account-locked");
                attemptInfo.put("locked-time-remaining", lockExpiry - nowMillis);
            }

            return attemptInfo;

        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
            return new LinkedHashMap();
        }
    }

    public static Map incrementLoginAttempt(String ipAddress)
    {
        try
        {
            int attemptLimit = 5;
            Long nowMillis = Environment.getInstance().getCurrentUTCTimeMillis();

            //check previous attempt
            LoginAttempt prevAttempt = LoginAttempt.find("remoteIp", ipAddress).first();
            if (prevAttempt == null)
            {
                prevAttempt = new LoginAttempt();
            }

            //update
            prevAttempt.lastTried = nowMillis;
            prevAttempt.remoteIp = ipAddress;
            prevAttempt.failCount++;
            prevAttempt.locked = prevAttempt.failCount.equals(attemptLimit);
            prevAttempt.save();

            Map attemptInfo = new LinkedHashMap();
            //for last try
            if (prevAttempt.locked)
            {
                attemptInfo.put("reason", "msg-account-locked");
                attemptInfo.put("locked-time-remaining", getLoginLockDuration());
            }
            else
            {
                attemptInfo.put("reason", "msg-incorrect-login");
                attemptInfo.put("login-attempt-remaining", attemptLimit - prevAttempt.failCount);
            }

            return attemptInfo;

        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return new LinkedHashMap();
        }
    }

    public static void clearLoginAttempts(String ipAddress)
    {
        try
        {
            LoginAttempt.find("remoteIp", ipAddress).delete();
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
        }
    }

    //returns duration in milliseconds
    private static long getLoginLockDuration()
    {
        return ConfigsShared.getInstance().incorrectLoginLockMins() * 60 * 1000L;
    }
}
