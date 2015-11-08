package client;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class Connection implements Runnable {
	SocketChannel channel;
	Client client;
	Thread inputThread;

	public Connection(SocketChannel channel, Client client) {
		this.channel = channel;
		this.client = client;
		inputThread = new Thread(this);
		inputThread.start();
	}

	public void run() {
		try {
			ByteBuffer inputBuffer = ByteBuffer.allocate(4096);
			inputBuffer.limit(4);

			boolean readingHeader = true;
			InputLoop: while (!Thread.interrupted()) {
				if (channel.read(inputBuffer) == -1)
					break InputLoop;
				
				
				if (inputBuffer.remaining() == 0) {
					inputBuffer.rewind();
					if (readingHeader) {
						int commandLength = inputBuffer.getInt();
						if (commandLength < 0) {
							throw new RuntimeException("Illegal command length: " + commandLength);
						}
						if (commandLength > inputBuffer.capacity()) {
							inputBuffer = ByteBuffer.allocate(commandLength);
						}
						inputBuffer.rewind();
						inputBuffer.limit(commandLength);
						readingHeader = false;
					} else {
						byte[] contents = new byte[inputBuffer.limit()];
						inputBuffer.get(contents);
						ObjectInputStream deserializer = new ObjectInputStream(new ByteArrayInputStream(contents));
						Object command = deserializer.readObject();
						client.handleCommand(command);
						inputBuffer.rewind();
						inputBuffer.limit(4);
						readingHeader = true;
					}
				}
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (ClosedByInterruptException e) {
			// Do nothing
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				channel.close();
			} catch (IOException e) {}
			client.removeConnection();
		}
	}
	
	public void send(Object command) throws IOException {
		ByteArrayOutputStream objectBuffer = new ByteArrayOutputStream();
		ObjectOutputStream serializer = new ObjectOutputStream(objectBuffer);
		serializer.writeObject(command);
		ByteBuffer outputBuffer = ByteBuffer.allocate(objectBuffer.size() + 4);
		int length = objectBuffer.size();
		outputBuffer.putInt(length);
		outputBuffer.put(objectBuffer.toByteArray());
		outputBuffer.rewind();
		channel.write(outputBuffer);
	}
	
	public void close() {
		inputThread.interrupt();
	}
}