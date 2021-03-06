package ara.util;

public class ElectionMessage extends Message {

	public final TypeMessage	type;
	public final Round			round;
	public final Round			acceptNum;
	public final Long			acceptVal;

	public ElectionMessage(long idsrc, long iddest, int pid, TypeMessage type, Round round, Round acceptNum,
			Long acceptVal) {
		super(idsrc, iddest, pid);
		this.type = type;
		this.round = round.copy();
		this.acceptNum = acceptNum.copy();
		this.acceptVal = acceptVal;
	}

	
	@Override
	public int hashCode() {
		final int	prime	= 31;
		int			result	= super.hashCode();
		result = prime * result + ((acceptNum == null) ? 0 : acceptNum.hashCode());
		result = prime * result + ((acceptVal == null) ? 0 : acceptVal.hashCode());
		result = prime * result + ((round == null) ? 0 : round.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ElectionMessage other = (ElectionMessage) obj;
		if (acceptNum == null) {
			if (other.acceptNum != null)
				return false;
		} else if (!acceptNum.equals(other.acceptNum))
			return false;
		if (acceptVal == null) {
			if (other.acceptVal != null)
				return false;
		} else if (!acceptVal.equals(other.acceptVal))
			return false;
		if (round == null) {
			if (other.round != null)
				return false;
		} else if (!round.equals(other.round))
			return false;
		if (type != other.type)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "ElectionMessage [type=" + type + ", round=" + round + ", acceptNum=" + acceptNum + ", acceptVal="
				+ acceptVal + "]";
	}

}