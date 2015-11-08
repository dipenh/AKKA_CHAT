package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.HashMap;

import javax.swing.*;

import akkachat.Server;
import messages.*;

public class Client {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Client();
			}
		});
	}

	private JFrame frame;
	private JTabbedPane chatsPane;
	private JTextArea logArea;
	private JButton connectDisconnectButton;
	private JButton joinDialogButton;

	private JDialog connectDialog;
	private JTextField addressField;
	private JTextField nameField;

	private JDialog joinDialog;
	private JTextField channelField;

	private Connection connection;

	private HashMap<String, JPanel> channelPanels = new HashMap<String, JPanel>();
	private HashMap<String, JTextArea> channelMessageAreas = new HashMap<String, JTextArea>();
	private HashMap<String, JTextField> channelMessageFields = new HashMap<String, JTextField>();

	public Client() {
		{// Construct the main frame
			frame = new JFrame("Akka chat client");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().setPreferredSize(new Dimension(800, 600));
			frame.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1;
			c.weighty = 0;

			JPanel bar = new JPanel();
			frame.getContentPane().add(bar, c);

			connectDisconnectButton = new JButton("Connect");
			connectDisconnectButton.addActionListener(connectDisconnectListener);
			bar.add(connectDisconnectButton);

			joinDialogButton = new JButton("Join");
			joinDialogButton.setEnabled(false);
			joinDialogButton.addActionListener(joinDialogListener);
			bar.add(joinDialogButton);

			chatsPane = new JTabbedPane();
			c.fill = GridBagConstraints.BOTH;
			c.gridy = 1;
			c.weighty = 1;
			frame.getContentPane().add(chatsPane, c);

			logArea = new JTextArea();
			logArea.setEditable(false);
			logArea.setLineWrap(true);
			chatsPane.addTab("Log", logArea);

			frame.pack();
			frame.setVisible(true);
		}

		{// Construct the connect dialog
			connectDialog = new JDialog(frame, true);
			connectDialog.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;

			JLabel nameLabel = new JLabel("Name");
			c.gridx = 0;
			connectDialog.add(nameLabel, c);

			nameField = new JTextField(40);
			nameField.setText("user" + (int) (Math.random() * 10000));
			c.gridy = 1;
			connectDialog.add(nameField, c);

			JLabel addressLabel = new JLabel("Server address");
			c.gridy = 2;
			connectDialog.add(addressLabel, c);

			addressField = new JTextField(40);
			addressField.setText("localhost");
			c.gridy = 3;
			connectDialog.add(addressField, c);

			JButton connectButton = new JButton("Connect");
			connectButton.addActionListener(connectListener);
			c.gridy = 4;
			connectDialog.add(connectButton, c);

			connectDialog.pack();
		}

		{// Construct the join dialog
			joinDialog = new JDialog(frame, true);
			joinDialog.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;

			JLabel channelLabel = new JLabel("Channel");
			c.gridx = 0;
			joinDialog.add(channelLabel, c);

			channelField = new JTextField(40);
			channelField.setText("general");
			c.gridy = 1;
			joinDialog.add(channelField, c);

			JButton joinButton = new JButton("Join");
			joinButton.addActionListener(joinListener);
			c.gridy = 2;
			joinDialog.add(joinButton, c);

			joinDialog.pack();
		}
	}

	private void addChatTab(final String channel) {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridwidth = 3;
		c.gridy = 0;

		JTextArea messageArea = new JTextArea();
		messageArea.setEditable(false);
		messageArea.setLineWrap(true);
		c.fill = GridBagConstraints.BOTH;
		panel.add(new JScrollPane(messageArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), c);

		JTextField messageField = new JTextField();
		messageField.addActionListener(createSendMessageListener(channel));
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridy = 1;
		c.weighty = 0;
		panel.add(messageField, c);
		
		JButton sayButton = new JButton("Say");
		sayButton.addActionListener(createSendMessageListener(channel));
		c.gridx = 1;
		c.weightx = 0;
		panel.add(sayButton, c);

		chatsPane.addTab(channel, panel);
		chatsPane.setSelectedIndex(chatsPane.getTabCount() - 1);
		
		{// Tab title
			JPanel tabPanel = new JPanel(new GridBagLayout());
			tabPanel.setBackground(new Color(0,0,0,0));
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = 0;
			c.gridy = 0;
			
			JLabel tabLabel = new JLabel(channel);
			tabPanel.add(tabLabel, c);
			
			Icon closeIcon = UIManager.getIcon("InternalFrame.closeIcon");
			JButton leaveButton;
			if (closeIcon != null) {
				leaveButton = new JButton(closeIcon);
				leaveButton.setBorder(BorderFactory.createEmptyBorder());
			} else {
				leaveButton = new JButton("x");
			}
			leaveButton.addActionListener(createLeaveChannelListener(channel));
			c.gridx = 1;
			c.weightx = 0;
			c.insets = new Insets(0, 4, 0, 0);
			tabPanel.add(leaveButton, c);
			
			chatsPane.setTabComponentAt(chatsPane.getTabCount() - 1, tabPanel);
		}
		
		channelPanels.put(channel, panel);
		channelMessageAreas.put(channel, messageArea);
		channelMessageFields.put(channel, messageField);
	}

	private ActionListener connectDisconnectListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (connection != null) {
				connection.close();
			} else {
				connectDialog.setVisible(true);
			}
		}
	};

	private ActionListener connectListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String[] addressParts = addressField.getText().split(":");
			String host = addressParts[0];
			int port = Server.DEFAULT_PORT;
			if (addressParts.length == 2) {
				try {
					port = Integer.parseInt(addressParts[1]);
				} catch (NumberFormatException e1) {
					JOptionPane.showMessageDialog(connectDialog, "Port must be an integer.", "Invalid port",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (addressParts.length > 2) {
				JOptionPane.showMessageDialog(connectDialog,
						"Too many colons. Please use either <hostname/ip> or <hostname/ip>:<port>.", "Invalid address",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			SocketChannel channel;
			try {
				channel = SocketChannel.open(new InetSocketAddress(host, port));
			} catch (UnresolvedAddressException e1) {
				JOptionPane.showMessageDialog(connectDialog, e1.getMessage(), "Could not connect",
						JOptionPane.ERROR_MESSAGE);
				return;
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(connectDialog, e1.getMessage(), "Could not connect",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			log("Connected");

			connection = new Connection(channel, Client.this);
			connectDisconnectButton.setText("Disconnect");
			joinDialogButton.setEnabled(true);
			connectDialog.setVisible(false);

			try {
				connection.send(new SetName(nameField.getText()));
			} catch (IOException e1) {
				connection.close();
				log("Could not authenticate user");
				log(e1);
			}
		}
	};

	private ActionListener joinDialogListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			joinDialog.setVisible(true);
		}
	};

	private ActionListener joinListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			try {
				connection.send(new JoinChannel(channelField.getText()));
			} catch (IOException e1) {
				log(e1.toString());
			}
			joinDialog.setVisible(false);
		}
	};

	private ActionListener createSendMessageListener(final String channel) {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (channelMessageFields.containsKey(channel)) {
					JTextField field = channelMessageFields.get(channel);
					String message = field.getText();
					field.setText("");
	
					try {
						connection.send(new Say(channel, message));
					} catch (IOException e1) {
						log("Could not send message");
						log(e1);
					}
				}
			}
		};
	}

	private ActionListener createLeaveChannelListener(final String channel) {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					connection.send(new LeaveChannel(channel));
				} catch (IOException e1) {
					log("Could not leave channel");
					log(e1);
				}
			}
		};
	}
	
	private void removeChannel(String channel) {
		if (channelPanels.containsKey(channel)) {
			chatsPane.remove(channelPanels.get(channel));
			channelPanels.remove(channel);
			channelMessageAreas.remove(channel);
			channelMessageFields.remove(channel);
		}
	}

	public void handleCommand(final Object cmd) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (cmd instanceof LogMessage) {
					log(((LogMessage) cmd).content);
				} else if (cmd instanceof JoinChannel) {
					String channel = ((JoinChannel) cmd).channel;
					addChatTab(channel);
				} else if (cmd instanceof LeaveChannel) {
					String channel = ((LeaveChannel) cmd).channel;
					removeChannel(channel);
				} else if (cmd instanceof Backlog) {
					String channel = ((Backlog) cmd).channel;
					if (channelMessageAreas.containsKey(channel)) {
						JTextArea targetArea = channelMessageAreas.get(channel);
						for (ChatMessage line : ((Backlog) cmd).lines) {
							targetArea.append(line.source + ": " + line.content + "\n");
						}
					}
				} else {
					log("Unsupported server message: " + cmd.getClass().getCanonicalName());
				}
			}
		});
	}

	public void removeConnection() {
		connection = null;
		connectDisconnectButton.setText("Connect");
		joinDialogButton.setEnabled(false);
		for (JPanel panel : channelPanels.values()) {
			chatsPane.remove(panel);
		}
		channelPanels.clear();
		channelMessageAreas.clear();
		channelMessageFields.clear();
		log("Disconnected");
	}

	private void log(String message) {
		logArea.append(message + "\n");
	}
	
	private void log(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		log(writer.toString());
	}
}