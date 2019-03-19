package platform.devices;

import lib.util.Util;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class Resolution
{
    private final int width;
    private final int height;

    public Resolution(int width, int height)
    {
        if (width < 1 || height < 1)
        {
            throw new IllegalArgumentException(String.format("%sx%s", width, height));
        }

        this.width = width;
        this.height = height;
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", width, height);
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int pixelCount()
    {
        return width * height;
    }

    public Resolution aspectRatio()
    {
        int gcd = Util.gcd(width, height);
        return new Resolution(width / gcd, height / gcd);
    }
}
