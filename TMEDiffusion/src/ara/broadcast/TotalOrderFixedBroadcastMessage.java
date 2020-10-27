package ara.broadcast;

import ara.util.Message;

public class TotalOrderFixedBroadcastMessage extends FIFOBroadcastMessage {
	public TotalOrderFixedBroadcastMessage(long idsrc, long iddest, int pid, long idSender, long numseq,
			Message message) {
		super(idsrc, iddest, pid, idSender, numseq, message);
		// TODO Auto-generated constructor stub
	}
}