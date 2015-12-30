package myMsgLevel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import messages.engine.AcceptCallback;
import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.Engine;
import messages.engine.Server;

public class myEngine extends Engine {

	private Selector m_selector;
	private SelectionKey m_key;
	private SocketChannel m_ch;// Common channel for launching connection or
								// data transferring
	private myChannel m_MyChannel;
	private Channel channel;
	private ServerSocketChannel m_sch;
	private InetAddress serverIP;

	private Charset charset = Charset.forName("UTF-8");
	private ByteBuffer writeBuffer;
	private ByteBuffer readBuffer;

	public myEngine() {
		try {
			this.serverIP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			System.err.println("Get localhost ip address error! " + e1.getMessage());
		}
		try {
			m_selector = SelectorProvider.provider().openSelector();
		} catch (IOException e) {
			System.err.println("Open selector error! " + e.getMessage());
		}
		
	}

	/**
	 * NIO engine mainloop Wait for selected events on registered channels
	 * Selected events for a given channel may be ACCEPT, CONNECT, READ, WRITE
	 * Selected events for a given channel may change over time
	 */
	@Override
	public void mainloop() {
		long delay = 0;
		for (;;) {

			try {
				m_selector.select(delay);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Iterator<?> selectedKeys = this.m_selector.selectedKeys().iterator();
			if (selectedKeys.hasNext()) {
				SelectionKey key = (SelectionKey) selectedKeys.next();
				selectedKeys.remove();
				if (!key.isValid()) {
					System.err.println(">>> Ping:  ---> readable key=" + key);
					System.exit(-1);
				} else if (key.isAcceptable()) {
					try {
						handleAccept(key);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (key.isReadable()) {
					System.out.println("<<< Pong:  ---> readable key=" + key);
					try {
						handleDataIn(key);
					} catch (IOException e) {
						System.err.println("Reading data error! "+ e.getMessage());
					}
				} else if (key.isWritable()) {
					// Let channel write
					handleWrite(); // ? remove it?
				} else if (key.isConnectable()) {
					System.out.println(">>> Ping:  ---> connectable key=" + key);
					try {
						handleConnect(key);
					} catch (IOException e) {
						System.err.println("Connecting to server error! " + e.getMessage());
					}
				}
			}
		}
	}

	// ==============to be discussed===============
	private void handleWrite() {
	}

	void send(byte bytes[], int offset, int length) throws IOException {

		ByteBuffer buf;
		buf = ByteBuffer.allocate(4 + length);
		buf.putInt(length);
		buf.put(bytes, offset, length);
		int count = buf.position();
		buf.position(0);
		if (count != m_ch.write(buf)) {
			System.out.println("  ---> wrote " + count + " bytes.");
			System.exit(-1);
		}
	}

	// =======================================================
	void handleConnect(SelectionKey key) throws IOException {
		if (key != m_key) {
			System.err.println(">>> Ping: SelectionKey mismatch!");
			System.exit(-1);
		}
		SocketChannel ch = (SocketChannel) key.channel();
		if (m_ch != ch) {
			System.err.println(">>> Ping: ServerSocker mismatch!");
			System.exit(-1);
		}
		ch.configureBlocking(false);
		ch.socket().setTcpNoDelay(true);
		ch.finishConnect();
		System.out.println(">>> Ping connected.");
		System.out.println(">>> Ping waiting for roomm...");
		m_key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}

	@Override
	public Server listen(int port, AcceptCallback callback) throws IOException {

		System.out.println("<<< Pong: accepting...");
		accept(port);
		return null;
	}

	@Override
	public void connect(InetAddress hostAddress, int port, ConnectCallback callback)
			throws UnknownHostException, SecurityException, IOException {
		// TODO send connecting request
		myConnectCallback e_MyConnectCallback = (myConnectCallback) callback;

		try {
			connectToServer(hostAddress, port);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// If succeeding, call ConnectCallback
		// TODO
		e_MyConnectCallback.connected(channel);
	}

	// =======================For active
	// mode============================================
	public void connectToServer(InetAddress hostAddress, int port)
			throws UnknownHostException, SecurityException, IOException, InterruptedException {
		// create a non-blocking socket channel
		m_ch = SocketChannel.open();
		m_ch.configureBlocking(false);
		m_ch.socket().setTcpNoDelay(true);

		// be notified when the connection to the server will be accepted
		m_key = m_ch.register(m_selector, SelectionKey.OP_CONNECT);

		// request to connect to the server
		m_ch.connect(new InetSocketAddress(hostAddress, port));
	}

	// ===============End of active mode==================================

	// ===================For passive mode===============================
	void accept(int port) throws IOException {

		// create a new non-blocking server socket channel
		m_sch = ServerSocketChannel.open();
		m_sch.configureBlocking(false);

		// bind the server socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(serverIP, port);
		m_sch.socket().bind(isa);

		m_key = m_sch.register(m_selector, SelectionKey.OP_ACCEPT);

	}

	private void handleAccept(SelectionKey key) throws IOException {
		if (key != m_key) {
			System.err.println("<<< Pong: SelectionKey mismatch!");
			System.exit(-1);
		}
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		if (ssc != m_sch) {
			System.err.println("<<< Pong: ServerSocker mismatch!");
			System.exit(-1);
		}
		m_ch = m_sch.accept();
		m_ch.configureBlocking(false);
		m_ch.socket().setTcpNoDelay(true);
		m_key = m_ch.register(m_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		System.out.println("<<< Pong: receiving...");
	}
	
	private void handleDataIn(SelectionKey key) throws IOException {
	    SocketChannel ch = (SocketChannel) key.channel();
	    
	    ByteBuffer buf = ByteBuffer.allocate(32);
	    int count = 0;
	    count = m_ch.read(buf);
	    if (count == -1) {
	      System.err.println("<<< Pong: end of stream!");
	      System.exit(-1);
	    }
	    
	    buf.flip();
	    CharBuffer charBuffer = charset.decode(buf);
	    System.out.println("Got: "+ charBuffer);
	    if (charBuffer.equals("Quit") || charBuffer.equals("quit")) {
	        System.out.flush();
	        System.exit(0);
		}

	  }
	// ================================================================

}
