package ara.broadcast;

import ara.util.Message;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDProtocol;

public interface BroadcastProtocol extends Protocol{
	
	public void broadcast(Node src, Message m);
	public default void deliver(Node host, Message m) {
		int pid_dessus=m.getPid();
		((EDProtocol)host.getProtocol(pid_dessus)).processEvent(host, pid_dessus, m); 
	}
	
}
