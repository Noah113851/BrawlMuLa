/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public final class BarreVieHUD {

    private static final Color[] COULEURS_EQUIPE = {
        new Color(60, 120, 255), new Color(255, 70, 70)
    };
    private static final Color[] COULEURS_JOUEUR = {
        new Color(60, 120, 255), new Color(255, 70, 70),
        new Color(70, 200, 90),  new Color(240, 200, 40)
    };

    private final JoueurSQL sql;
    private final ModeDeJeu mode;
    private final int[] pv;      // PV actuels (copie en mémoire)
    private final int[] pvMax;   // PV max
    private long derniereLecture = 0;

    public BarreVieHUD(JoueurSQL sql, ModeDeJeu mode) {
        this.sql = sql;
        this.mode = mode;
        this.pv = new int[4];
        this.pvMax = new int[4];
        for (int i = 0; i < mode.getNbJoueurs(); i++) {
            pvMax[i] = sql.lirePvMax(i + 1);
            pv[i] = pvMax[i];
        }
    }

    /** Relit les PV dans la base, au plus 2 fois par seconde. */
    private void actualiser() {
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereLecture < 500) return;
        derniereLecture = maintenant;
        for (int i = 0; i < mode.getNbJoueurs(); i++) {
            pv[i] = sql.lirePv(i + 1);
        }
    }

    public void dessiner(Graphics2D g) {
    actualiser();
    int n = mode.getNbJoueurs();
    int largeurBarre = 260, hauteurBarre = 22, marge = 30;
    int ecartVertical = 50;     // espace entre deux barres empilées (2v2)
    int ecartHorizontal = 30;   // espace entre deux barres côte à côte (1v1v1)
    int yHaut = 40;

    g.setFont(new Font("SansSerif", Font.BOLD, 14));

    for (int i = 0; i < n; i++) {
        int x, y;

        if (mode == ModeDeJeu.TRIO) {
            // 3 barres centrées, à la suite
            int total = 3 * largeurBarre + 2 * ecartHorizontal;
            int debut = (1280 - total) / 2;
            x = debut + i * (largeurBarre + ecartHorizontal);
            y = yHaut;
        } else if (mode == ModeDeJeu.DEUX_CONTRE_DEUX) {
            // équipe 0 à gauche, équipe 1 à droite, empilées
            int rang = (i < 2) ? i : i - 2;
            x = (i < 2) ? marge : 1280 - marge - largeurBarre;
            y = yHaut + rang * ecartVertical;
        } else {
            // DUEL : J1 à gauche, J2 à droite
            x = (i == 0) ? marge : 1280 - marge - largeurBarre;
            y = yHaut;
        }

        double ratio = pvMax[i] > 0 ? Math.max(0, (double) pv[i] / pvMax[i]) : 0;

        Color cadre = mode.parEquipes()
                ? COULEURS_EQUIPE[mode.getEquipe(i)]
                : COULEURS_JOUEUR[i];

        g.setColor(Color.BLACK);
        g.fillRect(x - 3, y - 3, largeurBarre + 6, hauteurBarre + 6);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y, largeurBarre, hauteurBarre);
        g.setColor(ratio > 0.5 ? Color.GREEN : ratio > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x, y, (int) (largeurBarre * ratio), hauteurBarre);
        g.setColor(cadre);
        g.drawRect(x - 3, y - 3, largeurBarre + 6, hauteurBarre + 6);
        g.setColor(Color.WHITE);
        g.drawString("J" + (i + 1) + "  " + pv[i] + "/" + pvMax[i], x, y - 7);
    }
}
}