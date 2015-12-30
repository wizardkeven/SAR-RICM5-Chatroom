package messages.engine;

public interface ConnectCallback {
  
  /**
   * Callback to notify that a previously connected channel has been closed.
   * 
   * @param channel
   */
  public void closed(Channel channel);

  /**
   * Callback to notify that a connection has succeeded.
   * @param channel
   */
  public void connected(Channel channel);
}
