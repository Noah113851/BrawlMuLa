package brawlmula;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
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
 * 4 presentations distinctes : medievale, festive, vegetale, holographique.
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
        private final boolean introductionPotager;
        private final boolean introductionColonie;
        private final Timer animation;
        private long debutNs;

        PanneauIntroCarte(String cheminCarte, String nomCarte) {
            if (nomCarte == null || nomCarte.trim().isEmpty()) {
                throw new IllegalArgumentException("Nom de carte manquant");
            }
            this.nom = nomCarte.toUpperCase(Locale.FRENCH);
            String cheminNormalise = cheminCarte.replace('\\', '/')
                    .toLowerCase(Locale.ROOT);
            this.introductionMarche = cheminNormalise.endsWith("/marche.png");
            this.introductionPotager = cheminNormalise.endsWith("/potager.png");
            this.introductionColonie = cheminNormalise.endsWith("/colonie.png");
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

                // 1) Camera : chaque carte a une trajectoire distincte.
                // CHATEAU : parcours diagonal haut-droite vers le centre.
                // MARCHE : diagonale bas-gauche vers le centre.
                // POTAGER : travelling VERTICAL haut -> bas, suivi d'un
                // large dezoom qui revele les grandes plateformes centrales.
                // Le ratio ORIGINAL du decor est toujours respecte.
                double echelleCouverture = Math.max(
                        w / (double) decor.getWidth(),
                        h / (double) decor.getHeight());
                double zoom;
                double fractionX;
                double fractionY;
                if (introductionColonie) {
                    // COLONIE : activation d'une camera de surveillance
                    // holographique. Gros plan a droite, recalage du signal,
                    // puis stabilisation sur l'arene complete.
                    // Contrairement aux autres cartes : petite vibration de
                    // camera en debut de connexion, puis verrouillage net.
                    double verrouillage = douceur((t - 0.12) / 0.54);
                    double stabilise = douceur((t - 0.22) / 0.47);
                    zoom = 1.48 - 0.48 * verrouillage;
                    fractionX = 0.83 - 0.33 * stabilise;
                    fractionY = 0.59 - 0.09 * stabilise;
                    double parasite = 1.0 - douceur((t - 0.03) / 0.38);
                    fractionX += 0.011 * Math.sin(t * 200.0) * parasite;
                    fractionY += 0.008 * Math.sin(t * 135.0) * parasite;
                } else if (introductionPotager) {
                    // POTAGER : travelling ascendant depuis le sol,
                    // comme une jeune pousse qui s'eleve vers la lumiere.
                    // Le champ s'ouvre doucement lors de la croissance.
                    double poussee = douceur((t - 0.04) / 0.73);
                    zoom = 1.57 - 0.57 * douceur(t / 0.83);
                    fractionX = 0.46 + 0.04 * poussee;
                    fractionY = 0.86 - 0.36 * poussee;
                } else if (introductionMarche) {
                    // MARCHE : travelling lateral rapide de gauche a droite,
                    // puis une tres legere correction de camera a l'arret.
                    double passage = douceur((t - 0.01) / 0.67);
                    double retour = douceur((t - 0.69) / 0.18);
                    zoom = 1.44 - 0.44 * douceur(t / 0.84);
                    fractionX = 0.12 + 0.52 * passage - 0.14 * retour;
                    fractionY = 0.71 - 0.22 * passage + 0.01 * retour;
                } else {
                    // CHATEAU : le decor est CENTRE des la premiere image.
                    // D'abord la herse se leve, ensuite la camera avance
                    // legerement DANS l'arene, sans aucun travelling lateral.
                    // Elle retrouve le cadrage normal avant le combat.
                    double avance = douceur((t - 0.34) / 0.29);
                    double stabilisation = douceur((t - 0.70) / 0.17);
                    zoom = 1.0 + 0.16 * (avance - stabilisation);
                    fractionX = 0.5;
                    fractionY = 0.5;
                }
                double largeurImage = decor.getWidth() * echelleCouverture * zoom;
                double hauteurImage = decor.getHeight() * echelleCouverture * zoom;

                int imageX = (int) Math.round(-(largeurImage - w) * fractionX);
                int imageY = (int) Math.round(-(hauteurImage - h) * fractionY);

                g.drawImage(decor, imageX, imageY,
                        (int) Math.ceil(largeurImage),
                        (int) Math.ceil(hauteurImage), null);

                // Effets propres a chaque univers, dessines par Graphics2D.
                // Aucun PNG supplementaire ni bibliotheque externe.
                if (introductionColonie) {
                    // Colonie conservee a l'identique (version validee).
                    dessinerScanColonie(g, w, h, t);
                } else if (introductionPotager) {
                    dessinerLumierePotager(g, w, h, t);
                    dessinerFeuillesPotager(g, w, h, t);
                } else if (introductionMarche) {
                    dessinerAmbianceMarche(g, w, h, t);
                } else {
                    dessinerAmbianceChateau(g, w, h, t);
                }

                // 2) Legere zone sombre en bas pour la lisibilite du titre.
                if (introductionColonie) {
                    g.setPaint(new GradientPaint(
                            0, h * 0.38f, new Color(3, 32, 42, 0),
                            0, h, new Color(2, 23, 36, 205)));
                } else if (introductionPotager) {
                    g.setPaint(new GradientPaint(
                            0, h * 0.46f, new Color(13, 30, 12, 0),
                            0, h, new Color(8, 24, 13, 185)));
                } else if (introductionMarche) {
                    g.setPaint(new GradientPaint(
                            0, h * 0.39f, new Color(58, 30, 12, 0),
                            0, h, new Color(34, 18, 13, 188)));
                } else {
                    g.setPaint(new GradientPaint(
                            0, h * 0.40f, new Color(12, 10, 27, 0),
                            0, h, new Color(14, 10, 25, 211)));
                }
                g.fillRect(0, 0, w, h);

                // 3) Bandeaux cinematographiques.
                int bande = Math.max(25, (int) Math.round(h * 0.048));
                g.setColor(new Color(4, 7, 14));
                g.fillRect(0, 0, w, bande);
                g.fillRect(0, h - bande, w, bande);

                // Premier plan : ornements propres aux trois cartes.
                // Pas d'ajout sur la Colonie, pour garder l'intro validee.
                if (!introductionColonie) {
                    if (introductionPotager) {
                        dessinerLianesPotager(g, w, h, t);
                    } else if (introductionMarche) {
                        dessinerGuirlandesMarche(g, w, h, t);
                    } else {
                        dessinerCadreChateau(g, w, h, t);
                    }
                }

                // 4) Titre seul : lettres pixelisees, sans texte au-dessus.
                // Le Chateau attend que la herse soit bien levee
                // avant de montrer son titre.
                boolean chateau = !introductionMarche
                        && !introductionPotager && !introductionColonie;
                double apparition = douceur((t - (chateau ? 0.51 : 0.31)) / 0.15);
                double disparition = 1.0 - douceur((t - 0.86) / 0.09);
                float opacite = (float) limiter(apparition * disparition);
                if (opacite > 0.0f) {
                    Graphics2D titre = (Graphics2D) g.create();
                    try {
                        titre.setComposite(AlphaComposite.getInstance(
                                AlphaComposite.SRC_OVER, opacite));
                        int gauche = (int) Math.round(w * 0.075);
                        int taillePixel = Math.min(9, Math.max(2,
                                (w - 2 * gauche) / Math.max(1, nom.length() * 6)));
                        if (chateau) {
                            // Titre centre pour accompagner la camera.
                            gauche = (w - (nom.length() * 6 - 1) * taillePixel) / 2;
                        }
                        int hautTitre = (int) Math.round(h * 0.685
                                + 21 * (1.0 - apparition));
                        if (!introductionColonie) {
                            // Chaque inscription possede son support et
                            // sa direction d'arrivee cinematique.
                            if (introductionMarche) {
                                gauche += (int) Math.round(
                                        -34 * (1.0 - apparition));
                                dessinerEnseigneMarche(titre, gauche,
                                        hautTitre, nom.length(), taillePixel, t);
                            } else if (introductionPotager) {
                                hautTitre += (int) Math.round(
                                        18 * (1.0 - apparition));
                                dessinerPlaquePotager(titre, gauche,
                                        hautTitre, nom.length(), taillePixel, t);
                            } else {
                                hautTitre += (int) Math.round(
                                        -23 * (1.0 - apparition));
                                dessinerBlasonChateau(titre, gauche,
                                        hautTitre, nom.length(), taillePixel, t);
                            }
                        }
                        // La Colonie conserve ses couleurs de titre exactes.
                        titre.setColor(introductionPotager
                                ? new Color(8, 22, 17, 245)
                                : introductionMarche
                                ? new Color(66, 31, 11, 245)
                                : new Color(9, 13, 28, 235));
                        dessinerTextePixel(titre, nom, gauche + 5,
                                hautTitre + 6, taillePixel);
                        titre.setColor(introductionPotager
                                ? new Color(232, 255, 196)
                                : introductionMarche
                                ? new Color(255, 244, 209)
                                : introductionColonie
                                ? new Color(255, 246, 219)
                                : new Color(255, 220, 157));
                        dessinerTextePixel(titre, nom, gauche, hautTitre,
                                taillePixel);
                    } finally {
                        titre.dispose();
                    }
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

    // ======================================================================
    // CHATEAU — herse en fer qui se souleve, arche medievale et poussiere.
    // ======================================================================
    private static void dessinerAmbianceChateau(Graphics2D g,
                                                int w, int h, double t) {
        Graphics2D fx = (Graphics2D) g.create();
        try {
            double entree = douceur(t / 0.18);
            double sortie = 1.0 - douceur((t - 0.78) / 0.12);
            float intensite = (float) limiter(entree * sortie);
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, intensite));

            // Faisceaux de lune : diagonales qui revelent la pierre.
            for (int i = 0; i < 5; i++) {
                double pulsation = 9.0 * Math.sin(t * 8.0 + i * 1.7);
                int sommet = (int) (w * (0.60 + i * 0.085));
                int base = (int) (w * (0.22 + i * 0.13) + pulsation);
                fx.setColor(new Color(248, 216, 158, 18 + i * 2));
                fx.fillPolygon(new int[]{sommet, sommet + 55,
                        base + 150, base - 50},
                        new int[]{0, 0, h, h}, 4);
            }

            // Poussieres lumineuses flottantes en leger decalage de camera.
            for (int i = 0; i < 32; i++) {
                double u = fract(0.157 + i * 0.6180339887);
                double v = fract(0.281 + i * 0.4142135623);
                int x = (int) (w * u + 14 * Math.sin(7 * t + i));
                int y = (int) (h * v - t * (35 + (i % 4) * 16));
                y = (y + h) % h;
                int rayon = 2 + i % 3;
                fx.setColor(new Color(255, 213, 140, 22));
                fx.fillOval(x - rayon * 3, y - rayon * 3,
                        rayon * 6, rayon * 6);
                fx.setColor(new Color(255, 230, 175, 144));
                fx.fillRect(x, y, rayon, rayon);
            }
        } finally {
            fx.dispose();
        }

        // La herse se souleve AVANT le zoom centre sur la carte.
        dessinerHerseChateau(g, w, h, t);
    }

    /**
     * Ouverture de la herse medievale : arche en pierres et grille en fer.
     * La grille coulisse VERTICALEMENT (aucune rotation/pont-levis).
     * Le passage est masque jusqu'a l'ouverture, puis le cadre disparait
     * doucement pour laisser la map entiere visible et bien centree.
     * Tout est dessine avec Graphics2D, sans nouvelle image ni police.
     */
    private static void dessinerHerseChateau(Graphics2D g,
                                             int w, int h, double t) {
        double ouverture = douceur((t - 0.025) / 0.355);
        float opacite = (float) (1.0 - douceur((t - 0.39) / 0.115));
        if (opacite <= 0.0f) {
            return;
        }

        Graphics2D fx = (Graphics2D) g.create();
        try {
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, opacite));
            fx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            double gauche = w * 0.137;
            double droite = w * 0.863;
            double centre = w / 2.0;
            double sommet = h * 0.082;
            double epaules = h * 0.28;

            // Arche en ogive : contour du passage derriere la grille.
            Path2D.Double passage = new Path2D.Double();
            passage.moveTo(gauche, h + 20);
            passage.lineTo(gauche, epaules);
            passage.curveTo(gauche, h * 0.165,
                    centre - w * 0.11, sommet + h * 0.025,
                    centre, sommet);
            passage.curveTo(centre + w * 0.11, sommet + h * 0.025,
                    droite, h * 0.165, droite, epaules);
            passage.lineTo(droite, h + 20);
            passage.closePath();

            // Mur de pierre : seule la zone HORS de l'arche est opaque.
            Area pierre = new Area(new Rectangle2D.Double(0, 0, w, h));
            pierre.subtract(new Area(passage));
            fx.setPaint(new GradientPaint(0, 0,
                    new Color(61, 66, 83), 0, h,
                    new Color(24, 29, 47)));
            fx.fill(pierre);

            // Joints de maçonnerie, au-dessus et autour de la porte.
            Graphics2D joint = (Graphics2D) fx.create();
            try {
                joint.setClip(pierre);
                joint.setStroke(new BasicStroke(2.0f));
                for (int rang = 0; rang < 17; rang++) {
                    int yy = (int) (rang * h / 15.0);
                    joint.setColor(new Color(10, 16, 28, 125));
                    joint.drawLine(0, yy, w, yy);
                    double decalage = rang % 2 == 0 ? 0 : 76;
                    for (double xx = -50 + decalage; xx < w + 60; xx += 152) {
                        int xj = (int) xx;
                        int ySuivant = (int) ((rang + 1) * h / 15.0);
                        joint.drawLine(xj, yy, xj, ySuivant);
                        joint.setColor(new Color(158, 158, 167, 25));
                        joint.drawLine(xj + 3, yy + 3, xj + 3, ySuivant - 3);
                        joint.setColor(new Color(10, 16, 28, 125));
                    }
                }
                joint.setColor(new Color(192, 182, 169, 38));
                for (int i = 0; i < 45; i++) {
                    int xx = (i * 173 + 53) % w;
                    int yy = (i * 109 + 47) % h;
                    joint.fillRect(xx, yy, 14 + i % 4 * 5, 2);
                }
            } finally {
                joint.dispose();
            }

            // Liserés de l'ogive : ombre profonde puis pierre éclairée.
            fx.setColor(new Color(8, 12, 24, 220));
            fx.setStroke(new BasicStroke(27f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            fx.draw(passage);
            fx.setColor(new Color(136, 139, 146, 227));
            fx.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            fx.draw(passage);
            fx.setColor(new Color(213, 193, 154, 108));
            fx.setStroke(new BasicStroke(3f));
            fx.draw(passage);

            // Pierre de clef au sommet du portail.
            Path2D.Double clef = new Path2D.Double();
            clef.moveTo(centre - 29, sommet - 10);
            clef.lineTo(centre + 29, sommet - 10);
            clef.lineTo(centre + 20, sommet + 37);
            clef.lineTo(centre, sommet + 53);
            clef.lineTo(centre - 20, sommet + 37);
            clef.closePath();
            fx.setColor(new Color(157, 148, 144));
            fx.fill(clef);
            fx.setColor(new Color(47, 48, 61));
            fx.setStroke(new BasicStroke(3f));
            fx.draw(clef);
            fx.setColor(new Color(218, 192, 128, 155));
            fx.fillOval((int) centre - 5, (int) sommet + 14, 10, 10);

            // La carte s'assombrit a travers la grille FERMEE.
            // Elle gagne sa luminosite a mesure que la grille se leve.
            Graphics2D interieur = (Graphics2D) fx.create();
            try {
                interieur.setClip(passage);
                interieur.setColor(new Color(6, 8, 18,
                        (int) Math.round(155 * (1.0 - ouverture))));
                interieur.fillRect(0, 0, w, h);

                // La herse se souleve dans la maçonnerie.
                double levee = ouverture * (h + 260);
                double basHerse = h + 82 - levee;
                double hautHerse = sommet - 38 - levee;

                // Lignes verticales en fer, rehaussées d'un reflet acier.
                int index = 0;
                for (double xx = gauche + 29; xx < droite - 12; xx += 55) {
                    int xBarre = (int) Math.round(xx);
                    int hautBarre = (int) Math.round(hautHerse);
                    int basBarre = (int) Math.round(basHerse);

                    interieur.setPaint(new GradientPaint(xBarre - 10, 0,
                            new Color(22, 26, 38), xBarre + 10, 0,
                            new Color(98, 102, 118)));
                    interieur.fillRect(xBarre - 11, hautBarre, 22,
                            Math.max(0, basBarre - hautBarre));
                    interieur.setColor(new Color(180, 179, 174, 115));
                    interieur.fillRect(xBarre - 7, hautBarre + 3, 3,
                            Math.max(0, basBarre - hautBarre - 9));
                    interieur.setColor(new Color(14, 19, 30));
                    interieur.fillPolygon(
                            new int[]{xBarre - 11, xBarre + 11, xBarre},
                            new int[]{basBarre - 2, basBarre - 2,
                                    basBarre + 20 + (index % 2) * 6}, 3);
                    index++;
                }

                // Traverses épaisses avec des rivets aux intersections.
                for (int rail = 0; rail < 5; rail++) {
                    int yRail = (int) Math.round(sommet + 65
                            + rail * 150 - levee);
                    int xRail = (int) Math.round(gauche + 13);
                    int largeurRail = (int) Math.round(droite - gauche - 26);
                    interieur.setColor(new Color(12, 17, 27));
                    interieur.fillRoundRect(xRail, yRail - 13,
                            largeurRail, 31, 10, 10);
                    interieur.setColor(new Color(86, 93, 105));
                    interieur.fillRoundRect(xRail + 2, yRail - 8,
                            largeurRail - 4, 14, 8, 8);
                    interieur.setColor(new Color(186, 174, 147));
                    for (double xx = gauche + 29; xx < droite - 12; xx += 55) {
                        interieur.fillOval((int) xx - 4, yRail - 4, 8, 8);
                    }
                }

                // Sceau central stylisé au milieu de la herse.
                int sceauY = (int) Math.round(h * 0.52 - levee);
                interieur.setColor(new Color(24, 28, 43));
                interieur.fillOval(w / 2 - 27, sceauY - 27, 54, 54);
                interieur.setColor(new Color(194, 153, 88));
                interieur.setStroke(new BasicStroke(4f));
                interieur.drawOval(w / 2 - 24, sceauY - 24, 48, 48);
                interieur.drawLine(w / 2 - 12, sceauY,
                        w / 2 + 12, sceauY);
                interieur.drawLine(w / 2, sceauY - 12,
                        w / 2, sceauY + 12);
            } finally {
                interieur.dispose();
            }

            // Petites torches lumineuses sur les murs latéraux.
            for (int cote = 0; cote < 2; cote++) {
                int tx = (int) (w * (cote == 0 ? 0.073 : 0.927));
                int ty = (int) (h * 0.36);
                fx.setColor(new Color(20, 18, 25));
                fx.fillRoundRect(tx - 9, ty - 7, 18, 44, 8, 8);
                fx.setPaint(new GradientPaint(tx, ty - 42,
                        new Color(255, 214, 105, 125), tx, ty + 19,
                        new Color(255, 157, 65, 0)));
                fx.fillOval(tx - 36, ty - 52, 72, 91);
                fx.setColor(new Color(249, 195, 91));
                fx.fillOval(tx - 5, ty - 23, 10, 24);
                fx.setColor(new Color(255, 240, 164));
                fx.fillOval(tx - 2, ty - 18, 5, 15);
            }
        } finally {
            fx.dispose();
        }
    }

    private static void dessinerCadreChateau(Graphics2D g,
                                             int w, int h, double t) {
        float alpha = (float) (0.79 * douceur((t - 0.20) / 0.20)
                * (1.0 - douceur((t - 0.83) / 0.10)));
        if (alpha <= 0) return;
        Graphics2D fx = (Graphics2D) g.create();
        try {
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            int m = 47, l = 74, y1 = 70, y2 = h - 72;
            fx.setColor(new Color(244, 207, 137, 205));
            fx.setStroke(new java.awt.BasicStroke(3f));
            for (int x : new int[]{m, w - m}) {
                int sens = (x == m) ? 1 : -1;
                fx.drawLine(x, y1, x + sens * l, y1);
                fx.drawLine(x, y1, x, y1 + l);
                fx.drawLine(x, y2, x + sens * l, y2);
                fx.drawLine(x, y2, x, y2 - l);
                fx.fillPolygon(new int[]{x, x + sens * 9, x, x - sens * 9},
                        new int[]{y1 - 12, y1, y1 + 12, y1}, 4);
            }
            fx.setColor(new Color(241, 220, 169, 130));
            fx.drawLine(w / 2 - 62, 72, w / 2 + 62, 72);
            fx.fillPolygon(new int[]{w / 2, w / 2 + 11,
                    w / 2, w / 2 - 11},
                    new int[]{61, 72, 83, 72}, 4);
        } finally {
            fx.dispose();
        }
    }

    private static void dessinerBlasonChateau(Graphics2D g, int x, int y,
                                               int len, int pixel, double t) {
        int largeur = len * 6 * pixel + 48;
        int haut = y - 17, hauteur = 111;
        // Support en pierre sombre, bordure d'or et chevrons.
        g.setColor(new Color(16, 15, 30, 198));
        g.fillRoundRect(x - 24, haut, largeur, hauteur, 15, 15);
        g.setColor(new Color(232, 180, 91, 230));
        g.setStroke(new java.awt.BasicStroke(3f));
        g.drawRoundRect(x - 24, haut, largeur, hauteur, 15, 15);
        g.setColor(new Color(248, 223, 155, 155));
        g.drawLine(x - 10, haut + 8, x + largeur - 38, haut + 8);
        g.drawLine(x - 10, haut + hauteur - 9,
                x + largeur - 38, haut + hauteur - 9);
        for (int signe : new int[]{-1,1}) {
            int dx = signe == -1 ? x - 17 : x + largeur - 30;
            g.setColor(new Color(255, 222, 155, 215));
            g.fillPolygon(new int[]{dx, dx+5, dx, dx-5},
                    new int[]{haut+hauteur/2-9, haut+hauteur/2,
                        haut+hauteur/2+9, haut+hauteur/2}, 4);
        }
    }

    // ======================================================================
    // MARCHE — atmosphere chaleureuse, guirlandes et enseigne en bois.
    // ======================================================================
    private static void dessinerAmbianceMarche(Graphics2D g,
                                               int w, int h, double t) {
        float alpha = (float) (0.78 * douceur(t / 0.20)
                * (1.0 - douceur((t - 0.83) / 0.11)));
        if (alpha <= 0) return;
        Graphics2D fx = (Graphics2D) g.create();
        try {
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));

            // Soleil chaud et faisceaux sous les auvents du village.
            fx.setPaint(new GradientPaint(w, h * 0.05f,
                    new Color(255, 229, 137, 76),
                    w / 3f, h * 0.70f, new Color(255, 192, 102, 0)));
            fx.fillRect(0, 0, w, h);
            for (int i = 0; i < 4; i++) {
                int origine = w - 160 + i * 37;
                fx.setColor(new Color(255, 245, 183, 19));
                fx.fillPolygon(new int[]{origine, origine + 30,
                        (int)(w * (0.17 + i * .11)) + 155,
                        (int)(w * (0.17 + i * .11)) - 70},
                        new int[]{0, 0, h, h}, 4);
            }

            // Confettis et petits morceaux de papier de fete.
            Color[] papiers = {
                new Color(255, 221, 122),
                new Color(218, 92, 65),
                new Color(109, 178, 141),
                new Color(245, 241, 217)
            };
            for (int i = 0; i < 29; i++) {
                int xx = (int) (w * fract(i * 0.75487766 + .13)
                        + (t * 105 + 14 * Math.sin(8*t + i)));
                xx = ((xx % (w+35)) + (w+35)) % (w+35);
                int yy = (int) (h * fract(i * 0.56984029 + .27)
                        + t * (25 + i%6 * 11));
                yy %= (h + 30);
                fx.setColor(papiers[i % papiers.length]);
                fx.fillRect(xx, yy, 4 + i % 3 * 2, 2 + i % 2 * 3);
            }
        } finally {
            fx.dispose();
        }
    }

    private static void dessinerGuirlandesMarche(Graphics2D g,
                                                 int w, int h, double t) {
        float alpha = (float) (0.87 * douceur((t - 0.10) / 0.16)
                * (1.0 - douceur((t - 0.82) / 0.12)));
        if (alpha <= 0) return;
        Graphics2D fx = (Graphics2D) g.create();
        try {
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            fx.setStroke(new java.awt.BasicStroke(2.3f));
            Color[] fanions = {new Color(224, 97, 76),
                    new Color(245, 199, 84),
                    new Color(100, 179, 155),
                    new Color(243, 231, 198)};
            for (int fil = 0; fil < 2; fil++) {
                int precedentX = 0;
                int precedentY = 0;
                for (int i = 0; i <= 18; i++) {
                    int x = (int) Math.round(35 + i * (w - 70) / 18.0);
                    int y = (int) Math.round((55 + fil * 92)
                            + 34 * Math.sin(i * Math.PI / 18.0)
                            + 3 * Math.sin(7 * t + i * .55 + fil));
                    if (i > 0) {
                        fx.setColor(new Color(91, 63, 48, 200));
                        fx.drawLine(precedentX, precedentY, x, y);
                    }
                    if (i > 0 && i < 18) {
                        fx.setColor(fanions[(i + fil) % fanions.length]);
                        int vent = (int) (4 * Math.sin(12*t + i*.7));
                        fx.fillPolygon(new int[]{x-14,x+13,x+vent},
                                new int[]{y+2,y+2,y+33+(i%3)*4},3);
                    }
                    precedentX=x;
                    precedentY=y;
                }
            }
        } finally {
            fx.dispose();
        }
    }

    private static void dessinerEnseigneMarche(Graphics2D g, int x, int y,
                                                int len, int pixel, double t) {
        int largeur = len*6*pixel + 56;
        int haut = y - 17;
        // Bois, petites planches, vis et cordes comme une vraie enseigne.
        g.setColor(new Color(76, 43, 25, 223));
        g.fillRoundRect(x - 28, haut, largeur, 112, 14, 14);
        g.setColor(new Color(207, 144, 77, 229));
        g.setStroke(new java.awt.BasicStroke(3.5f));
        g.drawRoundRect(x - 28, haut, largeur, 112, 14, 14);
        g.setColor(new Color(151, 90, 46, 159));
        for (int yy=haut+19; yy<haut+108; yy+=25) {
            g.fillRect(x-22, yy, largeur-12, 3);
        }
        g.setColor(new Color(244, 211, 145, 203));
        for (int dx : new int[]{-15, largeur-41}) {
            g.fillOval(x + dx, haut + 8, 6, 6);
            g.fillOval(x + dx, haut + 96, 6, 6);
        }
    }

    // ======================================================================
    // POTAGER — soleil, lianes qui poussent et pollen en suspension.
    // ======================================================================
    private static void dessinerLumierePotager(Graphics2D g,
                                               int w, int h, double t) {
        float alpha = (float) (0.82 * douceur((t-0.08)/0.19)
                * (1.0 - douceur((t-0.85)/0.10)));
        if (alpha<=0) return;
        Graphics2D fx=(Graphics2D)g.create();
        try {
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            for (int i=0; i<5; i++) {
                int sx=(int)(w*(.39+i*.063));
                int ex=(int)(w*(.10+i*.22));
                fx.setColor(new Color(255,249,173,18+i*2));
                fx.fillPolygon(new int[]{sx,sx+44,ex+87,ex-95},
                        new int[]{0,0,h,h},4);
            }
            // Pollen en contre-jour qui monte en flottant.
            for(int i=0;i<33;i++){
                double u=fract(.18+i*.6180339887);
                double v=fract(.44+i*.754877666);
                int x=(int)(u*w+19*Math.sin(t*9+i*.9));
                int y=(int)(v*h - t*(60+i%6*13));
                y=((y%h)+h)%h;
                int taille=2+i%3;
                fx.setColor(new Color(252,255,166,46));
                fx.fillOval(x-7,y-7,taille+14,taille+14);
                fx.setColor(new Color(238,255,170,211));
                fx.fillOval(x,y,taille,taille);
            }
        } finally {fx.dispose();}
    }

    private static void dessinerLianesPotager(Graphics2D g,
                                             int w, int h, double t) {
        float alpha=(float)(0.84*douceur((t-.11)/.22)
                *(1.0-douceur((t-.83)/.12)));
        if(alpha<=0)return;
        Graphics2D fx=(Graphics2D)g.create();
        try{
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,alpha));
            double croissance=douceur((t-.09)/.56);
            for(int cote=0;cote<2;cote++){
                int sens=(cote==0?1:-1);
                fx.setColor(new Color(25,87,37,239));
                fx.setStroke(new java.awt.BasicStroke(5f,
                        java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                double px=cote==0?0:w;
                double py=h;
                int etapes=(int)Math.round(35*croissance);
                for(int i=1;i<=etapes;i++){
                    double u=i/35.0;
                    double x=(cote==0?0:w) + sens*(90*u+22*Math.sin(u*10));
                    double y=h-(h*.72)*u;
                    fx.drawLine((int)px,(int)py,(int)x,(int)y);
                    px=x;py=y;
                }
                for(int i=3;i<=etapes;i+=4){
                    double u=i/35.0;
                    int x=(int)((cote==0?0:w)+sens*(90*u+22*Math.sin(u*10)));
                    int y=(int)(h-(h*.72)*u);
                    int direction= (i%8==3?1:-1);
                    fx.setColor(i%8==3
                            ?new Color(111,191,84,238)
                            :new Color(168,216,102,228));
                    fx.fillOval(x+sens*direction*4,y-7,
                            12+((i%3)*4),8+((i%2)*3));
                    fx.setColor(new Color(25,87,37,239));
                }
            }
        } finally{fx.dispose();}
    }

    private static void dessinerPlaquePotager(Graphics2D g,int x,int y,
                                               int len,int pixel,double t){
        int largeur=len*6*pixel+54;
        int haut=y-17;
        g.setColor(new Color(24,63,30,204));
        g.fillRoundRect(x-27,haut,largeur,112,28,28);
        g.setColor(new Color(145,205,109,228));
        g.setStroke(new java.awt.BasicStroke(3.5f));
        g.drawRoundRect(x-27,haut,largeur,112,28,28);
        g.setColor(new Color(214,237,143,119));
        g.drawRoundRect(x-19,haut+6,largeur-16,100,22,22);
        for(int i=0;i<5;i++){
            int feuilleX=x-19+i*18;
            int feuilleY=haut-(i%2)*8;
            g.setColor(i%2==0?new Color(125,199,96):new Color(195,218,105));
            g.fillOval(feuilleX,feuilleY-5,18,10);
        }
        for(int i=0;i<4;i++){
            int feuilleX=x+largeur-52-i*17;
            int feuilleY=haut+105+(i%2)*6;
            g.setColor(i%2==0?new Color(155,213,110):new Color(98,164,75));
            g.fillOval(feuilleX,feuilleY,18,10);
        }
    }

    /** Partie fractionnaire positive, sans hasard ni etat mutable. */
    private static double fract(double x) {
        return x - Math.floor(x);
    }

    /**
     * Signal holographique de la Colonie : balayage cyan, trame numerique,
     * coins de ciblage et brieves interferences en debut de sequence.
     * Purement graphique : aucun PNG, aucune collision ni effet sur le jeu.
     * Les effets s'effacent avant l'affichage complet du titre COLONIE.
     */
    private static void dessinerScanColonie(Graphics2D g,
                                            int w, int h, double t) {
        double entree = douceur(t / 0.13);
        double sortie = 1.0 - douceur((t - 0.66) / 0.18);
        float opacite = (float) limiter(0.68 * entree * sortie);
        if (opacite <= 0.0f) {
            return;
        }

        Graphics2D fx = (Graphics2D) g.create();
        try {
            fx.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, opacite));

            // Trame horizontale legere, comme une projection numerique.
            fx.setColor(new Color(65, 225, 247, 34));
            for (int y = 0; y < h; y += 9) {
                fx.drawLine(0, y, w, y);
            }

            // Balayage vertical lumineux : il traverse le cadre en 2 s.
            double avancee = douceur((t - 0.02) / 0.52);
            int scanY = (int) Math.round(-30 + (h + 60) * avancee);
            fx.setColor(new Color(49, 235, 251, 40));
            fx.fillRect(0, scanY - 17, w, 35);
            fx.setColor(new Color(121, 255, 248, 188));
            fx.fillRect(0, scanY, w, 3);

            // Cadre de projection : quatre coins, sans cacher le decor.
            int marge = Math.max(30, w / 19);
            int haut = Math.max(45, h / 9);
            int bas = h - haut;
            int longueur = Math.max(28, w / 17);
            fx.setColor(new Color(111, 239, 252, 205));
            fx.fillRect(marge, haut, longueur, 3);
            fx.fillRect(marge, haut, 3, longueur);
            fx.fillRect(w - marge - longueur, haut, longueur, 3);
            fx.fillRect(w - marge - 3, haut, 3, longueur);
            fx.fillRect(marge, bas - 2, longueur, 3);
            fx.fillRect(marge, bas - longueur, 3, longueur);
            fx.fillRect(w - marge - longueur, bas - 2, longueur, 3);
            fx.fillRect(w - marge - 3, bas - longueur, 3, longueur);

            // Interferences breves avant la stabilisation de l'image.
            double parasite = 1.0 - douceur((t - 0.08) / 0.23);
            if (parasite > 0.0) {
                int lignes = 9;
                for (int i = 0; i < lignes; i++) {
                    int yy = (int) Math.round(h * ((i * 0.173 + t * 4.1) % 1.0));
                    int xx = (int) Math.round(w * ((i * 0.211 + t * 0.8) % 0.70));
                    int longueurBande = 40 + (i * 43) % 160;
                    int alpha = (int) Math.round(105 * parasite);
                    fx.setColor(new Color(112, 250, 243, alpha));
                    fx.fillRect(xx, yy, Math.min(longueurBande, w - xx),
                            i % 3 == 0 ? 4 : 2);
                }
            }
        } finally {
            fx.dispose();
        }
    }

    /**
     * Petites feuilles stylisees qui flottent lentement dans la lumiere.
     * Effet visuel propre au Potager (sans sprite ni collision).
     */
    private static void dessinerFeuillesPotager(Graphics2D g,
                                               int w, int h, double t) {
        float opacite = (float) (0.55 * douceur((t - 0.09) / 0.16)
                * (1.0 - douceur((t - 0.79) / 0.12)));
        if (opacite <= 0.0f) {
            return;
        }
        Graphics2D feuilles = (Graphics2D) g.create();
        try {
            feuilles.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, opacite));
            for (int i = 0; i < 14; i++) {
                double departX = (0.12 + (i * 0.071) % 0.75) * w;
                double departY = (0.16 + (i * 0.047) % 0.52) * h;
                double mouvementX = 15.0 * Math.sin(i * 1.7 + t * 6.0);
                double mouvementY = t * (65.0 + (i % 5) * 17.0);
                int x = (int) Math.round(departX + mouvementX);
                int y = (int) Math.round(departY + mouvementY);
                int taille = 5 + (i % 3) * 2;
                feuilles.setColor(i % 3 == 0
                        ? new Color(237, 243, 151)
                        : new Color(174, 231, 117));
                feuilles.fillPolygon(
                        new int[]{x, x + taille, x + 2 * taille, x + taille},
                        new int[]{y, y - taille / 2, y, y + taille / 2}, 4);
            }
        } finally {
            feuilles.dispose();
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