package nio.sample;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

public class Pong extends Thread {

  InetAddress m_localhost;
  Selector m_selector;
  ServerSocketChannel m_sch;//Allow you to listen for incoming TCP connections, like a web server does.
  							//For each incoming connection, a SocketChannel is created
  SelectionKey m_skey; //Can read and write data over the network via TCP
  SocketChannel m_ch;
  SelectionKey m_key;
  int m_port;

  Pong(int port) throws Exception {
    m_port = port;
    m_localhost = InetAddress.getByName("localhost");
    m_selector = SelectorProvider.provider().openSelector();
  }

  public void run() {
    try {
      System.out.println("<<< Pong: accepting...");
      accept(m_port);
      waitForAccept();
      System.out.println("<<< Pong: accepted.");
      
      System.out.println("<<< Pong: receiving...");
      receive();
      
    } catch (Exception ex) {
      System.err.println("<<< Pong: threw an exception: " + ex.getMessage());
      ex.printStackTrace(System.err);
    }
  }

  void accept(int port) throws IOException {

    // create a new non-blocking server socket channel
    m_sch = ServerSocketChannel.open();
    m_sch.configureBlocking(false);

    // bind the server socket to the specified address and port
    InetSocketAddress isa = new InetSocketAddress(m_localhost, port);
    m_sch.socket().bind(isa);

    m_skey = m_sch.register(m_selector, SelectionKey.OP_ACCEPT);

  }

  private void handleAccept(SelectionKey key) throws IOException {
    if (key != m_skey) {
      System.err.println("<<< Pong: SelectionKey mismatch!");
      System.exit(-1);
    }
    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
    if (ssc != m_sch) {
      System.err.println("<<< Pong: ServerSocker mismatch!");
      System.exit(-1);
    }
    m_ch = m_sch.accept();
    m_ch.configureBlocking(false);
    m_ch.socket().setTcpNoDelay(true);
    m_key = m_ch.register(m_selector, SelectionKey.OP_READ);
  }

  void waitForAccept() throws IOException {
    long delay = 0;
    for (;;) {
      m_selector.select(delay);
      Iterator<?> selectedKeys = m_selector.selectedKeys().iterator();
      if (selectedKeys.hasNext()) {
        SelectionKey key = (SelectionKey) selectedKeys.next();
        selectedKeys.remove();
        if (!key.isValid()) {
          System.err.println("<<< Pong:  ---> invalid key=" + key);
        } else if (key.isAcceptable()) {
          handleAccept(key);
          return;
        } else if (key.isReadable()) {
          System.err.println("<<< Pong:  ---> readable key=" + key);
        } else if (key.isWritable()) {
          System.err.println("<<< Pong:  ---> writable key=" + key);
        } else if (key.isConnectable()) {
          System.err.println("<<< Pong:  ---> connectable key=" + key);
        }
      }
    }
  }


  private void handleDataIn(SelectionKey key) throws IOException {
    SocketChannel ch = (SocketChannel) key.channel();
    
    ByteBuffer buf = ByteBuffer.allocate(4);
    int count = 0;
    count = m_ch.read(buf);
    if (count == -1) {
      System.err.println("<<< Pong: end of stream!");
      System.exit(-1);
    }
    buf.position(0);
    int len = buf.getInt();
    buf = ByteBuffer.allocate(len);
    do {
      count = m_ch.read(buf);
    } while (buf.position()<len);
    buf.position(0);
    byte bytes[] = new byte[len];
    buf.get(bytes);
    for (int i = 0; i < len; i++)
      System.out.println("<<< Pong: bytes[" + i + "]=" + bytes[i]);

    System.out.println("<<< Pong: read all bytes.");
    System.out.flush();
    System.exit(0);
  }

  void receive() throws IOException {
    long delay = 0;
    for (;;) {
      m_selector.select(delay);
      Iterator<?> selectedKeys = this.m_selector.selectedKeys().iterator();
      if (selectedKeys.hasNext()) {
        SelectionKey key = (SelectionKey) selectedKeys.next();
        selectedKeys.remove();
        if (!key.isValid()) {
          System.err.println("<<< Pong:  ---> invalid key=" + key);
          System.exit(-1);
        } else if (key.isAcceptable()) {
          System.err.println("<<< Pong:  ---> acceptable key=" + key);
          System.exit(-1);
        } else if (key.isReadable()) {
          System.out.println("<<< Pong:  ---> readable key=" + key);
          handleDataIn(key);
        } else if (key.isWritable()) {
          System.err.println("<<< Pong:  ---> writable key=" + key);
          System.exit(-1);
        } else if (key.isConnectable()) {
          System.err.println("<<< Pong:  ---> connectable key=" + key);
          System.exit(-1);
        }
      }
    }
  }

}
