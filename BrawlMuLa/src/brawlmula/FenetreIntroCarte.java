package brawlmula;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.text.Normalizer;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Introduction cinematographique 2D d'une carte BrawlMuLa.
 * Premiere version : test autonome sur la carte Chateau.
 * Aucune modification de Jeu, Avatar ou Selection n'est necessaire.
 */
public final class FenetreIntroCarte extends JFrame {

    private static final int LARGEUR = 1280;
    private static final int HAUTEUR = 760;
    private static final long DUREE_NS = 4_000_000_000L; // 4 secondes

    private final PanneauIntroCarte panneau;
    private final Runnable apresIntroduction;
    private boolean terminee = false;

    /**
     * Test simple : apres l'intro, ouvre la carte demandee normalement.
     */
    public FenetreIntroCarte(String cheminCarte, String nomCarte) {
        this(cheminCarte, nomCarte,
                () -> new FenetreDeJeu(cheminCarte).setVisible(true));
    }

    /**
     * Permettra plus tard de lancer le vrai match multijoueur depuis Selection.
     * La fonction apresIntroduction ne sera executee qu'une seule fois.
     */
    public FenetreIntroCarte(String cheminCarte, String nomCarte,
                            Runnable apresIntroduction) {
        if (apresIntroduction == null) {
            throw new IllegalArgumentException("Action de fin manquante");
        }
        this.apresIntroduction = apresIntroduction;
        this.panneau = new PanneauIntroCarte(cheminCarte, nomCarte);

        setTitle("BrawlMuLa - Introduction : " + nomCarte);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setContentPane(panneau);
        pack();
        setLocationRelativeTo(null);

        // Entree ou Espace : passer l'introduction.
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "passerIntro");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "passerIntro");
        getRootPane().getActionMap().put("passerIntro", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                terminerEtLancerCombat();
            }
        });

        // Le chronometre demarre lorsque la fenetre est vraiment visible.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                panneau.demarrer();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                panneau.arreter();
            }
        });
    }

    private void terminerEtLancerCombat() {
        if (terminee) {
            return;
        }
        terminee = true;
        panneau.arreter();
        dispose();
        // L'intro se termine AVANT la creation du Jeu : aucune physique
        // ou synchronisation reseau ne tourne pendant la presentation.
        apresIntroduction.run();
    }

    private static double limiter(double valeur) {
        return Math.max(0.0, Math.min(1.0, valeur));
    }

    private static double douceur(double valeur) {
        double t = limiter(valeur);
        return t * t * (3.0 - 2.0 * t);
    }

    private final class PanneauIntroCarte extends JPanel {

        private final BufferedImage decor;
        private final String nom;
        private final boolean introductionMarche;
        private final Timer animation;
        private long debutNs;

        PanneauIntroCarte(String cheminCarte, String nomCarte) {
            if (nomCarte == null || nomCarte.trim().isEmpty()) {
                throw new IllegalArgumentException("Nom de carte manquant");
            }
            this.nom = nomCarte.toUpperCase(Locale.FRENCH);
            this.introductionMarche = cheminCarte.replace('\\', '/')
                    .toLowerCase(Locale.ROOT).endsWith("/marche.png");
            this.decor = chargerDecor(cheminCarte);
            setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
            setBackground(Color.BLACK);
            setDoubleBuffered(true);

            animation = new Timer(16, e -> {
                if (debutNs == 0L) {
                    return;
                }
                if (System.nanoTime() - debutNs >= DUREE_NS) {
                    terminerEtLancerCombat();
                } else {
                    repaint();
                }
            });
            animation.setCoalesce(true);
        }

        private BufferedImage chargerDecor(String cheminCarte) {
            URL url = getClass().getResource(cheminCarte);
            if (url == null) {
                throw new IllegalStateException(
                        "Decor introuvable : " + cheminCarte);
            }
            try {
                BufferedImage resultat = ImageIO.read(url);
                if (resultat == null) {
                    throw new IOException("Image non reconnue");
                }
                return resultat;
            } catch (IOException ex) {
                throw new IllegalStateException(
                        "Impossible de lire le decor : " + cheminCarte, ex);
            }
        }

        void demarrer() {
            if (debutNs != 0L) {
                return;
            }
            debutNs = System.nanoTime();
            animation.start();
            repaint();
        }

        void arreter() {
            animation.stop();
        }

        @Override
        protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = (Graphics2D) source.create();
            try {
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }

                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                double t = debutNs == 0L ? 0.0
                        : limiter((System.nanoTime() - debutNs)
                                / (double) DUREE_NS);
                double mouvement = douceur(t / 0.91);

                // 1) Camera : chaque carte a sa propre trajectoire.
                // Le Chateau reste EXACTEMENT comme dans la version validee.
                // Le Marche part d'un plan serre en bas a gauche, puis la
                // camera glisse en diagonale vers le centre en dezoomant.
                // Le ratio ORIGINAL du decor est toujours respecte.
                double echelleCouverture = Math.max(
                        w / (double) decor.getWidth(),
                        h / (double) decor.getHeight());
                double zoom;
                double fractionX;
                double fractionY;
                if (introductionMarche) {
                    zoom = 1.36 - 0.36 * mouvement;
                    fractionX = 0.20 + 0.30 * mouvement;
                    fractionY = 0.73 - 0.23 * mouvement;
                } else {
                    zoom = 1.30 - 0.30 * mouvement;
                    fractionX = 0.69 - 0.19 * mouvement;
                    fractionY = 0.34 + 0.16 * mouvement;
                }
                double largeurImage = decor.getWidth() * echelleCouverture * zoom;
                double hauteurImage = decor.getHeight() * echelleCouverture * zoom;

                int imageX = (int) Math.round(-(largeurImage - w) * fractionX);
                int imageY = (int) Math.round(-(hauteurImage - h) * fractionY);

                g.drawImage(decor, imageX, imageY,
                        (int) Math.ceil(largeurImage),
                        (int) Math.ceil(hauteurImage), null);

                // 2) Legere zone sombre en bas pour la lisibilite du titre.
                g.setPaint(new GradientPaint(
                        0, h * 0.46f, new Color(6, 10, 25, 0),
                        0, h, new Color(5, 9, 20, 200)));
                g.fillRect(0, 0, w, h);

                // 3) Bandeaux cinematographiques.
                int bande = Math.max(25, (int) Math.round(h * 0.048));
                g.setColor(new Color(4, 7, 14));
                g.fillRect(0, 0, w, bande);
                g.fillRect(0, h - bande, w, bande);

                // 4) Titre seul : lettres pixelisees, sans texte au-dessus.
                double apparition = douceur((t - 0.31) / 0.15);
                double disparition = 1.0 - douceur((t - 0.86) / 0.09);
                float opacite = (float) limiter(apparition * disparition);
                if (opacite > 0.0f) {
                    g.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, opacite));
                    int gauche = (int) Math.round(w * 0.075);
                    // Pixel de 9 px : rendu 100 % carre, sans lissage.
                    int taillePixel = Math.min(9, Math.max(2,
                            (w - 2 * gauche) / Math.max(1, nom.length() * 6)));
                    int hautTitre = (int) Math.round(h * 0.685
                            + 21 * (1.0 - apparition));

                    // Ombre decalee, comme un titre de jeu en pixel art.
                    g.setColor(new Color(9, 13, 28, 235));
                    dessinerTextePixel(g, nom, gauche + 5, hautTitre + 6,
                            taillePixel);
                    g.setColor(new Color(255, 246, 219));
                    dessinerTextePixel(g, nom, gauche, hautTitre,
                            taillePixel);
                    g.setComposite(AlphaComposite.SrcOver);
                }

                // 5) Fondu noir au depart et a l'arrivee.
                double entreeNoire = 1.0 - douceur(t / 0.12);
                double sortieNoire = douceur((t - 0.88) / 0.12);
                int noirAlpha = (int) Math.round(
                        255.0 * Math.max(entreeNoire, sortieNoire));
                if (noirAlpha > 0) {
                    g.setColor(new Color(0, 0, 0, noirAlpha));
                    g.fillRect(0, 0, w, h);
                }

            } finally {
                g.dispose();
            }
        }
    }

    /**
     * Petite police matricielle integree dans le code : aucun fichier de
     * police a installer ni risque de substitution sur une autre machine.
     * Chaque lettre fait 5 colonnes x 7 lignes, avec 1 colonne d'espace.
     * Un espace superieur est reserve aux accents (CHATEAU / MARCHE, etc.).
     */
    private static void dessinerTextePixel(Graphics2D g, String texte,
                                          int x, int y, int pixel) {
        for (int i = 0; i < texte.length(); i++) {
            char lettre = texte.charAt(i);
            String decompos = Normalizer.normalize(
                    String.valueOf(lettre), Normalizer.Form.NFD);
            char base = Character.toUpperCase(decompos.charAt(0));
            int[] lignes = pixelsLettre(base);
            int xLettre = x + i * 6 * pixel;

            for (int ligne = 0; ligne < lignes.length; ligne++) {
                int masque = lignes[ligne];
                for (int colonne = 0; colonne < 5; colonne++) {
                    if ((masque & (1 << (4 - colonne))) != 0) {
                        g.fillRect(xLettre + colonne * pixel,
                                y + (ligne + 2) * pixel, pixel, pixel);
                    }
                }
            }

            // Gestion des accents francais sur les lettres pixelisees.
            if (decompos.indexOf('\u0302') >= 0) { // circonflexe
                g.fillRect(xLettre + 2 * pixel, y, pixel, pixel);
                g.fillRect(xLettre + pixel, y + pixel, pixel, pixel);
                g.fillRect(xLettre + 3 * pixel, y + pixel, pixel, pixel);
            } else if (decompos.indexOf('\u0301') >= 0) { // aigu
                g.fillRect(xLettre + 3 * pixel, y, pixel, pixel);
                g.fillRect(xLettre + 2 * pixel, y + pixel, pixel, pixel);
            } else if (decompos.indexOf('\u0300') >= 0) { // grave
                g.fillRect(xLettre + pixel, y, pixel, pixel);
                g.fillRect(xLettre + 2 * pixel, y + pixel, pixel, pixel);
            } else if (decompos.indexOf('\u0308') >= 0) { // trema
                g.fillRect(xLettre + pixel, y + pixel, pixel, pixel);
                g.fillRect(xLettre + 3 * pixel, y + pixel, pixel, pixel);
            }
        }
    }

    /** Une ligne contient cinq bits, du pixel gauche au pixel droit. */
    private static int[] pixelsLettre(char c) {
        switch (c) {
            case 'A': return new int[]{14,17,17,31,17,17,17};
            case 'B': return new int[]{30,17,17,30,17,17,30};
            case 'C': return new int[]{14,17,16,16,16,17,14};
            case 'D': return new int[]{30,17,17,17,17,17,30};
            case 'E': return new int[]{31,16,16,30,16,16,31};
            case 'F': return new int[]{31,16,16,30,16,16,16};
            case 'G': return new int[]{14,17,16,23,17,17,15};
            case 'H': return new int[]{17,17,17,31,17,17,17};
            case 'I': return new int[]{31,4,4,4,4,4,31};
            case 'J': return new int[]{7,2,2,2,18,18,12};
            case 'K': return new int[]{17,18,20,24,20,18,17};
            case 'L': return new int[]{16,16,16,16,16,16,31};
            case 'M': return new int[]{17,27,21,21,17,17,17};
            case 'N': return new int[]{17,25,21,19,17,17,17};
            case 'O': return new int[]{14,17,17,17,17,17,14};
            case 'P': return new int[]{30,17,17,30,16,16,16};
            case 'Q': return new int[]{14,17,17,17,21,18,13};
            case 'R': return new int[]{30,17,17,30,20,18,17};
            case 'S': return new int[]{15,16,16,14,1,1,30};
            case 'T': return new int[]{31,4,4,4,4,4,4};
            case 'U': return new int[]{17,17,17,17,17,17,14};
            case 'V': return new int[]{17,17,17,17,17,10,4};
            case 'W': return new int[]{17,17,17,21,21,21,10};
            case 'X': return new int[]{17,17,10,4,10,17,17};
            case 'Y': return new int[]{17,17,10,4,4,4,4};
            case 'Z': return new int[]{31,1,2,4,8,16,31};
            case '0': return new int[]{14,19,21,21,21,25,14};
            case '1': return new int[]{4,12,4,4,4,4,14};
            case '2': return new int[]{14,17,1,2,4,8,31};
            case '3': return new int[]{30,1,1,14,1,1,30};
            case '4': return new int[]{2,6,10,18,31,2,2};
            case '5': return new int[]{31,16,16,30,1,1,30};
            case '6': return new int[]{14,16,16,30,17,17,14};
            case '7': return new int[]{31,1,2,4,8,8,8};
            case '8': return new int[]{14,17,17,14,17,17,14};
            case '9': return new int[]{14,17,17,15,1,1,14};
            case '-': return new int[]{0,0,0,31,0,0,0};
            case ' ': return new int[]{0,0,0,0,0,0,0};
            default: return new int[]{31,17,2,4,4,0,4};
        }
    }

    /** Dans NetBeans : clic droit -> Run File pour tester le Chateau. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FenetreIntroCarte(
                        "/ressources/Maps/chateau.png", "CHÂTEAU")
                        .setVisible(true));
    }
}