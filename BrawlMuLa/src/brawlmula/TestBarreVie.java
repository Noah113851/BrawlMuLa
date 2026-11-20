/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class TestBarreVie {

    public static void main(String[] args) {
        // Change le mode ici : DUEL, TRIO ou DEUX_CONTRE_DEUX
        final ModeDeJeu mode = ModeDeJeu.TRIO;

        final JoueurSQL sql = new JoueurSQL();
        for (int i = 1; i <= mode.getNbJoueurs(); i++) {
            sql.initialiserPv(i);   // remet les PV au maximum
        }
        final BarreVieHUD hud = new BarreVieHUD(sql, mode);

        JPanel panneau = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(Color.BLACK);
                hud.dessiner((Graphics2D) g);
            }
        };
        panneau.setPreferredSize(new Dimension(1280, 200));

        JFrame fenetre = new JFrame("Test barres de vie - touches 1 a 4 = coup de 40");
        fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenetre.setContentPane(panneau);
        fenetre.pack();
        fenetre.setLocationRelativeTo(null);

        fenetre.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int joueur = e.getKeyCode() - KeyEvent.VK_0;   // touche 1 -> joueur 1
                if (joueur >= 1 && joueur <= mode.getNbJoueurs()) {
                    sql.retirerPv(joueur, 40);
                }
            }
        });

        new Timer(100, e -> panneau.repaint()).start();
        fenetre.setVisible(true);
    }
}