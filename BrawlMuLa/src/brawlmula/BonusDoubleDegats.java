package brawlmula;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Piment magique a ramasser : le prochain coup qui inflige des degats
 * est double. Un seul exemplaire est actif a la fois, sur une plateforme.
 * Aucun PNG supplementaire n'est necessaire.
 */
public final class BonusDoubleDegats {
    private static final double LARGEUR = 52;
    private static final double HAUTEUR = 60;
    private final Random hasard = new Random();
    private Plateforme support;
    private double decalageX;
    private double x;
    private double y;
    private boolean actif;

    public void apparaitreAleatoirement(List<Plateforme> plateformes) {
        if (plateformes == null) return;
        List<Plateforme> supports = new ArrayList<>();
        for (Plateforme p : plateformes) {
            // Eviter les plateformes intermittentes qui peuvent disparaitre.
            if (p.estActive() && !p.estIntermittente()
                    && p.getLargeur() >= LARGEUR + 20) {
                supports.add(p);
            }
        }
        if (supports.isEmpty()) return;
        support = supports.get(hasard.nextInt(supports.size()));
        double marge = Math.max(0, support.getLargeur() - LARGEUR - 20);
        decalageX = 10 + hasard.nextDouble() * marge;
        x = support.getX() + decalageX;
        y = support.getY() - HAUTEUR;
        actif = true;
    }

    /** Recoit la position choisie par l'hote sur les autres PC. */
    public void synchroniser(double nouveauX, double nouveauY,
                             List<Plateforme> plateformes) {
        x = nouveauX;
        y = nouveauY;
        actif = true;
        support = null;
        if (plateformes != null) {
            for (Plateforme p : plateformes) {
                if (p.estActive() && !p.estIntermittente()
                        && x + LARGEUR / 2 >= p.getX() - 10
                        && x + LARGEUR / 2 <= p.getX() + p.getLargeur() + 10
                        && Math.abs(p.getY() - (y + HAUTEUR)) <= 28) {
                    support = p;
                    decalageX = x - p.getX();
                    break;
                }
            }
        }
    }

    public void miseAJour() {
        if (!actif || support == null) return;
        if (!support.estActive()) {
            ramasser();
            return;
        }
        x = support.getX() + decalageX;
        y = support.getY() - HAUTEUR;
    }

    public void ramasser() {
        actif = false;
        support = null;
    }

    /** Calcul de degats independant du rendu et du reseau (testable). */
    public static int calculerDegats(int degatsBase, boolean avecBonus) {
        long degats = (long) Math.max(0, degatsBase) * (avecBonus ? 2L : 1L);
        return (int) Math.min(Integer.MAX_VALUE, degats);
    }

    public boolean isActif() { return actif; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Rectangle2D getHitbox() {
        return new Rectangle2D.Double(x, y, LARGEUR, HAUTEUR);
    }

    /** Dessin vectoriel : piment rouge dore, halo anime et badge x2. */
    public void dessiner(Graphics source) {
        if (!actif) return;
        Graphics2D g = (Graphics2D) source.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            long temps = System.nanoTime();
            double pulsation = 0.5 + 0.5 * Math.sin(temps / 340_000_000.0);
            int cx = (int) Math.round(x + LARGEUR / 2);
            int cy = (int) Math.round(y + HAUTEUR / 2);
            int rayon = 29 + (int) Math.round(pulsation * 3);
            g.setColor(new Color(255, 143, 20, 40 + (int) (pulsation * 48)));
            g.fill(new Ellipse2D.Double(cx - rayon, cy - rayon,
                                        rayon * 2, rayon * 2));

            Graphics2D p = (Graphics2D) g.create();
            try {
                p.translate(cx, cy + 2);
                p.rotate(-0.32);
                // Piment incurve stylise, pointe en bas.
                Path2D.Double corps = new Path2D.Double();
                corps.moveTo(-13, -19);
                corps.curveTo(8, -26, 23, -4, 16, 12);
                corps.curveTo(10, 28, -1, 30, -20, 25);
                corps.curveTo(-5, 21, -4, 7, -14, -5);
                corps.curveTo(-19, -11, -17, -17, -13, -19);
                corps.closePath();
                p.setColor(new Color(119, 26, 15));
                p.setStroke(new BasicStroke(2.5f));
                p.fill(corps);
                p.setColor(new Color(246, 50, 33));
                p.fill(corps);
                p.setColor(new Color(124, 32, 18));
                p.draw(corps);
                p.setColor(new Color(255, 155, 80, 160));
                p.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND,
                                            BasicStroke.JOIN_ROUND));
                p.drawArc(-11, -15, 16, 25, 40, 120);
                Path2D.Double tige = new Path2D.Double();
                tige.moveTo(-9, -17);
                tige.curveTo(-15, -23, -14, -31, -6, -36);
                tige.curveTo(-5, -28, 1, -26, 4, -24);
                tige.curveTo(1, -19, -3, -18, -9, -17);
                tige.closePath();
                p.setColor(new Color(74, 165, 61));
                p.fill(tige);
                p.setColor(new Color(25, 86, 33));
                p.setStroke(new BasicStroke(2f));
                p.draw(tige);
            } finally {
                p.dispose();
            }
            // Inscription lisible sans ressource de police externe.
            g.setColor(new Color(56, 18, 4, 215));
            g.fillRoundRect(cx - 19, cy + 16, 38, 19, 8, 8);
            g.setColor(new Color(255, 218, 91));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("x2", cx - 10, cy + 31);
        } finally {
            g.dispose();
        }
    }
}
