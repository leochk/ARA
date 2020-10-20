package ara.broadcast;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;

public class EndControler implements Control {

	private static final String PAR_PROTO_APPLICATIF="application";
	
	private final int pid_application;
	
	public EndControler(String prefix) {
		pid_application=Configuration.getPid(prefix+"."+PAR_PROTO_APPLICATIF);
	}
	
	@Override
	public boolean execute() {
		
		System.out.println("################################# AFFICHAGE DES VALEURS ###########################");
		for(int i=0;i<Network.size();i++){
			Node node =Network.get(i);
			ApplicativeProtocol prot = (ApplicativeProtocol)node.getProtocol(pid_application);
			System.out.println("On node "+node.getID()+" variable = "+prot.getVariable()+"  ("+ (node.getFailState()==Fallible.OK?"alive":"dead")+")");
		}
				
		
		return false;
	}

}
