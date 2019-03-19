package models.node;

import com.google.code.morphia.annotations.Entity;

import play.modules.morphia.Model;

/**
 * @author keith
 *
 * Node Remote Shall.
 */

@Entity
public class NodeRemoteShell  extends Model {
	public String host;
	public int port;
	public String username;
	public boolean open;
}
