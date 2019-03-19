package controllers.api;

import com.google.gson.Gson;
import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import models.*;
import models.backwardcompatibility.Feature;
import models.backwardcompatibility.Role;
import platform.Environment;
import platform.RoleManager;
import platform.access.DefaultRole;
import platform.access.FeatureRestriction;
import play.mvc.With;

import java.util.*;

import static lib.util.Util.isNullOrEmpty;

/**
 * @author KAI Square
 * @sectiontitle Role Management
 * @sectiondesc APIs to manage user roles
 * @publicapi
 */

@With(APIInterceptor.class)
public class RoleManagement extends APIController
{

    /**
     * @param bucket e.g. kaisquare, passed intrinsically as part of URL
     *
     * @servtitle Returns roles of the specified bucket
     * @httpmethod POST
     * @uri /api/{bucket}/getbucketroles
     * @responsejson {
     * "result": "ok",
     * "roles": [
     * {
     * "name": "Administrator",
     * "description": "Admin account with access to all features",
     * "features": [
     * {
     * "name": "login",
     * "type": "common",
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 302
     * }
     * ],
     * "id": 63
     * }
     * ],
     * "id": 2
     * },
     * {
     * "name": "Supervisor",
     * "description": "Access to all features except admin settings",
     * "features": [],
     * "id": 3
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketroles(String bucket) throws ApiException
    {
        try
        {
            MongoBucket targetBucket = MongoBucket.getByName(bucket);

            List<Role> sqlRoles = new ArrayList<>();
            List<MongoRole> mongoRoles = MongoRole.q().filter("bucketId", targetBucket.getBucketId()).fetchAll();

            for (MongoRole mongoRole : mongoRoles)
            {
                sqlRoles.add(new Role(mongoRole));
            }

            // sort by name
            Collections.sort(sqlRoles, new Comparator<Role>()
            {
                @Override
                public int compare(Role o1, Role o2)
                {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("roles", sqlRoles);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket      Passed intrinsically as part of URL e.g. kaisquare
     * @param role-name   The name of role. e.g administrator. Mandatory
     * @param description The description of role. e.g admin role.
     *
     * @servtitle Returns roles of the specified bucket
     * @httpmethod POST
     * @uri /api/{bucket}/addbucketrole
     * @responsejson {
     * "result": "ok",
     * "role-id": "3"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void addbucketrole(String bucket) throws ApiException
    {
        try
        {
            String roleName = readApiParameter("role-name", true);
            String roleDescription = readApiParameter("description", false);

            if (!isNullOrEmpty(roleDescription) && roleDescription.length() > 255)
            {
                throw new ApiException("description-limit-exceeded");
            }

            MongoBucket targetBucket = MongoBucket.getByName(bucket);
            if (targetBucket == null)
            {
                throw new ApiException("Company ID not found");
            }

            // validate role-name is exist or not.
            List<MongoRole> bucketRoles = MongoRole.q().filter("bucketId", targetBucket.getBucketId()).fetchAll();
            for (MongoRole role : bucketRoles)
            {
                if (role.getName().equalsIgnoreCase(roleName))
                {
                    throw new ApiException(String.format("Repeat role name (%s) in %s", roleName, targetBucket.getName()));
                }
            }

            MongoRole newRole = new MongoRole(targetBucket.getBucketId(), roleName, roleDescription);
            newRole.setRoleId(MongoRole.generateNewId());
            newRole.save();

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("role-id", newRole.getRoleId());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket      Passed intrinsically as part of URL e.g. kaisquare
     * @param role-id     The id of role. e.g 3. Mandatory
     * @param role-name   The name of role. e.g administrator. Mandatory
     * @param description The description of role. e.g admin role.
     *
     * @servtitle Updates role of the specified bucket
     * @httpmethod POST
     * @uri /api/{bucket}/editbucketrole
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void editbucketrole(String bucket) throws ApiException
    {
        try
        {
            String roleId = readApiParameter("role-id", true);
            String roleName = readApiParameter("role-name", true);
            String roleDescription = readApiParameter("role-description", false);

            if (!isNullOrEmpty(roleDescription) && roleDescription.length() > 255)
            {
                throw new ApiException("description-limit-exceeded");
            }

            Map map = new ResultMap();
            MongoRole targetRole = MongoRole.getById(roleId);
            if (targetRole == null)
            {
                throw new ApiException("Role not found");
            }

            MongoBucket targetBucket = MongoBucket.getByName(bucket);
            if (targetBucket == null)
            {
                throw new ApiException("Company ID not found");
            }
            // validate role-name is exist or not.
            List<MongoRole> bucketRoles = MongoRole.q().filter("bucketId", targetBucket.getBucketId()).fetchAll();

            for (MongoRole bucketRole : bucketRoles)
            {
                if (!roleId.equals(bucketRole.getRoleId()))
                {
                    if (bucketRole.getName().equalsIgnoreCase(roleName))
                    {
                        throw new ApiException("Repeat role name");
                    }
                }
            }

            //check if defaults
            if (DefaultRole.isDefault(targetRole.getName()) && !targetRole.getName().equals(roleName))
            {
                throw new ApiException("no-default-role-name-change");
            }

            targetRole.setName(roleName);
            targetRole.setDescription(roleDescription);
            targetRole.save();
            map.put("result", "ok");

            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket  Passed intrinsically as part of URL e.g. kaisquare
     * @param role-id The id of role. e.g 3. Mandatory
     *
     * @servtitle Remove role from the specified bucket
     * @httpmethod POST
     * @uri /api/{bucket}/removerole
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removerole(String bucket) throws ApiException
    {
        try
        {
            String roleId = readApiParameter("role-id", true);

            MongoRole targetRole = MongoRole.getById(roleId);
            if (targetRole == null)
            {
                throw new ApiException("role not found.");
            }

            if (DefaultRole.isDefault(targetRole.getName()))
            {
                throw new ApiException("Deleting a default role is not allowed");
            }

            //remove from users
            MongoBucket targetBucket = MongoBucket.getByName(bucket);
            Iterable<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", targetBucket.getBucketId()).fetch();
            for (MongoUser user : bucketUsers)
            {
                if (user.getRoleIds().contains(targetRole.getRoleId()))
                {
                    user.getRoleIds().remove(targetRole.getRoleId());
                    user.save();
                }
            }

            targetRole.delete();

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
     * @param role-id The id of role. e.g 3. Mandatory
     *
     * @servtitle Returns features assigned to the specified role
     * @httpmethod POST
     * @uri /api/{bucket}/getrolefeatures
     * @responsejson {
     * "result": "ok"
     * "features": [
     * {
     * "name": "common",
     * "type": "common",
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 302
     * },
     * {
     * "name": "logout",
     * "version": "2",
     * "id": 303
     * }
     * ],
     * "id": 63
     * }
     * ],
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getrolefeatures(String bucket) throws ApiException
    {
        try
        {
            String roleId = readApiParameter("role-id", true);

            MongoRole targetRole = MongoRole.getById(roleId);
            if (targetRole == null)
            {
                throw new ApiException("Role not found");
            }

            // for compatibility
            List<Feature> sqlFeatures = new ArrayList<>();
            for (String featureName : targetRole.getFeatureNames())
            {
                MongoFeature mongoFeature = MongoFeature.getByName(featureName);
                if (mongoFeature != null)
                {
                    sqlFeatures.add(new Feature(mongoFeature));
                }
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("features", sqlFeatures);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param role-id             The id of role. Mandatory
     * @param feature-assignments Comma separated values. e.g ["live-view", "monitoring"]. Mandatory
     *
     * @servtitle Updates the list of features of a role
     * @httpmethod GET
     * @uri /api/{bucket}/updaterolefeatures
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "Error updating features for role"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updaterolefeatures(String bucket) throws ApiException
    {
        try
        {
            String roleId = readApiParameter("role-id", true);
            String featureAssignments = readApiParameter("feature-assignments", true);

            MongoRole targetRole = MongoRole.getById(roleId);
            if (targetRole == null)
            {
                throw new ApiException("Role not found");
            }

            List<String> featureNames = new ArrayList<>();
            featureNames = new Gson().fromJson(featureAssignments, featureNames.getClass());

            boolean result = RoleManager.getInstance().updateFeatures(roleId, featureNames);
            if (result == false)
            {
                throw new ApiException("Error updating features for role");
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
     * @param user-id The id of user. e.g 3. Mandatory
     *
     * @servtitle Returns features of the current user(for UI restrictions)
     * @httpmethod POST
     * @uri /api/{bucket}/getuserfeatures
     * @responsejson {
     * "result": "ok",
     * "features": [
     * {
     * "name": "common",
     * "type": "common",
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 302
     * },
     * {
     * "name": "logout",
     * "version": "2",
     * "id": 303
     * }
     * ],
     * "id": 63
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getuserfeatures(String bucket) throws ApiException
    {
        try
        {
            String userId = getCallerUserId();

            MongoUser currentUser = MongoUser.getById(userId);
            if (currentUser == null)
            {
                throw new ApiException("User not found");
            }

            //compile all features from different roles
            List<Feature> sqlFeatures = new ArrayList<>();
            for (String roleId : currentUser.getRoleIds())
            {
                MongoRole userRole = MongoRole.getById(roleId);
                if (userRole == null)
                {
                    continue;
                }
                for (String featureName : userRole.getFeatureNames())
                {
                    MongoFeature mongoFeature = MongoFeature.getByName(featureName);
                    Feature sqlFeature = new Feature(mongoFeature);
                    if (mongoFeature != null && !sqlFeatures.contains(sqlFeature))
                    {
                        sqlFeatures.add(sqlFeature);
                    }
                }
            }

            Collections.sort(sqlFeatures, Feature.sortByPosition);

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("features", sqlFeatures);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param user-id The id of user. e.g 3. Mandatory
     *
     * @servtitle Returns roles of the specified user (used for assigning roles by admin)
     * @httpmethod POST
     * @uri /api/{bucket}/getuserrolesbyuserid
     * @responsejson {
     * "result": "ok",
     * "roles": [
     * {
     * "name": "Administrator",
     * "description": "Admin account with access to all features",
     * "features": [
     * {
     * "name": "login",
     * "type": "common",
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 302
     * }
     * ],
     * "id": 63
     * }
     * ],
     * "id": 2
     * },
     * {
     * "name": "Supervisor",
     * "description": "Access to all features except admin settings",
     * "features": [],
     * "id": 3
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getuserrolesbyuserid(String bucket) throws ApiException
    {
        try
        {
            String userId = readApiParameter("user-id", true);

            if (isNullOrEmpty(userId))
            {
                throw new ApiException("User ID is missing");
            }

            MongoUser targetUser = MongoUser.getById(userId);
            if (targetUser == null)
            {
                throw new ApiException("User not found");
            }

            List<Role> sqlRoles = new ArrayList<>();
            for (String roleId : targetUser.getRoleIds())
            {
                MongoRole mongoRole = MongoRole.getById(roleId);
                if (mongoRole != null)
                {
                    sqlRoles.add(new Role(mongoRole));
                }
            }

            // sort by name
            Collections.sort(sqlRoles, new Comparator<Role>()
            {
                @Override
                public int compare(Role o1, Role o2)
                {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("roles", sqlRoles);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param user-id          The id of user. Mandatory
     * @param role-assignments Comma separated values. e.g ["administrator", "supervisor"]. Mandatory
     *
     * @servtitle Updates the list of features of a role
     * @httpmethod GET
     * @uri /api/{bucket}/updateuserroles
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "Error updating user's roles"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updateuserroles(String bucket) throws ApiException
    {
        try
        {
            String userId = readApiParameter("user-id", true);
            String roleAssignments = readApiParameter("role-assignments", true);

            //Convert json to java map
            Gson gson = new Gson();
            List<String> roleIds = new ArrayList<>();
            roleIds = gson.fromJson(roleAssignments, roleIds.getClass());

            boolean result = RoleManager.getInstance().updateUserRoles(userId, roleIds);
            if (!result)
            {
                throw new ApiException("Error updating user's roles");
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
     * @servtitle Returns a list of features assignable to roles, based on environment (cloud or node)
     * @httpmethod POST
     * @uri /api/{bucket}/getassignablerolefeatures
     * @responsejson {
     * "result": "ok",
     * "features": [ {@link Feature} ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getassignablerolefeatures()
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            MongoBucket bucket = MongoBucket.getById(callerBucketId);
            Environment env = Environment.getInstance();

            // for compatibility
            TreeSet<Feature> sqlFeatures = new TreeSet<>(Feature.sortByPosition);
            for (String featureName : bucket.getFeatureNames())
            {
                MongoFeature mongoFeature = MongoFeature.getByName(featureName);
                if (mongoFeature == null)
                {
                    continue;
                }
                if (env.onKaiNode() && mongoFeature.getRestriction().equals(FeatureRestriction.CLOUD_ONLY))
                {
                    continue;
                }
                if (env.onCloud() && mongoFeature.getRestriction().equals(FeatureRestriction.NODE_ONLY))
                {
                    continue;
                }

                sqlFeatures.add(new Feature(mongoFeature));
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("features", sqlFeatures);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

}
