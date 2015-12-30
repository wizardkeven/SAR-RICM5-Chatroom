package chat.gui;

public class ChatException extends Exception {

	private static final long serialVersionUID = 1L;
	
		String message;

		public ChatException() {
		}

		public ChatException(String message) {
			this.message = message;
		}

		public String getMessage(){
			return message;
		}

}
