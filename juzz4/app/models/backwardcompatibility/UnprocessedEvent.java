package models.backwardcompatibility;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import models.backwardcompatibility.Event;
import play.Logger;

/**
 * This event model is for report process, once the event is processed, it will be removed
 * @deprecated please use {@link models.UnprocessedVcaEvent} instead
 */
@Deprecated
public class UnprocessedEvent extends Event
{
	
	public static UnprocessedEvent copyFrom(Event e)
	{
		UnprocessedEvent re = new UnprocessedEvent();
		Field[] fields = e.getClass().getDeclaredFields();
		for (Field f : fields)
		{
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			
			try {
				f.setAccessible(true);
				Field reField = re.getClass().getField(f.getName());
				reField.setAccessible(true);
				reField.set(re, f.get(e));
			} catch (Exception e1) {
				Logger.error("unable to copy event field '%s': %s", f.getName(), e1.getMessage());
			}
		}
		
		return re;
	}

}
