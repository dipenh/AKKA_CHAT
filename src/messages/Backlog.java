package messages;

import java.io.Serializable;
import java.util.*;
import tasks.Channel;

/**
 * Notify a user of new messages in a {@link Channel}
 */
public class Backlog implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String channel;
	public final List<ChatMessage> lines;

	public Backlog(String channel, Collection<ChatMessage> lines) {
		this.channel = channel;
		this.lines = Collections.unmodifiableList(new ArrayList<ChatMessage>(lines));
	}

	public Backlog(String channel, ChatMessage... lines) {
		this.channel = channel;
		this.lines = Collections.unmodifiableList(Arrays.asList(lines));
	}
}
