package ara.util;

public class Round implements Comparable<Round> {
	public Integer	round;
	public Long	id;

	public Round(int round, long id) {
		this.round = round;
		this.id = id;
	}

	public void incr() {
		this.round += 1;
	}

	public int getRound() {
		return round;
	}

	@Override
	public String toString() {
		return "<" + round + "," + id + ">";
	}

	@Override
	public int compareTo(Round o) {
		if (round == o.round) {
			return id.compareTo(o.id);
		}
		return round.compareTo(o.round);
	}


	@Override
	public int hashCode() {
		final int	prime	= 31;
		int			result	= 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((round == null) ? 0 : round.hashCode());
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
		Round other = (Round) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (round == null) {
			if (other.round != null)
				return false;
		} else if (!round.equals(other.round))
			return false;
		return true;
	}

	public Round copy() {
		return new Round(this.round.intValue(), this.id.longValue());
	}
}