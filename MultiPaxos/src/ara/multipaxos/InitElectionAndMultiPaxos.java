package ara.multipaxos;

import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitElectionAndMultiPaxos implements Control {

	public InitElectionAndMultiPaxos(String prefix) {
	}

	@Override
	public boolean execute() {

		int				applicative_pid	= Configuration.lookupPid("applicativeE");

		List<Integer>	ids				= new ArrayList<>();
		List<Integer>	idsShuffled		= new ArrayList<>();
		for (int i = 0; i < Network.size(); i++) {
			ids.add(i);
		}

		while (ids.size() > 0) {
			int i = CommonState.r.nextInt(ids.size());
			idsShuffled.add(ids.remove(i));
		}

		for (int i = 0; i < Network.size(); i++) {
			Node				src	= Network.get(idsShuffled.get(i));
			MultiPaxosProtocol	ep	= (MultiPaxosProtocol) src.getProtocol(applicative_pid);
			ep.findLeader(src);
		}
		
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < Network.size(); j++) {
				Node				src	= Network.get(j);
				MultiPaxosProtocol	ep	= (MultiPaxosProtocol) src.getProtocol(applicative_pid);
				ep.initPaxos(src);
			}
		}

		return false;
	}
}
