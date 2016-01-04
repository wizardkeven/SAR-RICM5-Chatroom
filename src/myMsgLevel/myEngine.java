package myMsgLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.crypto.Data;

import com.sun.swing.internal.plaf.synth.resources.synth;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import messages.engine.AcceptCallback;
import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Server;
import sun.net.NetworkClient;

public class myEngine extends Engine {

	private static final String REMOTE_HOST_NAME = "SAR_RICM5_GUOKAI";
	private Selector m_selector;
	private SelectionKey m_key;
	// private SocketChannel m_ch;// Common channel for launching connection or
	// // data transferring
	private myChannel m_MyChannel;
	private HashMap<SocketChannel, Integer> socketChannelsList; // Common
																// channel list
																// for launching
																// connection or
	// data transferring
	private Channel channel;
	private ServerSocket server;
	private ServerSocketChannel m_sch;
	// private ArrayList<ServerSocketChannel> sscList = new
	// ArrayList<ServerSocketChannel>();
	private InetAddress serverIP;

	private Charset charset = Charset.forName("UTF-8");
	private ByteBuffer writeBuffer;
	private BufferedReader readBuffer;
	private SocketChannel mainSocketChannel;
	private static String hostName;
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
	private boolean delivered = false;
	private String info="";
	private int clientNumber;
	private ArrayList<SocketChannel> readySocketChannel;
	private static final long TIMEOUT = 10000;

	/**
	 * This hashmap is important. It keeps track of the data
	 */
	private Map<SocketChannel, String> dataTracking = new HashMap<SocketChannel, String>();
	
	Runnable monitorReceive = new Runnable() {
		
		@Override
		public void run() {
			try {

				while (true) {
					/**
					 * selector.select(TIMEOUT) is waiting for an OPERATION to
					 * be ready and is a blocking call. For example, if a client
					 * connects right this second, then it will break from the
					 * select() call and run the code below it. The TIMEOUT is
					 * not needed, but its just so it doesn't block
					 * undefinitely.
					 */
					if (clientNumber!=readySocketChannel.size()) {
						continue;
					}
					if (m_selector == null) {
						continue;
					}

					m_selector.select(TIMEOUT);
					if (!readySocketChannel.isEmpty()) {
						try {
							for (int i = 0; i < readySocketChannel.size(); i++) {
								SocketChannel socketChannel = readySocketChannel.get(i);
								socketChannel.register(m_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//								this.connectCount++;
							}
						} catch (ClosedChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					synchronized (m_selector) {
						/**
						 * If we are here, it is because an operation happened
						 * (or the TIMEOUT expired). We need to get the
						 * SelectionKeys from the selector to see what
						 * operations are available. We use an iterator for
						 * this.
						 */
						Iterator<SelectionKey> keys = m_selector.selectedKeys().iterator();
						while (keys.hasNext()) {
							SelectionKey key = keys.next();
							// remove the key so that we don't process this
							// OPERATION
							// again.
							keys.remove();

							// key could be invalid if for example, the client
							// closed
							// the connection.
							if (!key.isValid()) {
								continue;
							}

							if (key.isReadable()) {
								read(key);
							}
						}
					}

				}
		} catch (IOException e) {
			e.printStackTrace();
		}
		}

		synchronized private void read(SelectionKey key) throws IOException {
			SocketChannel ch = (SocketChannel) key.channel();
			ByteBuffer buf = ByteBuffer.allocate(1024);
			buf.clear();
			int count = 0;
			count = ch.read(buf);
			if (count == -1) {
				buf.clear();
				return;
//				System.err.println("<<<"+hostName+": end of stream!");
//				System.exit(-1);
			}

			// byte[] data = new byte[1000];
			buf.flip();
			// buf.get(data, 0, count);
			// System.out.println("Received: "+new String(data));
			CharBuffer charBuffer = charset.decode(buf);
			buf.clear();
			 System.out.print(charBuffer);
			// echoReceiving(key,data);
			if (charBuffer.toString().startsWith(REMOTE_HOST_NAME)) {
				String[] displayInfo = charBuffer.toString().split(REMOTE_HOST_NAME);
				Date timeStamp = new Date(System.currentTimeMillis());
				System.out.println(displayInfo[1] + " said at: " + timeStamp + " : " + displayInfo[2]);
				charBuffer.clear();
				if (displayInfo[2].equalsIgnoreCase("quit")) {
					ch.close();
					// System.exit(0);
					System.out.println(ch.toString() + ">>> closes.");
				}
				// this.info = hostName+" received message from "+
				// displayInfo[1];
				// info = "Cao, wo shi "+hostName;
				// handleWrite(key);
			}
			if (charBuffer.toString().equalsIgnoreCase("quit")) {
				System.out.flush();
				System.exit(0);
			}
		}
	};

	Runnable monitorInput = new Runnable() {
		BufferedReader bufferedReader;
		int sendCount=0;

		@Override
		public void run() {

			// while (true) {
			//// if(!mainSocketChannel.isOpen()) {
			try {


				// A run the server as long as the thread is not interrupted.

				while (true) {
					/**
					 * selector.select(TIMEOUT) is waiting for an OPERATION to
					 * be ready and is a blocking call. For example, if a client
					 * connects right this second, then it will break from the
					 * select() call and run the code below it. The TIMEOUT is
					 * not needed, but its just so it doesn't block
					 * undefinitely.
					 */
					if (info.isEmpty()&&readySocketChannel!=null&&clientNumber!=readySocketChannel.size()) {
						continue;
					}
					if (m_selector == null) {
						continue;
					}
//					if (info.isEmpty()&&clientNumber==readySocketChannel.size()) {
						bufferedReader = new BufferedReader(new InputStreamReader(System.in));
						info = bufferedReader.readLine();
						bufferedReader = null;
//					}
					
					if (!readySocketChannel.isEmpty()) {
						try {
							for (int i = 0; i < readySocketChannel.size(); i++) {
								SocketChannel socketChannel = readySocketChannel.get(i);
								socketChannel.register(m_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//								connectCount++;
							}
						} catch (ClosedChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					m_selector.select(TIMEOUT);

					synchronized (m_selector) {
						/**
						 * If we are here, it is because an operation happened
						 * (or the TIMEOUT expired). We need to get the
						 * SelectionKeys from the selector to see what
						 * operations are available. We use an iterator for
						 * this.
						 */
						Iterator<SelectionKey> keys = m_selector.selectedKeys().iterator();
						while (keys.hasNext()) {
							SelectionKey key = keys.next();
							// remove the key so that we don't process this
							// OPERATION
							// again.
							keys.remove();

							// key could be invalid if for example, the client
							// closed
							// the connection.
							if (!key.isValid()) {
								continue;
							}

							if (key.isWritable()) {
								if (!info.isEmpty()) {
									System.out.println("Writing...");
									write(key);
								}else {
									sendCount=0;
								}
								
							}
						}
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		synchronized private void write(SelectionKey key) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			String mString;
			CharBuffer charBuffer;

			mString = info;
			charBuffer = CharBuffer.wrap(REMOTE_HOST_NAME + hostName + REMOTE_HOST_NAME + mString + "\n");
			info = "";
			// System.out.println("Info from Ping: " + charBuffer);
			writeBuffer = charset.encode(charBuffer);
			writeBuffer.compact();
			writeBuffer.flip();
			// send(key, writeBuffer);
			channel.write(writeBuffer);//
			System.out.println("finished send");
			// } else {
			// delivered = false;
			// }
			// }
			writeBuffer.clear();
			sendCount++;

		}

	};
	private boolean NoClient;

	public myEngine(String hostID, int i) {
		NoClient = i==0;
		myEngine.hostName = hostID;
		try {
			this.serverIP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			System.err.println("Get localhost ip address error! " + e1.getMessage());
		}
		try {
			this.m_selector = Selector.open();// SelectorProvider.provider().openSelector();
		} catch (IOException e) {
			System.err.println("Open selector error! " + e.getMessage());
		}

		this.socketChannelsList = new HashMap<SocketChannel, Integer>();
		this.info = "Hello, I am " + hostName;
		readySocketChannel = new ArrayList<SocketChannel>();
		Thread monitorInputThread = new Thread(monitorInput);
		monitorInputThread.start();
//		Thread monitorReceiveThread = new Thread(monitorReceive);
//		monitorReceiveThread.start();
//		 this.startEcho();

	}

	@Override
	public void startEcho() {
		// TODO Auto-generated method stub
		super.startEcho();

	}

	/**
	 * NIO engine mainloop Wait for selected events on registered channels
	 * Selected events for a given channel may be ACCEPT, CONNECT, READ, WRITE
	 * Selected events for a given channel may change over time
	 */
	@Override
	synchronized public void mainloop() {
		// long delay = 0;
		for (;;) {
			int num = 0;
			try {
				num = this.m_selector.select();
				if (num == 0) {
					continue;
				}
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
//			if(readySocketChannel.size()==clientNumber)
//				System.out.println("Total client number is "+clientNumber);

			if (!this.readySocketChannel.isEmpty()) {
				try {
					for (int i = 0; i < readySocketChannel.size()&& readySocketChannel.size()==clientNumber; i++) {
						SocketChannel socketChannel = this.readySocketChannel.get(i);
						socketChannel.register(m_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
						this.connectCount++;
					}
				} catch (ClosedChannelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				synchronized (m_selector) {
					this.m_selector.select(TIMEOUT);
					Iterator<?> selectedKeys = this.m_selector.selectedKeys().iterator();
					if (selectedKeys.hasNext()) {
						SelectionKey key = (SelectionKey) selectedKeys.next();

						if (!key.isValid()) {
							System.err.println(">>> Ping:  ---> readable key=" + key);
							System.exit(-1);
						} else if (key.isAcceptable()) {
							try {
//								System.out.println(">>> Ping:  ---> readable key=" + key);
								handleAccept(key);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} 
						else if (key.isReadable()) {
							// System.out.println("<<< Pong: ---> readable key=" +
							// key);
							try {
								handleRead(key);
							} catch (IOException e) {
								System.err.println("Reading data error! " + e.getMessage());
							}
						}
						// else if (key.isWritable()) {
						// // Let channel write
						// // System.out.print(hostName+": is reading....");
						// // rwEnabled =true;
						// if (readBuffer.read() != -1 || !info.isEmpty()) {
						// handleWrite(key);
						// }
						//
						// }
						// else if (key.isConnectable()) {
						//
						// try {
						//
						// handleConnect(key);
						// } catch (IOException e) {
						// System.err.println("Connecting to server error! " +
						// e.getMessage());
						// }
						//
						// System.out.println(">>> Ping: ---> connectable key=" +
						// key);
						// }
						selectedKeys.remove();

					}
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	// ==============to be discussed===============
	synchronized private void handleWrite(SelectionKey key) {
		// REMOTE_HOST_NAME as a separator of the message aiming at identifying
		// the remote sender
		SocketChannel channel = (SocketChannel) key.channel();
		String mString;
		CharBuffer charBuffer;
		if (this.info.isEmpty()) {
			mString = dataTracking.get(channel);
			dataTracking.remove(channel);
			charBuffer = CharBuffer.wrap(REMOTE_HOST_NAME + hostName + REMOTE_HOST_NAME + mString + "\n");
		} else {
			mString = info;
			charBuffer = CharBuffer.wrap(REMOTE_HOST_NAME + hostName + REMOTE_HOST_NAME + mString + "\n");
			this.info = "";
		}

		this.info = "";
		// System.out.println("Info from Ping: " + charBuffer);
		writeBuffer = charset.encode(charBuffer);
		writeBuffer.compact();
		writeBuffer.flip();
		send(key, writeBuffer);
	}

	synchronized private void send(SelectionKey key, ByteBuffer byteBuffer) {
		try {
			int length = 0;
			// synchronized (this.socketChannelsList) {
			// for (SocketChannel c_SocketChannel : socketChannelsList.keySet())
			// {
			// if
			// (c_SocketChannel!=null&&socketChannelsList.get(c_SocketChannel))
			// {
			SocketChannel ss_socketChannel = (SocketChannel) key.channel();
			length = ss_socketChannel.write(byteBuffer);//

			// } else {
			// delivered = false;
			// }
			// }
			byteBuffer.clear();
			// if (delivered) {
			// System.out.print(length+" words have been delivered
			// successfully!");
			// }else {
			// System.out.print("Message has not been completely delivered!");
			// }
			// }

			// if (byteBuffer.position() != length) {
			// System.out.println(" ---> wrote " + count + " bytes.");
			// System.exit(-1);
			// }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	void send(byte bytes[], int offset, int length) throws IOException {

		ByteBuffer buf;
		buf = ByteBuffer.allocate(4 + length);
		buf.putInt(length);
		buf.put(bytes, offset, length);
		int count = buf.position();
		buf.position(0);
		// if (count != m_ch.write(buf)) {
		// System.out.println(" ---> wrote " + count + " bytes.");
		// System.exit(-1);
		// }
	}

	// =======================================================
	synchronized void handleConnect(SelectionKey key) throws IOException {
		SocketChannel ch = (SocketChannel) key.channel();
		// if (m_ch != ch) {
		// System.err.println(">>> Ping: ServerSocker mismatch!");
		// System.exit(-1);
		// }
		// ch.configureBlocking(false);
		// ch.socket().setTcpNoDelay(true);
		if (ch.isConnectionPending()) {
			ch.finishConnect();
			System.out
					.println("Connection was pending but now is finiehed connecting to port: " + ch.socket().getPort());
			System.out.println(">>> Ping connected.");
			ch.register(m_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			// this.m_key.interestOps(SelectionKey.OP_READ |
			// SelectionKey.OP_WRITE);
			// socketChannelsList.put(ch, );
		}
	}

	@Override
	public Server listen(int port, AcceptCallback callback) throws IOException {

		System.out.println("<<< Pong: accepting...");
		// create a new non-blocking server socket channel
		// ServerSocketChannel tempSSC= ServerSocketChannel.open();
		this.m_sch = ServerSocketChannel.open();
		this.m_sch.configureBlocking(false);
		this.server = m_sch.socket();

		// bind the server socket to the specified address and port
		// InetSocketAddress isa = new InetSocketAddress(serverIP, port);
		// tempSSC.socket().bind(new InetSocketAddress(port));
		this.server.bind(new InetSocketAddress(port));

		this.m_key = this.m_sch.register(this.m_selector, SelectionKey.OP_ACCEPT);
		return new myServer(new InetSocketAddress(port), this.m_selector);
		// return null;
	}

	@Override
	public void connect(InetAddress hostAddress, int port, ConnectCallback callback)
			throws UnknownHostException, SecurityException, IOException {
		// TODO send connecting request
		myConnectCallback e_MyConnectCallback = (myConnectCallback) callback;

		// try {
		// connectToServer(hostAddress, port);
		// create a non-blocking socket channel
		SocketChannel tempSocketChannel;
		tempSocketChannel = SocketChannel.open();
		tempSocketChannel.configureBlocking(false);
		tempSocketChannel.socket().setTcpNoDelay(true);

		// be notified when the connection to the server will be accepted
		this.m_key = tempSocketChannel.register(this.m_selector, SelectionKey.OP_CONNECT);

		// request to connect to the server
		tempSocketChannel.connect(new InetSocketAddress(port));
		this.socketChannelsList.put(tempSocketChannel, port);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// If succeeding, call ConnectCallback
		// TODO
		e_MyConnectCallback.connected(channel);
	}

	// =======================For active
	// mode============================================
	public void connectToServer(InetSocketAddress requsetedServerAddress)
			throws UnknownHostException, SecurityException, IOException, InterruptedException {
		this.clientNumber++;
		// create a non-blocking socket channel
		// SocketChannel tempSocketChannel;
		// tempSocketChannel = SocketChannel.open();
		// tempSocketChannel.configureBlocking(false);
		// tempSocketChannel.socket().setTcpNoDelay(true);

		// // be notified when the connection to the server will be accepted
		// this.m_key = tempSocketChannel.register(this.m_selector,
		// SelectionKey.OP_CONNECT);
		//
		// // request to connect to the server
		// tempSocketChannel.connect(requsetedServerAddress);
		// socketChannelsList.put(tempSocketChannel, port);
		Thread client = new Thread(new myClient(hostName, requsetedServerAddress, readySocketChannel));
		client.start();
	}

	// ===============End of active mode==================================

	// ===================For passive mode===============================
	// synchronized void accept(int port) throws IOException {
	//
	// // create a new non-blocking server socket channel
	// ServerSocketChannel tempSSC= ServerSocketChannel.open();
	// tempSSC.configureBlocking(false);
	// server = tempSSC.socket();
	//
	// // bind the server socket to the specified address and port
	//// InetSocketAddress isa = new InetSocketAddress(serverIP, port);
	//// tempSSC.socket().bind(new InetSocketAddress(port));
	// this.server.bind(new InetSocketAddress(port));
	//
	// m_key = tempSSC.register(m_selector, SelectionKey.OP_ACCEPT);
	//// this.m_sch = tempSSC;
	//// sscList.add(tempSSC);
	//
	// }

	/**
	 * Handle server socket accepting issues
	 * 
	 * @param key
	 * @throws IOException
	 */
	synchronized private void handleAccept(SelectionKey key) throws IOException {
		// if (key != m_key) {
		// System.err.println("<<< Pong: SelectionKey mismatch!");
		// System.exit(-1);
		// }
		// ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		Socket client = this.server.accept();

		this.acceptCount++;
		System.out.println("Client Connected...." + "you have " + this.acceptCount + " clients connected");
		// if (!sscList.contains(ssc)) {//ssc != m_sch
		// System.err.println("<<< Pong: ServerSocker mismatch!");
		// System.exit(-1);
		// }
		SocketChannel clientChannel = client.getChannel();
		// tempSocketChannel = ssc.accept();
		clientChannel.configureBlocking(false);
		// clientChannel.socket().setTcpNoDelay(true);
//		if (NoClient) {
//			this.m_key = clientChannel.register(this.m_selector, SelectionKey.OP_WRITE);
//		}else {
//			this.m_key = clientChannel.register(this.m_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//		}
		this.m_key = clientChannel.register(this.m_selector, SelectionKey.OP_WRITE);
		info = "Hello, I am "+hostName;
		this.mainSocketChannel = clientChannel;
		notify();
		// rwEnabled = true;
		// this.socketChannelsList.put(clientChannel,);
		System.out.println("<<<" + hostName + ": receiving...");
		// byte[] hello = new String("Hello from server: "+hostName).getBytes();
		dataTracking.put(clientChannel, info);

	}

	synchronized private void handleRead(SelectionKey key) throws IOException {
		synchronized (key) {
			SocketChannel ch = (SocketChannel) key.channel();
			ByteBuffer buf = ByteBuffer.allocate(1024);
			buf.clear();
			int count = 0;
			count = ch.read(buf);
			if (count == -1) {
				System.err.println("<<< Pong: end of stream!");
				System.exit(-1);
			}

			// byte[] data = new byte[1000];
			buf.flip();
			// buf.get(data, 0, count);
			// System.out.println("Received: "+new String(data));
			CharBuffer charBuffer = charset.decode(buf);
			buf.clear();
			// System.out.print(charBuffer);
			// echoReceiving(key,data);
			if (charBuffer.toString().startsWith(REMOTE_HOST_NAME)) {
				String[] displayInfo = charBuffer.toString().split(REMOTE_HOST_NAME);
				Date timeStamp = new Date(System.currentTimeMillis());
				System.out.println(displayInfo[1] + " said at: " + timeStamp + " : " + displayInfo[2]);
				charBuffer.clear();
				if (displayInfo[2].equalsIgnoreCase("quit")) {
					ch.close();
					// System.exit(0);
					System.out.println(ch.toString() + ">>> closes.");
				}
				// this.info = hostName+" received message from "+
				// displayInfo[1];
				// info = "Cao, wo shi "+hostName;
				// handleWrite(key);
			}
			if (charBuffer.toString().equalsIgnoreCase("quit")) {
				System.out.flush();
				System.exit(0);
			}
		}

	}
	// ================================================================

	// private void echoReceiving(SelectionKey key, byte[] data) {
	//
	// SocketChannel socketChannel = (SocketChannel) key.channel();
	//// dataTracking.put(socketChannel, data);
	// }

	/**
	 * Launch a server for incoming connections requests
	 * 
	 * @param inetSocketAddress
	 */
	public void startServer(InetSocketAddress inetSocketAddress) {
		// TODO Auto-generated method stub
		Thread server = new Thread(new myServer(inetSocketAddress, this.m_selector));
		server.start();
	}

}
