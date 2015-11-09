package tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import messages.*;
import akka.actor.*;

public class Channel extends UntypedActor {
	private Set<ActorRef> userSet = new HashSet<ActorRef>();
	private List<ChatMessage> history = new ArrayList<ChatMessage>();

	private String getChannelName() {
		return self().path().name();
	}

	private void tellAll(Backlog bLog) {
		for (ActorRef user : userSet) {
			// user.tell(bLog, getSelf());
			tellOne(user, bLog);
		}
	}
	
	private void tellOthers(ActorRef userRef, Backlog bLog) {
		List<ActorRef> userList = new ArrayList<>(userSet);
		userList.remove(userRef);
		for (ActorRef user : userList) {
			tellOne(user, bLog);
		}
	}

	private void tellOne(ActorRef user, Backlog bLog) {
		user.tell(bLog, getSelf());
	}

	private Backlog getBackLog(String source, String content, boolean isNewActor, boolean addToHistory) {
		List<ChatMessage> cMList = new ArrayList<ChatMessage>();

		if (isNewActor)
			return new Backlog(getChannelName(), history);
		else {
			ChatMessage cMessage = new ChatMessage(source, content);
			if(addToHistory)history.add(cMessage);
			cMList.add(cMessage);
			return new Backlog(getChannelName(), cMList);
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		// TODO: Implement the required functionality.
		if (msg instanceof ChatMessage) {
			ChatMessage cMessage = (ChatMessage) msg;
			if (!cMessage.content.isEmpty())
				tellAll(getBackLog(cMessage.source, cMessage.content, false, true));
			// else System.out.println("NOT SENDING EMPTY MESSAGE");
		} else if (msg instanceof AddUser) {
			ActorRef user = ((AddUser) msg).user;
			if (userSet.contains(user)){
				tellOne(user, getBackLog(getChannelName(), "You are already in this channel", false, false));
			}else {
				userSet.add(user);
				getContext().watch(user);
				user.tell(new UserAdded(getChannelName(), getSelf()), getSelf());
				tellOne(user, getBackLog(getChannelName(), "", true, false));
				tellOthers(user, getBackLog(getChannelName(), "User has joined the channel", false, false));
			}
		} else if (msg instanceof RemoveUser) {
			ActorRef rUser = ((RemoveUser) msg).user;
			userSet.remove(rUser);
			tellAll(getBackLog(getChannelName(), "User has left the channel", false, false));
			getSender().tell( new UserRemoved(getChannelName(), ((RemoveUser) msg).user), getSelf());

		}else if (msg instanceof JoinChannel){
			getSender().tell(new UserAdded(getChannelName(), getSender()), getSelf());
		
		} else if (msg instanceof Terminated) {
			getSelf().tell(new RemoveUser(((Terminated) msg).actor()), getSelf());
		} else {			
			unhandled(msg);
		}
			
	}
}