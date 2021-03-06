package ara.util;

public abstract class Message {
	
	public final long idsrc;
	public final long iddest;
	public final int pid;

	public long getIdSrc() {
		return idsrc;
	}

	public long getIdDest() {
		return iddest;
	}

	public int getPid() {
		return pid;
	}

	public Message(long idsrc, long iddest, int pid) {
		this.iddest = iddest;
		this.idsrc = idsrc;
		this.pid = pid;
	}

	@Override
	public int hashCode() {
		final int	prime	= 31;
		int			result	= 1;
		result = prime * result + (int) (iddest ^ (iddest >>> 32));
		result = prime * result + (int) (idsrc ^ (idsrc >>> 32));
		result = prime * result + pid;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (iddest != other.iddest)
			return false;
		if (idsrc != other.idsrc)
			return false;
		if (pid != other.pid)
			return false;
		return true;
	}
	
	
}
