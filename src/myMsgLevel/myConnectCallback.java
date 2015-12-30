package myMsgLevel;

import messages.engine.Channel;
import messages.engine.ConnectCallback;

public class myConnectCallback implements ConnectCallback {

	
	static myChannel c_MyChannel= null;
	public myConnectCallback(myChannel m_MyChannel) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void closed(Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	synchronized public void connected(Channel channel) {
		c_MyChannel = (myChannel) channel;
		c_MyChannel.setConnected(true);
		notifyAll();
	}

	public myChannel getC_MyChannel() {
		return c_MyChannel;
	}

	public void setC_MyChannel(myChannel c_MyChannel) {
		this.c_MyChannel = c_MyChannel;
	}

}
