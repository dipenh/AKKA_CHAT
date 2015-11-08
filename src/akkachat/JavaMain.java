package akkachat;

public class JavaMain {
	public static void main(String[] args) {
        // This convenience method creates an Actor system and top-level actor
		akka.Main.main(new String[] { Server.class.getName() });
	}
}
