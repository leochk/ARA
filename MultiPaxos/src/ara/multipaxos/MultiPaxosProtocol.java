package ara.multipaxos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ara.util.ElectionMessage;
import ara.util.PaxosMessage;
import ara.util.Round;
import ara.util.TypeMessage;
import peersim.config.Configuration;
import peersim.config.IllegalParameterException;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class MultiPaxosProtocol implements EDProtocol {

	private final int							protocol_id;

	// ELECTION
	private Round								ballotNum				= new Round(0, 0);
	private Round								acceptNum				= new Round(0, 0);
	private long								acceptVal				= -1L;
	private Map<Round, List<ElectionMessage>>	promiseByRound			= new HashMap<>();
	private Map<Long, Integer>					valueAccepted			= new HashMap<>();
	private long								delayAfterRefuse		= 0L;

	private long								electionTime			= 0L;
	private long								leader					= -1L;
	private final boolean						init_0;
	private boolean								didInit					= false;
	private final long							min, range, step;

	private static boolean						cyclicElection			= false;

	// MULTIPAXOS
	private Round								ballotNumP				= new Round(0, 0);
	private Round								acceptNumP				= new Round(0, 0);
	private List<Long>							acceptValP				= new ArrayList<>();
	private Map<Round, List<PaxosMessage>>		promiseByRoundP			= new HashMap<>();
	private Map<List<Long>, Integer>			valueAcceptedP			= new HashMap<>();
	private List<Long>							historique				= new ArrayList<>();
	private boolean								waitingDeicison			= false;
	private LinkedList<Long>					queueRequest			= new LinkedList<>();
	private Set<Long>							requestOk				= new HashSet<>();
	private List<Long>							historiqueLantency		= new ArrayList<>();

	// FAUTES
	private static float						probaFauteTransitoire;
	private static float						probaFauteFranche;
	private static List<Node>					faulty_nodes			= new ArrayList<Node>();

	// BUFFERIZATION
	private static int							buffer_N				= 1;

	private static boolean						SHOWLOG;
	private static long							TOTAL_MESSAGE_ELEC		= 0;
	public static long							cptReq					= 0;

	private static final String					PAR_MINDELAY			= "minDelay";
	private static final String					PAR_MAXDELAY			= "maxDelay";
	private static final String					PAR_STEPDELAY			= "stepDelay";
	private static final String					PAR_SHOWLOG				= "showLog";
	private static final String					PAR_INIT_VAL_0			= "init_value_0";
	private static final String					PAR_PROB_FAUTE_TRANSI	= "probaFauteTransitoire";
	private static final String					PAR_PROB_FAUTE_FRANCHE	= "probaFauteFranche";
	private static final String					PAR_BUFFER_N			= "bufferN";
	private static final String					PAR_CYCLIC_ELEC			= "cyclicElection";

	public MultiPaxosProtocol(String prefix) {

		String tmp[] = prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length - 1]);
		init_0 = Configuration.getBoolean(prefix + "." + PAR_INIT_VAL_0);
		min = Configuration.getLong(prefix + "." + PAR_MINDELAY);
		long max = Configuration.getLong(prefix + "." + PAR_MAXDELAY, min);
		step = Configuration.getLong(prefix + "." + PAR_STEPDELAY);
		buffer_N = Configuration.getInt(prefix + "." + PAR_BUFFER_N);
		cyclicElection = Configuration.getBoolean(prefix + "." + PAR_CYCLIC_ELEC);
		probaFauteTransitoire = (float) Configuration.getDouble(prefix + "." + PAR_PROB_FAUTE_TRANSI);
		probaFauteFranche = (float) Configuration.getDouble(prefix + "." + PAR_PROB_FAUTE_FRANCHE);

		SHOWLOG = Configuration.getBoolean(prefix + "." + PAR_SHOWLOG);

		if (max < min)
			throw new IllegalParameterException(prefix + "." + PAR_MAXDELAY,
					"The maximum latency cannot be smaller than the minimum latency");
		range = max - min + 1;
	}

	private void errLog(String str) {
		if (SHOWLOG)
			System.err.println(CommonState.getIntTime() + " : " + str);
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {

		if (protocol_id != pid) {
			throw new IllegalArgumentException("Receive Message for wrong protocol");
		}

		if (event instanceof ElectionMessage) {
			processElectionMessage(node, pid, event);
		}

		if (event instanceof PaxosMessage) {
			processPaxosMessage(node, pid, event);
		}
	}

	private void processElectionMessage(Node node, int pid, Object event) {
		TOTAL_MESSAGE_ELEC++;

		ElectionMessage	mess	= (ElectionMessage) event;
		TypeMessage		type	= mess.type;
		int				n		= Network.size();
		int				nodeId	= (int) node.getID();

		Object			answer;

		if (type == TypeMessage.FINDLEADER) {
			/*
			 * node i only answer PROMISE to node j that has sent him PREPARE with higher
			 * round than node i's round
			 */
			TypeMessage answerType;

			if (mess.round.compareTo(ballotNum) >= 0) {
				ballotNum = mess.round;
				answerType = TypeMessage.PROMISE;
			} else {
				answerType = TypeMessage.REJECT;
			}

			/*
			 * answer contains acceptNum_i and acceptVal_i to node j
			 */
			Node dest = getNodeOfID(mess.getIdSrc());
			answer = new ElectionMessage(nodeId, mess.getIdSrc(), protocol_id, answerType, ballotNum, acceptNum,
					acceptVal);

			long delay = getRandomDelay();
			send(node, dest, answer, protocol_id, delay, false);
		}

		if (type == TypeMessage.PROMISE) {
			List<ElectionMessage> tmp;
			/*
			 * store all messages PROMISE from nodes j that node i has sent PREPARE
			 */
			if (promiseByRound.containsKey(mess.round))
				tmp = promiseByRound.get(mess.round);
			else
				tmp = new ArrayList<>();

			tmp.add(mess);
			promiseByRound.put(mess.round, tmp);

			/*
			 * When received a majority (n/2+1) PROMISE from nodes j, node i propose his
			 * value if all acceptValue_j from nodes are -1. Otherwise, he re-propose
			 * acceptValue_j with the higher acceptNum_j
			 */
			int nbMessagePromise = tmp.size();

			if (nbMessagePromise == n / 2 + 1) {
//				errLog("(" + nodeId + ") " + type + " majority \t\t" + mess.round + " " + mess.acceptNum + " "
//						+ mess.acceptVal);

				Long	val	= -1L;
				Round	aN	= new Round(0, 0);

				for (List<ElectionMessage> l : promiseByRound.values()) {
					for (ElectionMessage em : l) {
						if ((em.acceptVal != -1L && em.acceptNum.compareTo(aN) > 0)) {
							val = em.acceptVal;
							aN = em.acceptNum;
						}
					}
				}

				if (val == -1L) {
					aN = ballotNum;
					if (init_0 && !didInit) {
						val = 0L;
						didInit = true;
					} else {
						val = node.getID();
					}
				}

				acceptVal = val;
				acceptNum = aN;

				long delay = getRandomDelay();
				broadcastElectionMessage(node, TypeMessage.ACCEPT, ballotNum, aN, val, delay);
			}
		}

		/*
		 * On reception of REJECT message, wait for a delay before re-ask for a leader
		 */
		if (type == TypeMessage.REJECT && leader == -1) {
			delayAfterRefuse += step;
			long delay = delayAfterRefuse;

			delay += getRandomDelay();
			findLeader(node, delay);
		}

		/*
		 * node i accept acceptNum_j from node j if acceptNum_j is higher than
		 * ballotNum_i, then broadcast the value accepted to the system
		 */
		if (type == TypeMessage.ACCEPT && leader == -1) {
			if (mess.acceptNum.compareTo(acceptNum) >= 0) {
				acceptNum = mess.acceptNum;
				acceptVal = mess.acceptVal;
				long delay = (range == 1 ? min : min + CommonState.r.nextLong(range));
				broadcastElectionMessage(node, TypeMessage.ACCEPTED, mess.round, mess.acceptNum, mess.acceptVal, delay);
			}
		}

		/*
		 * when node i has received a majority (n/2+1) acceptVal_j, we can decide the
		 * leader id
		 */
		if (type == TypeMessage.ACCEPTED && leader == -1) {

			int nb = 1;

			if (valueAccepted.containsKey(mess.acceptVal)) {
				nb = valueAccepted.get(mess.acceptVal) + 1;
				valueAccepted.put(mess.acceptVal, nb);
			} else {
				valueAccepted.put(mess.acceptVal, 1);
			}

			if (nb == (n / 2 + 1)) {
				if (nodeId == mess.acceptVal)
					errLog("(" + nodeId + ") is leader");
				leader = mess.acceptVal;
				electionTime = CommonState.getTime();

				// on election, sending first ping message to leader, containing first request
				// if node is leader, it immediately sends PREPARE for multipaxos
				if (nodeId != leader) {
					sendPing(node);
				} else {
					waitingDeicison = true;
					sendPrepareRequest(node);
				}
			}
		}
	}

	private void sendPing(Node node) {
		if (queueRequest.size() == 0)
			return;

		Long			req		= queueRequest.removeFirst();

		// ping message
		PaxosMessage	ping	= new PaxosMessage(node.getID(), leader, protocol_id, TypeMessage.PING, ballotNumP,
				new Round(-1, -1), new ArrayList<>(), new ArrayList<>());
		ping.listIdreq.add(req);
		send(node, getNodeOfID(leader), ping, protocol_id, getRandomDelay(), true);

		// timeout ping message
		if (node.getID() != leader) {
			PaxosMessage timeoutping = new PaxosMessage(node.getID(), node.getID(), protocol_id,
					TypeMessage.TIMEOUTPING, ballotNumP, new Round(-1, -1), new ArrayList<>(), new ArrayList<>());
			timeoutping.listIdreq.add(req);
			send(node, node, timeoutping, protocol_id, (min + range) * 3, true);

		}

	}

	private void sendPrepareRequest(Node node) {
		if (queueRequest.size() == 0)
			return;

		List<Long> requests = new ArrayList<>();
		while (queueRequest.size() > 0 && requests.size() < buffer_N)
			requests.add(queueRequest.removeFirst());

		Round r2 = new Round(ballotNumP.round + 1, node.getID());
		ballotNumP = r2;
		broadcastPaxosMessage(node, TypeMessage.PREPARE, ballotNumP, new Round(-1, -1), new ArrayList<>(), requests,
				getRandomDelay());
	}

	static long	ping				= 0;
	static long	pong				= 0;
	static long	timeout				= 0;
	static long	accept				= 0;
	static long	accepted			= 0;
	static long	reject				= 0;

	static long	nbMessageMultiPaxos	= 0;

	private void processPaxosMessage(Node node, int pid, Object event) {

		PaxosMessage	mess	= (PaxosMessage) event;
		TypeMessage		type	= mess.type;
		int				n		= Network.size();
		int				nodeId	= (int) node.getID();

		Object			answer;

		if (type != TypeMessage.PING && type != TypeMessage.PONG_OK && type != TypeMessage.TIMEOUTPING) {
			nbMessageMultiPaxos++;
		}

		if (type == TypeMessage.PING) {
			ping++;
			queueRequest.addLast(mess.listIdreq.get(0));

			PaxosMessage pong = new PaxosMessage(node.getID(), mess.idsrc, mess.pid, TypeMessage.PONG_OK, mess.round,
					mess.acceptNum, mess.acceptVal, new ArrayList<>(mess.listIdreq));
			send(node, getNodeOfID(mess.idsrc), pong, protocol_id, getRandomDelay(), true);

			if (node.getID() == leader && !waitingDeicison) {
				waitingDeicison = true;
				sendPrepareRequest(node);
			}
		}

		if (type == TypeMessage.PONG_OK) {
			pong++;
			// new request
			queueRequest.addLast(cptReq++);
			requestOk.add(mess.listIdreq.get(0));
			sendPing(node);
		}

		if (type == TypeMessage.TIMEOUTPING) {
			timeout++;
			if (!requestOk.contains(mess.listIdreq.get(0))) {
				queueRequest.addFirst(mess.listIdreq.get(0));
				if (cyclicElection) {
					leader = (leader + 1) % Network.size();
					sendPing(node);
				} else {
					leader = -1;
					acceptNum = new Round(0, 0);
					acceptVal = -1L;
					promiseByRound = new HashMap<>();
					valueAccepted = new HashMap<>();
					findLeader(node);
				}
			}
		}
		if (type == TypeMessage.PREPARE) {

			TypeMessage answerType;
			if (mess.round.compareTo(ballotNumP) >= 0) {
				ballotNumP = mess.round;
				answerType = TypeMessage.PROMISE;
			} else {
				answerType = TypeMessage.REJECT;
			}

			if (historique.size() == 0 || historique.get(historique.size() - 1) != -1) {
				historique.add(-1L);
				historiqueLantency.add((long) CommonState.getIntTime());
			}

			Node dest = getNodeOfID(mess.getIdSrc());
			answer = new PaxosMessage(nodeId, mess.getIdSrc(), protocol_id, answerType, mess.round, acceptNumP,
					acceptValP, new ArrayList<>(mess.listIdreq));

			send(node, dest, answer, protocol_id, getRandomDelay(), true);

		}
		if (type == TypeMessage.REJECT) {
			reject++;
			while (mess.listIdreq.size() > 0)
				queueRequest.addFirst(mess.listIdreq.remove(0));
			waitingDeicison = false;
			sendPing(node);
		}

		if (type == TypeMessage.PROMISE) {

			List<PaxosMessage> tmp;

			if (promiseByRoundP.containsKey(mess.round))
				tmp = promiseByRoundP.get(mess.round);
			else
				tmp = new ArrayList<>();

			tmp.add(mess);
			promiseByRoundP.put(mess.round, tmp);

			/*
			 * When received a majority (n/2+1) PROMISE from nodes j, node i propose his
			 * value if all acceptValue_j from nodes are -1. Otherwise, he re-propose
			 * acceptValue_j with the higher acceptNum_j
			 */
			int nbMessagePromise = tmp.size();

			if (nbMessagePromise == n / 2 + 1) {
				errLog("PAXOS : (" + nodeId + ") " + type + " majority \t\t" + mess.round + " " + mess.acceptNum + " "
						+ mess.acceptVal + " " + mess.listIdreq);

				List<Long>	val	= new ArrayList<>();
				Round		aN	= new Round(0, 0);

				for (List<PaxosMessage> l : promiseByRoundP.values()) {
					for (PaxosMessage em : l) {
						if ((em.acceptVal.size() != 0 && em.acceptNum.compareTo(aN) > 0)) {
							val = em.acceptVal;
							aN = em.acceptNum;
						}
					}
				}

				if (val.size() == 0) {
					aN = ballotNumP;
					val = new ArrayList<>(mess.listIdreq);
				}

				acceptValP = val;
				acceptNumP = aN;

				long delay = getRandomDelay();
				broadcastPaxosMessage(node, TypeMessage.ACCEPT, ballotNumP, aN, val, new ArrayList<>(mess.listIdreq),
						delay);
			}
		}

		/*
		 * node i accept acceptNum_j from node j if acceptNum_j is higher than
		 * ballotNum_i, then broadcast the value accepted to the system
		 */
		if (type == TypeMessage.ACCEPT) {
			accept++;
			if (mess.acceptNum.compareTo(acceptNumP) >= 0) {

				acceptNumP = mess.acceptNum;
				acceptValP = mess.acceptVal;
				long delay = (range == 1 ? min : min + CommonState.r.nextLong(range));
				broadcastPaxosMessage(node, TypeMessage.ACCEPTED, mess.round, mess.acceptNum, mess.acceptVal,
						new ArrayList<>(mess.listIdreq), delay);
			}
		}

		/*
		 * when node i has received a majority (n/2+1) acceptVal_j, we can decide the
		 * leader id
		 */
		if (type == TypeMessage.ACCEPTED) {
			accepted++;
			int nb = 1;

			if (valueAcceptedP.containsKey(mess.acceptVal)) {
				nb = valueAcceptedP.get(mess.acceptVal) + 1;
				valueAcceptedP.put(mess.acceptVal, nb);
			} else {
				valueAcceptedP.put(mess.acceptVal, nb);
			}

			if (nb == n / 2 + 1) {
				if (leader == nodeId)
					errLog("(" + nodeId + ") ping = " + ping + " pong = " + pong + " timeout = " + timeout
							+ " accept = " + accept + " accepted = " + accepted);

				historique.set(historique.size() - 1, mess.listIdreq.get(0));
				for (int i = 1; i < mess.listIdreq.size(); i++)
					historique.add(mess.listIdreq.get(i));

				long latency = CommonState.getIntTime() - historiqueLantency.get(historiqueLantency.size() - 1);
				historiqueLantency.set(historiqueLantency.size() - 1, latency);

				acceptNumP = new Round(-1, -1);
				acceptValP = new ArrayList<>();
				promiseByRoundP = new HashMap<>();
				valueAcceptedP = new HashMap<>();

				waitingDeicison = false;

				// on election, sending first ping message to leader, containing first request
				if (nodeId != leader) {
					sendPing(node);
				} else {
					waitingDeicison = true;
					// queueRequest.add(cptReq++);
					sendPrepareRequest(node);
				}
			}
		}
	}

	public void findLeader(Node host) {
		findLeader(host, getRandomDelay());
	}

	public void findLeader(Node host, long delay) {
		Round r2 = new Round(ballotNum.round + 1, host.getID());
		broadcastElectionMessage(host, TypeMessage.FINDLEADER, r2, new Round(-1, -1), -1L, delay);
	}

	public void initPaxos(Node host) {
		queueRequest.addLast(cptReq++);
		if (leader != -1) {
			sendPing(host);
		}

	}

	public void send(Node src, Node dest, Object msg, int pid, long delay, boolean faultable) {
		if (dest == null)
			return;

		EDSimulator.add(delay, msg, dest, pid);
		if (faultable && CommonState.r.nextFloat() <= probaFauteFranche && faulty_nodes.size() < Network.size() / 2 - 1
				&& !faulty_nodes.contains(src)) {

			src.setFailState(Fallible.DEAD);
			faulty_nodes.add(src);

			if (leader == src.getID())
				errLog("(" + src.getID() + ")" + " is dead after sending a " + msg.getClass().getCanonicalName());
		}
	}

	private void broadcastElectionMessage(Node host, TypeMessage type, Round round, Round acceptNum, Long value,
			long delay) {
		long			src	= host.getID();
		ElectionMessage	m	= new ElectionMessage(src, -1, protocol_id, type, round, acceptNum, value);

		for (int i = 0; i < Network.size(); i++) {
			Node dst = Network.get(i);
			send(host, dst, m, protocol_id, delay, false);
		}
	}

	private void broadcastPaxosMessage(Node host, TypeMessage type, Round round, Round acceptNum, List<Long> values,
			List<Long> idreqs, long delay) {
		long			src	= host.getID();
		PaxosMessage	m	= new PaxosMessage(src, -1, protocol_id, type, round, acceptNum, values, idreqs);

		for (int i = 0; i < Network.size(); i++) {
			Node dst = Network.get(i);
			send(host, dst, m, protocol_id, delay, true);
		}
	}

	private Node getNodeOfID(long idDest) {
		for (int i = 0; i < Network.size(); i++) {
			if (Network.get(i).getID() == idDest) {
				return Network.get(i);
			}
		}
		return null;
	}

	public long getRandomDelay() {
		return (range == 1 ? min : min + CommonState.r.nextLong(range));
	}

	public static boolean isSHOWLOG() {
		return SHOWLOG;
	}

	public boolean isInit_0() {
		return init_0;
	}

	public long getMin() {
		return min;
	}

	public long getRange() {
		return range;
	}

	public long getStep() {
		return step;
	}

	public List<Long> getQueueReq() {
		return queueRequest;
	}

	public Object clone() {
		MultiPaxosProtocol ap = null;
		try {
			ap = (MultiPaxosProtocol) super.clone();
			ap.ballotNum = new Round(0, 0);
			ap.acceptNum = new Round(0, 0);
			ap.acceptVal = -1L;
			ap.promiseByRound = new HashMap<>();
			ap.leader = -1L;
			ap.valueAccepted = new HashMap<>();
			ap.delayAfterRefuse = 0L;
			ap.didInit = false;
			ap.queueRequest = new LinkedList<>();
			ap.requestOk = new HashSet<Long>();

			ap.ballotNumP = new Round(0, 0);
			ap.acceptNumP = new Round(0, 0);
			ap.acceptValP = new ArrayList<>();
			ap.promiseByRoundP = new HashMap<>();
			ap.valueAcceptedP = new HashMap<>();
			ap.historique = new ArrayList<>();
			ap.historiqueLantency = new ArrayList<>();

		} catch (CloneNotSupportedException e) {
		} // never happens
		return ap;
	}

	public Round getCurrentRound() {
		return ballotNum;
	}

	public List<Long> getHistorique() {
		return historique;
	}

	public Round getAcceptRound() {
		return acceptNum;
	}

	public Long getAcceptVal() {
		return acceptVal;
	}

	public long getTOTAL_MESSAGE_ELEC() {
		return TOTAL_MESSAGE_ELEC;
	}

	public long getLeaderOf() {
		return leader;
	}

	public long getElectionTime() {
		return electionTime;
	}

	public Set<Long> getReqOk() {
		return requestOk;
	}

	public static float getProbaFauteTransitoire() {
		return probaFauteTransitoire;
	}

	public static float getProbaFauteFranche() {
		return probaFauteFranche;
	}

	public static long getNbMultiPaxosMessage() {
		return nbMessageMultiPaxos + TOTAL_MESSAGE_ELEC;
	}

	public List<Long> getHistoriqueLatency() {
		return historiqueLantency;
	}

	public int getBufferN() {
		return buffer_N;
	}

}