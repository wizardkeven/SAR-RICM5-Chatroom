package myMsgLevel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;

import messages.engine.Server;

public class myServer extends Server{
	
	private int port;
	private InetAddress m_localhost;
	private ServerSocketChannel serverSocketChannel;
	
	public myServer(InetAddress serverIP, int port, ServerSocketChannel serverSocketChannel) throws Exception {
		m_localhost = InetAddress.getByName("localhost");
		this.port = port;
		this.serverSocketChannel = serverSocketChannel;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void close() {
		try {
			this.serverSocketChannel.close();
		} catch (IOException e) {
			System.err.println("ServerSocketChannel closing error! "+ e.getMessage());
		}
	}

}
