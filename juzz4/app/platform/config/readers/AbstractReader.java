package platform.config.readers;

import lib.util.JsonReader;
import play.Logger;

import java.io.File;

/**
 * @author Aye Maung
 * @since v4.5
 */
public abstract class AbstractReader
{
    private JsonReader reader;

    abstract protected String configJsonFile();

    public synchronized boolean loadConfigs()
    {
        if (reader == null)
        {
            reader = new JsonReader();
        }

        try
        {
            reader.load(new File(configJsonFile()));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public JsonReader reader()
    {
        if (reader == null)
        {
            loadConfigs();
        }

        return reader;
    }

    /**
     * @param key parameter name. Use dots '.' to access deeper levels (e.g. theme.cdn-path)
     *
     * @return the reader for the subset map based on the key
     */
    public JsonReader reader(String key)
    {
        if (!reader().containsKey(key))
        {
            Logger.error("%s key does not exist", key);
            return null;
        }

        JsonReader subReader = new JsonReader();
        subReader.load(reader().getAsMap(key));
        return subReader;
    }
}
