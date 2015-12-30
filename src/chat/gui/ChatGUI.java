package chat.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import chat.gui.IChatRoom.IChatListener;

public class ChatGUI {

  EventContainer cont;
  TextArea deliveredMessages;
  TextField inputArea;
  Frame frame;
  IChatRoom room;
  String clientName;
  Button enterButton;
  Button leaveButton;
  TextArea groupArea;
  Vector<String> group = new Vector<String>();

  /*
   * A container whose sole purpose is to allow to hook the event pump of AWT
   * and provide a way to post events from threads other than the window thread.
   * It is just added with a border layout in between the Frame and the overall
   * GUI components.
   * This may not be the best approach... If you find a better way, let us know.
   */
  class EventContainer extends Container {

    public EventContainer() {
      enableEvents(0);
    }

    public void processEvent(AWTEvent evt) {
      if (evt instanceof DeliverEvent) {
        DeliverEvent e = (DeliverEvent) evt;
        // Thread thread = Thread.currentThread();
        // System.out.println("Display thread "+thread+" appending msg="+e.msg);
        deliveredMessages.append(e.msg + "\n");
      } else
        super.processEvent(evt);
    }
  }

  /*
   * An event class to deliver a newly delivered message...
   */
  class DeliverEvent extends AWTEvent {
    public static final int TIMER_EVENT = AWTEvent.RESERVED_ID_MAX + 5555;
    String msg;

    public DeliverEvent(Container c, String msg) {
      super(c, TIMER_EVENT);
      this.msg = msg;
    }
  }

  
  ///////=============To be completed==============================
  IChatListener listener = new IChatListener() {

    @Override
    public void deliver(String msg) {
      // Be really carefull here, you are not on the GUI thread
      // so you should not invoke directly:
      // deliveredMessages.append(e.msg + "\n");
      // This somewhat works with AWT, but it is still dangerous
      // in case of re-layouts...
      // Notice that it would not work at all with SWT from Eclipse,
      // The correct way is to switch to the GUI thread...
      try {
        // Thread thread = Thread.currentThread();
        // System.out.println("Thread "+thread+" received msg="+msg);
        EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        queue.postEvent(new DeliverEvent(cont, msg));
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(-1); // offending exception, commit Seppuku !
        return;
      }
    }

    @Override
    public void joined(String name) {
      System.out.println(name + " joined...");
      group.addElement(name);
      updateGroup();
    }

    @Override
    public void left(String name) {
      System.out.println(name + " left...");
      group.removeElement(name);
      updateGroup();
    }
  };

 
  
  
  
  public ChatGUI(String name, IChatRoom room) {
    this.room = room;
    this.clientName = name;

    frame = new Frame();
    frame.setLayout(new BorderLayout());
    frame.setTitle(name);
    frame.addWindowListener(wl); // so to enter the chat room once the window
                                 // has been opened.

    // Add our event container... so that we can have
    // a way to post events to the AWT Dispatch thread.
    cont = new EventContainer();
    cont.setLayout(new BorderLayout());
    frame.add(cont, BorderLayout.CENTER);

    // delivered messages in the center
    deliveredMessages = new TextArea(10, 40);
    deliveredMessages.setEditable(false);
    deliveredMessages.setForeground(Color.red);
    deliveredMessages.setBackground(Color.black);

    cont.add(deliveredMessages, BorderLayout.CENTER);

    // on the right hand side, the status of the group
    groupArea = new TextArea(0, 0);
    groupArea.setEditable(false);
    groupArea.setForeground(Color.green);
    groupArea.setBackground(Color.black);
    cont.add(groupArea, BorderLayout.EAST);

    // -----------------------------------------------
    // this is the input zone, for the message being typed
    // as well as the different buttons.
    Container inputZone = new Container();
    inputZone.setLayout(new BorderLayout());

    inputArea = new TextField(50);
    inputArea.addActionListener(new SendListener(this));

    Container buttons = new Container();
    buttons.setLayout(new FlowLayout());

    enterButton = new Button("join");
    enterButton.addActionListener(new JoinListener(this));
    enterButton.setEnabled(false);
    buttons.add(enterButton);

    leaveButton = new Button("leave");
    leaveButton.addActionListener(new LeaveListener(this));
    leaveButton.setEnabled(false);
    buttons.add(leaveButton);

    Button quit_button = new Button("quit");
    quit_button.addActionListener(new QuitListener(this));
    buttons.add(quit_button);

    inputZone.add(inputArea, BorderLayout.CENTER);
    inputZone.add(buttons, BorderLayout.SOUTH);

    cont.add(inputZone, BorderLayout.SOUTH);

    frame.setSize(500, 300);
    frame.show();

  }

  void updateGroup() {
    System.out.println("Updating group...");
    int rows = deliveredMessages.getColumns();
    if (rows < group.size())
      rows = group.size();
    groupArea.setRows(rows);
    int columns = 0;
    for (int i = 0; i < group.size(); i++) {
      String n = group.get(i);
      if (n.length() > columns)
        columns = n.length();
    }
    groupArea.setColumns(columns);
    groupArea.setText("");
    for (int i = 0; i < group.size(); i++)
      groupArea.append(group.elementAt(i) + "\n");

    groupArea.invalidate();
    groupArea.doLayout();
    groupArea.repaint();
    // frame.doLayout();
    // frame.repaint();
  }

  WindowListener wl = new WindowListener() {

    @Override
    public void windowOpened(WindowEvent e) {
      try {
        room.enter(clientName, listener);
        leaveButton.setEnabled(true);
      } catch (Exception ex) {
        ex.printStackTrace(System.err);
        System.exit(-1);
      }
    }

    @Override
    public void windowClosing(WindowEvent e) {
      System.exit(-1); // Die, do not linger around still receiving messages...
    }

    @Override
    public void windowClosed(WindowEvent e) {
      System.exit(-1); // Die, do not linger around still receiving messages...
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

  };

  /**
   * Listener on the "join" button to re-join the group.
   */
  class JoinListener implements ActionListener {
    ChatGUI gui;

    public JoinListener(ChatGUI i) {
      gui = i;
    }

    public void actionPerformed(ActionEvent e) {
      try {
        room.enter(clientName, listener);//Ask for joining in a specific group
        enterButton.setEnabled(false);
        leaveButton.setEnabled(true);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(-1); // offending exception, commit Seppuku !
      }
    }
  }

  /**
   * Listener on the "leave" button to leave the group.
   */
  class LeaveListener implements ActionListener {
    ChatGUI gui;

    public LeaveListener(ChatGUI i) {
      gui = i;
    }

    public void actionPerformed(ActionEvent e) {
      try {
        room.leave();
        enterButton.setEnabled(true);
        leaveButton.setEnabled(false);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(-1); // offending exception, commit Seppuku !
      }
    }
  }

  /**
   * Listener to quit the application.
   */
  class QuitListener implements ActionListener {
    ChatGUI gui;

    public QuitListener(ChatGUI i) {
      gui = i;
    }

    public void actionPerformed(ActionEvent e) {
      System.exit(0);
    }
  }

  /**
   * Listener on the "send" button to send the message typed in the input text
   * area.
   */
  class SendListener implements ActionListener {
    ChatGUI gui;

    public SendListener(ChatGUI i) {
      gui = i;
    }

    public void actionPerformed(ActionEvent e) {
      try {
        String msg = clientName + " says:" + gui.inputArea.getText();
        gui.inputArea.setText("");
        room.send(msg);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(-1); // offending exception, commit Seppuku !
      }
    }
  }
  
  
  

}
