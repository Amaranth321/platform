package platform.analytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Aye Maung
 */
public class PolygonRegion
{
    public String name;
    public List<NormalizedPoint> points;

    public PolygonRegion()
    {
        points = new ArrayList<NormalizedPoint>();
    }

    public String toString()
    {
        String retStr = "";
        for (int i = 0; i < points.size(); i++)
        {
            NormalizedPoint point = points.get(i);
            retStr += point.getX() + "," + point.getY();

            if (i < points.size() - 1)
            { //if not last
                retStr += ",";
            }
        }
        return retStr;
    }
}
