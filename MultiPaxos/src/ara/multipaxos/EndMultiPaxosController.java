package ara.multipaxos;

import java.util.Arrays;

import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;

public class EndMultiPaxosController extends AbstractEndMultiPaxosControler {

	public EndMultiPaxosController(String prefix) {
		super(prefix);
	}

	public String getString() {
		MultiPaxosProtocol	prot	= null;
		StringBuilder		str		= new StringBuilder();

		for (int i = 0; i < Network.size(); i++) {
			if (Network.get(i).getFailState() == Fallible.OK) {
				prot = (MultiPaxosProtocol) Network.get(i).getProtocol(pid_application);
				break;
			}
		}

		double latency = 0f;
		for (int i = 0; i < prot.getHistoriqueLatency().size() - 1; i++) {
			latency += prot.getHistoriqueLatency().get(i);
		}
		latency /= prot.getHistoriqueLatency().size();

		double debit = prot.getHistorique().size() * 1f / CommonState.getIntTime();
		
		
		str.append(CommonState.r.getLastSeed() + ";" + Network.size() + ";" + prot.getNbMultiPaxosMessage() + ";"
				+ String.format("%.12f", debit).replace(',', '.') + ";" + latency + ";" + prot.getProbaFauteFranche()
				+ ";" + prot.getProbaFauteTransitoire() + "\n");
		return str.toString();
	}

	@Override
	public String getFilePath() {
		return "data2";
	}

	@Override
	public String getFormatFile() {
		return "seed;size;nbMsg;debit;latency;probaFF;probaFT\n";
	}

	@Override
	public String getErrorString() {
		MultiPaxosProtocol	prot	= null;
		StringBuilder		str		= new StringBuilder();

		for (int i = 0; i < Network.size(); i++) {
			if (Network.get(i).getFailState() == Fallible.OK) {
				prot = (MultiPaxosProtocol) Network.get(i).getProtocol(pid_application);
				break;
			}
		}
		str.append(CommonState.r.getLastSeed() + ";" + Network.size() + ";" + -1 + ";" + -1 + ";" + -1 + ";"
				+ prot.getProbaFauteFranche() + ";" + prot.getProbaFauteTransitoire() + "\n");
		return str.toString();
	}

}
