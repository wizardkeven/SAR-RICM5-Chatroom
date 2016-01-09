package myMsgLevel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import messages.engine.Server;

public class myServer extends Server implements Runnable {

	public int port;
	private InetSocketAddress server_address;
	private ServerSocketChannel serverChannel;
	private Selector selector;
	private static String hostName;
	public final static long TIMEOUT = 10000;
	private static final String REMOTE_HOST_NAME = "SAR_RICM5_GUOKAI";// Message separator
	//Flag of completion of broadcast
	private static final String FINISHED_KEYWORD = "SAR_DELIVERED";
	// private Charset charset = Charset.forName("UTF-8");
	// Keeps every socket channel which has requested for broadcasting its
	// message with its message
	private ArrayList<String> dataTracking;
	// Keeps all the living socket channels
	private ArrayList<SocketChannel> livingSCList;
	// Keeps all the socket channels to which the earliest requesting socket
	// channel has broadcasted its requesting message
	private ArrayList<SocketChannel> sentSCList;

	// Keeps all the channels which has requested for broadcasting its message
	// with its delivered flag. Notice that
	// newly requesting channel will always be added sequentially to the very
	// end of the list
	// So the socket channel which is in top of this list is the socket channel
	// which requested earliest
	// Therefore, the server will always serve for the very top socket channel
	// for every writable cycle
	// If server delivered to all the other channels, it will return a delivered
	// confirmation message to the current serving
	// client and then remove it from the top and serve for the next
	private ArrayList<SocketChannel> requestSCList = new ArrayList<SocketChannel>();
	// private HashMap<SocketChannel, ArrayList<SocketChannel>> dataSendWorshop
	// = new HashMap<SocketChannel,ArrayList<SocketChannel>>();
	private boolean delivered = false;// Flag represents whether the message for
										// the earliest requesting socket
										// channel has been delivered
	private byte[] FINISH_BROADCAST_MESSAGE;
	private boolean sendingTimeout = false;

	public myServer(String hostID, InetSocketAddress inetSocketAddress, Selector m_selector) {
		this.hostName = hostID;
		this.server_address = inetSocketAddress;
		this.selector = m_selector;
		this.livingSCList = new ArrayList<SocketChannel>();
		this.sentSCList = new ArrayList<SocketChannel>();
		this.dataTracking = new ArrayList<String>();
		initServer();

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
		try {
			// A run the server as long as the thread is not interrupted.
			while (!Thread.currentThread().isInterrupted()) {
				/**
				 * selector.select(TIMEOUT) is waiting for an OPERATION to be
				 * ready and is a blocking call. For example, if a client
				 * connects right this second, then it will break from the
				 * select() call and run the code below it. The TIMEOUT is not
				 * needed, but its just so it doesn't block undefinitely.
				 */
				selector.select(TIMEOUT);

				/**
				 * If we are here, it is because an operation happened (or the
				 * TIMEOUT expired). We need to get the SelectionKeys from the
				 * selector to see what operations are available. We use an
				 * iterator for this.
				 */
				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					// remove the key so that we don't process this OPERATION
					// again.
					keys.remove();

					// key could be invalid if for example, the client closed
					// the connection.
					if (!key.isValid()) {
						continue;
					} else if (key.isAcceptable()) {
						System.out.println("Accepting connection");
						accept(key);
					} else if (key.isReadable()) {
						System.out.println("Reading connection");
						read(key);
					} else if (key.isWritable()) {
						// System.out.println("Writing...");
						// If this channel has been sent the message, then we
						// should ignore it
						if (requestSCList == null || requestSCList.size() == 0 || sentSCList == null
								|| sentSCList.contains((SocketChannel) key.channel())) {
							continue;
						}else {

							write(key);
						}

					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Since we are accepting, we must instantiate a serverSocketChannel by
	 * calling key.channel(). We use this in order to get a socketChannel (which
	 * is like a socket in I/O) by calling serverSocketChannel.accept() and we
	 * register that channel to the selector to listen to a WRITE OPERATION. I
	 * do this because my server sends a hello message to each client that
	 * connects to it. I then
	 */
	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		// We register every connection a read and write interests
		socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		System.out.println("server connected to: " + socketChannel);
		// Once accepted a connection, we add this channel in the living socket
		// channel list for tracking the following actions
		livingSCList.add(socketChannel);
		socketChannel = null;
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		// Check if this called channel is open and not in the sent list and do
		// not sent to the message source
		if (livingSCList.contains(channel)) {

			// If this is the first sending, then we initialize the environment
			if (sentSCList.isEmpty()) {
				delivered = false;
			}
			// If this is not the origin requesting socket channel we proceed
			// the broadcasting
			if (!requestSCList.get(0).equals(channel) && !delivered) {

				// get the message
				String currentMSG = dataTracking.get(0);
				byte[] data = currentMSG.getBytes();
				// send the message
				if (data.length == 0) {
					System.out.println("Error sending parameter!");
				}
				// send to the client
				channel.write(ByteBuffer.wrap(data));
				// add this channel to the sent list
				sentSCList.add(channel);
				//
				// dataSendWorshop.get(requestSCList.get(0)).remove(channel);
				// If all other host have been sent the message, then delete
				// this interested channel and data
				// Need to be optimized
				if (sentSCList.size() == livingSCList.size() - 1) {
					// This message has been successfully broadcasted
					delivered = true;
				}
			} else if (requestSCList.get(0).equals(channel) && delivered) {
				// else we confirm the completion of broadcast and prepare
				// to send the delivered message to the origin sc
				String finished = REMOTE_HOST_NAME + this.hostName + REMOTE_HOST_NAME + System.currentTimeMillis()
						+ REMOTE_HOST_NAME + FINISHED_KEYWORD+dataTracking.get(0);
				channel.write(ByteBuffer.wrap(finished.getBytes()));
				// delete the message from the list
				dataTracking.remove(0);
				// delete the requested channel
				requestSCList.remove(0);
				// dump the sent list
				sentSCList.clear();
			} else if (requestSCList.get(0).equals(channel) && !delivered) {
				if (sendingTimeout) {
					// if send timeout, do something
				}
			}

		} else if (!livingSCList.contains(channel)) {
			System.err.println(channel + " is not in the current living socket channel list, server error!");
		}

		// key.interestOps(SelectionKey.OP_READ);

	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(1024);

		int read;
		try {
			read = channel.read(readBuffer);
		} catch (IOException e) {
			System.out.println("Reading problem, closing connection");
			key.cancel();
			channel.close();
			return;
		}
		if (read == -1) {
			System.out.println("Nothing was there to be read, closing connection");
			channel.close();
			key.cancel();
			return;
		}
		readBuffer.flip();
		byte[] data = new byte[1000];
		readBuffer.get(data, 0, read);
		readBuffer.clear();
		System.out.println("Received: " + new String(data));

		handleMessage(key, data);
	}

	private void handleMessage(SelectionKey key, byte[] data) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		String msg = new String(data);
		// We see 3 consequent keyword as an indicator of termination of
		// connection
		if (msg.contains(REMOTE_HOST_NAME + REMOTE_HOST_NAME + REMOTE_HOST_NAME)) {
			// update alive channel list
			livingSCList.remove(socketChannel);
			return;
		}
		// We add this channel to the requestSCList to be processed afterwards
		requestSCList.add(socketChannel);
		// dataSendWorshop.put(socketChannel, livingSCList);
		// And the same time track it with its message
		dataTracking.add(msg);
		// key.interestOps(SelectionKey.OP_WRITE);
	}

	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return this.server_address.getPort();
	}

}
