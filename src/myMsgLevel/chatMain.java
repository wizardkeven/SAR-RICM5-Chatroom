package myMsgLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.HashMap;

import com.sun.glass.ui.TouchInputSupport;


/**
 * This chatMain must be launched parameterized beginning with "-port=..." and "-connection=..."
 * Each parameter should be separated by a comma;
 * @author keven
 *
 */
public class chatMain {
	
	private static String hostID;
	private static final String PORT_ARG = "-port=";
	private static final String CONNECTION_ARG = "-connection=";
	private static final String HOSTID_ARG = "-host=";
	private static final String HOST_SERVER_CAPACITY = "-serverCapacity";
	private static String port;
	private static int[] portOrder = new int[6];// Allow 6 connections at most

	private enum conType {
		CONNECT, ACCEPT
	}

	private static HashMap<Integer, conType> connectionPaire; // key contains
																// the
																// port number,
																// while value
																// contains its
																// connection
																// type
	private static int hostCapacity;//Host server capacity

	static {
		connectionPaire = new HashMap<Integer, conType>();
	}

	/*
	 * This method parses arguments and fills them in the connectionPair
	 */
	static void parseArgs(String args[]) {
		String[] portSerial;
		for (int i = 0; i < args.length; i++){
			if (args[i].startsWith(HOSTID_ARG)) {
				hostID = args[i].substring(HOSTID_ARG.length());
			}else if (args[i].startsWith(PORT_ARG)) {
				port = args[i].substring(PORT_ARG.length());
				try {
					// In case of multiple connections
					if (port.contains(",")) {
						portSerial = port.split(",");
						for (int j = 0; j < portSerial.length; j++) {
							int portNum = Integer.parseInt(portSerial[j]);
							portOrder[j] = portNum;
							connectionPaire.put(portNum, null);
						}

					} else {// In case of one single connections
						int portNum = Integer.parseInt(port);
						portOrder[0] = portNum;
						connectionPaire.put(portNum, null);
					}
				} catch (Exception e) {
					System.out.print(e.getMessage());
				}
			} else if (args[i].startsWith(CONNECTION_ARG)) {
				try {
					String connections = args[i].substring(CONNECTION_ARG.length());
					if (connections.contains(",")) {
						String[] connectionsTypes = connections.split(",");
						for (int j = 0; j < connectionsTypes.length; j++) {
							if (connectionsTypes[j].equals(conType.ACCEPT.toString())) {
								connectionPaire.put(portOrder[j], conType.ACCEPT);
							} else if (connectionsTypes[j].equals(conType.CONNECT.toString())) {
								connectionPaire.put(portOrder[j], conType.CONNECT);
							} else {
								Exception Exception = new Exception("Unknown error! System will exit!");
								throw Exception;
							}
						}
					} else {
						if (connections.equals(conType.ACCEPT.toString())) {
							connectionPaire.put(portOrder[0], conType.ACCEPT);
						} else if (connections.equals(conType.CONNECT.toString())) {
							connectionPaire.put(portOrder[0], conType.CONNECT);
						} else {
							Exception Exception = new Exception("Unknown error! System will exit!");
							throw Exception;
						}
					}
				} catch (Exception e) {
					System.err.print(e.toString());
				}

			}else if (args[i].startsWith(HOST_SERVER_CAPACITY)) {
				String hCapacity = args[i].substring(CONNECTION_ARG.length());
				hostCapacity = Integer.parseInt(hCapacity);
			}
		}
		if (connectionPaire.isEmpty()) {
			System.err.println("No connection parameter entering, system will exit!");
			System.exit(-1);
		}
//		if (connectionPaire.values().size() != portOrder.length) {
//			System.err.println(
//					"Parameter error! PLease check the coherence of ports and connctionType! System will exit.");
//			System.exit(-1);
//		}
	}

	public static void main(String[] args) throws Exception {
		
		parseArgs(args);
		
		InetAddress m_localhost;
		Selector m_selector;
		ServerSocketChannel m_sch;// Allow you to listen for incoming TCP
									// connections, like a web server does.
									// For each incoming connection, a
									// SocketChannel is created
		SelectionKey m_skey; // Can read and write date over the network via TCP
		SocketChannel m_ch = null;
		int m_port;
		SocketChannel m_Channel = null;
		
		myConnectCallback m_MyConnectCallback = null;
		myAcceptCallback m_MyAcceptCallback;

		myChannel m_MyChannel = new myChannel(m_ch);
//		BufferedReader br;
//		String line;
//		do {
//			line = "";
//			br = new BufferedReader(new InputStreamReader(System.in));
//			line = br.readLine();
//		} while (line.isEmpty() || !line.equals("connect") || !line.equals("accept")); // For
//																						// a
//																						// client
//																						// who
//																						// launches
//																						// a
//																						// connection
//		// he/she should enter a "connect"; otherwise wait. Or enters a "accept"
//		// to open a server
//
//		br.close();// Close buffer

		m_localhost = InetAddress.getByName("localhost");
//		m_selector = SelectorProvider.provider().openSelector();
//		m_port = 12345;

		myEngine m_Engine = new myEngine(hostID);

		synchronized (connectionPaire) {
			for (int port : connectionPaire.keySet()) {
				System.out.print(port + ": " + connectionPaire.get(port) + "\n");

				if (connectionPaire.get(port).equals(conType.ACCEPT)) {
//					Thread.sleep(200);
					m_Engine.listen(port, new myAcceptCallback(m_MyChannel));
				} else if (connectionPaire.get(port).equals(conType.CONNECT)) {
//					Thread.sleep(100);
					m_MyConnectCallback = new myConnectCallback(m_MyChannel);
					m_Engine.connect(m_localhost, port, m_MyConnectCallback);
				}

			}
		}
		m_Engine.mainloop();// Main thread running always


//		Runnable echo = new Runnable() {
//
//			private BufferedReader bufferedReader;
//
//			public void run() {
//
//				synchronized (this) {
//					while (!m_MyChannel.isConnected()) {
//						try {
//							m_MyChannel.wait();
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				}
//
//				System.out.println(">>> Ping got writing room...");
//
//				String info = "I am Guo Kai, say hello to you!";
//				for (;;) {
//					handleInput(info);
//					info = "";
//					bufferedReader = new BufferedReader(new InputStreamReader(System.in));
//					try {
//						info = bufferedReader.readLine();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					if (info.equals("Quit") || info.equals("quit")) {
//						try {
//							m_MyChannel.close();
//						} catch (Exception e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						System.exit(0);
//						System.out.println(">>> Ping sent bytes.");
//					}
//				}
//			}
//
//			private ByteBuffer byteBuffer;
//			private Charset charset = Charset.forName("UTF-8");;
//
//			/**
//			 * @param info
//			 */
//			protected void handleInput(String info) {
//				CharBuffer charBuffer = CharBuffer.wrap(info + "\n");
//				System.out.println("Info from Ping: " + info);
//				byteBuffer = charset.encode(charBuffer);
//				byteBuffer.compact();
//				byteBuffer.flip();
//				m_MyChannel.send(byteBuffer);
//			}
//		};

	}

}
