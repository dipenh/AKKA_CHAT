package akkachat;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import tasks.*;
import akka.actor.*;
import akka.event.*;
import akka.io.*;
import akka.io.Tcp.*;

/**
 * This is the entry point to the server. This actor creates the other top-level
 * actors and listens for connections from clients. A {@link Session} is created
 * as a child for each new connection.
 */
public class Server extends UntypedActor {
	public static final int DEFAULT_PORT = 10656; 
	
	private final LoggingAdapter log = Logging.getLogger(context().system(), this);
	private final ActorRef tcp = Tcp.get(context().system()).getManager();
	private final InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", DEFAULT_PORT);
	private long nextSessionId = 1;

	long getSessionId() {
		return nextSessionId++;
	}

	@Override
	public void preStart() throws Exception {
		context().system().actorOf(Props.create(ChannelManager.class), "channels");
		
		// Create the bot
		context().system().actorOf(Props.create(PartyBot.class), "bot");
		
		// Create the joker
		context().system().actorOf(Props.create(Joker.class), "joker");
		
		ArrayList<Inet.SocketOption> options = new ArrayList<Inet.SocketOption>();
		options.add(TcpSO.reuseAddress(true));
		Command bind = TcpMessage.bind(self(), localAddr, 10, options, false);
		tcp.tell(bind, self());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Tcp.Bind) {
			log.info("Accepting connections on " + localAddr);
		} else if (msg instanceof Tcp.CommandFailed) {
			Command cmd = ((Tcp.CommandFailed) msg).cmd();
			if (cmd instanceof Bind) {
				log.error("Could not bind to: " + localAddr);
			} else {
				log.error("Network error");
			}
			context().stop(self());
		} else if (msg instanceof Tcp.Connected) {
			ActorRef connection = getSender();
			context().actorOf(Props.create(Session.class, connection), "session_" + getSessionId());
		} else
			unhandled(msg);
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		// Stop sessions on any exceptions
		return SupervisorStrategy.stoppingStrategy();
	}
}