package myMsgLevel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class myClient implements Runnable{
	private Selector selector;
	private InetSocketAddress l_InetSocketAddress;
	private boolean connectionFinished = false;
	private String hostName_l;
	private ArrayList<SocketChannel> scList;

	public myClient(String hostName, InetSocketAddress requsetedServerAddress, ArrayList<SocketChannel> readySocketChannel) {
		this.l_InetSocketAddress = requsetedServerAddress;
		this.scList = readySocketChannel;
		this.hostName_l = hostName;
		initClient();
	}

	/**
	 * Initialize client parameters and configurations
	 */
	private void initClient() {
	}

	@Override
	public void run() {
		SocketChannel channel;
		try {
			selector = Selector.open();
			channel = SocketChannel.open();
			channel.configureBlocking(false);

			channel.register(selector, SelectionKey.OP_CONNECT);
			channel.connect(this.l_InetSocketAddress);

			while (!connectionFinished){//!Thread.interrupted()

				selector.select(1000);
				
				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

				while (keys.hasNext()){
					SelectionKey key = keys.next();
					keys.remove();

					if (!key.isValid()) continue;

					if (key.isConnectable()){
//						System.out.println("I am connected to the server");
						connect(key);
					}	
				}	
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
	}

	@Override
	protected void finalize() throws Throwable {
		System.out.print(this.hostName_l+": finished connecting to remote: "+l_InetSocketAddress.getPort()+"\n");
		super.finalize();
	}

	synchronized private void connect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		if (channel.isConnectionPending()){
			channel.finishConnect();
			System.out.println("I am connected to the server");
		}
		channel.configureBlocking(false);
//		channel.register(this.selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
		connectionFinished  = true;
		scList.add(channel);
		notifyAll();
	}

}
