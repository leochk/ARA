package ara.broadcast;


import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Initialisation implements Control{
	
	public  Initialisation(String prefix) {}
	
	@Override
	public boolean execute() {
				
		
		System.err.println("Should Find : "+getEstimation());
		int applicative_pid=Configuration.lookupPid("applicative");
		Node src = Network.get(0);
		ApplicativeProtocol ap = (ApplicativeProtocol)src.getProtocol(applicative_pid);
		ap.BroadcastModif(src);
			
		
		return false;
	}

	private int getEstimation() {
		int sum=0;
		for(int i=0;i< Network.size();i++){
			sum+=(i+1);
			sum*=(i+1);
		}
		return sum;
	}	
}
