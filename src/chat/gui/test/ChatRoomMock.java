package chat.gui.test;

import java.util.Random;

import chat.gui.ChatException;
import chat.gui.ChatGUI;
import chat.gui.IChatRoom;

/**
 * This class is a mock to test the Chat GUI. It simulates the asynchronous
 * behavior of a messaging middleware, which deliver messages on its own thread
 * and on its own terms.
 */
public class ChatRoomMock implements IChatRoom {

  IChatListener m_listener;
  String m_name;
  boolean m_inRoom;
  EventPump m_pump;

  public static void main(String args[]) {
    ChatRoomMock room;
    String name;
    if (args.length < 1)
      name = "Client0";
    else
      name = args[0];
    room = new ChatRoomMock();
    new ChatGUI(name, room);
    // we are done here...
    // the main thread terminates
    // but the GUI thread will remain
    // and the event pump thread will also remain.
  }

  ChatRoomMock() {
    m_pump = new EventPump(this);
    m_pump.start();
    // comment this line out if you don't want
    // fake asynchronous messages.
    produceFakeMessages();
  }

  /**
   * Creates a background thread whose only purpose is to fake received messages
   */
  private void produceFakeMessages() {
    // This thread is to create fake messages...
    // You can comment
    Thread thread = new Thread(new Runnable() {
      public void run() {
        double base = System.currentTimeMillis();
        Random rand = new Random();
        for (;;) {
          try {
            int sleep = rand.nextInt(400);
            Thread.sleep(800 + sleep);
            int no = rand.nextInt(10);
            double time = (double) System.currentTimeMillis();
            time = (time - base) / 1000.0;
            received("Fake" + no + " says: time is " + time);
          } catch (Exception ex) {
          }
        }
      }
    }, "Fake");
    thread.start();
  }

  /**
   * This is to simulate the reception of a message from the network. That
   * received message should go into the logic that will decide when to deliver
   * it. In this mock, the logic is to deliver immediately through an
   * asynchronous continuation, effectively adding this message to the queue of
   * messages that are pending delivery.
   * 
   * @param msg
   */
  private void received(final String msg) {
    m_pump.enqueue(new Runnable() {
      public void run() {
        if (m_listener != null)
          m_listener.deliver(msg);
      };
    });
  }

  
  @Override
  public void enter(String clientName, IChatListener listener)
      throws ChatException {
    if (m_inRoom)
      throw new ChatException("Already in the chat room");
    //TODO
    //launch a request to join in a specific group
    
    ////////////////////////////////
    m_inRoom = true;
    m_name = clientName;
    m_listener = listener;
    m_pump.enqueue(new Runnable() {
      public void run() {
        m_listener.joined(m_name);
      };
    });
  }

  @Override
  public void leave() throws ChatException {
    if (!m_inRoom)
      throw new ChatException("Not in the chat room");
    m_inRoom = false;
    m_pump.enqueue(new Runnable() {
      public void run() {
        m_listener.left(m_name);
      };
    });
  }

  @Override
  public void send(final String msg) throws ChatException {
    if (!m_inRoom)
      throw new ChatException("Not in the chat room");
    m_pump.enqueue(new Runnable() {
      public void run() {
        m_listener.deliver(msg);
      };
    });
  }

}
