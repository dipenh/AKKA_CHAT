package tasks;

import messages.*;
import akka.actor.*;
import scala.collection.mutable.HashSet;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class PartyBot extends UntypedActor {
	final String identifyId = "1";
	private HashSet<ActorRef> channels = new HashSet<ActorRef>();
	
	{
		context().system().scheduler().schedule(Duration.Zero(), Duration.create(5, TimeUnit.SECONDS),
				  new Runnable() {
				    @Override
				    public void run() {
				    	ActorSelection selection = context().system().actorSelection("/user/channels/*");
				    	selection.tell(new Identify(identifyId), getSelf());
				    }
				}, context().system().dispatcher());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		// TODO: Implement the required functionality.
		if (msg instanceof ActorIdentity){

			ActorIdentity identity = (ActorIdentity) msg; 
	        if (identity.correlationId().equals(identifyId)) {
	            ActorRef ref = identity.getRef();
	            if (ref == null){
//	            	System.out.println("There is no channel");
	            } 
	            else {
	            	if(!channels.contains(ref)){
	            		channels.add(ref);
	            		ref.tell(new JoinChannel(ref.path().toString()), getSelf());
	            	}
	            }
	        }
		}else if (msg instanceof UserAdded){
			getSender().tell(new ChatMessage("PartyBot", "Party! Party!"), getSelf());
		}else if(msg instanceof UserRemoved){
			channels.remove(getSender());
		}
		else{
			unhandled(msg);
		}
	}
	
}