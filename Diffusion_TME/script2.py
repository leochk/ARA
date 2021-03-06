#! bin/python3
# -*- coding: utf-8 -*-

import os
import subprocess
import matplotlib.pyplot as plt
import numpy as np
from numpy import genfromtxt
import matplotlib.ticker as plticker

absolutePath = os.path.dirname(os.path.realpath(__file__))
FILE_DATA = "data2"

def writeInFile(seed, size, probaFauteTransitoire, probaFauteFranche):
	fileOut = open("bin/configMultiPaxos", "w", encoding='utf-8')
	out = "random.seed "+str(seed)+"\n\
network.size "+ str(size) +"\n\
simulation.endtime 50000\n\
\n\
init.i InitElectionAndMultiPaxos\n\
protocol.applicativeE MultiPaxosProtocol\n\
\n\
protocol.applicativeE.init_value_0 true\n\
protocol.applicativeE.minDelay 5\n\
protocol.applicativeE.maxDelay 500\n\
protocol.applicativeE.stepDelay 10\n\
protocol.applicativeE.showLog false\n\
protocol.applicativeE.probaFauteTransitoire "+str(probaFauteTransitoire)+"\n\
protocol.applicativeE.probaFauteFranche "+ str(probaFauteFranche)+"\n\
protocol.applicativeE.bufferN 1 \n\
protocol.applicativeE.cyclicElection false\n\
\n\
control.endControler EndMultiPaxosController\n\
control.endControler.applicationE applicativeE\n\
control.endControler.at -1\n\
control.endControler.FINAL"
	fileOut.write(out)
	fileOut.close()

def testFF(minNode, maxNode, nbTests, maxProbaFFIteration, stepProba, filename):	
	myfile = absolutePath+"/logs/"+filename
	if os.path.isfile(myfile):
		os.remove(myfile)
	
	subprocess.call(absolutePath+"/javac.sh", shell=True)
	cmd = "java -cp bin/:lib/*: peersim.Simulator bin/configMultiPaxos"

	probaFF = 0
	for j in range (0, maxProbaFFIteration):
		for i in range(minNode, maxNode+1):
			for k in range(0,nbTests):
				writeInFile(k, i, 0, probaFF)
				subprocess.call(cmd, shell=True)
		probaFF = probaFF + stepProba

def testFFParam():
	minNode = 2
	maxNode = 30
	nbTests = 30
	maxProbaFFIteration = 3
	stepProba = 0.00005

	testFF(minNode, maxNode, nbTests, maxProbaFFIteration, stepProba, FILE_DATA)
	myfile = absolutePath+"/logs/"+FILE_DATA
	data = genfromtxt(myfile, delimiter=';')
	
	traceTestFF(minNode, maxNode, nbTests, maxProbaFFIteration, stepProba, data)

def traceTestFF(minNode, maxNode, nbTests, maxProbaFFIteration, stepProba, data, ylims=None):
	
	yMessages = {}
	yDebit = {}
	yLatency = {}

	tmpMessages = []
	tmpDebit = []
	tmpLatency = []

	key = ''
	for line in data:
		if (np.isnan(line[0])):
			continue
		
		if (line[2] != -1):
			tmpMessages.append(line[2])
			tmpDebit.append(line[3])
			tmpLatency.append(line[4])
		
		
		if line[0] == nbTests-1:
				
			if line[5] not in yMessages:
				yMessages[line[5]] = []
			
			if line[5] not in yDebit:
				yDebit[line[5]] = []
			
			if line[5] not in yLatency:
				yLatency[line[5]] = []
						
			yMessages[line[5]].append(np.mean(tmpMessages))
			yDebit[(line[5])].append(np.mean(tmpDebit))
			yLatency[(line[5])].append(np.mean(tmpLatency))

			tmpMessages = []
			tmpDebit = []
			tmpLatency = []
			
			key = line[5]	
		
	fig, axs = plt.subplots(1, 3, figsize=(20, 10))
	x = np.arange(len(yMessages[key]))
	
		
	for k, v in yMessages.items():
		axs[0].plot(x, np.array(v), label="proba FF = " +str(k))
	axs[0].legend()
	axs[0].set_title('Nombre de messages envoyés \nen fonction du nombre de noeuds')
	axs[0].set_xlabel('Nombre de noeuds')
	axs[0].set_ylabel('Nombre de messages')

	for k, v in yDebit.items():
		axs[1].plot(x, np.array(v), label="proba FF = " +str(k))
	axs[1].legend()
	axs[1].set_title('Débit de requête traitées \nen fonction du nombre de noeuds')
	axs[1].set_xlabel('Nombre de noeuds')
	axs[1].set_ylabel('Débit (nombre total de requêtes hors élection validées / temps total)')

	for k, v in yLatency.items():
		axs[2].plot(x, np.array(v), label="proba FF = " +str(k))
	axs[2].legend()
	axs[2].set_title('Latence des requêtes avant décision \nen fonction du nombre de noeuds')
	axs[2].set_xlabel('Nombre de noeuds')
	axs[2].set_ylabel('Latence (unité)')

	plt.tight_layout()

	if ylims != None:
		for i in range(3):
			axs[i].set_ylim([0, ylims[i][1]])
	
	plt.savefig(absolutePath+"/logs/MP_Graph_NodeParam")

	ylimsReturn = []
	for i in range(3):
		ylimsReturn.append(axs[i].get_ylim())
	return ylimsReturn

def main():
	font = {'family' : 'DejaVu Sans',
	        'weight' : 'bold',
	        'size'   : 16}

	plt.rc('font', **font)
	
	testFFParam()

main()