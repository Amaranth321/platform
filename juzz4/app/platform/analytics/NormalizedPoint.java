package platform.analytics;

import play.Logger;

/**
 * Author:  Aye Maung
 */
public class NormalizedPoint
{
    private Double x;
    private Double y;

    public NormalizedPoint(Double x, Double y) throws Exception
    {
        if (x < 0)
        {
            Logger.error("x value is less than 0. Changed to 0");
            x = 0.0;
        }
        else if (x > 1)
        {
            Logger.error("x value is greater than 1. Changed to 1");
            x = 1.0;
        }

        if (y < 0)
        {
            Logger.error("y value is less than 0. Changed to 0");
            y = 0.0;
        }
        else if (y > 1)
        {
            Logger.error("y value is greater than 1. Changed to 1");
            y = 1.0;
        }

        this.x = x;
        this.y = y;
    }

    public Double getX()
    {
        return x;
    }

    public Double getY()
    {
        return y;
    }

}

