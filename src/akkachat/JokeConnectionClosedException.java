package akkachat;

/**
 * Thrown by {@link JokeGenerator} when the connection has closed unexpectedly.
 * No more jokes will be sent from that instance after this.
 */
public class JokeConnectionClosedException extends Exception {
	private static final long serialVersionUID = 1L;
}
