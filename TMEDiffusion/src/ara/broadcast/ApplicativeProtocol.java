package ara.broadcast;

import java.util.HashMap;
import java.util.Map;

import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

public class ApplicativeProtocol implements EDProtocol {

	private static final String PAR_BROADCASTID = "broadcast";
	private long variable = 0;
	private boolean broadcast_done = false;
	private Map<Long, Integer> nbMessReceived;

	private final int protocol_id;
	private final int broadcast_protocol_id;

	public ApplicativeProtocol(String prefix) {
		nbMessReceived = new HashMap<Long, Integer>();
		String tmp[] = prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length - 1]);
		broadcast_protocol_id = Configuration.getPid(prefix + "." + PAR_BROADCASTID);
	}

	public long getVariable() {
		return variable;
	}

	public Object clone() {
		ApplicativeProtocol ap = null;
		try {
			ap = (ApplicativeProtocol) super.clone();
			ap.nbMessReceived = new HashMap<Long, Integer>();

		} catch (CloneNotSupportedException e) {
		} // never happens
		return ap;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if (protocol_id != pid) {
			throw new IllegalArgumentException("Receive Message for wrong protocol");
		}

		if (event instanceof ApplicativeMessage) {

			ApplicativeMessage mess = (ApplicativeMessage) event;

			if (mess.isPlus()) {
				variable += mess.getVal();
				System.err
						.println(" Node " + node.getID() + " receive from " + mess.getIdSrc() + "  + " + mess.getVal());
			} else {
				variable *= (Long) mess.getVal();
				System.err
						.println(" Node " + node.getID() + " receive from " + mess.getIdSrc() + "  * " + mess.getVal());
			}

			if (this.nbMessReceived.containsKey(mess.getIdSrc())) {
				int val = nbMessReceived.get(mess.getIdSrc());
				this.nbMessReceived.put(mess.getIdSrc(), val + 1);
			} else {
				this.nbMessReceived.put(mess.getIdSrc(), 1);
			}
			if (shouldBroadcast(node)) {
				BroadcastModif(node);
			}
		}

	}

	private boolean shouldBroadcast(Node host) {

		if (broadcast_done) {
			return false;
		}

		// trouver le noeud fiable d'ID inférieur le plus élevé
		Node winner = null;
		for (int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			if (n.getID() >= host.getID()) {
				continue;
			}
			if (n.getFailState() == Fallible.OK) {// on triche ici sur la détection de défaillance
				if (winner == null) {
					winner = n;
				} else if (n.getID() > winner.getID()) {
					winner = n;
				}
			}
		}
		if (winner == null) {// personne avant moi n'est vivant
			return true;
		}

		if (!nbMessReceived.containsKey(winner.getID())) {
			return false;
		}
		if (nbMessReceived.get(winner.getID()) < 2) {
			return false;
		}

		return true;
	}

	public void BroadcastModif(Node host) {
		// System.out.println("time " + CommonState.getTime() + " Node " + host.getID()
		// + " make broadcast");
		long idSrc = host.getID();
		BroadcastProtocol b = (BroadcastProtocol) host.getProtocol(broadcast_protocol_id);
		System.err.println(" Node " + host.getID() + " make broadcast + " + (idSrc + 1));
		b.broadcast(host, new ApplicativeMessage(idSrc, -2, protocol_id, new Long(idSrc + 1), true));
		System.err.println(" Node " + host.getID() + " make broadcast * " + (idSrc + 1));
		b.broadcast(host, new ApplicativeMessage(idSrc, -2, protocol_id, new Long(idSrc + 1), false));
		broadcast_done = true;

	}

	public static class ApplicativeMessage extends Message {

		private final long val;
		private final boolean plus;

		public ApplicativeMessage(long idsrc, long iddest, int pid, long val, boolean plus) {
			super(idsrc, iddest, pid);
			this.val = val;
			this.plus = plus;
		}

		public long getVal() {
			return val;
		}

		public boolean isPlus() {
			return plus;
		}
	}

}