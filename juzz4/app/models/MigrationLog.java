package models;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.4
 *
 */
@Entity
public class MigrationLog extends Model {
    private final String whichFn;
    private final String migratingItem;
    private final String exception;

    public static ServerPagedResult<MigrationLog> list(int skip, int take) {
        Model.MorphiaQuery query = MigrationLog.q().order("-_created");
        return QueryHelper.preparePagedResult(query, skip, take);
    }

    public static void log(String migratingItem, Exception e) {
        new MigrationLog(Util.getCallerFn(), migratingItem, e).save();
    }

    private MigrationLog(String callerFn, String migratingItem, Exception e) {
        this.whichFn = callerFn;
        this.migratingItem = migratingItem;
        this.exception = e == null ? "" : Util.getStackTraceString(e);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", whichFn, migratingItem, exception);
    }

}
