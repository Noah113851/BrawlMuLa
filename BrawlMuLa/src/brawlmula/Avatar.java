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
public class Avatar {
    private BufferedImage sprite;
    protected double w, x, y, z;
    private boolean bas, gauche, haut, droite ;

    public Avatar() {
        try {
            this.sprite = ImageIO.read(getClass().getClassLoader().getResource("resources/carotte_basique_avec_poings.png"));
        } catch (IOException ex) {
            Logger.getLogger(Avatar.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.w = 100;
        this.x = 100;
        this.y = 150;
        this.z = 100;
        this.bas = false;
        this.gauche = false;
        this.haut = false;
        this.droite = false;
    }
    
    public void setBas(boolean bas) {
        this.bas = bas;    
    }
    
    public void setGauche(boolean gauche) {
        this.gauche = gauche;
    }
    
    public void setHaut(boolean haut) {
        this.haut = haut;

}
    public void setDroite(boolean droite) {
        this.droite = droite;
    }

    public void miseAJour() {
        if (this.bas) {
            w -= 5;
        }
        if (this.gauche) {
            x -= 5;
        }
        if (this.haut) {
            y -= 5;
        }
        if (this.droite) {
            x += 5;
        }
        if (x > 607-52) {
            x = 607-52;
        }
        if (x < 0) {
            x = 0;
        }
    }

    public void rendu(Graphics2D contexte) {
        contexte.drawImage(this.sprite, (int) x, (int) y, null);
    }

}
