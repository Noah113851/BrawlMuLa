/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.List;
import java.util.Random;
import javax.swing.ImageIcon;

public class VerDeTerre {
    private int x, y;
    private final int largeur = 40;
    private final int hauteur = 25;
    private boolean actif = false;
    private Image sprite;
    private final Random rand = new Random();

    public VerDeTerre() {
        // Ajoutez une image ver_de_terre.png dans BrawlMuLa/src/ressources/
        java.net.URL url = getClass().getResource("/ressources/ver_de_terre.png");
        if (url != null) {
            this.sprite = new ImageIcon(url).getImage();
        }
    }

    // Appelé uniquement par l'hôte (Joueur 1 / Serveur)
    public void apparaitreAleatoirement(List<Plateforme> plateformes) {
        if (plateformes != null && !plateformes.isEmpty()) {
            Plateforme p = plateformes.get(rand.nextInt(plateformes.size()));
            // Utilisez les getters de votre classe Plateforme.java
            int zoneSpawn = (int) Math.max(1, p.getLargeur() - largeur);
            this.x = (int) (p.getX() + rand.nextInt(zoneSpawn));
            this.y = (int) (p.getY() - hauteur);
            this.actif = true;
        }
    }

    // Appelé par le client (Joueur 2) lors de la réception réseau
    public void synchroniser(boolean actif, int x, int y) {
        this.actif = actif;
        this.x = x;
        this.y = y;
    }

    public void ramasser() {
        this.actif = false;
    }

    public boolean isActif() { return actif; }
    public int getX() { return x; }
    public int getY() { return y; }

    public Rectangle getHitbox() {
        return new Rectangle(x, y, largeur, hauteur);
    }

    public void dessiner(Graphics g) {
        if (!actif) return;
        if (sprite != null) {
            g.drawImage(sprite, x, y, largeur, hauteur, null);
        } else {
            // Rectangle rose de secours si l'image n'est pas encore ajoutée
            g.setColor(new Color(235, 130, 150));
            g.fillRoundRect(x, y, largeur, hauteur, 12, 12);
        }
     }
}
