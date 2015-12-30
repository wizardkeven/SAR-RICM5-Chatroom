package nioImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.security.GuardedObject;
import java.util.Iterator;

import sun.reflect.generics.repository.FieldRepository;

public class Ping extends Thread {

  InetAddress m_localhost;
  Selector m_selector;
  SocketChannel m_ch; //Can read and write data over the network via TCP
  SelectionKey m_key;
  int m_port;

  Charset charset = Charset.forName("UTF-8");
  BufferedReader bufferedReader;
  ByteBuffer byteBuffer;
  
  Ping(int port) throws Exception {
    m_localhost = InetAddress.getByName("localhost");
    m_selector = SelectorProvider.provider().openSelector();
    m_port = port;
  }

  public void run() {
    try {
      sleep(2);
      System.out.println(">>> Ping connecting...");
      connect(m_port);
      waitUntilConnected();
      System.out.println(">>> Ping connected.");
      
      System.out.println(">>> Ping waiting for roomm...");
      m_key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      waitForRoom();
      
      System.out.println(">>> Ping got writing room...");
     
      String info = "I am Guo Kai, say hello to you!";
      for(;;){
    	  handleInput(info);
    	  info="";
    	  bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    	  info = bufferedReader.readLine();
    	  if(info.equals("Quit") || info.equals("quit")){
    		  m_ch.close();
    	      System.exit(0);
    	      System.out.println(">>> Ping sent bytes.");
    	  }
      }
//      byte bytes[] = new byte[] { 0, 1, 2, 3, 4 };
//      send(bytes, 0, bytes.length);
//      send(info.getBytes(), 0, info.getBytes().length); 
    } catch (Exception ex) {
      System.err.println("Ping: threw an exception: " + ex.getMessage());
      ex.printStackTrace(System.err);
    }
  }


/**
 * @param info
 */
protected void handleInput(String info) {
	CharBuffer charBuffer = CharBuffer.wrap(info+"\n");
      System.out.println("Info from Ping: "+info);
      byteBuffer = charset.encode(charBuffer);
      byteBuffer.compact();
      byteBuffer.flip();
      send(byteBuffer);
}

  private void send(ByteBuffer byteBuffer) {
	try {
		int length = m_ch.write(byteBuffer);
		byteBuffer.clear();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
}

public void connect(int port) throws UnknownHostException, SecurityException,
      IOException, InterruptedException {
    // create a non-blocking socket channel
    m_ch = SocketChannel.open();
    m_ch.configureBlocking(false);
    m_ch.socket().setTcpNoDelay(true);

    // be notified when the connection to the server will be accepted
    m_key = m_ch.register(m_selector, SelectionKey.OP_CONNECT);

    // request to connect to the server
    m_ch.connect(new InetSocketAddress(m_localhost, port));
  }

  void handleConnect(SelectionKey key) throws IOException {
    if (key != m_key) {
      System.err.println(">>> Ping: SelectionKey mismatch!");
      System.exit(-1);
    }
    SocketChannel ch = (SocketChannel) key.channel();
    if (m_ch != ch) {
      System.err.println(">>> Ping: ServerSocker mismatch!");
      System.exit(-1);
    }
    ch.configureBlocking(false);
    ch.socket().setTcpNoDelay(true);
    ch.finishConnect();
  }

  void waitUntilConnected() throws IOException {
    long delay = 0;
    for (;;) {
      m_selector.select(delay);
      Iterator<?> selectedKeys = this.m_selector.selectedKeys().iterator();
      if (selectedKeys.hasNext()) {
        SelectionKey key = (SelectionKey) selectedKeys.next();
        selectedKeys.remove();
        if (!key.isValid()) {
          System.err.println(">>> Ping:  ---> readable key=" + key);
          System.exit(-1);
        } else if (key.isAcceptable()) {
          System.err.println(">>> Ping:  ---> acceptable key=" + key);
          System.exit(-1);
        } else if (key.isReadable()) {
          System.err.println(">>> Ping:  ---> readable key=" + key);
          System.exit(-1);
        } else if (key.isWritable()) {
          System.err.println(">>> Ping:  ---> writable key=" + key);
          System.exit(-1);
        } else if (key.isConnectable()) {
          System.out.println(">>> Ping:  ---> connectable key=" + key);
          handleConnect(key);
          return;
        }
      }
    }
  }

  void send(byte bytes[], int offset, int length) throws IOException {
    
    ByteBuffer buf;
    buf = ByteBuffer.allocate(4+length);
    buf.putInt(length);
    buf.put(bytes, offset, length);
    int count = buf.position();
    buf.position(0);
    if (count != m_ch.write(buf)) {
      System.out.println("  ---> wrote " + count + " bytes.");
      System.exit(-1);
    }
  }

  void waitForRoom() throws IOException {
    long delay = 0;
    for (;;) {
      m_selector.select(delay);
      Iterator<?> selectedKeys = this.m_selector.selectedKeys().iterator();
      if (selectedKeys.hasNext()) {
        SelectionKey key = (SelectionKey) selectedKeys.next();
        selectedKeys.remove();
        if (!key.isValid()) {
          System.err.println(">>> Ping:  ---> readable key=" + key);
          System.exit(-1);
        } else if (key.isAcceptable()) {
          System.err.println(">>> Ping:  ---> acceptable key=" + key);
          System.exit(-1);
        } else if (key.isReadable()) {
          System.err.println(">>> Ping:  ---> readable key=" + key);
          System.exit(-1);
        } else if (key.isWritable()) {
          System.out.println(">>> Ping:  ---> writable key=" + key);
          return;
        } else if (key.isConnectable()) {
          System.err.println(">>> Ping:  ---> connectable key=" + key);
          System.exit(-1);
        }
      }
    }
  }

}
