package akkachat;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import scala.concurrent.*;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.*;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import messages.*;

/**
 * The server side representation of the client. This actor handles
 * communication between the client and the channels.
 * 
 * Each new incarnation of {@link Session} will publish itself on the Event
 * Stream with a {@link NewSession} message.
 */
public class Session extends UntypedActor {
	private final ExecutionContext ec = context().system().dispatcher();
	private final HashMap<String, ActorRef> channels = new HashMap<String, ActorRef>();
	private final ActorRef connection;
	private String username = "anonymous";

	public Session(ActorRef tcpConnection) {
		connection = context().actorOf(Props.create(SerializingConnection.class, tcpConnection), "connection");
		context().watch(connection);
	}
	
	@Override
	public void preStart() {
		context().system().eventStream().publish(new NewSession(self()));;
	}

	@Override
	public void onReceive(final Object msg) throws Exception {
		if (msg instanceof Backlog || msg instanceof LogMessage) {
			connection.tell(SerializingConnection.Action.Send(msg), self());
		} else if (msg instanceof SetName) {
			username = ((SetName) msg).username;
			log("Hello " + username + "!");
		} else if (msg instanceof JoinChannel) {
			final String channelName = ((JoinChannel) msg).channel;
			Future<Object> channelFuture = Patterns.ask(context().actorSelection("/user/channels"),
					new GetOrCreateChannel(channelName),
					new Timeout(new FiniteDuration(5, TimeUnit.SECONDS)));
			channelFuture.onComplete(new OnComplete<Object>() {
				@Override
				public void onComplete(Throwable failure, Object channel) throws Throwable {
					if (failure == null) {
						if (channel instanceof ActorRef) {
							((ActorRef)channel).tell(new AddUser(self()), self());
						} else {
							self().tell(new LogMessage("Joining '" + channelName + "' failed: channel manager didn't return an ActorRef"), self());
						}
					} else
						self().tell(new LogMessage("Joining '" + channelName + "' failed: " + failure.getMessage()), self());
				}
			}, ec);
		} else if (msg instanceof UserAdded) {
			UserAdded added = (UserAdded) msg;
			channels.put(added.channelName, added.channel);
			connection.tell(SerializingConnection.Action.Send(new JoinChannel(added.channelName)), self());
		} else if (msg instanceof LeaveChannel) {
			String channelName = ((LeaveChannel) msg).channel;
			ActorRef channel = channels.get(channelName);
			if (channel != null) {
				channel.tell(new RemoveUser(self()), self());
			}
		} else if (msg instanceof UserRemoved) {
			String channelName = ((UserRemoved) msg).channelName;
			channels.remove(channelName);
			connection.tell(SerializingConnection.Action.Send(new LeaveChannel(channelName)), self());
		} else if (msg instanceof Say) {
			final String channelName = ((Say) msg).channel;
			ActorRef channel = channels.get(channelName);
			if (channel != null) {
				channel.tell(new ChatMessage(username, ((Say) msg).content), self());
			} else
				fatalError("Tried to send message to non-joined channel '" + channelName + "'");
		} else if (msg instanceof Terminated) {
			if (((Terminated) msg).getActor().equals(connection)) {
				context().stop(self());
			}
		} else
			unhandled(msg);
	}

	void log(String message) {
		connection.tell(SerializingConnection.Action.Send(new LogMessage(message)), self());
	}

	void fatalError(String message) {
		log(message);
		connection.tell(SerializingConnection.Action.Close(), self());
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		// Stop the connection on any exception
		return SupervisorStrategy.stoppingStrategy();
	}
}