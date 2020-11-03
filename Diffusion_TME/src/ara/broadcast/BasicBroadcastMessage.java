package ara.broadcast;

import ara.util.Message;

public class BasicBroadcastMessage extends Message {

	
	private final Message message;
	
	
	public BasicBroadcastMessage(long idsrc, long iddest, int pid, Message m) {
		super(idsrc, iddest, pid);
		this.message=m;
	}
	
	public Message getMessage() {
		return message;
	}

}
