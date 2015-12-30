package myMsgLevel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import messages.engine.Channel;
import messages.engine.DeliverCallback;

public class myChannel extends Channel {

	SocketChannel m_SocketChannel;
	private static boolean Connected = false;

	public myChannel(SocketChannel channel) {
		m_SocketChannel = channel;
	}

	@Override
	public void setDeliverCallback(DeliverCallback callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	public static boolean isConnected() {
		return Connected;
	}

	public static void setConnected(boolean connected) {
		Connected = connected;
	}

	public SocketChannel getM_SocketChannel() {
		return m_SocketChannel;
	}

	public void setM_SocketChannel(SocketChannel m_SocketChannel) {
		this.m_SocketChannel = m_SocketChannel;
	}

	synchronized public void send(ByteBuffer byteBuffer) {
		try {
			int length = m_SocketChannel.write(byteBuffer);
			byteBuffer.clear();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
