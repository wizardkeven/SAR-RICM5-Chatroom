package messages.engine;

public class Util {

  /** Read a signed 32bit value */
  static public int readInt32(byte bytes[], int offset) {
    int val;
    val = ((bytes[offset] & 0xFF) << 24);
    val |= ((bytes[offset+1] & 0xFF) << 16);
    val |= ((bytes[offset+2] & 0xFF) << 8);
    val |= (bytes[offset+3] & 0xFF);
    return val;
  }


  /** Write a signed 32bit value */
  static public void writeInt32(byte[] bytes, int offset, int value) {
    bytes[offset]= (byte)((value >> 24) & 0xff);
    bytes[offset+1]= (byte)((value >> 16) & 0xff);
    bytes[offset+2]= (byte)((value >> 8) & 0xff);
    bytes[offset+3]= (byte)(value & 0xff);
  }

}
