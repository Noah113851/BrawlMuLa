package EcouteursCorrection;

import javax.swing.JFrame;

/**
 *
 * @author manal BENAISSA
 */
public class TestEcouteurs {

    public static void main(String[] args) {
        
        
        //Ok, testons un peu nos Listeners ! Là on créer juste une bête fenêtre !
        JFrame fenetre = new JFrame("Ma fenêtre");
        fenetre.setVisible(true);
        fenetre.setSize(400, 200); //Vous pouvez régler les dimensions de la fenêtre ici.
        fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Ca c'est ce qui nous intéresse ! Là, on dit à la fenêtre "fenetre", "ok, maintenant écoute BIEN le clavier et la souris ! Et si tu détectes un "event", tu exécutes les méthodes dans les deux autres classes !"
        fenetre.addKeyListener(new EcouteurClavier());
        fenetre.addMouseListener(new EcouteurSouris());
    }

}
