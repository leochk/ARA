
# Simulation Multi-Paxos sur Peersim

Requis pour lancer le projet :
 - Java 8+
 - Peersim
 - Python 3
 - Matplotlib 3.3.3
 -  NumPy 1.19.4

Pour lancer les tests, utilisez les commandes suivantes :
```$ python3 scriptI.py```  (Étude expérimentale 1 : itérations d’élection) 
```$ python3 scriptII.py```  (Étude expérimentale 2 : Multi-Paxos séquentiel)
```$ python3 scriptIII.py```  (Étude expérimentale 3 : Réduire le coût en messages)

Les courbes seront sauvegardés dans le répertoire ```logs/```. Étant donné le temps d'exécution particulièrement long, nous y avons laissé nos propres courbes.

Vous pouvez lancer vos propres simulation en éditant le fichier ```src/config``` : 
 
 - ```init_value_0 <boolean>``` : valeur initiale proposée égale à 0 si ```true```, valeur initiale proposée égale à l'identifiant du nœud sinon.
 - ```showLog <boolean>``` : affiche sur le terminal les messages d'exécution pour le débuggage si ```true```.
 - ```probaFauteTransitoire <float>``` : probabilité d'avoir une faute transitoire à l'envoi d'un message (non fonctionnel)
 -   ```probaFauteFranche <float>``` : probabilité d'avoir une faute franche après l'envoi d'un message
 -  ```bufferN <int>``` : taille du buffer des messages dans le Multi-Paxos (ie. nombre de requête contenu dans les messages)
  - ```cyclicElection <boolean>``` : à la détection d'une panne du leader, réélection cyclique si ```true```. Sinon, réélection par protocole de Paxos

Puis, compiler et lancer le projet :
```$ ./javac.sh && ./java_ex.sh```


