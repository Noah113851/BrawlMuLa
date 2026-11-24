package brawlmula;

import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class FenetreDeJeu extends JFrame implements ActionListener, KeyListener {

    private final BufferedImage framebuffer;
    private final Graphics2D contexte;
    private final JLabel jLabel1;
    private final Jeu jeu;
    private final Timer timer;

    public FenetreDeJeu() {
        this("Carrot", "Nikku", "Tomato", "Potato", "/ressources/Maps/chateau.png", null, 1);
    }

    public FenetreDeJeu(String carteChoisie) {
        this("Carrot", "Nikku", "Tomato", "Potato", carteChoisie, null, 1);
    }

    public FenetreDeJeu(String j1, String j2, String j3, String j4,
                        String carteChoisie, Reseau reseau, int monNumeroJoueur) {
        setTitle("BrawlMuLa - " + carteChoisie.substring(carteChoisie.lastIndexOf('/') + 1));
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        jLabel1 = new JLabel();
        jLabel1.setPreferredSize(new Dimension(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR));
        setContentPane(jLabel1);
        pack();
        setLocationRelativeTo(null);

        jeu = new Jeu(j1, j2, j3, j4, carteChoisie, reseau, monNumeroJoueur);
        addKeyListener(this);
        setFocusable(true);

        jLabel1.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                FenetreDeJeu.this.requestFocusInWindow();
                if (SwingUtilities.isLeftMouseButton(evt)) {
                    jeu.attaquer();
                } else if (javax.swing.SwingUtilities.isRightMouseButton(evt)) {
                    // Attaque lourde avec le clic droit
                    jeu.attaquerLourd();
                }
            }
        });

        framebuffer = new BufferedImage(jLabel1.getWidth(), jLabel1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        jLabel1.setIcon(new ImageIcon(framebuffer));
        contexte = framebuffer.createGraphics();

        timer = new Timer(40, this);
        timer.setCoalesce(true);
        timer.start();
        Musiques_sons.jouerMusiqueCarte(carteChoisie);
    }

    @Override
    public void actionPerformed(ActionEvent evenement) {
        if (evenement.getSource() == timer) {
            jeu.miseAJour();
            jeu.rendu(contexte);
            jLabel1.repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent evt) {
        // Aucun traitement pour les caracteres saisis.
    }

    @Override
    public void keyPressed(KeyEvent evt) {
        jeu.gererToucheAppuyee(evt.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        jeu.gererToucheRelachee(evt.getKeyCode());
    }

    @Override
    public void dispose() {
        Musiques_sons.stop();
        if (timer != null) timer.stop();
        if (jeu != null) jeu.fermer();
        if (contexte != null) contexte.dispose();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FenetreDeJeu().setVisible(true));
    }
}

