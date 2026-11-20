/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class VerDeTerre {
    private double x, y;
    private double largeur = 50;
    private final double hauteur = 60; // Changez uniquement cette valeur pour agrandir/réduire le verre
    private boolean actif = false;
    private BufferedImage sprite;
    private final Random rand = new Random();

    public VerDeTerre() {
        try {
            URL url = getClass().getResource("/ressources/verredeterre.png");
            if (url != null) {
                BufferedImage brute = ImageIO.read(url);
                if (brute != null) {
                    this.sprite = nettoyerEtRecadrer(brute);
                    // Calcule automatiquement la largeur exacte selon les vraies proportions du verre
                    this.largeur = Math.round(this.hauteur * (sprite.getWidth() / (double) sprite.getHeight()));
                }
            }
        } catch (Exception e) {
            System.err.println("[BrawlMuLa] Erreur chargement verdeterre.png : " + e.getMessage());
        }
    }

    /**
     * Rend le fond blanc transparent et coupe les marges vides autour du verre.
     */
    private static BufferedImage nettoyerEtRecadrer(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int gauche = w, droite = -1, haut = h, bas = -1;

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int argb = src.getRGB(px, py);
                int alpha = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // Supprime la transparence existante ou le fond blanc pur
                if (alpha < 20 || (r > 248 && g > 248 && b > 248)) {
                    img.setRGB(px, py, 0x00000000);
                } else {
                    img.setRGB(px, py, argb);
                    gauche = Math.min(gauche, px);
                    droite = Math.max(droite, px);
                    haut = Math.min(haut, py);
                    bas = Math.max(bas, py);
                }
            }
        }

        if (droite >= gauche && bas >= haut) {
            return img.getSubimage(gauche, haut, droite - gauche + 1, bas - haut + 1);
        }
        return img;
    }

    public void apparaitreAleatoirement(List<Plateforme> plateformes) {
        if (plateformes == null || plateformes.isEmpty()) return;

        List<Plateforme> actives = new ArrayList<>();
        for (Plateforme p : plateformes) {
            if (p.estActive()) {
                actives.add(p);
            }
        }

        if (!actives.isEmpty()) {
            Plateforme p = actives.get(rand.nextInt(actives.size()));
            double marge = Math.max(0, p.getLargeur() - largeur);
            this.x = p.getX() + (rand.nextDouble() * marge);
            this.y = p.getY() - hauteur;
            this.actif = true;
        }
    }

    public void synchroniser(boolean actif, double x, double y) {
        this.actif = actif;
        this.x = x;
        this.y = y;
    }

    public void ramasser() {
        this.actif = false;
    }

    public boolean isActif() { return actif; }
    public double getX() { return x; }
    public double getY() { return y; }

    public Rectangle2D getHitbox() {
        return new Rectangle2D.Double(x, y, largeur, hauteur);
    }

    public void dessiner(Graphics g) {
        if (!actif) return;
        if (sprite != null) {
            g.drawImage(sprite, (int) x, (int) y, (int) largeur, (int) hauteur, null);
        } else {
            g.setColor(new Color(235, 130, 150));
            g.fillRoundRect((int) x, (int) y, (int) largeur, (int) hauteur, 12, 12);
        }
    }
}