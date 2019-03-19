package models.abstracts;

import platform.Environment;
import play.modules.morphia.Model;

/**
 * Author:  Aye Maung
 *
 */
public abstract class PlatformError extends Model {
    private final Long time;
    private final String source;
    private final String error;

    public PlatformError(String source, String error, Long time) {
        this.source = source;
        this.error = error;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public String getSource() {
        return source;
    }

    public String getError() {
        return error;
    }

}
