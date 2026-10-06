# Git

## Etape 0 : Gérer le proxy (uniquement sur les PC de l'ENSMM)
- Cliquez droit sur un dossier, allez sur Git TortoiseGit > Settings > Network
- Activez le Proxy en cliquant sur "Enable Proxy Server" et avec les paramètres suivants :
	__Server Adress :__ proxy-www.ens2m.fr __Port :__ 3128

## Etape 1 : Cloner le projet
### Sur les PC de l'ENSMM
- Clique droit sur un dossier où vous allez cloner votre dépot, puis "TortoiseGit" > Clone
- On vous demandera une URL, c'est celle sur le site de votre dépot sur GitHub, en cliquant sur le bouton vert "Code".
- Cliquez sur OK, on vous demandera peut-être de confirmer l'accès au Git par Tortoize, vous dites "oui" ;)
- Vous avez à présent le dépot chez vous !

### Sur votre PC personnel (sous Windows)
- Installez avant "Github Desktop" : https://desktop.github.com/ (super intuitif, je recommande !)
- Quand c'est fait et que GitDesktop est ouvert, cliquez en haut à gauche sur "File" > "Clone Repository"
- Il y a un onglet "URL", allez dessus, et donnez le lien URL du dépot Git + à quel endroit vous allez créer le dépot local.
- C'est tout ! :)

# Etape 2 : Mettre à jour le dépot
Quand vous créez/modifiez des fichiers, TortoiseGit ou GitDesktop sont au courant et savent ce que vous avez crée/modifié !
Ce qui vous restera à faire, sera de : Pull/Add/Commit/Push !

## Pull
Il permet de récupérer les fichiers (et donc les modifications de vos collègues) sur le dépot distant. Toujours à faire en premier si vous ne voulez pas de conflit !

## Add
A priori vous n'avez pas besoin de le faire, car comme je le disais, TortoiseGit ou GitDesktop sont au courant de vos modifications.

## Commit
Pour TortoiseGit : Clique droit dans votre dossier du projet puis "Commit -> main"
Pour GitDesktop : En bas à gauche !

- Il faut d'abord écrire un message de commit (un truc clair svp...), par exemple : "Ajout de la classe Truc". (Je vous conseille d'ajouter un tag "[MOT]", "[IHM]", "[SQL]" devant le message pour montrer que c'est une modif du moteur, de l'interface graphique ou de la base de donnée.)
- Cliquez sur "Commit" pour GitDesktop ou sur "OK" pour TortoiseGit !

## Push
Pour TortoiseGit : Quand vous faites un commit, il vous propose directement de push, sinon : clique droit > "Push"
Pour GitDesktop : En haut à droite !



