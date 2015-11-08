package messages;

import java.io.Serializable;
import akkachat.Session;

/**
 * Instruct a {@link Session} to join a channel
 */
public class JoinChannel implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String channel;

	public JoinChannel(String channel) {
		this.channel = channel;
	}
}
