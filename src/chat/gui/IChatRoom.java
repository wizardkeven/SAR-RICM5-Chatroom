package chat.gui;

public interface IChatRoom {

  /**
   * This is the callback for the chat room.
   * It is given when entering a chat room, 
   * and it is delivered messages as well as
   * it is kept informed about who joins or leaves 
   * the group. 
   */
  public interface IChatListener {
    public void deliver(String msg);
    public void joined(String name);
    public void left(String name);
  }
  
  /**
   * Enter the chat 
   * @throws ChatException if already in the chat.
   */  
  public void enter (String clientName, IChatListener l) throws ChatException;


  /**
   * Leave the chat 
   * @throws ChatException if not in the chat.
   */  
  public void leave () throws ChatException;


  /**
   * Send a message to the chat 
   * @throws ChatException if not in the chat. 
   */   
  public void send (String msg) throws ChatException;


}
