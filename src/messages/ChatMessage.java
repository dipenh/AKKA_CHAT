package messages;

import java.io.Serializable;

/**
 * Represents a chat message from a named source (e.g. a client or a chat bot)
 */
public class ChatMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String source;
	public final String content;
	
	public ChatMessage(String source, String content) {
		this.source = source;
		this.content = content;
	}
}
