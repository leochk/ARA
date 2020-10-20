package hello;

import java.util.List;

import ara.util.Message;

public class HelloMessage extends Message {

	private final List<Integer> info;

	public HelloMessage(long idsrc, long iddest, int pid, List<Integer> info) {
		super(idsrc, iddest, pid);
		this.info = info;
	}
	
	public List<Integer> getInfo() {
		return info;
	}
}
