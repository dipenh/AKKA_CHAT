package messages;

import java.io.Serializable;
import akkachat.Session;

/**
 * Instruct a {@link Session} to leave a channel
 */
public class LeaveChannel implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String channel;

	public LeaveChannel(String channel) {
		this.channel = channel;
	}
}
