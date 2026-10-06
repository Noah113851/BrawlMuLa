package jeudebase;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * C'est la classe Jeu mais coté INTERFACE GRAPHIQUE ! L'idée c'est que dans les attributs, il y aura aussi le "jeu" mais coté Moteur, que vous allez mettre ausi à jour.
 *
 * @author Manal BENAISSA
 */
public class Jeu {

    private BufferedImage fond;
    public Avatar avatar;

    public Jeu() {
        try {
            this.fond = ImageIO.read(getClass().getClassLoader().getResource("resources/fond.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(Jeu.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.avatar = new Avatar();
    }

    /**
    * Cette méthode est importante, c'est cool si vous respectez d'ailleurs ce format avec une méthode MiseàJour() et une méthode rendu(). C'est MiseàJour() qui va mettre à jour le jeu coté moteur et déplacer votre joueur coté IG.
    */
    public void miseAJour() {
        this.avatar.miseAJour();
    }

    /**
    * Maitnenant que vous avez changé du coté "console" votre jeu (avec MiseàJour()) bah... il faut l'afficher. C'est ce que fait rendu()
    */
    public void rendu(Graphics2D contexte) {
        contexte.drawImage(this.fond, 0, 0, null);
        this.avatar.rendu(contexte);
    }

}
