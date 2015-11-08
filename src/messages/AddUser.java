package messages;

import tasks.Channel;
import akka.actor.ActorRef;

/**
 * Instruct a {@link Channel} to add an actor as a user
 */
public class AddUser {
	public final ActorRef user;

	public AddUser(ActorRef user) {
		this.user = user;
	}
}
