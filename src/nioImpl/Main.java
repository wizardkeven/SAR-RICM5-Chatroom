package nioImpl;
public class Main {

  public static void main(String args[]) throws Exception {
	Ping ping = new Ping(1234);
//	Pong pong = new Pong(1234);
    ping.start();
//    pong.start();

  }

}
