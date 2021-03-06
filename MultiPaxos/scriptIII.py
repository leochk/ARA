#! bin/python3
# -*- coding: utf-8 -*-

import os
import subprocess
import matplotlib.pyplot as plt
import numpy as np
from numpy import genfromtxt
import matplotlib.ticker as plticker

absolutePath = os.path.dirname(os.path.realpath(__file__))
FILE_DATA = "data3"

def writeInFile(seed, size, probaFauteTransitoire, probaFauteFranche, bufferSize, cyclicElection):
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
protocol.applicativeE.bufferN "+ str(bufferSize) +"\n\
protocol.applicativeE.cyclicElection "+ str(cyclicElection) +"\n\
\n\
control.endControler EndMultiPaxosBufferedController\n\
control.endControler.applicationE applicativeE\n\
control.endControler.at -1\n\
control.endControler.FINAL"
	fileOut.write(out)
	fileOut.close()

def test(minNode, maxNode, nbTests, minBufferPower, maxBufferPower, probaFF, cyclicElection, filename):	
	myfile = absolutePath+"/logs/"+filename
	if os.path.isfile(myfile):
		os.remove(myfile)
	
	subprocess.call(absolutePath+"/javac.sh", shell=True)
	cmd = "java -cp bin/:lib/*: peersim.Simulator bin/configMultiPaxos"

	for x in (2**p for p in range(minBufferPower, maxBufferPower)):
		for i in range(minNode, maxNode+1):
			for k in range(0,nbTests):
				writeInFile(k, i, 0, probaFF, x, cyclicElection)
				subprocess.call(cmd, shell=True)

def testCyclic():
	minNode = 2
	maxNode = 30
	nbTests = 30
	minBufferPower = 0
	maxBufferPower = 1
	probaFF = 0.0001

	test(minNode, maxNode, nbTests, minBufferPower, maxBufferPower, probaFF, True, FILE_DATA)
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataCyclic = genfromtxt(myfile, delimiter=';')

	test(minNode, maxNode, nbTests, minBufferPower, maxBufferPower, probaFF, False, FILE_DATA)
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataNonCyclic = genfromtxt(myfile, delimiter=';')

	traceTestCyclic(minNode, maxNode, nbTests, probaFF, dataCyclic, dataNonCyclic)

	probaFF = 0.00005

	test(minNode, maxNode, nbTests, minBufferPower, maxBufferPower, probaFF, True, FILE_DATA)
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataCyclic = genfromtxt(myfile, delimiter=';')

	test(minNode, maxNode, nbTests, minBufferPower, maxBufferPower, probaFF, False, FILE_DATA)
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataNonCyclic = genfromtxt(myfile, delimiter=';')

	traceTestCyclic(minNode, maxNode, nbTests, probaFF, dataCyclic, dataNonCyclic)

def traceTestCyclic(minNode, maxNode, nbTests, probaFF, dataCyclic, dataNonCyclic, ylims=None):

	yMessages = []
	yDebits = []
	yLatency = []

	tmpMessages = []
	tmpDebit = []
	tmpLatency = []

	for i in range(minNode-1):
		yMessages.append(0)
		yDebits.append(0)
		yLatency.append(0)

	for line in dataCyclic:
		if (np.isnan(line[0])):
			continue
		
		if line[2] != -1:
			tmpMessages.append(line[2])
			tmpDebit.append(line[3])
			tmpLatency.append(line[4])
		
		if line[0] == nbTests-1:
			yMessages.append(np.mean(tmpMessages))
			yDebits.append(np.mean(tmpDebit))
			yLatency.append(np.mean(tmpLatency))

			tmpMessages = []
			tmpDebit = []
			tmpLatency = []		
	
	yMessagesWithout = []
	yDebitsWithout = []
	yLatencyWithout = []

	tmpMessages = []
	tmpDebit = []
	tmpLatency = []

	for i in range(minNode-1):
		yMessagesWithout.append(0)
		yDebitsWithout.append(0)
		yLatencyWithout.append(0)

	for line in dataNonCyclic:
		if (np.isnan(line[0])):
			continue
			
		if line[2] != -1:
			tmpMessages.append(line[2])
			tmpDebit.append(line[3])
			tmpLatency.append(line[4])
			
		if line[0] == nbTests-1:
			yMessagesWithout.append(np.mean(tmpMessages))
			yDebitsWithout.append(np.mean(tmpDebit))
			yLatencyWithout.append(np.mean(tmpLatency))

			tmpMessages = []
			tmpDebit = []
			tmpLatency = []
	
	x = np.arange(len(yMessages))

	fig, axs = plt.subplots(1, 3, figsize=(20, 10))
	
	axs[0].plot(x, yMessages, label='cyclic election')
	axs[0].plot(x, yMessagesWithout, label='paxos election')
	axs[0].legend()
	axs[0].set_title('Nombre de messages envoyés \nen fonction du nombre de noeuds \n(probaFF = '+ str(probaFF) + ')')
	axs[0].set_xlabel('Nombre de noeuds')
	axs[0].set_ylabel('Nombre de messages')

	axs[1].plot(x, yDebits, label='cyclic election')
	axs[1].plot(x, yDebitsWithout, label='paxos election')
	axs[1].legend()
	axs[1].set_title('Débit de requête traitées \nen fonction du nombre de noeuds \n(probaFF = '+ str(probaFF) + ')')
	axs[1].set_xlabel('Nombre de noeuds')
	axs[1].set_ylabel('Débit (nombre total de requêtes hors élection validées / temps total)')


	axs[2].plot(x, yLatency, label='cyclic election')
	axs[2].plot(x, yLatencyWithout, label='paxos election')
	axs[2].legend()
	axs[2].set_title('Latence des requêtes avant décision \nen fonction du nombre de noeuds \n(probaFF = '+ str(probaFF) + ')')
	axs[2].set_xlabel('Nombre de noeuds')
	axs[2].set_ylabel('Latence (unité)')

	plt.tight_layout()
	
	if ylims != None:
		for i in range(3):
			axs[i].set_ylim([0, ylims[i][1]])

	for i in range(3):
			axs[i].set_xlim([minNode, maxNode])
	
	plt.savefig(absolutePath+"/logs/Graph_CyclicElec_"+str(probaFF).replace('.', '_'))

	ylimsReturn = []
	for i in range(3):
		ylimsReturn.append(axs[i].get_ylim())
	return ylimsReturn


def testBuffer():
	minNode = 2
	maxNode = 30
	nbTests = 30
	minBufferPower = 0
	maxBufferPower = 3
	probaFF = 0

	test(minNode, maxNode, nbTests, minBufferPower, maxBufferPower, probaFF, False, FILE_DATA)
	myfile = absolutePath+"/logs/"+FILE_DATA
	data = genfromtxt(myfile, delimiter=';')
	
	traceTest(minNode, maxNode, nbTests, maxBufferPower, probaFF, False, data)
	"""
	probaFF = 0.00005

	test(minNode, maxNode, nbTests, minBufferPower, maxBufferPower, probaFF, False, FILE_DATA)
	myfile = absolutePath+"/logs/"+FILE_DATA
	data = genfromtxt(myfile, delimiter=';')
	
	traceTest(minNode, maxNode, nbTests, maxBufferPower, probaFF, False, data)
	"""

def traceTest(minNode, maxNode, nbTests, maxBuffer, probaFF, cyclicElection, data, ylims=None):
	
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
				
			if line[7] not in yMessages:
				yMessages[line[7]] = []
			
			if line[7] not in yDebit:
				yDebit[line[7]] = []
			
			if line[7] not in yLatency:
				yLatency[line[7]] = []
			
			yMessages[line[7]].append(np.mean(tmpMessages))
			yDebit[(line[7])].append(np.mean(tmpDebit))
			yLatency[(line[7])].append(np.mean(tmpLatency))

			tmpMessages = []
			tmpDebit = []
			tmpLatency = []
			
			key = line[7]

	fig, axs = plt.subplots(1, 3, figsize=(20, 10))
	x = np.arange(len(yMessages[key]))

	for k, v in yMessages.items():
		axs[0].plot(x, np.array(v), label="buffer size = " +str(k))
	axs[0].legend()
	axs[0].set_title('Nombre de messages envoyés \nen fonction du nombre de noeuds \n(probaFF = '+ str(probaFF) + ')')
	axs[0].set_xlabel('Nombre de noeuds')
	axs[0].set_ylabel('Nombre de messages')

	for k, v in yDebit.items():
		axs[1].plot(x, np.array(v), label="buffer size = " +str(k))
	axs[1].legend()
	axs[1].set_title('Débit de requête traitées \nen fonction du nombre de noeuds \n(probaFF = '+ str(probaFF) + ')')
	axs[1].set_xlabel('Nombre de noeuds')
	axs[1].set_ylabel('Débit (nombre total de requêtes hors élection validées / temps total)')

	for k, v in yLatency.items():
		axs[2].plot(x, np.array(v), label="buffer size = " +str(k))
	axs[2].legend()
	axs[2].set_title('Latence des requêtes avant décision \nen fonction du nombre de noeuds \n(probaFF = '+ str(probaFF) + ')')
	axs[2].set_xlabel('Nombre de noeuds')
	axs[2].set_ylabel('Latence (unité)')

	plt.tight_layout()

	if ylims != None:
		for i in range(3):
			axs[i].set_ylim([0, ylims[i][1]])

	plt.savefig(absolutePath+"/logs/Graph_Buffered_"+str(probaFF).replace('.', '_'))

	ylimsReturn = []
	for i in range(3):
		ylimsReturn.append(axs[i].get_ylim())
	return ylimsReturn

def main():
	font = {'family' : 'DejaVu Sans',
	        'weight' : 'bold',
	        'size'   : 16}

	plt.rc('font', **font)
	
	testBuffer()
	#testCyclic()

main()
