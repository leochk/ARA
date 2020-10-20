package ara.broadcast;

import java.util.HashSet;
import java.util.Set;

import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

public class ReliableBroadcast implements EDProtocol, BroadcastProtocol {

	private static final String PAR_TRANSPORT = "transport";

	private int cur_num_seq = 0;
	private final int protocol_id;
	private final int transport_id;

	private Set<ReliableBroadcastMessage> rec;

	public ReliableBroadcast(String prefix) {
		rec = new HashSet<ReliableBroadcastMessage>();
		String tmp[] = prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length - 1]);
		transport_id = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
	}

	@Override
	public Object clone() {
		ReliableBroadcast rb = null;
		try {
			rb = (ReliableBroadcast) super.clone();
			rb.rec = new HashSet<ReliableBroadcastMessage>();
		} catch (CloneNotSupportedException e) {
		} // never happens
		return rb;
	}

	@Override
	public void broadcast(Node src, Message m) {
		Transport tr = (Transport) src.getProtocol(transport_id);
		long idsrc = src.getID();
		for (int i = 0; i < Network.size(); i++) {
			Node dst = Network.get(i);
			long idDest = Network.get(i).getID();
			ReliableBroadcastMessage broadcast_m = new ReliableBroadcastMessage(idsrc, idDest, protocol_id, idsrc,
					cur_num_seq, m);
			tr.send(src, dst, broadcast_m, protocol_id);

		}
		cur_num_seq++;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if (protocol_id != pid) {
			throw new RuntimeException("Receive Message for wrong protocol");
		}
		if (event instanceof ReliableBroadcastMessage) {
			ReliableBroadcastMessage rbm = (ReliableBroadcastMessage) event;
			if (!rec.contains(rbm)) {
				rec.add(rbm);
				if (rbm.getIdSender() != node.getID()) {
					Transport tr = (Transport) node.getProtocol(transport_id);
					for (int i = 0; i < Network.size(); i++) {
						Node dst = Network.get(i);
						long idDest = Network.get(i).getID();
						if (idDest != node.getID()) {
							ReliableBroadcastMessage broadcast_m = new ReliableBroadcastMessage(node.getID(), idDest,
									protocol_id, rbm.getIdSender(), rbm.getNumseq(), rbm.getMessage());
							tr.send(node, dst, broadcast_m, protocol_id);
						}
					}
				}
				// on délivre le message
				deliver(node, rbm.getMessage());

			}

		}
	}

}
