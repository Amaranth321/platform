package models;

import com.google.code.morphia.annotations.Entity;

import play.modules.morphia.Model;

/**
 *
 * @author Nischal
 */
@Entity
public class UserLabel extends Model {
    public Long bucketId;
    public Long userId;
    public String label;

    public String toString() {
        return label;
    }
}