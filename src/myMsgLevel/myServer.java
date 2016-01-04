package myMsgLevel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

import messages.engine.Server;

public class myServer extends Server implements Runnable {

	public int port;
	private InetSocketAddress server_address;
	private ServerSocketChannel serverChannel;
	private Selector selector;
	public final static long TIMEOUT = 10000;

	public myServer(InetSocketAddress inetSocketAddress, Selector m_selector) {
		this.server_address = inetSocketAddress;
		this.selector = m_selector;
//		initServer();
	}

	/**
	 * Initialize a server to get ready for incoming connections
	 */
	private void initServer() {
		System.out.println("initializing server");
		// We do not want to call init() twice and recreate the selector or the
		// serverChannel.
		if (selector != null)
			return;
		if (serverChannel != null)
			return;

		try {
			// This is how you open a Selector
			selector = Selector.open();
			// This is how you open a ServerSocketChannel
			serverChannel = ServerSocketChannel.open();
			// You MUST configure as non-blocking or else you cannot register
			// the serverChannel to the Selector.
			serverChannel.configureBlocking(false);
			// bind to the address that you will use to Serve.
			serverChannel.socket().bind(this.server_address);

			/**
			 * Here you are registering the serverSocketChannel to accept
			 * connection, thus the OP_ACCEPT. This means that you just told
			 * your selector that this channel will be used to accept
			 * connections. We can change this operation later to read/write,
			 * more on this later.
			 */
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void close() {
		try {
			this.serverChannel.close();
		} catch (IOException e) {
			System.err.println("ServerSocketChannel closing error! " + e.getMessage());
		}
	}

	@Override
	public void run() {
		System.out.println("Now accepting connections...");
		try{
			// A run the server as long as the thread is not interrupted.
			while (!Thread.currentThread().isInterrupted()){
				/**
				 * selector.select(TIMEOUT) is waiting for an OPERATION to be ready and is a blocking call.
				 * For example, if a client connects right this second, then it will break from the select()
				 * call and run the code below it. The TIMEOUT is not needed, but its just so it doesn't 
				 * block undefinitely.
				 */
//				selector.select(TIMEOUT);

				/**
				 * If we are here, it is because an operation happened (or the TIMEOUT expired).
				 * We need to get the SelectionKeys from the selector to see what operations are available.
				 * We use an iterator for this. 
				 */
				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

				while (keys.hasNext()){
					SelectionKey key = keys.next();
					// remove the key so that we don't process this OPERATION again.
					keys.remove();

					// key could be invalid if for example, the client closed the connection.
					if (!key.isValid()){
						continue;
					}
					/**
					 * In the server, we start by listening to the OP_ACCEPT when we register with the Selector.
					 * If the key from the keyset is Acceptable, then we must get ready to accept the client
					 * connection and do something with it. Go read the comments in the accept method.
					 */
					if (key.isAcceptable()){
						System.out.println("Accepting connection");
						accept(key);
					}
				}
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Since we are accepting, we must instantiate a serverSocketChannel by calling key.channel().
	 * We use this in order to get a socketChannel (which is like a socket in I/O) by calling
	 *  serverSocketChannel.accept() and we register that channel to the selector to listen 
	 *  to a WRITE OPERATION. I do this because my server sends a hello message to each
	 *  client that connects to it. I then 
	 */
	private void accept(SelectionKey key) throws IOException{
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_WRITE|SelectionKey.OP_READ);
		socketChannel = null;
	}
	
	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return this.server_address.getPort();
	}

}
