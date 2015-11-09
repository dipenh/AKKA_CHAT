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
	private final String jokerPath = "/user/joker";
	private final String jokeGenPath = "jokeGenerator";
	private final String channelPath = "/user/channels";
	private final String channelName = "jokes";
	private ActorRef channelRef;
	private ActorRef jokeGenerator;
	private boolean isStarting = true;
	
	{
		ActorSelection selection = context().system().actorSelection(channelPath+"/"+channelName);
		selection.tell(new Identify(identifyId), getSelf());
		context().system().eventStream().subscribe(getSelf(), NewSession.class);
	}
	
	
//	@Override
//	public void preStart() throws Exception {
//		// TODO Auto-generated method stub
//		super.preStart();
//		ActorSelection selection = contex().system().actorSelection("/user/channels/jokes");
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
	            	if(isStarting){
	            		context().system().actorSelection(channelPath).tell(new GetOrCreateChannel(channelName), getSelf());
	            		isStarting = false;
	            	}else{
	            		jokeGenerator = genJokeGenerator();
	            	}
	            } 
	         }
		}else if(msg instanceof ActorRef){
			channelRef = (ActorRef) msg;
			jokeGenerator = genJokeGenerator();
		}else if (msg instanceof String) { // (msg instanceof Object)
			System.out.println("JOKE: "+msg);
			channelRef.tell(new ChatMessage(channelName, msg.toString()), getSelf());
		}else if (msg instanceof NewSession){
			channelRef.tell(new AddUser(((NewSession) msg).session), getSelf());
		}else if (msg instanceof Terminated){
			context().unwatch(jokeGenerator);
			ActorSelection selection = context().system().actorSelection(jokerPath + "/"+ jokeGenPath);
			selection.tell(new Identify(identifyId), getSelf());
		}else{
			unhandled(msg);
		}
	}
	
	@Override
	public void aroundPreStart() {
		// TODO Auto-generated method stub
		super.aroundPreStart();
	}
	
	private ActorRef genJokeGenerator(){
		ActorRef jGenerator = context().actorOf(Props.create(JokeGenerator.class), jokeGenPath);
		context().watch(jGenerator);
		return jGenerator;
	}
	
	@Override
	public SupervisorStrategy supervisorStrategy() {
		// TODO Auto-generated method stub
		return svStrategy;
	}
	
	private SupervisorStrategy svStrategy = new OneForOneStrategy(10, Duration.create("1 minute"), 
			new Function<Throwable, Directive>() {
				
				@Override
				public Directive apply(Throwable t) throws Exception {
					// TODO Auto-generated method stub
					
					if (t instanceof DidNotGetJokeException){
//						System.out.println("Exception: DidNotGetJokeException");
						return resume();
					}
					else if(t instanceof JokeConnectionClosedException){
//						System.out.println("Exception: JokeConnectionClosedException");
						ActorRef jkGen = getSender();
						context().stop(jkGen);
						return resume();
					}
					else {
						return escalate();
					}
				}
			});
	
}