package akkachat;

import java.util.concurrent.TimeUnit;

import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.UntypedActor;

/**
 * Sends a joke to its parent every 5 seconds.
 * 
 * Can throw {@link DidNotGetJokeException}, which indicates that one joke will
 * not be sent.
 * 
 * Can throw {@link JokeConnectionClosedException}, which indicates that no more
 * jokes will be sent from this instance.
 */
public class JokeGenerator extends UntypedActor {
	private static final String[] JOKES = new String[] {
		"Did you hear about the blind carpenter who picked up his hammer and saw?",
		"Two aerials meet on a roof - fall in love - get married.  The ceremony was rubbish... but the reception was brilliant!",
		"Two flies are playing football in a saucer. One says to the other, 'Make an effort, we’re playing in the cup tomorrow.'",
		"Never date a tennis player. Love means nothing to them.",
		"People who process expired passports are so lazy, they’re always cutting corners.",
		"Recently in court, I was found guilty of being egotistical. I am appealing.",
		"Went to my allotment and found that there was twice as much soil as there was the week before. The plot thickens.",
		"I wasn’t sure about this beard at first but it’s grown on me.",
		"I bought a muzzle for my pet duck. Nothing flashy, but it fits the bill.",
	};
	
	private class JokeConnection {
		private boolean isClosed = false;
		private int jokeIndex = -1;
		
		private JokeConnection() {}
		
		public String readJoke() throws JokeConnectionClosedException, DidNotGetJokeException {
			if (!isClosed) {
				if (Math.random() < 0.1) {
					isClosed = true;
				}
				if (Math.random() < 0.9) {
					jokeIndex = (jokeIndex + 1) % JOKES.length;
					return JOKES[jokeIndex];
				} else
					throw new DidNotGetJokeException();
			} else
				throw new JokeConnectionClosedException();
		}
	}
	
	private final JokeConnection connection;
	private final ExecutionContext ec = context().system().dispatcher();
	private final Object getJoke = new Object();

	public JokeGenerator() {
		connection = new JokeConnection();
		context().system().scheduler().schedule(FiniteDuration.Zero(), FiniteDuration.create(5, TimeUnit.SECONDS),
				self(), getJoke, ec, self());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg == getJoke) {
			String joke = connection.readJoke();
			context().parent().tell(joke, self());
		} else
			unhandled(msg);
	}

}
