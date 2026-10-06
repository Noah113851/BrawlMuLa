# AIDE DU PROF
Ca c'est cadeau ! ;) Je vais mettre ici des bouts de code pour vous aider, surtout pour la partie IHM ! (Je sais ô combien cette partie vous fait peur...) Je rajouterai peut-être au fur et à mesure selon la demande.

__En cas de pépin__ : manal.benaissa@femto-st.fr (mettez en titre le tag `[PROJET-VS1]` pour que se soit visible pour moi.) 

## GitHub
Il se peut que vous ne pouvez pas push/pull sur votre dépot git, il y a deux choses à vérifier :
1. Votre proxy est bien configuré (si vous êtes à l'ensmm, sinon oubliez cette partie !) : http://proxy-www.ens2m.fr:3128/
2. Que vous essayez de vous connecter, non pas avec le mot de passe de votre compte Git mais avec le token qui vous a été donné.

__Rappel :__
Pour envoyer dans le dépot distant (GitHub ici) votre travail : 
1. `git pull` : Permet de récupérer ce qui a été fait dans le dépot pendant que vous travaillez.
2. `git add [vos fichiers]` : Ajoute vos fichiers dans le futur commit (carton de déménagement ;)) avant l'envoi dans le dépot distant.
3. `git commit + message du commit` : Vous fermez le carton et lui donnez un message écrit dessus avant de l'envoyer.
4. `git push` : Vous envoyez le carton !

## Gestion du projet

Ca c'est ce que je vous ai montré durant le TP, comment je vois plus ou moins votre travail (mais prenez pas mes mots comme parole d'Evangile hein ! Ca dépendra lourdement de votre organisation interne !) :

![Alt text](https://zupimages.net/up/22/45/mhtd.png "Gestion du projet")

A mon avis, vous devriez passer avant tout du temps sur le moteur ! Pour un premier sprint, je vois bien une interface graphique très rudimentaire (avec des carrés/cercles de couleur...) mais un moteur capable de gérer un joueur + un terrain (même pas d'obstacles, monstres etc... Full Basic !)


## IHM : EventListener
C'est ici que j'ai mis comment "écouter" votre __souris__ et votre __clavier__. Lisez bien les commentaires, tout est dans le code ! Je vous conseille pour vous approprier le truc, de faire l'exercice suivant :

1. Créez une petite fenêtre avec un cercle de couleur dedans : 
![Alt text](https://zupimages.net/up/22/45/sntl.png "Comment créer la fenêtre.")

2. Si vous appuyez sur les touches directionnels (haut, bas, gauche, droite), cela bouge le cercle. (attention à ce que le cercle ne sorte pas du cadre !)
3. Si vous cliquez sur un point de la fenêtre, cela "téléporte" le cercle à cet endroit.
4. Si vous appuyez sur les touches suivantes, cela change la couleur du cercle : "b" pour bleu, "r" pour rouge, "g" pour vert. 

## SQL : Requêtes
Ne gérez pas ça tout de suite, focus sur le moteur avant ! Mais Essayez d'avoir 1 ou deux camarades qui se spécialisent en SQL, notamment en se formant un chouilla sur ce site : https://sql.sh/ (vous devez pas devenir un bac+5 en SQL hein, juste savoir faire un `select`, `update`... Les commandes de base !)

