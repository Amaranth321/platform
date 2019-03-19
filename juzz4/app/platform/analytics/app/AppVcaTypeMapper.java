package platform.analytics.app;

import platform.analytics.Program;
import platform.analytics.VcaType;
import play.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class exists to continue supporting the current VCA types while apps are being introduced.
 * <p/>
 * Vca types will be replaced by Apps in the future.
 * Analytics engine is already running based on app IDs, not VcaTypes
 *
 * @author Aye Maung
 * @since v4.5
 */
public final class AppVcaTypeMapper
{
    private static final Map<String, VcaType> appTypeMap = new LinkedHashMap<>();

    static
    {
        //These initializations are necessary because the current platform doesn't support
        //app-based VCA management yet.
        //so, appId needs to be mapped to a VcaType
        appTypeMap.put("peoplecount", VcaType.PEOPLE_COUNTING);
        appTypeMap.put("passerby", VcaType.PASSERBY);
        appTypeMap.put("face_ap", VcaType.AUDIENCE_PROFILING);
        appTypeMap.put("ds_obj_detector", VcaType.OBJECT_DETECTION);
    }

    public static String getAppId(Program program, VcaType vcaType)
    {
        if (program == Program.KAI_X2)
        {
            return getAppIdForKaiX2(vcaType);
        }
        else if(program == Program.KAI_X3) 
        {
        	return getAppIdForKaiX3(vcaType);
        }
        else
        {
            return getAppIdForKaiX1(vcaType);
        }
    }

    public static VcaType getVcaType(String appId)
    {
        if (appId.contains(Program.KAI_X1.name()))
        {
            return getVcaTypeForKaiX1(appId);
        }
        else if(appId.contains(Program.KAI_X3.name())) 
        {
        	return getVcaTypeForKaiX3(appId);
        }
        else 
        {
        	return getVcaTypeForKaiX2(appId);
        }
    }

    public static Program getProgram(String appId)
    {
        if(appId.contains(Program.KAI_X1.name())) {
        	return Program.KAI_X1;
        }else if(appId.contains(Program.KAI_X3.name())) {
        	return Program.KAI_X3;
        }else {
        	return Program.KAI_X2;
        }
//    	return appId.contains(Program.KAI_X1.name()) ? Program.KAI_X1 : Program.KAI_X2;
    }

    /**
     * Kai X1 does not have appId. This method will compose an ID based on VcaType
     */
    private static String getAppIdForKaiX1(VcaType vcaType)
    {
        return String.format("%s_%s", Program.KAI_X1.name(), vcaType.getVcaTypeName());
    }

    /**
     * counter part of getAppIdForKaiX1 method
     */
    private static VcaType getVcaTypeForKaiX1(String appId)
    {
        return VcaType.parse(appId.replace(Program.KAI_X1.name() + "_", ""));
    }

    private static VcaType getVcaTypeForKaiX2(String appId)
    {
        VcaType mappedType = appTypeMap.get(appId);
        if (mappedType == null)
        {
            throw new UnsupportedOperationException("No mapped VCA type found for " + appId);
        }

        return mappedType;
    }
    
    private static VcaType getVcaTypeForKaiX3(String appId) {
    	appId = appId.replace(String.format("%s_",Program.KAI_X3.name()),"");
    	VcaType mappedType = appTypeMap.get(appId);
        if (mappedType == null)
        {
            throw new UnsupportedOperationException("No mapped VCA type found for " + appId);
        }

        return mappedType;
    }
    
    private static String getAppIdForKaiX2(VcaType vcaType)
    {
        for (Map.Entry<String, VcaType> entry : appTypeMap.entrySet())
        {
            if (entry.getValue() == vcaType)
            {
                return entry.getKey();
            }
        }
        throw new UnsupportedOperationException("No appId found for " + vcaType);
    }
    
    private static String getAppIdForKaiX3(VcaType vcaType) {
    	for (Map.Entry<String, VcaType> entry : appTypeMap.entrySet())
        {
            if (entry.getValue() == vcaType)
            {
                return String.format("%s_%s", Program.KAI_X3.name(), entry.getKey());
            }
        }
        throw new UnsupportedOperationException("No appId found for " + vcaType);
    }
}
