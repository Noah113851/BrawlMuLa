package jeudebase;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * Exemple de fenetre de jeu en utilisant uniquement des commandes
 *
 * @author guillaume.laurent
 */
public class FenetreDeJeu extends JFrame implements ActionListener {

    private BufferedImage framebuffer;
    private Graphics2D contexte;
    private JLabel jLabel1;
    private Jeu jeu;
    private Timer timer;
    
    //Les Listeners
    private EcouteurClavier keyL;

    public FenetreDeJeu() {
        // initialisation de la fenetre
        this.setSize(607, 380);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.jLabel1 = new JLabel();
        this.jLabel1.setPreferredSize(new java.awt.Dimension(607, 380));
        this.setContentPane(this.jLabel1);
        this.pack();

        // Creation du jeu
        this.jeu = new Jeu();
        
        //Ajout du listener ici
        this.keyL = new EcouteurClavier(jeu); //Of course on le met APRES avoir déclaré Jeu()....
        this.addKeyListener(this.keyL);

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
        //Les fameuses deux étapes importantes : MiseàJour et rendu qu'on trouve dans la classe Jeu.
        this.jeu.miseAJour();
        this.jeu.rendu(contexte);
        
        //Ca c'est juste pour appliquer le rendu()...
        this.jLabel1.repaint();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FenetreDeJeu fenetre = new FenetreDeJeu();
        fenetre.setVisible(true);
    }

}
