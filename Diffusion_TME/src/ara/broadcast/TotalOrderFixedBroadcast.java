package ara.broadcast;

import java.util.HashSet;
import java.util.Set;

import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

public class TotalOrderFixedBroadcast implements EDProtocol, BroadcastProtocol {

	private static final String PAR_TRANSPORT = "transport";

	private final int transport_id;
	private final int protocol_id;
	private static int cur_num_seq = 0;
	private Set<FIFOBroadcastMessage>[] rec;
	private long nextdev[];

	public TotalOrderFixedBroadcast(String prefix) {
		rec = new HashSet[Network.size()];
		for (int i = 0; i < Network.size(); i++) {
			rec[i] = new HashSet<>();
		}
		nextdev = new long[Network.size()];

		transport_id = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		String tmp[] = prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length - 1]);
	}

	@Override
	public void broadcast(Node src, Message m) {
		Transport tr = (Transport) src.getProtocol(transport_id);
		long idsrc = src.getID();
		for (int i = 0; i < Network.size(); i++) {
			Node dst = Network.get(i);
			long idDest = Network.get(i).getID();
			FIFOBroadcastMessage broadcast_m = new FIFOBroadcastMessage(idsrc, idDest, protocol_id, idsrc, cur_num_seq,
					m);
			tr.send(src, dst, broadcast_m, protocol_id);
		}
		cur_num_seq++;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		
		if (protocol_id != pid) {
			throw new RuntimeException("Receive Message for wrong protocol");
		}
		if (event instanceof FIFOBroadcastMessage) {
			FIFOBroadcastMessage fbm = (FIFOBroadcastMessage) event;

			long s = fbm.getIdSender();
			if (!rec[(int) node.getID()].contains(fbm)) {
				rec[(int) node.getID()].add(fbm);
				FIFOBroadcastMessage m;
				while ((m = func(node.getID(), s)) != null) {
					deliver(node, m.getMessage());
					nextdev[(int) node.getID()] += 1;
					rec[(int) node.getID()].remove(m);
				}
			}
		}
		
	}

	private FIFOBroadcastMessage func(long receiver, long sender) {
		FIFOBroadcastMessage res = null;
		for (FIFOBroadcastMessage m : rec[(int) receiver]) {
			if (m.getNumseq() == nextdev[(int) receiver]) {
				res = m;
			}
		}
		return res;
	}

	@Override
	public Object clone() {
		TotalOrderFixedBroadcast bb = null;
		try {
			bb = (TotalOrderFixedBroadcast) super.clone();
		} catch (CloneNotSupportedException e) {
		} // never happens
		return bb;
	}

}
