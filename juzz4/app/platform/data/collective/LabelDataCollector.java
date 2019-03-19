package platform.data.collective;

import platform.data.Triggerable;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Collectors are meant to trigger something based on the collected data.
 * <p/>
 * If you just want to compile data and don't need to use triggers, DO NOT use this class.
 *
 * @author Aye Maung
 * @since v4.5
 */
public abstract class LabelDataCollector
{
    private final String labelId;
    private List<Triggerable> triggers;

    protected LabelDataCollector(String labelId)
    {
        this.labelId = labelId;
        this.triggers = new ArrayList<>();
    }

    protected String getLabelId()
    {
        return labelId;
    }

    protected void addTrigger(Triggerable triggerable)
    {
        triggers.add(triggerable);
    }

    protected abstract void compile(Object... data);

    public void collect(Object... data)
    {
        compile(data);

        for (Triggerable trigger : triggers)
        {
            try
            {
                trigger.checkAndTrigger();
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }

        DataCollectorFactory.getInstance().save(this);
    }
}
