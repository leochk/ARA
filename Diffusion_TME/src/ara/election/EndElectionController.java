package ara.election;

import peersim.core.CommonState;
import peersim.core.Network;

public class EndElectionController extends AbstractEndMultiPaxosControler {

	public EndElectionController(String prefix) {
		super(prefix);
	}

	public String getString() {
		MultiPaxosProtocol	prot	= (MultiPaxosProtocol) Network.get(0).getProtocol(pid_application);
		StringBuilder		str		= new StringBuilder();

		str.append(CommonState.r.getLastSeed() + ";" + Network.size() + ";" + prot.getStep() + ";" + prot.getMin() + ";"
				+ (prot.getMin() + prot.getRange() - 1) + ";" + prot.isInit_0() + ";" + prot.getTOTAL_MESSAGE_ELEC()
				+ ";" + prot.getCurrentRound().getRound() + ";" + prot.getElectionTime() + "\n");
		return str.toString();
	}

	@Override
	public String getFilePath() {
		return "data";
	}

	@Override
	public String getFormatFile() {
		return "seed;size;stepDelay;minDelay;maxDelay;init_0;messages;rounds;time\n";
	}

	@Override
	public String getErrorString() {
		MultiPaxosProtocol	prot	= (MultiPaxosProtocol) Network.get(0).getProtocol(pid_application);
		StringBuilder		str		= new StringBuilder();

		str.append(CommonState.r.getLastSeed() + ";" + Network.size() + ";" + prot.getStep() + ";" + prot.getMin() + ";"
				+ (prot.getMin() + prot.getRange() - 1) + ";" + prot.isInit_0() + ";" + -1 + ";" + -1 + ";" + -1
				+ "\n");

		return str.toString();
	}

}
