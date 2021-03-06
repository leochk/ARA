package ara.election;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public abstract class AbstractEndMultiPaxosControler implements Control {

	protected static final String	PAR_PROTO_APPLICATIF	= "applicationE";

	protected final int				pid_application;

	public AbstractEndMultiPaxosControler(String prefix) {
		pid_application = Configuration.getPid(prefix + "." + PAR_PROTO_APPLICATIF);
	}

	@Override
	public boolean execute() {
		boolean		okLeader	= true;
		boolean		okHisto		= true;
		long		leader		= 0L;
		List<Long>	hist		= null;

		for (int i = 0; i < Network.size(); i++) {
			Node				node	= Network.get(i);
			MultiPaxosProtocol	prot2	= (MultiPaxosProtocol) node.getProtocol(pid_application);

			if (node.getFailState() == Node.OK) {
				leader = prot2.getLeaderOf();
				hist = prot2.getHistorique();
				break;
			}

		}

		for (int i = 0; i < Network.size(); i++) {
			Node				node	= Network.get(i);
			MultiPaxosProtocol	prot2	= (MultiPaxosProtocol) node.getProtocol(pid_application);

			System.out.println("Node " + node.getID() + " state = " + node.getFailState() + " leader = "
					+ prot2.getLeaderOf() + " reqPending = " + prot2.getQueueReq().size() + " hist = "
					+ prot2.getHistorique().size());

			if (node.getFailState() == Node.OK) {
				okLeader &= (leader == prot2.getLeaderOf());
				okHisto &= (hist.equals(prot2.getHistorique()));
			}

		}

		okLeader &= (leader != -1);
		
		boolean isError = false;
		if (!okLeader || Network.get((int) leader) == null) {
			System.out.println("ERROR : NO LEADER / NODES HAVE DIFFERENT LEADERS");
			isError = true;
		} else if (!okHisto) {
			System.out.println("ERROR : NODES HAVE DIFFERENT H");
			isError = true;
		}
		
		System.out.print(getFormatFile());
		System.out.println((isError) ? getErrorString() : getString());
		writeFile(isError);

		return false;
	}

	public abstract String getString();

	public abstract String getErrorString();

	public abstract String getFilePath();

	public abstract String getFormatFile();

	protected void writeFile(boolean isError) {
		FileWriter		fw;
		BufferedWriter	output;

		try {
			File file = new File("logs/" + getFilePath());
			
			String stringout = (isError) ? getErrorString() : getString();
			
			if (file.exists()) {
				fw = new FileWriter("logs/" + getFilePath(), true);
				output = new BufferedWriter(fw);
				output.write(stringout);
			} else {
				fw = new FileWriter("logs/" + getFilePath(), true);
				output = new BufferedWriter(fw);
				file.createNewFile();
				output.write(getFormatFile());
				output.write(stringout);
			}

			output.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
