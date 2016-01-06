package myMsgLevel;

import java.net.InetSocketAddress;
import java.util.Random;

public class clientMain {

	public static void main(String[] args) {
//		for (int i = 0; i < 4; i++) {
			Thread client = new Thread(new myClient("client"+Math.random(), new InetSocketAddress(9090)));
			client.start();
//		}
	}

}
