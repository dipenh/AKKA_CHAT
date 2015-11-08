package messages;

import akka.actor.ActorRef;

public class UserAdded {
	public final String channelName;
	public final ActorRef channel;
	
	public UserAdded(String channelName, ActorRef channel) {
		this.channelName = channelName;
		this.channel = channel;
	}
}
