package akkachat;

/**
 * Thrown by {@link JokeGenerator} when a joke could not be retrieved. This
 * causes a single joke to be skipped.
 */
public class DidNotGetJokeException extends Exception {
	private static final long serialVersionUID = 1L;
}
