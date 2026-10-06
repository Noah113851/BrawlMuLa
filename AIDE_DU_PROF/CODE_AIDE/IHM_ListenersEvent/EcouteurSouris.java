package EcouteursCorrection;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 *
 * @author manal BENAISSA
 */
public class EcouteurSouris implements MouseListener {

    @Override
    //MouseClicked est la méthode que vous devez compléter si vous avez une action à faire à chaque détection de clic !
    public void mouseClicked(MouseEvent event){
        //"event" vous donne des infos sur là où vous avez cliqué ! Ici, je peux choper les coordonnées X,Y (en pixel...) de là où j'ai cliqué ! :)
        System.out.println("La souris a cliqué aux coordonnées (" + event.getX() + ", " + event.getY() +")"); 
        System.out.flush();
        
        //Ici du coup, rien interdit que si vos cordonnés se trouvent dans la partie gauche de la fenêtre, vous faites un truc... Et si c'est la partie droite, vous faites un autre truc ! Essayez ! ;)
        // TODO HERE
    }

    @Override
    //Le clic peut être vu comme la décomposition de deux mouvements : vous pressez, puis pvous relachez. Si vous avez besoin de faire cette distinction, complétez ici !
    public void mousePressed(MouseEvent event) {
        System.out.println("Vous avez pressé aux coordonnées : " + event.getPoint().toString()); //Vous pouvez récupérer les coordonnés sous forme de Point aussii, (plutôt que X puis Y...)
        System.out.flush();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        System.out.println("Vous avez cliqué " + event.getClickCount() + " fois !"); //On peut même récupérer le nombre de clic !
        System.out.flush();
    }
    
    @Override
    //Vous occupez pas de ces deux là, vous en avez pas besoin ;)
    public void mouseEntered(MouseEvent event) {
        //NOTHING TODO HERE
    }
    
    @Override
    public void mouseExited(MouseEvent event) {
        //NOTHING TODO HERE
    }
}
