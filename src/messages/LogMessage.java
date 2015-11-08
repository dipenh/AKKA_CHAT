package messages;

import java.io.Serializable;
import akkachat.Session;

/**
 * An informational message which {@link Session} will forward to the client
 */
public class LogMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String content;

	public LogMessage(String content) {
		this.content = content;
	}
}
