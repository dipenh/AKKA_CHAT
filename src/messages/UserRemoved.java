package messages;

import tasks.Channel;
import akka.actor.ActorRef;

/**
 * Confirm that a user has been removed from a {@link Channel}
 */
public class UserRemoved {
	public final String channelName;
	public final ActorRef channel;
	
	public UserRemoved(String channelName, ActorRef channel) {
		this.channelName = channelName;
		this.channel = channel;
	}
}
