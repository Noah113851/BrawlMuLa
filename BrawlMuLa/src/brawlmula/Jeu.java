package brawlmula;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;


public class Jeu {

    private BufferedImage fond;
    public Avatar avatar;

    public Jeu() {
        try {
            this.fond = ImageIO.read(getClass().getClassLoader().getResource("brawlmula/INTERFACE/acceuil.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(Jeu.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.avatar = new Avatar();
    }

    public void miseAJour() {
        this.avatar.miseAJour();
    }

    public void rendu(Graphics2D contexte) {
        contexte.drawImage(this.fond, 0, 0, 960, 540, null);
        this.avatar.rendu(contexte);
    }

}


//package brawlmula;
//
//import java.awt.Color;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import javax.imageio.ImageIO;
//
//public class Jeu {
//
//    public Avatar avatar;
//    private BufferedImage fond;
//
//    public Jeu(String nomPerso, String cheminCarte) {
//        try {
//            fond = ImageIO.read(getClass().getResource(cheminCarte));
//        } catch (Exception ex) {
//            System.err.println("Fond introuvable : " + cheminCarte);
//        }
//        avatar = new Avatar(nomPerso, 100);
//    }
//
//    public void miseAJour() {
//        avatar.miseAJour();
//    }
//
//    public void rendu(Graphics2D g) {
//        if (fond != null) {
//            g.drawImage(fond, 0, 0, 960, 540, null);
//        } else {
//            g.setColor(Color.BLACK);
//            g.fillRect(0, 0, 960, 540);
//        }
//        avatar.rendu(g);
//    }
//}