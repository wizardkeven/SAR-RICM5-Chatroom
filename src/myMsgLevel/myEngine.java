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
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

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

public class myEngine extends Engine{

	private static final String REMOTE_HOST_NAME = "SAR_RICM5_GUOKAI";
	private Selector m_selector;
	private SelectionKey m_key;
//	private SocketChannel m_ch;// Common channel for launching connection or
//								// data transferring
	private myChannel m_MyChannel;
	private HashMap<SocketChannel,Integer> socketChannelsList; // Common channel list for launching connection or
	// data transferring
	private Channel channel;
	private ServerSocket server;
	private ServerSocketChannel m_sch;
//	private ArrayList<ServerSocketChannel> sscList = new ArrayList<ServerSocketChannel>();
	private InetAddress serverIP;

	private Charset charset = Charset.forName("UTF-8");
	private ByteBuffer writeBuffer;
	private BufferedReader readBuffer;
	private boolean rwEnabled = false;
	private static String hostName;
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm"); 
	private boolean delivered =false;
	private String info;
	private int clientNumber;
	
	

	public myEngine(String hostID) {
		
		myEngine.hostName = hostID;
		try {
			this.serverIP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			System.err.println("Get localhost ip address error! " + e1.getMessage());
		}
		try {
			this.m_selector = Selector.open();//SelectorProvider.provider().openSelector();
		} catch (IOException e) {
			System.err.println("Open selector error! " + e.getMessage());
		}
		
		this.socketChannelsList = new HashMap<SocketChannel,Integer>();
		this.info = "Hello, I am "+hostName;

	}

	/**
	 * NIO engine mainloop Wait for selected events on registered channels
	 * Selected events for a given channel may be ACCEPT, CONNECT, READ, WRITE
	 * Selected events for a given channel may change over time
	 */
	@Override
	synchronized public void mainloop() {
//		long delay = 0;
		for (;;) {
//			
//			if (rwEnabled) {
//				
//				readBuffer = new BufferedReader(new InputStreamReader(System.in));
//				try {
//					info = readBuffer.readLine();
//					if (info.equalsIgnoreCase("quit")) {
//						m_sch.close();
//						System.out.println(">>> "+" sent bytes.");
//						System.exit(0);
//					}
////					handleWrite(info);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			try {
//				try {
//					this.m_selector.select(delay);
//				} catch (IOException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			int num = 0;
			try {
				num = this.m_selector.select();
	            if (num == 0)
	            {
	                continue;
	            }
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}


			Iterator<?> selectedKeys = this.m_selector.selectedKeys().iterator();

			if (selectedKeys.hasNext()) {
				SelectionKey key = (SelectionKey) selectedKeys.next();
				
				if (!key.isValid()) {
					System.err.println(">>> Ping:  ---> readable key=" + key);
					System.exit(-1);
				} else if (key.isAcceptable()) {
					try {
						System.out.println(">>> Ping:  ---> readable key=" + key);
						handleAccept(key);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (key.isReadable()) {
					rwEnabled = true;
//					System.out.println("<<< Pong:  ---> readable key=" + key);
					try {
						handleDataIn(key);
					} catch (IOException e) {
						System.err.println("Reading data error! " + e.getMessage());
					}
				} else if (key.isWritable()) {
					// Let channel write
					rwEnabled =true;
					if (!this.info.isEmpty()) {
						handleWrite(key);
					}
					
				} else if (key.isConnectable()) {

						try {
							SocketChannel ssTest = (SocketChannel) key.channel();
//							if (!ssTest.isConnected()) {
//								ssTest.connect(new InetSocketAddress(socketChannelsList.get(ssTest)));
//								System.out.print(hostName+" retries to connect to server!\n");
//							}else {
							handleConnect(key);
//							}
						} catch (IOException e) {
							System.err.println("Connecting to server error! " + e.getMessage());
						}
					
					System.out.println(">>> Ping:  ---> connectable key=" + key);
//					try {
//						handleConnect(key);
//					} catch (IOException e) {
//						System.err.println("Connecting to server error! " + e.getMessage());
//					}
				}
				selectedKeys.remove();
				
			}
		}
	}

	// ==============to be discussed===============
	synchronized private void handleWrite(SelectionKey key) {
		//REMOTE_HOST_NAME as a separator of the message aiming at identifying the remote sender
		CharBuffer charBuffer = CharBuffer.wrap(REMOTE_HOST_NAME+hostName+REMOTE_HOST_NAME+info + "\n");
		this.info = "";
		System.out.println("Info from Ping: " + charBuffer);
		writeBuffer = charset.encode(charBuffer);
		writeBuffer.compact();
		writeBuffer.flip();
		send(key,writeBuffer);
	}

	synchronized private void send(SelectionKey key, ByteBuffer byteBuffer) {
		try {
			int length = 0 ;
//			synchronized (this.socketChannelsList) {
//				for (SocketChannel c_SocketChannel : socketChannelsList.keySet()) {
//					if (c_SocketChannel!=null&&socketChannelsList.get(c_SocketChannel)) {
			SocketChannel ss_socketChannel = (SocketChannel) key.channel();
						length = ss_socketChannel.write(byteBuffer);//
						
//					} else {
//						delivered = false;
//					}
//				}
				byteBuffer.clear();
//				if (delivered) {
//					System.out.print(length+" words have been delivered successfully!");
//				}else {
//					System.out.print("Message has not been completely delivered!");
//				}
//			}

//			if (byteBuffer.position() != length) {
//				System.out.println("  ---> wrote " + count + " bytes.");
//				System.exit(-1);
//			}
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
//		if (count != m_ch.write(buf)) {
//			System.out.println("  ---> wrote " + count + " bytes.");
//			System.exit(-1);
//		}
	}

	// =======================================================
	synchronized void handleConnect(SelectionKey key) throws IOException {
//		if (key != m_key) {
//			System.err.println(">>> Ping: SelectionKey mismatch!");
//			System.exit(-1);
//		}
		SocketChannel ch = (SocketChannel) key.channel();
//		if (m_ch != ch) {
//			System.err.println(">>> Ping: ServerSocker mismatch!");
//			System.exit(-1);
//		}
		ch.configureBlocking(false);
		ch.socket().setTcpNoDelay(true);
		if (ch.isConnectionPending()) {
			ch.finishConnect();
			rwEnabled = true;
			System.out
					.println("Connection was pending but now is finiehed connecting to port: " + ch.socket().getPort());
			System.out.println(">>> Ping connected.");
			ch.register(m_selector,SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//			this.m_key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//			socketChannelsList.put(ch, );
		}
	}

	@Override
	public Server listen(int port, AcceptCallback callback) throws IOException {

		System.out.println("<<< Pong: accepting...");
		// create a new non-blocking server socket channel
//		ServerSocketChannel tempSSC= ServerSocketChannel.open();
		m_sch = ServerSocketChannel.open();
		m_sch.configureBlocking(false);
		server = m_sch.socket();

		// bind the server socket to the specified address and port
//		InetSocketAddress isa = new InetSocketAddress(serverIP, port);
//		tempSSC.socket().bind(new InetSocketAddress(port));
		this.server.bind(new InetSocketAddress(port));

		m_key = m_sch.register(this.m_selector, SelectionKey.OP_ACCEPT);
		return new myServer(serverIP, port, m_sch);
	}

	@Override
	public void connect(InetAddress hostAddress, int port, ConnectCallback callback)
			throws UnknownHostException, SecurityException, IOException {
		// TODO send connecting request
		myConnectCallback e_MyConnectCallback = (myConnectCallback) callback;

//		try {
//			connectToServer(hostAddress, port);
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
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// If succeeding, call ConnectCallback
		// TODO
		e_MyConnectCallback.connected(channel);
	}

	// =======================For active
	// mode============================================
	synchronized public void connectToServer(InetAddress hostAddress, int port)
			throws UnknownHostException, SecurityException, IOException, InterruptedException {
		// create a non-blocking socket channel
		SocketChannel tempSocketChannel;
		tempSocketChannel = SocketChannel.open();
		tempSocketChannel.configureBlocking(false);
		tempSocketChannel.socket().setTcpNoDelay(true);

		// be notified when the connection to the server will be accepted
		this.m_key = tempSocketChannel.register(this.m_selector, SelectionKey.OP_CONNECT);

		// request to connect to the server
		tempSocketChannel.connect(new InetSocketAddress(port));
//		socketChannelsList.put(tempSocketChannel, port);
	}

	// ===============End of active mode==================================

	// ===================For passive mode===============================
//	synchronized void accept(int port) throws IOException {
//
//		// create a new non-blocking server socket channel
//		ServerSocketChannel tempSSC= ServerSocketChannel.open();
//		tempSSC.configureBlocking(false);
//		server = tempSSC.socket();
//
//		// bind the server socket to the specified address and port
////		InetSocketAddress isa = new InetSocketAddress(serverIP, port);
////		tempSSC.socket().bind(new InetSocketAddress(port));
//		this.server.bind(new InetSocketAddress(port));
//
//		m_key = tempSSC.register(m_selector, SelectionKey.OP_ACCEPT);
////		this.m_sch = tempSSC;
////		sscList.add(tempSSC);
//
//	}

	/**
	 * Handle server socket accepting issues
	 * 
	 * @param key
	 * @throws IOException
	 */
	synchronized private void handleAccept(SelectionKey key) throws IOException {
//		if (key != m_key) {
//			System.err.println("<<< Pong: SelectionKey mismatch!");
//			System.exit(-1);
//		}
//		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		Socket client = this.server.accept();
		
		clientNumber++;
        System.out.println("Client Connected...." + "you have " + clientNumber + " clients connected");
//		if (!sscList.contains(ssc)) {//ssc != m_sch
//			System.err.println("<<< Pong: ServerSocker mismatch!");
//			System.exit(-1);
//		}
		SocketChannel clientChannel = client.getChannel();
//		tempSocketChannel = ssc.accept();
		clientChannel.configureBlocking(false);
		clientChannel.socket().setTcpNoDelay(true);
		this.m_key = clientChannel.register(this.m_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//		rwEnabled = true;
//		this.socketChannelsList.put(clientChannel,);
		System.out.println("<<<"+hostName+": receiving...");

	}

	 private void handleDataIn(SelectionKey key) throws IOException {
		synchronized (key) {
			SocketChannel ch = (SocketChannel) key.channel();
			ByteBuffer buf = ByteBuffer.allocate(2048);
			int count = 0;
			count = ch.read(buf);
			if (count == -1) {
				System.err.println("<<< Pong: end of stream!");
				System.exit(-1);
			}
			buf.flip();
			CharBuffer charBuffer = charset.decode(buf);
			buf.clear();
//			System.out.print(charBuffer);
			if (charBuffer.toString().startsWith(REMOTE_HOST_NAME)) {
				String[] displayInfo = charBuffer.toString().split(REMOTE_HOST_NAME);
				Date timeStamp = new Date(System.currentTimeMillis());
				System.out.println(displayInfo[1] + " said at: " + timeStamp + " : " + displayInfo[2]);
				charBuffer.clear();
//				this.info = hostName+" received message from "+ displayInfo[1];
				info = "Cao, wo shi "+hostName;
//				handleWrite(key);
			}
			if (charBuffer.toString().equalsIgnoreCase("quit")) {
				System.out.flush();
				System.exit(0);
			}
		}

	}
	// ================================================================

}
