package controllers.api;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.kaisquare.util.Hash;
import controllers.interceptors.APIInterceptor;
import jobs.cloud.EmailVerificationOfUsers;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InternalException;
import models.*;
import models.access.UserSession;
import models.notification.BucketNotificationSettings;
import models.notification.UserNotificationSettings;
import platform.BucketManager;
import platform.DeviceManager;
import platform.FeatureManager;
import platform.UserProvisioning;
import platform.access.DefaultUser;
import platform.access.UserSessionManager;
import platform.content.export.ReportBuilder;
import platform.content.export.manual.UserListCsv;
import platform.db.cache.CacheClient;
import platform.events.EventType;
import platform.notification.NotifyMethod;
import play.i18n.Lang;
import play.mvc.With;

import java.lang.reflect.Type;
import java.util.*;

/**
 * @author KAI Square
 * @sectiontitle User Management
 * @sectiondesc APIs for managing user accounts and preferences.
 * @publicapi
 */

@With(APIInterceptor.class)
public class UserManagement extends APIController
{
    private static final Object NOTI_CHANGE_LOCK = new Object();

    private static class UserPreferenceSerializer implements JsonSerializer<MongoUserPreference>
    {
        @Override
        public JsonElement serialize(MongoUserPreference userPreference, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("APNSDeviceToken"         , userPreference.getApnsDeviceToken());
            obj.addProperty("GCMDeviceToken"          , userPreference.getGcmDeviceToken());
            obj.addProperty("POSFakeDataEnabled"      , userPreference.isPosFakeDataEnabled());
            obj.addProperty("autoRotation"            , userPreference.isAutoRotation());
            obj.addProperty("autoRotationTime"        , userPreference.getAutoRotationTime());
            obj.addProperty("duration"                , userPreference.getDuration());
            obj.addProperty("emailNotificationEnabled", userPreference.isEmailNotificationEnabled());
            obj.addProperty("id"                      , userPreference.getUserId());
            obj.addProperty("numberOfViews"           , userPreference.getUserId());
            obj.addProperty("pushNotificationEnabled" , userPreference.isPushNotificationEnabled());
            obj.addProperty("slotSettingAssignments"  , userPreference.getSlotSettingAssignments());
            obj.addProperty("smsNotificationEnabled"  , userPreference.isSmsNotificationEnabled());
            obj.addProperty("theme"                   , userPreference.getTheme());

            return obj;
        }
    }

    private static void checkBucketAccess(String callerBucketId, String targetBucketId) throws ApiException
    {
        MongoBucket callerBucket = MongoBucket.getById(callerBucketId);
        MongoBucket targetBucket = MongoBucket.getById(targetBucketId);
        if (!callerBucket.hasControlOver(targetBucket))
        {
            throw new ApiException("msg-no-rights-to-buckets");
        }
    }

    /**
     * @param user-name The user name to check. e.g Nischal. Mandatory
     *
     * @servtitle Checks the user name availability in current bucket.
     * @httpmethod GET
     * @uri /api/{bucket}/isusernameavailable
     * @responsejson {
     * "result": "yes"
     * }
     * @responsejson {
     * "result": "no"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void isusernameavailable(String bucket) throws ApiException
    {
        try
        {
            String bucketId = getCallerBucketId();
            if (bucketId == null || bucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            String username = readApiParameter("user-name", true);

            MongoUser existingUser = MongoUser.q()
                    .filter("bucketId", bucketId)
                    .filter("login", username)
                    .get();

            Map map = new ResultMap();
            map.put("result", (existingUser != null) ? "no" : "yes");

            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param name      The name of user.e.g Nischal Regmi. Mandatory
     * @param user-name The account login username e.g Nischal. Mandatory
     * @param email     The email address.e.g nregmi@alucio.com. Mandatory
     * @param password  The password e.g *****. Mandatory
     * @param labels    Tag labels for user e.g ["admin", "alucio"]
     * @param phone     The phone number of user. e.g +9779849094463 / 9779849094463. Mandatory
     * @param language  Preferred language of user. en, zh-tw, zn-ch
     *
     * @servtitle Adds user to the current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/adduser
     * @responsejson {
     * "result": "ok"
     * "id": <i>Id of currently added user</i>
     * }
     * @responsejson {
     * "result": "error"
     * "reason": "user-limit-reached"
     * }
     * @responsejson {
     * "result": "error"
     * "reason": "email-exists"
     * }
     * @responsejson {
     * "result": "error"
     * "reason": "unknown"
     * }
     */
    public static void adduser(String bucket) throws ApiException
    {
        try
        {
            String bucketId = getCallerBucketId();
            if (bucketId == null || bucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            if (!BucketManager.getInstance().checkBucketUserLimit(bucketId))
            {
                throw new ApiException("user-limit-reached");
            }

            String name = readApiParameter("name", true);
            String username = readApiParameter("user-name", true).toLowerCase();
            String password = readApiParameter("password", true);
            String email = readApiParameter("email", true);
            String labels = readApiParameter("labels", false);
            String phone = readApiParameter("phone", true);
            String language = readApiParameter("languange", false);

            // chech existing user (by email)
            MongoUser existingUser = MongoUser.q()
                    .filter("bucketId", bucketId)
                    .filter("email", email)
                    .get();

            if (existingUser != null)
            {
                throw new ApiException("email-exists");
            }

            //create new user object
            MongoUser user = new MongoUser(bucketId, name, username, "", email, phone, "en");
            user.setUserId(MongoUser.generateNewId());
            // password policy check
            String passwordHash = Hash.sha256(password + user.getCreationTimestamp());
            new BucketPasswordPolicy(Long.parseLong(user.getBucketId())).findOrCreate().validateBucketPasswordPolicy(password, passwordHash);

            user.setActivated(true);
            user.setBucketId(bucketId);
            user.setPassword(passwordHash);
            user.getServiceNames().addAll(FeatureManager.getInstance().getCommonServiceNames());

            user.save();
            
            //Save password history
            UserPasswordHistory userPasswordHistory = new UserPasswordHistory(Long.parseLong(user.getUserId()), user.getPassword(), true);
            userPasswordHistory.save();
            
            String[] labelArr = null;
            if (labels != null && !labels.isEmpty())
            {
                labelArr = labels.split(",");
            }
            if (labelArr != null)
            {
                for (String labelData : labelArr)
                {
                    models.UserLabel userlabel = new UserLabel();
                    userlabel.bucketId = Long.parseLong(bucketId);
                    userlabel.userId = Long.parseLong(user.getUserId());
                    userlabel.label = labelData;
                    userlabel.save();
                }
            }

            //enable notifications for new user
//            new NotificationDispatcher(user.getUserId()).now();

            //enable messaging for new user
//            new MessageDispatcher(user.getUserId()).now();

            // after create user and send email for verification
            new EmailVerificationOfUsers(bucketId, user.getUserId(), request.host).now();

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("id", user.getUserId());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param user-id   The id of user. Mandatory
     * @param name      The name of user.e.g Nischal Regmi. Mandatory
     * @param user-name The account login username e.g Nischal. Mandatory
     * @param email     The email address.e.g nregmi@alucio.com. Mandatory
     * @param password  The password e.g *****. Mandatory
     * @param labels    Tag labels for user e.g ["admin", "alucio"]
     * @param phone     The phone number of user. e.g +977-9849094463
     * @param language  Preferred language of user. e.g eng
     *
     * @servtitle Updates user details of the current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/updateuser
     * @responsejson {
     * "result": "ok",
     * "id": <i>Id of currently added user</i>
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "user-limit-reached"
     * }
     * @responsejson {
     * "result": "error",,
     * "reason": "email-exists"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "username-unavailable"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updateuser(String bucket) throws ApiException
    {
        try
        {
            String currentBucketId = getCallerBucketId();

            //verify user-id validity
            if (currentBucketId == null || currentBucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            String userId = readApiParameter("user-id", true);
            String name = readApiParameter("name", true);
            String username = readApiParameter("user-name", true);
            String password = readApiParameter("password", false);
            String email = readApiParameter("email", true);
            String labels = readApiParameter("labels", false);
            String phone = readApiParameter("phone", false);

            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("User ID is invalid");
            }

            //check access
            checkBucketAccess(currentBucketId, user.getBucketId());

            //check username availability
            List<MongoUser> existingLoginUsers = MongoUser.q()
                    .filter("id <>", user.getUserId())
                    .filter("login", username)
                    .filter("bucketId", user.getBucketId())
                    .fetchAll();

            if (existingLoginUsers != null && existingLoginUsers.size() > 0)
            {
                throw new ApiException("username-in-use");
            }

            //check if defaults
            if (DefaultUser.isDefault(user.getLogin()) && !user.getLogin().equals(username))
            {
                throw new ApiException("no-default-username-change");
            }

            //check email availability
            List<MongoUser> existingEmailUsers = MongoUser.q()
                    .filter("id <>", user.getUserId())
                    .filter("email", email)
                    .filter("bucketId", user.getBucketId())
                    .fetchAll();

            if (existingEmailUsers != null && existingEmailUsers.size() > 0)
            {
                throw new ApiException("email-exists");
            }

            // update
            user.setName(name);
            user.setEmail(email);
            user.setLogin(username);
            user.setPhone(phone);

            //update password only if a new one is specified, otherwise leave it unchanged
            if (password != null && !password.trim().isEmpty())
            {
                String salt = user.getCreationTimestamp();
                String passwordHash = Hash.sha256(password + salt);
                user.setPassword(passwordHash);

                //Password policy check
                new BucketPasswordPolicy(Long.parseLong(user.getBucketId())).findOrCreate().validatePasswordPolicyByUser(Long.parseLong(user.getUserId()), password, user.getPassword());
            }

            //save user object
            user.save();
            
            //Save password history
            UserPasswordHistory userPasswordHistory = new UserPasswordHistory(Long.parseLong(user.getUserId()), user.getPassword(), false);
            userPasswordHistory.save();
            
            String[] labelArr = null;
            if (!labels.isEmpty())
            {
                labelArr = labels.split(",");
            }

            //delete existing label of updating user
            models.UserLabel.q()
                    .filter("userId", Long.parseLong(user.getUserId()))
                    .filter("bucketId", Long.parseLong(user.getBucketId())).delete();
            if (labelArr != null)
            {
                for (String labelData : labelArr)
                {
                    models.UserLabel userlabel = new UserLabel();
                    userlabel.bucketId = Long.parseLong(user.getBucketId());
                    userlabel.userId = Long.parseLong(user.getUserId());
                    userlabel.label = labelData;
                    userlabel.save();
                }
            }

            //remove cached user
            CacheClient cacheClient = CacheClient.getInstance();
            cacheClient.remove(cacheClient.getUser(user.getUserId()));

            //remove cached sessions
            Iterable<UserSession> userSessions = UserSession.q().filter("userId", user.getUserId()).fetch();
            for (UserSession userSession : userSessions)
            {
                cacheClient.remove(cacheClient.getLoginSession(userSession.getSessionKey()));
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Updates all the unique user labels of current bucket
     * @httpmethod GET
     * @uri /api/{bucket}/getbucketuserlabels
     * @responsejson {
     * "result": "ok",
     * "labels": ["alucio", "superuser"]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketuserlabels() throws ApiException
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            List<String> userLabelNames = new ArrayList<>();
            List<UserLabel> userLabels = UserLabel.q().filter("bucketId", Long.parseLong(callerBucketId)).fetchAll();
            for (UserLabel userLabel : userLabels)
            {
                if (!userLabelNames.contains(userLabel.label))
                {
                    userLabelNames.add(userLabel.label);
                }
            }
            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("labels", userLabelNames);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param user-id The id of user. Mandatory
     *
     * @servtitle Remove a user from current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/removeuser
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "cannot-self-delete"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removeuser(String bucket) throws ApiException
    {
        try
        {
            String currentBucketId = getCallerBucketId();

            //verify user-id validity
            if (currentBucketId == null || currentBucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            String userId = readApiParameter("user-id", true);

            // verify delete your current user
            String currentUserId = getCallerUserId();
            if (currentUserId == null || currentUserId.isEmpty())
            {
                throw new InternalException("User ID is invalid, this should not happen!");
            }
            if (userId.equals(currentUserId))
            {
                throw new ApiException("cannot-self-delete");
            }

            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("User ID invalid");
            }
            if (DefaultUser.isDefault(user.getLogin()))
            {
                throw new ApiException("Deleting a default user is not allowed");
            }

            //check access
            checkBucketAccess(currentBucketId, user.getBucketId());

            //remove user from devices
            List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(user.getUserId());
            if (userDevices != null && userDevices.size() > 0)
            {
                for (MongoDevice userDevice : userDevices)
                {
                    userDevice.getUserIds().remove(user.getUserId());
                    userDevice.save();
                }
            }

            //delete user's preferences object
            UserProvisioning.getUserPreference(user.getUserId()).delete();

            //delete notification settings
            UserNotificationSettings.q().filter("userId", user.getUserId()).delete();

            //delete user labels
            UserLabel.q().filter("userId", user.getUserId()).filter("bucketId", user.getBucketId()).delete();

            //remove sessions
            UserSessionManager.getInstance().removeSessionsOfUser(user.getUserId());

            //finally delete user
            user.delete();

            //remove cache
            CacheClient cacheClient = CacheClient.getInstance();
            cacheClient.remove(cacheClient.getUser(user.getUserId()));

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param user-id The id of user to activate. Mandatory
     *
     * @servtitle Activates a user
     * @httpmethod POST
     * @uri /api/{bucket}/activateuser
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void activateuser(String bucket) throws ApiException
    {
        try
        {
            String currentBucketId = getCallerBucketId();

            //verify user-id validity
            if (currentBucketId == null || currentBucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            //find user
            String userId = readApiParameter("user-id", true);
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("User ID invalid");
            }

            //check access
            checkBucketAccess(currentBucketId, user.getBucketId());

            //activate and save
            user.setActivated(true);
            user.save();

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param user-id The id of user to de-activate. Mandatory
     *
     * @servtitle De-activates a user
     * @httpmethod POST
     * @uri /api/{bucket}/deactivateuser
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void deactivateuser(String bucket) throws ApiException
    {
        try
        {
            String currentBucketId = getCallerBucketId();

            //verify user-id validity
            if (currentBucketId == null || currentBucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            String userId = readApiParameter("user-id", true);
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("User ID invalid");
            }

            //check access
            checkBucketAccess(currentBucketId, user.getBucketId());

            //de-activate and save
            user.setActivated(false);
            user.save();

            //remove sessions
            UserSessionManager.getInstance().removeSessionsOfUser(user.getUserId());

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Get a user's own profile.
     * @httpmethod POST
     * @uri /api/{bucket}/getuserprofile
     * @responsejson {
     * "result": "ok",
     * "name": "Jane Doe",
     * "user-name": "janedoe",
     * "email": "jane@domain.net",
     * "phone": "+123123123112",
     * "language": "en",
     * "roles": [
     * 		"Administrator", "Admin"
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "invalid-user-id"
     * }
     */
    public static void getuserprofile(String bucket) throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            MongoUser user = MongoUser.getById(currentUserId);
            if (user == null)
            {
                throw new ApiException("User ID invalid");
            }

            // get role names
            List<String> roleNames = new ArrayList<>();
            for (String roleId : user.getRoleIds())
            {
                MongoRole role = MongoRole.getById(roleId);
                if (role != null)
                {
                    roleNames.add(role.getName());
                }
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("name", user.getName());
            map.put("user-name", user.getLogin());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone());
            map.put("language", user.getLanguage());
            map.put("roles", roleNames);
            
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param name      The name of user.e.g Nischal Regmi. Mandatory
     * @param user-name The account login username e.g Nischal. Mandatory
     * @param email     The email address.e.g nregmi@alucio.com. Mandatory
     * @param phone     The phone number +9779849094463 / 9779849094463. Mandatory
     * @param language  The language eg. en, zh-tw, zn-ch
     *
     * @servtitle Update a user's own profile. This is different from {@link #updateuser updateuser()} because this
     * allows updation of one's own profile only.
     * @httpmethod POST
     * @uri /api/{bucket}/updateuserprofile
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updateuserprofile(String bucket) throws ApiException
    {
        try
        {
            String currentBucketId = getCallerBucketId();
            String currentUserId = getCallerUserId();

            String name = readApiParameter("name", true);
            String username = readApiParameter("user-name", true);
            String email = readApiParameter("email", true);
            String phone = readApiParameter("phone", true);
            String language = readApiParameter("language", false);

            MongoUser user = MongoUser.getById(currentUserId);

            //check username availability
            List<MongoUser> existingLoginUsers = MongoUser.q()
                    .filter("id <>", user.getUserId())
                    .filter("login", username)
                    .filter("bucketId", currentBucketId)
                    .fetchAll();
            if (existingLoginUsers != null && existingLoginUsers.size() > 0)
            {
                throw new ApiException("username-unavailable");
            }

            //check email availability
            List<MongoUser> existingEmailUsers = MongoUser.q()
                    .filter("id <>", user.getUserId())
                    .filter("email", email)
                    .filter("bucketId", currentBucketId)
                    .fetchAll();
            if (existingEmailUsers != null && existingEmailUsers.size() > 0)
            {
                throw new ApiException("email-exists");
            }

            user.setName(name);
            user.setEmail(email);
            user.setLogin(username);
            user.setPhone(phone);
            user.setLanguage(language);

            //save user object
            user.save();

            //remove cached user
            CacheClient cacheClient = CacheClient.getInstance();
            cacheClient.remove(cacheClient.getUser(user.getUserId()));

            //remove cached sessions
            List<UserSession> userSessions = UserSession.q().filter("userId", user.getUserId()).fetchAll();
            for (UserSession userSession : userSessions)
            {
                cacheClient.remove(cacheClient.getLoginSession(userSession.getSessionKey()));
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param old-password The old password of user. e.g oldPassw0rd. Mandatory
     * @param new-password The new password of user. e.g newPassw0rd. Mandatory
     *
     * @servtitle Change password of current user
     * @httpmethod POST
     * @uri /api/{bucket}/changepassword
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void changepassword() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            String oldPassword = readApiParameter("old-password", true);
            String newPassword = readApiParameter("new-password", true);

            Map map = new ResultMap();
            boolean success = UserProvisioning.resetPasswordWithOldPassword(currentUserId, oldPassword, newPassword);
            if (success)
            {
                map.put("result", "ok");
            }
            else
            {
                map.put("result", "error");
                map.put("reason", "password-incorrect");
            }
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Returns the specified user's preferences object.
     * If preferences object is not found in database, it is created with default values.
     * @httpmethod GET
     * @uri /api/{bucket}/getuserprefs
     * @responsejson {
     * "result": "ok",
     * "prefs": {
     * "APNSDeviceToken": "",
     * "GCMDeviceToken": "",
     * "pushNotificationEnabled": false,
     * "emailNotificationEnabled": false,
     * "smsNotificationEnabled": false,
     * "numberOfViews": 1,
     * "slotSettingAssignments": "{\"group\":{\"111\":\"Megacity\",\"411\":\"Front gate\",\"412\":\"Front gate\",
     * \"421\":\"Front gate\",\"422\":\"Front gate\"},\"camera\":{\"111\":\"1\",
     * \"411\":\"1\",\"412\":-1,\"421\":-1,\"422\":-1},\"channel\":{\"111\":2,
     * \"411\":0,\"412\":-1,\"421\":-1,\"422\":-1}}",
     * "duration": 4320,
     * "autoRotation": false,
     * "autoRotationTime": 15,
     * "id": 1,
     * "theme": "theme name"
     * }
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getuserprefs() throws ApiException
    {
        try
        {
            String userId = getCallerUserId();
            MongoUserPreference userPreference = UserProvisioning.getUserPreference(userId);

            if (userPreference == null)
            {
                throw new ApiException("preferences-object-not-available");
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("prefs", userPreference);
            renderJSON(map, new UserPreferenceSerializer());
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param slot-settings      Enable or disable push notification (true/false)
     * @param duration           Events of last XX days e.g 7
     * @param auto-rotation      Whether to auto rotate or not true/false
     * @param auto-rotation-time Time for auto rotation in seconds e.g 120 (i.e 2 minutes)
     * @param fake-pos-data-pref Enable or disable display of fake POS data in reports, in absence of real POS data
     * @param theme              Name of the theme to use
     *
     * @servtitle save user preference for future reference
     * @httpmethod POST
     * @uri /api/{bucket}/saveuserprefs
     * @responsejson {
     * "result":"ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void saveuserprefs() throws ApiException
    {
        try
        {
            String slotSettingAssignments = readApiParameter("slot-settings", false);
            String duration = readApiParameter("duration", false);
            String autoRotation = readApiParameter("auto-rotation", false);
            String autoRotationTime = readApiParameter("auto-rotation-time", false);
            String POSFakeDataEnabled = readApiParameter("fake-pos-data-pref", false);
            String theme = readApiParameter("theme", false);

            String userId = getCallerUserId();
            UserProvisioning.setUserPreference(userId, slotSettingAssignments, duration, autoRotation, autoRotationTime, POSFakeDataEnabled, theme);

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Exports user list as a file in the specified format i.e PDF or XLS
     * @httpmethod POST
     * @uri /api/{bucket}/exportuserlist     *
     * @responsejson {
     * "result": "ok",
     * "report-url": "/public/files/tmp/13Jun2014175343094.pdf" / "/public/files/tmp/13Jun2014175343094.xls"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportuserlist() throws ApiException
    {
        try
        {
            String currentBucketId = getCallerBucketId();

            //verify user-id validity
            if (currentBucketId == null || currentBucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            //verify bucket-id validity
            MongoBucket targetBucket = MongoBucket.getById(currentBucketId);
            if (targetBucket == null)
            {
                throw new InternalException("Bucket object is null, this should not happen!");
            }

            List<models.transients.UserInfo> userList = new ArrayList<>();
            List<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", currentBucketId).fetchAll();

            // sort by name
            Collections.sort(bucketUsers, new Comparator<MongoUser>()
            {
                @Override
                public int compare(MongoUser o1, MongoUser o2)
                {
                    return o1.getLogin().compareToIgnoreCase(o2.getLogin());
                }
            });

            for (MongoUser bucketUser : bucketUsers)
            {
                userList.add(bucketUser.getAsUserInfo());
            }

            ReportBuilder reportBuilder = new UserListCsv(userList, Lang.get());
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucketid bucket id
     *
     * @servtitle Returns all user information by bucket id
     * @httpmethod POST
     * @uri /api/{bucket}/getbucketusersbybucketid
     * @responsejson {
     * "reslut": "ok",
     * "bucketName": "kaisquare",
     * "bucketUsers": [
     * {
     * "phone": "1234567890",
     * "email": "admin123@yahoo.com.tw",
     * "name": "Admin",
     * "userName": "admin"
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketusersbybucketid()
    {
        try
        {
            String bucketId = readApiParameter("bucketid", true);

            MongoBucket bucket = MongoBucket.getById(bucketId);
            if (null == bucket)
            {
                throw new ApiException("Bucket ID is invalid, this should not happen!");
            }

            List<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", bucketId).fetchAll();
            if (bucketUsers.isEmpty())
            {
                throw new ApiException("bucket-has-no-user");
            }

            Map<String, String> user;
            List<Map<String, String>> resultUsers = new ArrayList<>();
            for (MongoUser u : bucketUsers)
            {
                user = new HashMap();
                user.put("userId", u.getUserId());
                user.put("userName", u.getLogin());
                user.put("name", u.getName());
                user.put("email", u.getEmail());
                user.put("phone", u.getPhone());
                resultUsers.add(user);
            }


            // sort by name
            Collections.sort(resultUsers, new Comparator<Map<String, String>>()
            {
                @Override
                public int compare(Map<String, String> o1, Map<String, String> o2)
                {
                    return o1.get("userName").compareToIgnoreCase(o2.get("userName"));
                }
            });

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("bucketName", bucket.getName());
            map.put("bucketUsers", resultUsers);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Returns the specified user's notification settings.
     * @httpmethod GET
     * @uri /api/{bucket}/getusernotificationsettings
     * @responsejson {
     * "result":"ok",
     * "settings": {
     * "event-vca-intrusion": ["ON_SCREEN", "EMAIL", "SMS", "MOBILE_PUSH"],
     * ...
     * ...
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getusernotificationsettings() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            MongoUser user = MongoUser.getById(currentUserId);
            UserNotificationSettings userSettings = user.getNotificationSettings();

            //bucket
            MongoBucket bucket = MongoBucket.getById(user.getBucketId());
            BucketNotificationSettings bucketSettings = bucket.getNotificationSettings();

            //filter out types that are disabled for the bucket
            Map<EventType, Set<NotifyMethod>> filteredMap = new LinkedHashMap<>();
            for (EventType eventType : userSettings.getSupportedEventTypes())
            {
                if (bucketSettings.isNotificationEnabled(eventType))
                {
                    filteredMap.put(eventType, userSettings.getNotifyMethods(eventType));
                }
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("settings", filteredMap);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-type     event type
     * @param notify-methods List of notification methods. e.g. ["ON_SCREEN", "EMAIL", "SMS", "MOBILE_PUSH"]
     *
     * @servtitle sets the specified user's notification settings.
     * @httpmethod GET
     * @uri /api/{bucket}/updateusernotificationsettings
     * @responsejson {
     * "result":"ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updateusernotificationsettings() throws ApiException
    {
        try
        {
            String currentUserId = getCallerUserId();
            String eventString = readApiParameter("event-type", true);
            String methodList = readApiParameter("notify-methods", true);

            //parse
            EventType eventType = EventType.parse(eventString);
            if (eventType.equals(EventType.UNKNOWN))
            {
                throw new ApiException("invalid-event-type");
            }

            //parse
            Set<NotifyMethod> notifyMethods;
            try
            {
                notifyMethods = new Gson().fromJson(methodList, new TypeToken<Set<NotifyMethod>>()
                {
                }.getType());
            }
            catch (Exception e)
            {
                throw new ApiException("invalid-notify-methods");
            }

            //settings
            MongoUser user = MongoUser.getById(currentUserId);

            synchronized (NOTI_CHANGE_LOCK)
            {
                UserNotificationSettings settings = user.getNotificationSettings();
                List<EventType> supportedTypes = settings.getSupportedEventTypes();
                if (!supportedTypes.contains(eventType))
                {
                    throw new ApiException("unsupported-event-type");
                }

                settings.setNotifyMethods(eventType, notifyMethods);
                settings.save();

            }

            //remove cache
            CacheClient cacheClient = CacheClient.getInstance();
            cacheClient.remove(cacheClient.getUser(user.getUserId()));

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
