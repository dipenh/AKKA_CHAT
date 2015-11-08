package messages;

import akka.actor.ActorRef;
import tasks.Channel;

/**
 * Instruct a {@link Channel} to remove an actor as a user
 */
public class RemoveUser {
	public final ActorRef user;

	public RemoveUser(ActorRef user) {
		this.user = user;
	}
}
