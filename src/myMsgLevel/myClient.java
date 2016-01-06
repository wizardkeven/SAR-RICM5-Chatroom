package myMsgLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class myClient implements Runnable {		
	private static final String FINISH_CHAT = "QUIT";
	private static final String REMOTE_HOST_NAME = "SAR_RICM5_GUOKAI";//Message separator
	private Charset charset = Charset.forName("UTF-8");
	private Selector selector;
	private InetSocketAddress l_InetSocketAddress;
	private boolean connectionFinished = false;
	private String hostName_l;
	
	// private ArrayList<SocketChannel> scList;
	private ArrayList<String> msgQueue;
	private String message = " is testing this channel! ";
	boolean sendFinished = true;
	private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");

	public myClient(String hostName, InetSocketAddress requsetedServerAddress) {
		this.l_InetSocketAddress = requsetedServerAddress;
		// this.scList = readySocketChannel;
		this.hostName_l = hostName;
		this.msgQueue = new ArrayList<String>();
		this.chatFinished = false;
		// initClient();
	}

	/**
	 * Initialize client parameters and configurations
	 */
//	private void initClient() {
//
//	}

	Runnable readConsole = new Runnable() {
		BufferedReader bufferedReader;

		@Override
		public void run() {
			try {
				while (!connectionFinished && !chatFinished) {
					continue;
				}
				while (!sendFinished) {
					continue;
				}
				while (true) {
					bufferedReader = new BufferedReader(new InputStreamReader(System.in));
//					message = bufferedReader.readLine();
					msgQueue.add(bufferedReader.readLine());
			
					bufferedReader = null;
					sendFinished = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	};
	private boolean chatFinished;
	private CharBuffer writeBuffer;
	private ByteBuffer readBuffer;

	@Override
	public void run() {
		SocketChannel channel;
		try {
			selector = Selector.open();
			channel = SocketChannel.open();
			channel.configureBlocking(false);

			channel.register(selector, SelectionKey.OP_CONNECT);
			channel.connect(this.l_InetSocketAddress);

			while (!chatFinished) {// !connectionFinished
				// if (connectionFinished) {
				// bufferedReader = new BufferedReader(new
				// InputStreamReader(System.in));
				//// message if (connectionFinished) {
				//// bufferedReader = new BufferedReader(new
				// InputStreamReader(System.in));
				//// message = bufferedReader.readLine();
				//// bufferedReader = null;
				////// sendFinished = false;
				//// }else {= bufferedReader.readLine();
				//// bufferedReader = null;
				// sendFinished = false;
				// }else {

				selector.select(1000);

				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					keys.remove();

					if (!key.isValid())
						continue;

					if (key.isConnectable()) {
						System.out.println(hostName_l + " is connecting to the server");
						connect(key);
					}else if (key.isReadable()) {
						read(key);
					}else if (key.isWritable()) {
						if (msgQueue.isEmpty()) {
							continue;
						}
						System.out.println("Writing...");
						write(key);
					}
				}
				// }
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(hostName_l + " has stopped chat!");
		try {
			this.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		System.out.print(this.hostName_l + ": finished connecting to remote: " + l_InetSocketAddress.getPort() + "\n");
		super.finalize();
	}

	private void connect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		if (channel.isConnectionPending()) {
			channel.finishConnect();

			System.out.println(hostName_l + " connected to the server");
		}
		channel.configureBlocking(false);
		channel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		connectionFinished = true;
		message = hostName_l + message;
		msgQueue.add(message);
		message = "";
		Thread readConsoleThread = new Thread(readConsole);
		readConsoleThread.start();
		// scList.add(channel);
		// notify();
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		this.readBuffer = ByteBuffer.allocate(1000);
		int length;
		try {
			length = channel.read(readBuffer);
		} catch (IOException e) {
			System.out.println("Reading problem, closing connection");
			key.cancel();
			channel.close();
			return;
		}
		if (length == -1) {
			System.out.println("Nothing was read from server");
			// channel.close();
			// key.cancel();
			return;
		}
		readBuffer.flip();
//		byte[] buff = new byte[1024];
		CharBuffer readCharBuf = charset.decode(readBuffer);
		readBuffer.clear();
//		readBuffer.get(buff, 0, length);
		String rcvdMSG = readCharBuf.toString();
		readCharBuf.clear();
		handleReceiving(rcvdMSG);
	}

	private void handleReceiving(String rcvdMSG) {
		String receivedMSG = rcvdMSG;
		if (receivedMSG.toString().startsWith(REMOTE_HOST_NAME)) {
			String[] displayInfo = rcvdMSG.toString().split(REMOTE_HOST_NAME);
			
			if (displayInfo.length!=3) {
				long rcvTime = Long.parseLong(displayInfo[2]);
				String timeStamp = sdf.format(new Date(rcvTime));
				System.out.println(displayInfo[1] + " <<" + timeStamp + ">> " + " : " + displayInfo[3]);
				if (displayInfo[2].equalsIgnoreCase("quit")) {
					// System.exit(0);
					System.out.println(displayInfo[1] + ">>> left chat group.");
				}
				// this.info = hostName+" received message from "+
				// displayInfo[1];
				// info = "Cao, wo shi "+hostName;
				// handleWrite(key);
			}
		}else {
			System.out.println(hostName_l + ">>> received a wrong formatting message!/n"+receivedMSG);	
		}
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		String msg = msgQueue.get(0);
		msgQueue.remove(0);
		long timeStamp = System.currentTimeMillis();
		System.out.println("At: " + sdf.format(timeStamp) + ": sending: " + msg);
		if (msg.equalsIgnoreCase(FINISH_CHAT)) {
			System.out.println(hostName_l + " will finish chat!");
			channel.close();
			chatFinished = true;
		} else {
			msg = REMOTE_HOST_NAME+hostName_l+REMOTE_HOST_NAME+timeStamp +REMOTE_HOST_NAME+ msg;
			writeBuffer = CharBuffer.wrap(msg);
			ByteBuffer sendBuffer =charset.encode(writeBuffer);		
			channel.write(sendBuffer);
			
			this.sendFinished = true;
		}

		// lets get ready to read.
		// key.interestOps(SelectionKey.OP_READ);
	}

}
