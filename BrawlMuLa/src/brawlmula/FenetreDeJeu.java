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
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Fenetre de test + fenetre de combat lancee depuis Selection.
 * Conserve exactement la boucle Jeu.miseAJour()/Jeu.rendu(), le framebuffer,
 * les touches Q/D/ESPACE/S et la cadence de 40 ms.
 * Le choix de la carte est transmis UNE SEULE FOIS a Jeu(String).
 */
public class FenetreDeJeu extends JFrame implements ActionListener, KeyListener {

    private final BufferedImage framebuffer;
    private final Graphics2D contexte;
    private final JLabel jLabel1;
    private final Jeu jeu;
    private final Timer timer;

    /** Lancement historique de la fenetre de test Chateau. */
    public FenetreDeJeu() {
        this("/ressources/Maps/chateau.png");
    }

    /** Lancement direct sur une carte pour le test ou un autre ecran. */
    public FenetreDeJeu(String carteChoisie) {
        setTitle("BrawlMuLa - " + carteChoisie.substring(carteChoisie.lastIndexOf('/') + 1));
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addKeyListener(this);

        jLabel1 = new JLabel();
        jLabel1.setPreferredSize(new java.awt.Dimension(
                ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR));
        setContentPane(jLabel1);
        pack();
        setLocationRelativeTo(null);

        // IMPORTANT : ne pas creer un PanneauCombat ici, ni appeler new Jeu()
        // sans argument. Jeu recupere la carte choisie et le MEME gestionnaire
        // de collisions que le decor / les plateformes affiches.
        Combat jeu = new Combat(carteChoisie);

        framebuffer = new BufferedImage(jLabel1.getWidth(),
                jLabel1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        jLabel1.setIcon(new ImageIcon(framebuffer));
        contexte = framebuffer.createGraphics();

        timer = new Timer(40, this);
        timer.setCoalesce(true);
        timer.start();
    }

    /** Appel utilise par Selection.java (noms des 4 joueurs + carte). */
    public FenetreDeJeu(String j1, String j2, String j3, String j4,
                        String carteChoisie) {
        this(carteChoisie);
        System.out.println("Match lance : " + j1 + " VS " + j2
                + " | Carte : " + carteChoisie);
        // Ce test affiche toujours 1 Avatar : le multijoueur est un module distinct.
    }

    @Override
    public void actionPerformed(ActionEvent evenement) {
        jeu.miseAJour();
        jeu.rendu(contexte);
        jLabel1.repaint();
    }

    @Override
    public void keyTyped(KeyEvent evt) { }

    @Override
    public void keyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_D) jeu.avatar.setDroite(true);
        if (evt.getKeyCode() == KeyEvent.VK_Q) jeu.avatar.setGauche(true);
        if (evt.getKeyCode() == KeyEvent.VK_SPACE) jeu.avatar.saute();
        if (evt.getKeyCode() == KeyEvent.VK_S) jeu.avatar.setBas(true);
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_D) jeu.avatar.setDroite(false);
        if (evt.getKeyCode() == KeyEvent.VK_Q) jeu.avatar.setGauche(false);
        if (evt.getKeyCode() == KeyEvent.VK_S) jeu.avatar.setBas(false);
    }

    @Override
    public void dispose() {
        if (timer != null) timer.stop();
        if (contexte != null) contexte.dispose();
        super.dispose();
    }

    /** Run File : garde le test Chateau habituel. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FenetreDeJeu().setVisible(true));
    }
}