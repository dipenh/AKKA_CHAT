package akkachat;

import java.io.*;

import scala.Tuple2;
import akka.actor.*;
import akka.io.*;
import akka.io.Tcp.Event;
import akka.util.ByteString;

/**
 * Used by {@link Session} for communication with the client.
 */
public class SerializingConnection extends UntypedActor {
	private enum ActionType {
		CLOSE, SEND
	}

	public static class Action {
		protected final ActionType type;
		protected final Object argument;

		private Action(ActionType type, Object argument) {
			this.type = type;
			this.argument = argument;
		}

		public static Action Close() {
			return new Action(ActionType.CLOSE, null);
		}

		public static Action Send(Object command) {
			return new Action(ActionType.SEND, command);
		}
	}

	private final ActorRef connection;
	private boolean writePending = false;
	private Event writeToken = new Event() {};
	private ByteString writeBuffer = ByteString.empty();
	private boolean writeBufferEmpty = true;
	private ByteString readBuffer = ByteString.empty();
	private int read = 0;
	private int commandLength = 0;
	private boolean closing = false;

	public SerializingConnection(ActorRef connection) {
		this.connection = connection;
		connection.tell(TcpMessage.register(self()), self());
	}

	void parseCommands() throws IOException, ClassNotFoundException {
		while (true) {
			if (commandLength == 0) {
				if (read >= 4) {
					Tuple2<ByteString, ByteString> headerSplit = readBuffer.splitAt(4);
					byte[] header = headerSplit._1.toArray();
					readBuffer = headerSplit._2;
					read -= 4;
					for (byte b : header) {
						commandLength <<= 8;
						commandLength |= b & 0xFF;
					}
					if (commandLength < 0) {
						throw new RuntimeException("Illegal client command length: " + commandLength);
					}
				} else {
					break;
				}
			} else {
				if (read >= commandLength) {
					Tuple2<ByteString, ByteString> commandSplit = readBuffer.splitAt(commandLength);
					read -= commandLength;
					commandLength = 0;
					byte[] commandBuffer = commandSplit._1.toArray();
					readBuffer = commandSplit._2;
					ObjectInputStream deserializer = new ObjectInputStream(new ByteArrayInputStream(commandBuffer));
					Object command = deserializer.readObject();
					if (!closing) {
						context().parent().tell(command, self());
					}
				} else {
					break;
				}
			}
		}
	}

	void sendCommand(Object command) throws IOException {
		ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
		ObjectOutputStream serializer = new ObjectOutputStream(outputBuffer);
		serializer.writeObject(command);
		byte[] commandData = outputBuffer.toByteArray();
		byte[] header = new byte[4];
		int length = commandData.length;
		for (int i = 0; i < 4; ++i) {
			header[3 - i] = (byte) length;
			length >>= 8;
		}
		writeBuffer = writeBuffer.concat(ByteString.fromArray(header)).concat(ByteString.fromArray(commandData));
		writeBufferEmpty = false;
		doWrite();
	}

	void doWrite() {
		if (!writePending && !writeBufferEmpty) {
			connection.tell(TcpMessage.write(writeBuffer, writeToken), self());
			writeBuffer = ByteString.empty();
			writeBufferEmpty = true;
			writePending = true;
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Tcp.Received) {
			ByteString data = ((Tcp.Received) msg).data();
			read += data.toArray().length; // Ugly hack due to ByteString.length() being ambiguous when called from Java
			readBuffer = readBuffer.concat(data);
			parseCommands();
		} else if (msg == writeToken) {
			writePending = false;
			doWrite();
		} else if (msg instanceof Tcp.CommandFailed) {
			throw new RuntimeException("TCP I/O error: " + msg);
		} else if (msg instanceof Tcp.ConnectionClosed) {
			context().stop(self());
		} else if (msg instanceof Action) {
			Action action = (Action) msg;
			switch (action.type) {
			case CLOSE:
				closing = true;
				break;
			case SEND:
				if (!closing) {
					sendCommand(action.argument);
				}
				break;
			default:
				throw new RuntimeException("Unsupported action: " + action.type);
			}
		} else
			unhandled(msg);
	}
}
