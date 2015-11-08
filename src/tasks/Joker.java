package tasks;

import scala.Option;
import scala.concurrent.duration.Duration;
import messages.*;
import akka.actor.*;
import static akka.actor.SupervisorStrategy.*;
import akka.japi.Function;
import akkachat.DidNotGetJokeException;
import akkachat.JokeConnectionClosedException;
import akkachat.JokeGenerator;

public class Joker extends UntypedActor {
	private final String identifyId = "1";
	private final String channelName = "jokes";
	private ActorRef channelRef;
	private ActorRef jokeGenerator;
	
	{
		ActorSelection selection = context().system().actorSelection("/user/channels/jokes");
		selection.tell(new Identify(identifyId), getSelf());
		context().system().eventStream().subscribe(getSelf(), NewSession.class);
	}
	@Override
	public void postRestart(Throwable arg0) throws Exception {
		// TODO Auto-generated method stub
		super.postRestart(arg0);
		System.out.println("\n FUCKER +n");
		jokeGenerator = context().actorOf(Props.create(JokeGenerator.class));
	}
	
//	@Override
//	public void preStart() throws Exception {
//		// TODO Auto-generated method stub
//		super.preStart();
//		ActorSelection selection = getContext().system().actorSelection("/user/channels/jokes");
//		selection.tell(new Identify(identifyId), getSelf());
//		jokeGenerator = context().actorOf(Props.create(JokeGenerator.class));
//	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		// TODO: Implement the required functionality.
		if(msg instanceof ActorIdentity){
			ActorIdentity identity = (ActorIdentity) msg; 
	        if (identity.correlationId().equals(identifyId)) {
	            ActorRef ref = identity.getRef();
	            if (ref == null){
//	            	System.out.println("There is no channel");
	            	context().system().actorSelection("/user/channels").tell(new GetOrCreateChannel(channelName), getSelf());
	            } 
	         }
		}else if(msg instanceof ActorRef){
//			System.out.println("THE CHANNEL IS CREATED " + msg);
			channelRef = (ActorRef) msg;
			jokeGenerator = context().actorOf(Props.create(JokeGenerator.class));
		}else if (msg instanceof String) { // (msg instanceof Object)
			System.out.println("JOKE: "+msg);
			channelRef.tell(new ChatMessage(channelName, msg.toString()), getSelf());
		}else if (msg instanceof NewSession){
//			channelRef.tell(new UserAdded(channelName, ((NewSession) msg).session), getSelf());
			channelRef.tell(new AddUser(((NewSession) msg).session), getSelf());
		}else{
			System.out.println("AALU "+msg);
			unhandled(msg);
		}
	}
	
	@Override
	public SupervisorStrategy supervisorStrategy() {
		// TODO Auto-generated method stub
//		return super.supervisorStrategy();
//		if (strategy != null)return super.supervisorStrategy();
		return svStrategy;
	}
	
	private static SupervisorStrategy svStrategy = new OneForOneStrategy(10, Duration.create("1 minute"), 
			new Function<Throwable, Directive>() {
				
				@Override
				public Directive apply(Throwable t) throws Exception {
					// TODO Auto-generated method stub
					
					if (t instanceof DidNotGetJokeException){
//						System.out.println("Exception is : DidNotGetJokeException");
						return resume();
					}
					else if(t instanceof JokeConnectionClosedException){
						System.out.println("Exception is : JokeConnectionClosedException");
						return restart();
					}
					else {
						return escalate();
						
					}
				}
			});
	
}