package messages;

import java.io.Serializable;
import tasks.*;

/**
 * Ask the {@link ChannelManager} to return the reference to and existing
 * {@link Channel} or create a new one.
 */
public class GetOrCreateChannel implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String name;

	public GetOrCreateChannel(String name) {
		this.name = name;
	}
}
