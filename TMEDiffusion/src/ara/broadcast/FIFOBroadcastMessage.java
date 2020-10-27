package ara.broadcast;

import ara.util.Message;

public class FIFOBroadcastMessage extends Message {

	private final long idSender;
	private final long numseq;
	private final Message message;

	public long getIdSender() {
		return idSender;
	}

	public long getNumseq() {
		return numseq;
	}

	public FIFOBroadcastMessage(long idsrc, long iddest, int pid, long idSender, long numseq, Message message) {
		super(idsrc, iddest, pid);
		this.idSender = idSender;
		this.numseq = numseq;
		this.message = message;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof FIFOBroadcastMessage)) {
			return false;
		}
		FIFOBroadcastMessage rbm = (FIFOBroadcastMessage) o;
		return rbm.idSender == this.idSender && rbm.numseq == this.numseq;

	}

	public Message getMessage() {
		return message;
	}

	@Override
	public int hashCode() {

		return (int) numseq * 1000 + (int) idSender;
	}

}