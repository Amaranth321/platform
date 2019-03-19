package platform.label;

import models.labels.LabelOthers;
import models.labels.LabelRegion;
import models.labels.LabelStore;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.5
 */
public enum LabelType
{
    STORE,
    REGION,
    OTHERS;

    public static boolean isValid(String type)
    {
        try
        {
            valueOf(type.trim().toUpperCase());
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public Model.MorphiaQuery getQuery()
    {
        Model.MorphiaQuery query;
        switch (this)
        {
            case STORE:
                query = LabelStore.q();
                break;

            case REGION:
                query = LabelRegion.q();
                break;

            case OTHERS:
                query = LabelOthers.q();
                break;

            default:
                throw new UnsupportedOperationException();
        }

        return query;
    }

}
