package tasks;


import messages.*;
import akka.actor.*;


public class ChannelManager extends UntypedActor {
	final String identifyId = "1";
	private String channelName;
	private ActorRef dest;

	public void onReceive(Object msg) throws Exception {
		// TODO: Implement the required functionality.
		if (msg instanceof GetOrCreateChannel) {
			dest = getSender();
			channelName = ((GetOrCreateChannel) msg).name;
			ActorSelection selection = context().actorSelection("/user/channels/"+channelName);
			selection.tell(new Identify(identifyId), getSelf());
			
		} else if(msg instanceof ActorIdentity){
			 ActorIdentity identity = (ActorIdentity) msg; 
		        if (identity.correlationId().equals(identifyId)) {
		            ActorRef ref = identity.getRef();
		            if (ref == null){
		            	ActorRef channel = context().actorOf(Props.create(Channel.class), channelName);
		            	context().watch(channel);
		            	dest.tell(channel, getSelf());
		            }	            
		            else {
		                dest.tell(ref, getSelf());
		            }
		        }
		}else
			unhandled(msg);
	}
}
