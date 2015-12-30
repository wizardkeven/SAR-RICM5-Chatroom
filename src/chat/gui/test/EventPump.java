package chat.gui.test;

import java.util.LinkedList;

class EventPump extends Thread implements Runnable {
  
  ChatRoomMock m_room;
  LinkedList<Runnable> m_queue;

  EventPump(ChatRoomMock chatRoomMock) {
    super("ChatRoomPump");
    m_room = chatRoomMock;
    m_queue = new LinkedList<Runnable>();
  }

  public void run() {
    for (;;) {
      synchronized (this) {
        while (!m_queue.isEmpty()) {
          Runnable r = m_queue.removeFirst();
          try {
            r.run();
          } catch (Throwable th) {
            th.printStackTrace(System.err);
          }
        }
        try {
          wait();
        } catch (InterruptedException ex) {
        }
      }
    }
  }

  synchronized void enqueue(Runnable r) {
    m_queue.addLast(r);
    notify();
  }
}