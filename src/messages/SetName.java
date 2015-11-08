package messages;

import java.io.Serializable;
import akkachat.Session;

/**
 * Set the client's name for a {@link Session}
 */
public class SetName implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String username;
	
	public SetName(String username) {
		this.username = username;
	}
}
