/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package brawlmula;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;



public class FenetreDeJeu extends JFrame implements ActionListener, KeyListener {

    private BufferedImage framebuffer;
    private Graphics2D contexte;
    private JLabel jLabel1;
    private Jeu jeu;
    private Timer timer;

    public FenetreDeJeu() {
        // initialisation de la fenetre
        this.setSize(960, 540);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addKeyListener(this);
        this.jLabel1 = new JLabel();
        this.jLabel1.setPreferredSize(new java.awt.Dimension(960, 540));
        this.setContentPane(this.jLabel1);
        this.pack();

        // Creation du jeu
        this.jeu = new Jeu();

        // Creation du buffer pour l'affichage du jeu et recuperation du contexte graphique
        this.framebuffer = new BufferedImage(this.jLabel1.getWidth(), this.jLabel1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.jLabel1.setIcon(new ImageIcon(framebuffer));
        this.contexte = this.framebuffer.createGraphics();

        // Creation du Timer qui appelle this.actionPerformed() tous les 40 ms
        this.timer = new Timer(40, this);
        this.timer.start();
    }

    // Methode appelee par le timer et qui effectue la boucle de jeu
    @Override
    public void actionPerformed(ActionEvent e) {
        this.jeu.miseAJour();
        this.jeu.rendu(contexte);
        this.jLabel1.repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // NOP
    }

    @Override
    public void keyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == evt.VK_D) {
            this.jeu.avatar.setDroite(true);
        }
        if (evt.getKeyCode() == evt.VK_Q) {
            this.jeu.avatar.setGauche(true);
        }
        if (evt.getKeyCode() == evt.VK_SPACE) {
            this.jeu.avatar.saute();
        }
        if (evt.getKeyCode() == evt.VK_S) {
            this.jeu.avatar.setBas(true);
        }
        
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        if (evt.getKeyCode() == evt.VK_D) {
            this.jeu.avatar.setDroite(false);
        }
        if (evt.getKeyCode() == evt.VK_Q) {
            this.jeu.avatar.setGauche(false);
        }
        if (evt.getKeyCode() == evt.VK_S) {
            this.jeu.avatar.setBas(false);
        }

    }

    public static void main(String[] args) {
        FenetreDeJeu fenetre = new FenetreDeJeu();
        fenetre.setVisible(true);
    }

}