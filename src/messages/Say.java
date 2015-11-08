package messages;

import java.io.Serializable;
import akkachat.Session;

/**
 * Instruct a {@link Session} to post a message on a channel
 */
public class Say implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String channel;
	public final String content;
	
	public Say(String channel, String content) {
		this.channel = channel;
		this.content = content;
	}
}
