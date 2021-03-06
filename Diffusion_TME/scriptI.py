#! bin/python3
# -*- coding: utf-8 -*-

import os
import subprocess
import matplotlib.pyplot as plt
import numpy as np
from numpy import genfromtxt
import matplotlib.ticker as plticker

absolutePath = os.path.dirname(os.path.realpath(__file__))
FILE_DATA = "data"

def writeInFile(seed, size, stepDelay, minDelay, maxDelay, init_0, endcontroler):
	fileOut = open("bin/configElection", "w", encoding='utf-8')
	out = "random.seed "+str(seed)+"\n\
network.size "+ str(size) +"\n\
simulation.endtime 500000\n\
\n\
init.i InitElection\n\
protocol.applicativeE MultiPaxosProtocol\n\
\n\
protocol.applicativeE.init_value_0 "+ str(init_0) +"\n\
protocol.applicativeE.minDelay "+ str(minDelay) +"\n\
protocol.applicativeE.maxDelay "+ str(maxDelay) +"\n\
protocol.applicativeE.stepDelay "+ str(stepDelay) +"\n\
protocol.applicativeE.showLog false\n\
protocol.applicativeE.probaFauteTransitoire 0\n\
protocol.applicativeE.probaFauteFranche 0\n\
protocol.applicativeE.bufferN 1 \n\
protocol.applicativeE.cyclicElection false\n\
\n\
control.endControler "+str(endcontroler)+"\n\
control.endControler.applicationE applicativeE\n\
control.endControler.at -1\n\
control.endControler.FINAL"
	fileOut.write(out)
	fileOut.close()

def test(minNode, maxNode, nbTests, stepDelay, minDelay, maxDelay, init_0, filename, endControler):	
	myfile = absolutePath+"/logs/"+filename
	if os.path.isfile(myfile):
    		os.remove(myfile)
	
	subprocess.call(absolutePath+"/javac.sh", shell=True)
	cmd = "java -cp bin/:lib/*: peersim.Simulator bin/configElection"

	for i in range(minNode, maxNode+1):
		for j in range(0,nbTests):
				writeInFile(j, i, stepDelay, minDelay, maxDelay, init_0, endControler)
				subprocess.call(cmd, shell=True)

def traceTestNodeParam(minNode, maxNode, nbTests, init_0, data, ylims=None):
	yMessages = []
	yRounds = []
	yTime = []

	tmpMessages = []
	tmpRounds = []
	tmpTime = []
	
	for i in range(minNode-1):
		yMessages.append([])
		yRounds.append([])
		yTime.append([])

	for line in data:
		if (np.isnan(line[0])):
			continue
		
		if line[6] != -1:
			tmpMessages.append(line[6])
			tmpRounds.append(line[7])
			tmpTime.append(line[8])
		
		if line[0] == nbTests-1:
			yMessages.append(tmpMessages)
			yRounds.append(tmpRounds)
			yTime.append(tmpTime)

			tmpMessages = []
			tmpRounds = []
			tmpTime = []

		
	
	fig, axs = plt.subplots(1, 3, figsize=(20, 10))
	
	strInit0 = ''
	if init_0:
		strInit0 = ' (init val = 0)'


	axs[0].boxplot(yMessages, patch_artist = True, showfliers=False)
	axs[0].set_title('Nombre de messages envoyés \nen fonction du nombre de noeuds'+strInit0)
	axs[0].set_xlabel('Nombre de noeuds')
	axs[0].set_ylabel('Nombre de messages')

	axs[1].boxplot(yRounds, patch_artist = True, showfliers=False)
	axs[1].set_title('Nombre de rounds avant décision \nen fonction du nombre de noeuds'+strInit0)
	axs[1].set_xlabel('Nombre de noeuds')
	axs[1].set_ylabel('Nombre de rounds')

	axs[2].boxplot(yTime, patch_artist = True, showfliers=False)
	axs[2].set_title('Temps nécessaire avant décision \nen fonction du nombre de noeuds'+strInit0)
	axs[2].set_xlabel('Nombre de noeuds')
	axs[2].set_ylabel('Temps (unité)')
	
	plt.tight_layout()
	

	if ylims != None:
		for i in range(3):
			axs[i].set_ylim([0, ylims[i][1]])
	else:
		for i in range(3):
			axs[i].set_ylim([0, axs[i].get_ylim()[1]])
	
	plt.savefig(absolutePath+"/logs/Graph_NodeParam"+strInit0)

	ylimsReturn = []
	for i in range(3):
		ylimsReturn.append(axs[i].get_ylim())
	return ylimsReturn

def traceTestBackoffParam(minNode, maxNode, nbTests, init_0, dataWithBackoff, dataWithoutBackoff, ylims=None):
	yMessages = []
	yRounds = []
	yTime = []

	tmpMessages = []
	tmpRounds = []
	tmpTime = []

	for i in range(minNode-1):
		yMessages.append(0)
		yRounds.append(0)
		yTime.append(0)

	for line in dataWithBackoff:
		if (np.isnan(line[0])):
			continue
		
		if line[6] != -1:
			tmpMessages.append(line[6])
			tmpRounds.append(line[7])
			tmpTime.append(line[8])
		
		if line[0] == nbTests-1:
			yMessages.append(np.mean(tmpMessages))
			yRounds.append(np.mean(tmpRounds))
			yTime.append(np.mean(tmpTime))

			tmpMessages = []
			tmpRounds = []
			tmpTime = []		
	
	yMessagesWithout = []
	yRoundsWithout = []
	yTimeWithout = []

	tmpMessages = []
	tmpRounds = []
	tmpTime = []

	for i in range(minNode-1):
		yMessagesWithout.append(0)
		yRoundsWithout.append(0)
		yTimeWithout.append(0)

	for line in dataWithoutBackoff:
		if (np.isnan(line[0])):
			continue
			
		if line[6] != -1:
			tmpMessages.append(line[6])
			tmpRounds.append(line[7])
			tmpTime.append(line[8])
			
		if line[0] == nbTests-1:
			yMessagesWithout.append(np.mean(tmpMessages))
			yRoundsWithout.append(np.mean(tmpRounds))
			yTimeWithout.append(np.mean(tmpTime))

			tmpMessages = []
			tmpRounds = []
			tmpTime = []
	
	x = np.arange(len(yMessages))

	fig, axs = plt.subplots(1, 3, figsize=(20, 10))
	
	strInit0 = ''
	if init_0:
		strInit0 = ' (init val = 0)'
	
	axs[0].plot(x, yMessages, label='with backoff')
	axs[0].plot(x, yMessagesWithout, label='without backoff')
	axs[0].legend()
	axs[0].set_title('Nombre de messages envoyés en moyenne \nen fonction du nombre de noeuds'+strInit0)
	axs[0].set_xlabel('Nombre de noeuds')
	axs[0].set_ylabel('Nombre de messages')

	axs[1].plot(x, yRounds, label='with backoff')
	axs[1].plot(x, yRoundsWithout, label='without backoff')
	axs[1].legend()
	axs[1].set_title('Nombre de rounds moyen avant décision \nen fonction du nombre de noeuds'+strInit0)
	axs[1].set_xlabel('Nombre de noeuds')
	axs[1].set_ylabel('Nombre de rounds')

	axs[2].plot(x, yTime, label='with backoff')
	axs[2].plot(x, yTimeWithout, label='without backoff')
	axs[2].legend()
	axs[2].set_title('Temps nécessaire en moyenne avant décision \nen fonction du nombre de noeuds'+strInit0)
	axs[2].set_xlabel('Nombre de noeuds')
	axs[2].set_ylabel('Temps (unité)')
	
	plt.tight_layout()
	
	if ylims != None:
		for i in range(3):
			axs[i].set_ylim([0, ylims[i][1]])
	else:
		for i in range(3):
			axs[i].set_ylim([0, axs[i].get_ylim()[1]])

	for i in range(3):
			axs[i].set_xlim([minNode, maxNode])
	
	
	plt.savefig(absolutePath+"/logs/Graph_BackoffParam"+strInit0)

	ylimsReturn = []
	for i in range(3):
		ylimsReturn.append(axs[i].get_ylim())
	return ylimsReturn

def testBackoffParam():
	minNode = 1
	maxNode = 30
	nbTests = 30
	stepDelay = 10

	test(minNode, maxNode, nbTests, 0, 5, 500, False, FILE_DATA, "EndElectionController")
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataWithoutBackoff = genfromtxt(myfile, delimiter=';')
	dataWithoutBackoff = np.delete(dataWithoutBackoff, (0), axis=0)
	

	test(minNode, maxNode, nbTests, stepDelay, 5, 500, False, FILE_DATA, "EndElectionController")
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataWithBackoff = genfromtxt(myfile, delimiter=';')
	dataWithBackoff = np.delete(dataWithBackoff, (0), axis=0)

	ylims = traceTestBackoffParam(minNode, maxNode, nbTests, False, dataWithBackoff, dataWithoutBackoff)
	ylims2 = traceTestNodeParam(minNode, maxNode, nbTests, False, dataWithoutBackoff)


	test(minNode, maxNode, nbTests, 0, 5, 500, True, FILE_DATA, "EndElectionController")
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataWithoutBackoff = genfromtxt(myfile, delimiter=';')
	dataWithoutBackoff = np.delete(dataWithoutBackoff, (0), axis=0)
	

	test(minNode, maxNode, nbTests, stepDelay, 5, 500, True, FILE_DATA, "EndElectionController")
	myfile = absolutePath+"/logs/"+FILE_DATA
	dataWithBackoff = genfromtxt(myfile, delimiter=';')
	dataWithBackoff = np.delete(dataWithBackoff, (0), axis=0)

	traceTestBackoffParam(minNode, maxNode, nbTests, True, dataWithBackoff, dataWithoutBackoff, ylims)
	traceTestNodeParam(minNode, maxNode, nbTests, True, dataWithoutBackoff, ylims2)



def traceTestBackoffParam2(node, nbTests, init_0, data, ylims=None):
	yMessages = []
	yRounds = []
	yTime = []

	tmpMessages = []
	tmpRounds = []
	tmpTime = []

	for dataByStep in data:
		
		for line in dataByStep:
			if (np.isnan(line[0])):
				continue
			if line[6] != -1:
				tmpMessages.append(line[6])
				tmpRounds.append(line[7])
				tmpTime.append(line[8])

		yMessages.append(np.mean(tmpMessages))
		yRounds.append(np.mean(tmpRounds))
		yTime.append(np.mean(tmpTime))

		tmpMessages = []
		tmpRounds = []
		tmpTime = []
	
	x = np.arange(len(yMessages))

	fig, axs = plt.subplots(1, 3, figsize=(20, 10))
	
	strInit0 = ''
	if init_0:
		strInit0 = ' (init val = 0)'
	
	axs[0].plot(x, yMessages)
	axs[0].set_title('Nombre de messages envoyés en moyenne \nen fonction du backoff sur '+str(node)+' noeuds'+strInit0)
	axs[0].set_xlabel('Backoff')
	axs[0].set_ylabel('Nombre de messages')

	axs[1].plot(x, yRounds)
	axs[1].set_title('Nombre de rounds moyen nécessaire avant décision \nen fonction du backoff sur '+str(node)+' noeuds'+strInit0)
	axs[1].set_xlabel('Backoff')
	axs[1].set_ylabel('Nombre de rounds')

	axs[2].plot(x, yTime)
	axs[2].set_title('Temps nécessaire en moyenne avant décision \nen fonction du backoff sur '+str(node)+' noeuds'+strInit0)
	axs[2].set_xlabel('Backoff')
	axs[2].set_ylabel('Temps (unité)')
	
	plt.tight_layout()

	if ylims != None:
		for i in range(3):
			axs[i].set_ylim([0, ylims[i][1]])
	else:
		for i in range(3):
			axs[i].set_ylim([0, axs[i].get_ylim()[1]])


	plt.savefig(absolutePath+"/logs/Graph_BackoffParam2"+strInit0)

	ylimsReturn = []
	for i in range(3):
		ylimsReturn.append(axs[i].get_ylim())
	return ylimsReturn

def testBackoffParam2():
	node = 30
	nbTests = 10
	stepDelay = 40

	data = []
	for i in range(stepDelay):
		test(node, node, nbTests, i, 5, 500, False, FILE_DATA, "EndElectionController")
		myfile = absolutePath+"/logs/"+FILE_DATA
		tmp = genfromtxt(myfile, delimiter=';')
		tmp = np.delete(tmp, (0), axis=0)
		data.append(tmp)
	
	ylims = traceTestBackoffParam2(node, nbTests, False, data)

	data = []
	for i in range(stepDelay):
		test(node, node, nbTests, i, 5, 500, True, FILE_DATA, "EndElectionController")
		myfile = absolutePath+"/logs/"+FILE_DATA
		tmp = genfromtxt(myfile, delimiter=';')
		tmp = np.delete(tmp, (0), axis=0)
		data.append(tmp)
	
	traceTestBackoffParam2(node, nbTests, True, data, ylims)


def main():
	font = {'family' : 'DejaVu Sans',
	        'weight' : 'bold',
	        'size'   : 13}
	plt.rc('font', **font)
	
	testBackoffParam()
	testBackoffParam2()

main()
