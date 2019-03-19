package platform.analytics;

import lib.util.Util;
import lib.util.exceptions.ApiException;

/**
 * @author Aye Maung
 * @since v4.5
 */
public enum Program
{
    KAI_X1,
    KAI_X2,
    //add by renzongke
	KAI_X3;

    public static Program parse(String programName) throws ApiException
    {
        //backward compatibility
        if (Util.isNullOrEmpty(programName))
        {
            return KAI_X1;
        }

        for (Program program : values())
        {
            if (program.name().equals(programName.toUpperCase()))
            {
                return program;
            }
        }

        throw new ApiException("invalid-program");
    }
}
