package models;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v2.0
 *
 */
@Entity
public class Poi extends Model {

    public String name;
    public String type;
    public String description;
    public String address;
    public String latitude;
    public String longitude;

    public Long bucketId;

    public Poi(){
    }
}
