/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
//package brawlmula;
//
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.imageio.ImageIO;

//public class Avatar {
//
//    private BufferedImage sprite;
//    protected double x, y;
//    private boolean gauche, droite, haut, bas;
//    private boolean regardeGauche = false;
//
//    private double vy;                 // vitesse verticale
//    private boolean auSol;             // vrai si l'avatar touche le sol
//    private static final double SOL = 420;       // hauteur du sol (y de départ)
//    private static final double GRAVITE = 10;   // accélération vers le bas par tick    
//    private static final double VITESSE_SAUT = 70; // force du saut
//
//    public Avatar() {
//        try {
//            this.sprite = ImageIO.read(getClass().getClassLoader().getResource("ressources/avatars/carrot_fight.png"));
//        } catch (IOException ex) {
//            Logger.getLogger(Avatar.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        this.x = 100;
//        this.y = SOL;
//        this.vy = 0;
//        this.auSol = true;
//        this.gauche = false;
//        this.droite = false;
//        this.bas = false;
//    }
//
//    public void setGauche(boolean gauche) {
//        this.gauche = gauche;
//    }
//
//    public void setDroite(boolean droite) {
//        this.droite = droite;
//    }
//
//    public void setBas(boolean bas) {
//        this.bas = bas;
//    }
//
//    // Déclenche le saut (seulement si l'avatar est au sol)
//    public void saute() {
//        if (this.auSol) {
//            this.vy = -VITESSE_SAUT;
//            this.auSol = false;
//        }
//    }
//
//    public void miseAJour() {
//        // déplacement horizontal (inchangé)
//        if (this.gauche) {
//            x -= 5;
//            regardeGauche = true;
//        }
//        if (this.droite) {
//            x += 5;
//            regardeGauche = false;
//        }
//        if (this.gauche) {
//            x -= 5;
//        }
//        if (this.droite) {
//            x += 5;
//        }
//        if (x > 960 - 100) {
//            x = 960 - 100;
//        }
//        if (x < 0) {
//            x = 0;
//        }
//// chute rapide : on annule la montée et on plonge
//        if (this.bas && !this.auSol) {
//            if (vy < 0) {
//                vy = 0;      // on stoppe la montée
//            }
//            vy += 3;         // on accélère la descente
//        }
//        // déplacement vertical
//        if (!this.auSol) {
//            y += vy;          // on applique la vitesse
//            vy += GRAVITE;    // la gravité ralentit la montée, puis accélère la chute
//            if (y >= SOL) {   // retour au sol
//                y = SOL;
//                vy = 0;
//                this.auSol = true;
//            }
//        }
//
//        // déplacement vers le bas
//        if (this.bas) {
//            if (!this.auSol) {
//                vy += 2;            // chute rapide (fast fall)
//            }
//        }
//    }
//
//    public void rendu(Graphics2D contexte) {
//        if (regardeGauche) {
//            // image retournée : on dessine de droite à gauche
//            contexte.drawImage(this.sprite, (int) x + 100, (int) y, -100, 100, null);
//        } else {
//            contexte.drawImage(this.sprite, (int) x, (int) y, 100, 100, null);
//        }
//    }
//
//}




package brawlmula;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class Avatar {

    private BufferedImage sprite;
    protected double x, y;
    private boolean gauche, droite, bas;
    private boolean regardeGauche = false;

    private double vy;                              // vitesse verticale
    private boolean auSol;                          // vrai si l'avatar touche le sol
    private static final double SOL = 420;          // hauteur du sol
    private static final double GRAVITE = 10;       // accélération vers le bas par tick
    private static final double VITESSE_SAUT = 70;  // force du saut
    private static final double VITESSE = 5;        // déplacement horizontal par tick

    private final String nom;
    private final int pvMax;
    private int pv;
    private final int degats;

     //nomPerso = "Carrot", "Nikku", "Tomato" ou "Potato" (colonne nom de la table)
    public Avatar(String nomPerso, double xDepart) {
        this.nom = nomPerso;

         //valeurs par défaut si la base ne répond pas
        String recherche = nomPerso.toLowerCase().replace("_fight", "").trim();
        String fichier = recherche + "_fight.png";
        int pvBase = 100;
        int degatsBase = 10;

        String url = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
        try (Connection c = DriverManager.getConnection(url, "etudiant", "TON_MOT_DE_PASSE");
             PreparedStatement r = c.prepareStatement(
                     "SELECT avatars_image, pv, degats FROM Avatars WHERE LOWER(nom) = ?")) {
            r.setString(1, recherche);
            try (ResultSet rs = r.executeQuery()) {
                if (rs.next()) {
                    fichier = rs.getString("avatars_image");
                    pvBase = rs.getInt("pv");
                    degatsBase = rs.getInt("degats");
                } else {
                    System.err.println("Personnage introuvable en base : " + nomPerso);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Erreur SQL : " + ex.getMessage());
        }

        this.pvMax = pvBase;
        this.pv = pvBase;
        this.degats = degatsBase;

        try {
            this.sprite = ImageIO.read(getClass().getClassLoader()
                    .getResource("ressources/avatars/" + fichier));
        } catch (Exception ex) {
            Logger.getLogger(Avatar.class.getName()).log(Level.SEVERE,
                    "Image introuvable : " + fichier, ex);
        }

        this.x = xDepart;
        this.y = SOL;
        this.vy = 0;
        this.auSol = true;
    }

     //----- Commandes clavier -----
    public void setGauche(boolean gauche) { this.gauche = gauche; }
    public void setDroite(boolean droite) { this.droite = droite; }
    public void setBas(boolean bas)       { this.bas = bas; }

    public void saute() {
        if (this.auSol) {
            this.vy = -VITESSE_SAUT;
            this.auSol = false;
        }
    }

     //----- Données du personnage -----
    public String getNom()   { return nom; }
    public int getPv()       { return pv; }
    public int getPvMax()    { return pvMax; }
    public int getDegats()   { return degats; }
    public double getX()     { return x; }
    public double getY()     { return y; }

    public void perdrePv(int montant) {
        pv = Math.max(0, pv - montant);
    }

    public boolean estMort() {
        return pv <= 0;
    }

     //----- Boucle de jeu -----
    public void miseAJour() {
        if (gauche) {
            x -= VITESSE;
            regardeGauche = true;
        }
        if (droite) {
            x += VITESSE;
            regardeGauche = false;
        }
        if (x > 960 - 100) x = 960 - 100;
        if (x < 0) x = 0;

        if (!auSol) {
            if (bas) {
                if (vy < 0) vy = 0;   // on coupe la montée
                vy += 3;              // chute rapide
            }
            y += vy;
            vy += GRAVITE;
            if (y >= SOL) {
                y = SOL;
                vy = 0;
                auSol = true;
            }
        }
    }

    public void rendu(Graphics2D contexte) {
        if (sprite != null) {
            if (regardeGauche) {
                contexte.drawImage(sprite, (int) x + 100, (int) y, -100, 100, null);
            } else {
                contexte.drawImage(sprite, (int) x, (int) y, 100, 100, null);
            }
        }

         //barre de vie au-dessus de l'avatar
        int largeur = 100;
        int ratio = (int) (largeur * ((double) pv / pvMax));
        contexte.setColor(Color.DARK_GRAY);
        contexte.fillRect((int) x, (int) y - 12, largeur, 6);
        contexte.setColor(Color.GREEN);
        contexte.fillRect((int) x, (int) y - 12, ratio, 6);
    }
}
