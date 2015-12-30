package myMsgLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;

public class chatMain {

	public static void main(String[] args) throws Exception {

		InetAddress m_localhost;
		Selector m_selector;
		ServerSocketChannel m_sch;// Allow you to listen for incoming TCP
									// connections, like a web server does.
									// For each incoming connection, a
									// SocketChannel is created
		SelectionKey m_skey; // Can read and write date over the network via TCP
		SocketChannel m_ch = null;
		int m_port;
		SocketChannel m_Channel = null;
		myConnectCallback m_MyConnectCallback = null;
		myAcceptCallback m_MyAcceptCallback;

		myChannel m_MyChannel = new myChannel(m_ch);
		BufferedReader br;
		String line;
		do {
			line = "";
			br = new BufferedReader(new InputStreamReader(System.in));
			line = br.readLine();
		} while (line.isEmpty() || !line.equals("connect") || !line.equals("accept")); // For
																						// a
																						// client
																						// who
																						// launches
																						// a
																						// connection
		// he/she should enter a "connect"; otherwise wait. Or enters a "accept"
		// to open a server

		br.close();// Close buffer

		m_localhost = InetAddress.getByName("localhost");
		m_selector = SelectorProvider.provider().openSelector();
		m_port = 12345;

		myEngine m_Engine = new myEngine();

		if (line.equals("connect")) {
			m_MyConnectCallback = new myConnectCallback(m_MyChannel);
			m_Engine.connect(m_localhost, m_port, m_MyConnectCallback);
		} else {
			m_MyAcceptCallback = new myAcceptCallback(m_MyChannel);
			m_Engine.listen(m_port, m_MyAcceptCallback);
		}
		
		m_Engine.mainloop();//Main thread running always

		Runnable echo = new Runnable() {
			
			private BufferedReader bufferedReader;

			public void run() {

				synchronized (this) {
					while (!m_MyChannel.isConnected()) {
						try {
							m_MyChannel.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				

				System.out.println(">>> Ping got writing room...");

				String info = "I am Guo Kai, say hello to you!";
				for (;;) {
					handleInput(info);
					info = "";
					bufferedReader = new BufferedReader(new InputStreamReader(System.in));
					try {
						info = bufferedReader.readLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (info.equals("Quit") || info.equals("quit")) {
						try {
							m_MyChannel.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.exit(0);
						System.out.println(">>> Ping sent bytes.");
					}
				}
			}
			
			private ByteBuffer byteBuffer;
			private Charset charset = Charset.forName("UTF-8");;

			/**
			 * @param info
			 */
			protected void handleInput(String info) {
				CharBuffer charBuffer = CharBuffer.wrap(info + "\n");
				System.out.println("Info from Ping: " + info);
				byteBuffer = charset.encode(charBuffer);
				byteBuffer.compact();
				byteBuffer.flip();
				m_MyChannel.send(byteBuffer);
			}
		};

	}


	


}
