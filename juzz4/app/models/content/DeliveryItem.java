package models.content;

import com.google.code.morphia.annotations.Entity;
import platform.content.delivery.Deliverable;
import platform.content.delivery.DeliveryMethod;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.3
 *
 */
@Entity
public class DeliveryItem extends Model {
    private final DeliveryMethod method;
    private final Deliverable deliverable;
    private int attempts;

    public DeliveryItem(DeliveryMethod method, Deliverable deliverable) {
        this.method = method;
        this.deliverable = deliverable;
    }

    public DeliveryMethod getMethod() {
        return method;
    }

    public Deliverable getDeliverable() {
        return deliverable;
    }

    public void incrementAttempts() {
        attempts++;
        save();
    }

    public int getAttempts() {
        return attempts;
    }
}
