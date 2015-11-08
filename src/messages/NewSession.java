package messages;

import akka.actor.ActorRef;
import akkachat.Session;

/**
 * Used by {@link Session} to advertise new connections
 */
public class NewSession {
	public final ActorRef session;

	public NewSession(ActorRef session) {
		this.session = session;
	}
}
