/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author dcragnaz
 */
//public class Avatar {
//    private BufferedImage sprite;
//    protected double w, x, y, z;
//    private boolean bas, gauche, haut, droite ;
//
//    public Avatar() {
//        try {
//            this.sprite = ImageIO.read(getClass().getClassLoader().getResource("resources/carotte_basique_avec_poings.png"));
//        } catch (IOException ex) {
//            Logger.getLogger(Avatar.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        this.w = 100;
//        this.x = 100;
//        this.y = 150;
//        this.z = 100;
//        this.bas = false;
//        this.gauche = false;
//        this.haut = false;
//        this.droite = false;
//    }
//    
//    public void setBas(boolean bas) {
//        this.bas = bas;    
//    }
//    
//    public void setGauche(boolean gauche) {
//        this.gauche = gauche;
//    }
//    
//    public void setHaut(boolean haut) {
//        this.haut = haut;
//
//}
//    public void setDroite(boolean droite) {
//        this.droite = droite;
//    }
//
//    public void miseAJour() {
//        if (this.bas) {
//            w -= 5;
//        }
//        if (this.gauche) {
//            x -= 5;
//        }
//        if (this.haut) {
//            y -= 5;
//        }
//        if (this.droite) {
//            x += 5;
//        }
//        if (x > 607-52) {
//            x = 607-52;
//        }
//        if (x < 0) {
//            x = 0;
//        }
//    }
//
//    public void rendu(Graphics2D contexte) {
//        contexte.drawImage(this.sprite, (int) x, (int) y, null);
//    }
//
//}
public class Avatar {

    private BufferedImage sprite;
    protected double x, y;
    private boolean gauche, droite, haut, bas;
    private boolean regardeGauche = false;

    private double vy;                 // vitesse verticale
    private boolean auSol;             // vrai si l'avatar touche le sol
    private static final double SOL = 420;       // hauteur du sol (y de départ)
    private static final double GRAVITE = 10;   // accélération vers le bas par tick    
    private static final double VITESSE_SAUT = 50; // force du saut

    public Avatar() {
        try {
            this.sprite = ImageIO.read(getClass().getClassLoader().getResource("ressources/avatars/carrot_fight.png"));
        } catch (IOException ex) {
            Logger.getLogger(Avatar.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.x = 100;
        this.y = SOL;
        this.vy = 0;
        this.auSol = true;
        this.gauche = false;
        this.droite = false;
        this.haut = false ;
        this.bas = false ;
    }

    public void setGauche(boolean gauche) {
        this.gauche = gauche;
    }

    public void setDroite(boolean droite) {
        this.droite = droite;
    }
    

    // Déclenche le saut (seulement si l'avatar est au sol)
    public void saute() {
        if (this.auSol) {
            this.vy = -VITESSE_SAUT;
            this.auSol = false;
        }
    }

    public void miseAJour() {
        // déplacement horizontal (inchangé)
        if (this.gauche) {
            x -= 5;
            regardeGauche = true;
        }
        if (this.droite) {
            x += 5;
            regardeGauche = false;
        }
        if (this.gauche) {
            x -= 5;
        }
        if (this.droite) {
            x += 5;
        }
        if (x > 960 - 100) {
            x = 960 - 100;
        }
        if (x < 0) {
            x = 0;
        }

        // déplacement vertical
        if (!this.auSol) {
            y += vy;          // on applique la vitesse
            vy += GRAVITE;    // la gravité ralentit la montée, puis accélère la chute
            if (y >= SOL) {   // retour au sol
                y = SOL;
                vy = 0;
                this.auSol = true;
            }
        }
    }

    public void rendu(Graphics2D contexte) {
        if (regardeGauche) {
    // image retournée : on dessine de droite à gauche
    contexte.drawImage(this.sprite, (int) x + 100, (int) y, -100, 100, null);
        } 
        else {
    contexte.drawImage(this.sprite, (int) x, (int) y, 100, 100, null);
        }
    }

}