package myMsgLevel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
	private static final String REMOTE_HOST_NAME = "SAR_RICM5_GUOKAI";//Message separator
	private Charset charset = Charset.forName("UTF-8");
	private Map<SocketChannel,byte[]> dataTracking = new HashMap<SocketChannel, byte[]>();
	private ArrayList<SocketChannel> readyList;
	private ArrayList<SocketChannel> sentSCList = new ArrayList<SocketChannel>();;
	private ArrayList<SocketChannel> requestSCList = new ArrayList<SocketChannel>();
	private HashMap<SocketChannel, ArrayList<SocketChannel>> dataSendWorshop = new HashMap<SocketChannel,ArrayList<SocketChannel>>();

	public myServer(String hostID, InetSocketAddress inetSocketAddress, Selector m_selector) {
		this.hostName = hostID;
		this.server_address = inetSocketAddress;
		this.selector = m_selector;
		this.readyList = new ArrayList<SocketChannel>();
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
		try{
			// A run the server as long as the thread is not interrupted.
			while (!Thread.currentThread().isInterrupted()){
				/**
				 * selector.select(TIMEOUT) is waiting for an OPERATION to be ready and is a blocking call.
				 * For example, if a client connects right this second, then it will break from the select()
				 * call and run the code below it. The TIMEOUT is not needed, but its just so it doesn't 
				 * block undefinitely.
				 */
				selector.select(TIMEOUT);

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
					}else if (key.isAcceptable()){
						System.out.println("Accepting connection");
						accept(key);
					}else if (key.isReadable()){
						System.out.println("Reading connection");
						read(key);
					}else if (key.isWritable()){
//						System.out.println("Writing...");
						if (dataTracking.get((SocketChannel)key.channel()) == null) {
							continue;
						}
						
						write(key);
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
		socketChannel.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
		System.out.println("server connected to: "+socketChannel);;
		readyList.add(socketChannel);
		socketChannel = null;
	}
	
	private void write(SelectionKey key) throws IOException{
		SocketChannel channel = (SocketChannel) key.channel();
//		boolean contained = false;
		//Check if this called channel is open and not in the sent list and donot sent to the msg source
		if (readyList.contains(channel)&&sentSCList!= null && !sentSCList.contains(channel) && !requestSCList.get(0).equals(channel)) {
			
			//get the message
			byte[] data = dataTracking.get(requestSCList.get(0));
			//send the message
			if (data.length == 0) {
				System.out.println("Error sending parameter!");
				}
			channel.write(ByteBuffer.wrap(data));
			//add this channel to the sent list
			sentSCList.add(channel);
			//
//			dataSendWorshop.get(requestSCList.get(0)).remove(channel);
			//If all other host have been sent the message, then delete this interested channel and data
			if (sentSCList.size() == readyList.size()-1) {
				//delete the message from the set
				dataTracking.remove(requestSCList.get(0));
				//delete the requested channel
				requestSCList.remove(0);
				//dump the sent list
				sentSCList.clear();
			}
			
			
		}
		if (!readyList.contains(channel)) {
			System.out.println(channel+" is not valid!");
			return;
		}
		
//		key.interestOps(SelectionKey.OP_READ);
		
	}
	private void read(SelectionKey key) throws IOException{
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
		if (read == -1){
			System.out.println("Nothing was there to be read, closing connection");
			channel.close();
			key.cancel();
			return;
		}
		readBuffer.flip();
		byte[] data = new byte[1000];
		readBuffer.get(data, 0, read);
		readBuffer.clear();
		System.out.println("Received: "+new String(data));

		handleMessage(key,data);
	}

	private void handleMessage(SelectionKey key, byte[] data){
		SocketChannel socketChannel = (SocketChannel) key.channel();
		String msg = data.toString();
		//We see 3 consequent keyword as an indicator of termination of connection
		if (msg.contains(REMOTE_HOST_NAME+REMOTE_HOST_NAME+REMOTE_HOST_NAME)) {
			//update alive channel list
			readyList.remove(socketChannel);
			return;
		}

		requestSCList.add(socketChannel);
		dataSendWorshop.put(socketChannel, readyList);
		dataTracking.put(socketChannel, data);
//		key.interestOps(SelectionKey.OP_WRITE);
	}

	
	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return this.server_address.getPort();
	}

}
