package myMsgLevel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;

import messages.engine.Server;

public class myServer extends Server{
	
	public int port;
	private InetAddress server_address;
	private ServerSocketChannel serverSocketChannel;
	
	public InetAddress getServer_address() {
		return server_address;
	}

	public void setServer_address(InetAddress server_address) {
		this.server_address = server_address;
	}
	
	public myServer(InetAddress serverIP, int port, ServerSocketChannel serverSocketChannel) {
		server_address =serverIP;
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
