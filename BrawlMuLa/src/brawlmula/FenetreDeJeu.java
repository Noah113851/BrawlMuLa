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

/** Fenetre de test du Chateau : meme architecture que la fenetre d'origine. */
public class FenetreDeJeu extends JFrame implements ActionListener, KeyListener {

    private BufferedImage framebuffer;
    private Graphics2D contexte;
    private JLabel jLabel1;
    private Jeu jeu;
    private Timer timer;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Combat.class.getName());
    private String joueur1;
    private String joueur2;
    private String joueur3;
    private String joueur4;
    private String carte;

    public FenetreDeJeu() {
        // Fenetre de test : meme repere que les plateformes du Chateau.
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addKeyListener(this);
        this.jLabel1 = new JLabel();
        this.jLabel1.setPreferredSize(new java.awt.Dimension(1280, 760));
        this.setContentPane(this.jLabel1);
        this.pack();
        this.setLocationRelativeTo(null);

        // Jeu garde l'avatar existant ; le decor et les plateformes sont
        // maintenant ceux du Chateau (voir Jeu.java).
        this.jeu = new Jeu();

        // Affichage original par framebuffer : inchange.
        this.framebuffer = new BufferedImage(this.jLabel1.getWidth(),
                this.jLabel1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.jLabel1.setIcon(new ImageIcon(framebuffer));
        this.contexte = this.framebuffer.createGraphics();

        // Une seule boucle : 40 ms pour le personnage ET les plateformes.
        this.timer = new Timer(40, this);
        this.timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.jeu.miseAJour();
        this.jeu.rendu(contexte);
        this.jLabel1.repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) { /* NOP */ }

    @Override
    public void keyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_D) {
            this.jeu.avatar.setDroite(true);
        }
        if (evt.getKeyCode() == KeyEvent.VK_Q) {
            this.jeu.avatar.setGauche(true);
        }
        if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
            this.jeu.avatar.saute();
        }
        if (evt.getKeyCode() == KeyEvent.VK_S) {
            this.jeu.avatar.setBas(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_D) {
            this.jeu.avatar.setDroite(false);
        }
        if (evt.getKeyCode() == KeyEvent.VK_Q) {
            this.jeu.avatar.setGauche(false);
        }
        if (evt.getKeyCode() == KeyEvent.VK_S) {
            this.jeu.avatar.setBas(false);
        }
    }

    @Override
    public void dispose() {
        if (timer != null) timer.stop();
        if (contexte != null) contexte.dispose();
        super.dispose();
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            FenetreDeJeu fenetre = new FenetreDeJeu();
            fenetre.setVisible(true);
        });
    }
}

