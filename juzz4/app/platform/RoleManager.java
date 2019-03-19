package platform;

import lib.util.ListUtil;
import lib.util.exceptions.ApiException;
import models.MongoFeature;
import models.MongoRole;
import models.MongoService;
import models.MongoUser;
import play.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class RoleManager
{
    private static final RoleManager instance = new RoleManager();

    private RoleManager()
    {
    }

    public static RoleManager getInstance()
    {
        return instance;
    }

    public void addFeatures(String roleId, List<String> addFeatureNames)
    {
        try
        {
            MongoRole role = MongoRole.getById(roleId);
            if (role == null)
            {
                throw new ApiException();
            }

            List<String> actualAddFeatureNames = ListUtil.getExtraItems(role.getFeatureNames(), addFeatureNames);
            if (actualAddFeatureNames.isEmpty())
            {
                return;
            }

            role.getFeatureNames().addAll(actualAddFeatureNames);
            role.save();

            // compiles service APIs
            List<String> addServiceNames = new ArrayList<>();
            for (String featureName : actualAddFeatureNames)
            {
                MongoFeature feature = MongoFeature.getByName(featureName);
                if (feature == null)
                {
                    continue;
                }
                addServiceNames.addAll(feature.getServiceNames());
            }

            // add these features to role's users
            List<MongoUser> users = MongoUser.q().filter("bucketId", role.getBucketId()).fetchAll();
            for (MongoUser user : users)
            {
                if (user.getRoleIds().contains(roleId))
                {
                    addServicesToUser(user.getUserId(), addServiceNames);
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    public boolean updateFeatures(String roleId, List<String> updateFeatureNames) throws ApiException
    {
        try
        {
            MongoRole role = MongoRole.getById(roleId);
            if (role == null)
            {
                throw new ApiException();
            }
            role.getFeatureNames().clear();
            role.getFeatureNames().addAll(updateFeatureNames);
            role.save();

            // update all users that belong to targetRole
            List<MongoUser> users = MongoUser.q().filter("bucketId", role.getBucketId()).fetchAll();
            for (MongoUser user : users)
            {
                if (user.getRoleIds().contains(roleId))
                {
                    updateUserRoles(user.getUserId(), user.getRoleIds());
                }
            }

            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public void removeFeatures(String roleId, List<String> removeFeatureNames)
    {
        try
        {
            MongoRole role = MongoRole.getById(roleId);
            if (role == null)
            {
                throw new ApiException();
            }

            List<String> actualRemoveFeatureNames = ListUtil.hasInCommon(role.getFeatureNames(), removeFeatureNames);
            if (actualRemoveFeatureNames.isEmpty())
            {
                return;
            }

            role.getFeatureNames().removeAll(actualRemoveFeatureNames);
            role.save();

            // compiles service APIs to remove from roles
            List<String> removeServiceNames = new ArrayList<>();
            for (String featureName : actualRemoveFeatureNames)
            {
                MongoFeature feature = MongoFeature.getByName(featureName);
                if (feature == null)
                {
                    continue;
                }
                removeServiceNames.addAll(feature.getServiceNames());
            }

            // exclude shared API and common services if any
            List<String> commonServiceNames = FeatureManager.getInstance().getCommonServiceNames();
            List<String> exemptedServiceNames = new ArrayList<>();

            for (String serviceName : removeServiceNames)
            {
                if (commonServiceNames.contains(serviceName))
                {
                    exemptedServiceNames.add(serviceName);
                    continue;
                }

                MongoService service = MongoService.getByName(serviceName);
                if (service == null)
                {
                    continue;
                }
                HashSet<String> sharedFeatureNames = FeatureManager.getInstance().getFeaturesSharingApi(serviceName);
                //System.out.println("== current serviceName: " + serviceName);
                //for (String fName : sharedFeatureNames)
                //{
                //    System.out.println("== shared feature:" + fName);
                //}

                for (String featureName : role.getFeatureNames())
                {
                    MongoFeature feature = MongoFeature.getByName(featureName);
                    if (feature == null)
                    {
                        continue;
                    }
                    if (sharedFeatureNames.contains(feature.getName()))
                    {
                        exemptedServiceNames.add(serviceName);
                    }
                }
            }

            removeServiceNames.removeAll(exemptedServiceNames);

            // remove these features from its users
            List<MongoUser> users = MongoUser.q().filter("bucketId", role.getBucketId()).fetchAll();
            for (MongoUser user : users)
            {
                if (user.getRoleIds().contains(roleId))
                {
                    removeServicesFromUser(user.getUserId(), removeServiceNames);
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    public boolean updateUserRoles(String userId, List<String> roleIds) throws ApiException
    {
        try
        {
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException();
            }

            //Compile all services from different roles
            List<String> completeServiceNames = new ArrayList<>();
            for (String roleId : roleIds)
            {
                MongoRole role = MongoRole.getById(roleId);
                if (role == null)
                {
                    continue;
                }

                for (String featureName : role.getFeatureNames())
                {
                    MongoFeature feature = MongoFeature.getByName(featureName);
                    if (feature == null)
                    {
                        continue;
                    }
                    for (String serviceName : feature.getServiceNames())
                    {
                        if (!completeServiceNames.contains(serviceName))
                        {
                            completeServiceNames.add(serviceName);
                        }
                    }
                }
            }

            //add common services
            List<String> commonServiceNames = FeatureManager.getInstance().getCommonServiceNames();
            completeServiceNames.addAll(commonServiceNames);

            //update user
            user.getRoleIds().clear();
            user.getRoleIds().addAll(roleIds);

            user.getServiceNames().clear();
            user.getServiceNames().addAll(completeServiceNames);
            user.save();

            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    private void removeServicesFromUser(String userId, List<String> removeServiceNames)
    {
        MongoUser user = MongoUser.getById(userId);
        if (user == null)
        {
            return;
        }
        List<String> actualRemoveServiceNames = ListUtil.hasInCommon(user.getServiceNames(), removeServiceNames);
        if (actualRemoveServiceNames.isEmpty())
        {
            return;
        }
        user.getServiceNames().removeAll(actualRemoveServiceNames);
        user.save();
    }

    private void addServicesToUser(String userId, List<String> addServiceNames)
    {
        MongoUser user = MongoUser.getById(userId);
        if (user == null)
        {
            return;
        }
        List<String> actualAddServiceNames = ListUtil.getExtraItems(user.getServiceNames(), addServiceNames);
        if (actualAddServiceNames.isEmpty())
        {
            return;
        }

        // combine and remove duplicates
        LinkedHashSet<String> finalServiceNamesSet = new LinkedHashSet<>(user.getServiceNames());
        finalServiceNamesSet.addAll(actualAddServiceNames);

        user.getServiceNames().clear();
        user.getServiceNames().addAll(finalServiceNamesSet);
        user.save();
    }
}
