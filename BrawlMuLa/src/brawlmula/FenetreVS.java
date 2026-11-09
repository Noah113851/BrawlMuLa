package brawlmula;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Ecran VS 2D adapte aux parties de 2, 3 ou 4 joueurs.
 * Design adapte a chaque carte. Les choix viennent de Selection.
 * L'ecran ne lance pas la physique du jeu : celle-ci demarre apres le VS.
 */
public final class FenetreVS extends JFrame {

    private static final int LARGEUR = 1280;
    private static final int HAUTEUR = 760;
    private static final long DUREE_NS = 3_600_000_000L;

    private final PanneauVS panneau;
    private final Runnable apresVS;
    private final Timer chronometre;
    private long debutNs;
    private boolean terminee;

    /** Compatibilite : les appels historiques affichent toujours 4 joueurs. */
    public FenetreVS(String cheminCarte, String j1, String j2,
                     String j3, String j4, Runnable apresVS) {
        this(cheminCarte,
                new String[]{j1, j2, j3, j4},
                new int[]{1, 2, 3, 4}, apresVS);
    }

    /**
     * Nouveau constructeur : seuls les joueurs presents sont affiches.
     * numerosJoueurs : numeros reels de 1 a 4 (ex : {1, 3}).
     * personnages : dans le meme ordre que les numeros.
     */
    public FenetreVS(String cheminCarte, String[] personnages,
                     int[] numerosJoueurs, Runnable apresVS) {
        if (apresVS == null) {
            throw new IllegalArgumentException("Action de fin du VS manquante.");
        }
        this.apresVS = apresVS;
        this.panneau = new PanneauVS(cheminCarte, personnages, numerosJoueurs);
        this.chronometre = new Timer(16, e -> {
            if (debutNs == 0L) return;
            if (System.nanoTime() - debutNs >= DUREE_NS) {
                terminer();
            } else {
                panneau.repaint();
            }
        });
        chronometre.setCoalesce(true);

        setTitle("BrawlMuLa - VS : " + panneau.nomCarte);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setContentPane(panneau);
        pack();
        setLocationRelativeTo(null);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "suivant");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "suivant");
        getRootPane().getActionMap().put("suivant", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { terminer(); }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                if (debutNs == 0L) {
                    debutNs = System.nanoTime();
                    panneau.debutNs = debutNs;
                    chronometre.start();
                }
            }
            @Override public void windowClosed(WindowEvent e) {
                chronometre.stop();
            }
        });
    }

    private void terminer() {
        if (terminee) return;
        terminee = true;
        chronometre.stop();
        dispose();
        apresVS.run();
    }

    private static double borne(double n) { return Math.max(0.0, Math.min(1.0, n)); }
    private static double doux(double n) {
        double t = borne(n);
        return t * t * (3.0 - 2.0 * t);
    }
    private static int couleurCanal(int valeur) {
        return Math.max(0, Math.min(255, valeur));
    }
    private static Color alpha(Color couleur, int a) {
        return new Color(couleur.getRed(), couleur.getGreen(),
                couleur.getBlue(), couleurCanal(a));
    }

    /** Panneau independant d'un JFrame, pratique pour le rendu et les tests. */
    static final class PanneauVS extends JPanel {

        private final String nomCarte;
        private final String carte;
        private final BufferedImage decor;
        private final String[] noms;
        private final BufferedImage[] portraits;
        private final int[] numerosJoueurs;
        private final Color principal;
        private final Color secondaire;
        private final Color lumiere;
        long debutNs;

        PanneauVS(String cheminCarte, String[] choix, int[] numerosJoueurs) {
            if (cheminCarte == null || choix == null || numerosJoueurs == null
                    || choix.length != numerosJoueurs.length
                    || choix.length < 2 || choix.length > 4) {
                throw new IllegalArgumentException(
                        "L'ecran VS attend 2, 3 ou 4 joueurs presents.");
            }
            boolean[] numerosUtilises = new boolean[5];
            for (int numero : numerosJoueurs) {
                if (numero < 1 || numero > 4 || numerosUtilises[numero]) {
                    throw new IllegalArgumentException(
                            "Numeros de joueur invalides ou en double.");
                }
                numerosUtilises[numero] = true;
            }
            this.numerosJoueurs = numerosJoueurs.clone();
            this.portraits = new BufferedImage[choix.length];
            String chemin = cheminCarte.replace('\\', '/').toLowerCase(Locale.ROOT);
            if (chemin.endsWith("chateau.png")) {
                carte = "CHATEAU";
                nomCarte = "CHÂTEAU";
                principal = new Color(50, 35, 87);
                secondaire = new Color(232, 177, 76);
                lumiere = new Color(255, 233, 163);
            } else if (chemin.endsWith("marche.png")) {
                carte = "MARCHE";
                nomCarte = "MARCHÉ";
                principal = new Color(121, 61, 36);
                secondaire = new Color(255, 178, 74);
                lumiere = new Color(255, 227, 158);
            } else if (chemin.endsWith("potager.png")) {
                carte = "POTAGER";
                nomCarte = "POTAGER";
                principal = new Color(24, 82, 56);
                secondaire = new Color(157, 211, 93);
                lumiere = new Color(218, 254, 161);
            } else if (chemin.endsWith("colonie.png")) {
                carte = "COLONIE";
                nomCarte = "COLONIE";
                principal = new Color(12, 39, 68);
                secondaire = new Color(64, 227, 247);
                lumiere = new Color(164, 255, 255);
            } else {
                throw new IllegalArgumentException("Carte inconnue : " + cheminCarte);
            }
            decor = lireImage(cheminCarte, true);
            noms = choix.clone();
            for (int i = 0; i < noms.length; i++) {
                if (noms[i] == null || noms[i].trim().isEmpty()) {
                    noms[i] = new String[]{"Carrot", "Nikku", "Tomato", "Potato"}[i];
                }
                String sprite = nomSprite(noms[i]);
                portraits[i] = recadrerAlpha(lireImage(
                        "/ressources/avatars/" + sprite, false));
            }
            setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
            setDoubleBuffered(true);
            setBackground(Color.BLACK);
        }

        private static String nomSprite(String nom) {
            String n = Normalizer.normalize(nom, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
            if (n.contains("nikku")) return "nikku_fight.png";
            if (n.contains("tomato")) return "tomato_fight.png";
            if (n.contains("potato")) return "potato_fight.png";
            return "carrot_fight.png";
        }

        private BufferedImage lireImage(String chemin, boolean obligatoire) {
            URL url = getClass().getResource(chemin);
            if (url == null) {
                if (obligatoire) throw new IllegalStateException("Image absente : " + chemin);
                System.err.println("[VS] Sprite absent : " + chemin);
                return null;
            }
            try {
                BufferedImage image = ImageIO.read(url);
                if (image == null && obligatoire) {
                    throw new IOException("Image illisible");
                }
                return image;
            } catch (IOException ex) {
                if (obligatoire) throw new IllegalStateException("Impossible de charger : " + chemin, ex);
                System.err.println("[VS] Sprite illisible : " + chemin);
                return null;
            }
        }

        private static BufferedImage recadrerAlpha(BufferedImage image) {
            if (image == null) return null;
            int g = image.getWidth(), d = -1;
            int h = image.getHeight(), b = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) > 8) {
                        g = Math.min(g, x); d = Math.max(d, x);
                        h = Math.min(h, y); b = Math.max(b, y);
                    }
                }
            }
            if (d < g) return image;
            return image.getSubimage(g, h, d - g + 1, b - h + 1);
        }

        @Override protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = (Graphics2D) source.create();
            try {
                g.scale(getWidth() / (double) LARGEUR,
                        getHeight() / (double) HAUTEUR);
                double t = debutNs == 0L ? 0.0
                        : borne((System.nanoTime() - debutNs) / (double) DUREE_NS);
                dessiner(g, t);
            } finally {
                g.dispose();
            }
        }

        /** t de 0 a 1 : rendu deterministe, meme sans fenetre Swing ouverte. */
        void dessiner(Graphics2D g, double t) {
            t = borne(t);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            dessinerFond(g, t);
            decorerCarte(g, t);
            dessinerBandeau(g, t);

            int[] positions = positionsCartes(noms.length);
            for (int i = 0; i < noms.length; i++) {
                double entree = doux((t - (0.035 + i * 0.038)) / 0.28);
                int glissement = (int) Math.round((1.0 - entree)
                        * (i < 2 ? -260 : 260));
                Graphics2D carteG = (Graphics2D) g.create();
                carteG.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, (float) entree));
                carteG.translate(positions[i] + glissement, 201);
                dessinerCarteJoueur(carteG, i, t);
                carteG.dispose();
            }
            if (noms.length == 3) {
                // Deux mini-VS entre les trois portraits : aucun visage masque.
                dessinerVS(g, t, 425, 411, 0.69, 0.0);
                dessinerVS(g, t, 855, 411, 0.69, 1.0);
            } else {
                // Pour 2 ou 4 joueurs, un seul VS au milieu de l'ecran.
                dessinerVS(g, t, 640, 409, 1.0, 0.0);
            }
            dessinerBas(g, t);

            // Leger fondu noir lorsque le moteur de jeu prend le relais.
            if (t > 0.91) {
                float opacite = (float) doux((t - 0.91) / 0.09);
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, opacite));
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, LARGEUR, HAUTEUR);
                g.setComposite(AlphaComposite.SrcOver);
            }
        }

        /** Coordonnees des cartes : aucun emplacement fictif n'est cree. */
        private static int[] positionsCartes(int nombre) {
            switch (nombre) {
                case 2: return new int[]{231, 811};
                case 3: return new int[]{91, 521, 951};
                case 4: return new int[]{38, 294, 748, 1004};
                default: throw new IllegalArgumentException("Nombre de joueurs invalide");
            }
        }

        private void dessinerFond(Graphics2D g, double t) {
            // Decor de la VRAIE map, conserve sans deformation.
            double base = Math.max(LARGEUR / (double) decor.getWidth(),
                    HAUTEUR / (double) decor.getHeight());
            double zoom = 1.065 + 0.025 * Math.sin(t * Math.PI);
            int w = (int) Math.ceil(decor.getWidth() * base * zoom);
            int h = (int) Math.ceil(decor.getHeight() * base * zoom);
            g.drawImage(decor, (LARGEUR - w) / 2, (HAUTEUR - h) / 2,
                    w, h, null);
            g.setPaint(new GradientPaint(0, 0,
                    alpha(principal, 210), 0, HAUTEUR,
                    new Color(6, 10, 19, 239)));
            g.fillRect(0, 0, LARGEUR, HAUTEUR);
            g.setColor(alpha(secondaire, 25));
            g.fillRoundRect(12, 14, 1256, 732, 24, 24);
            g.setColor(alpha(secondaire, 92));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(12, 14, 1256, 732, 24, 24);
        }

        private void dessinerBandeau(Graphics2D g, double t) {
            // Titre de map en pixels, independant des polices installees.
            g.setPaint(new GradientPaint(0, 25,
                    alpha(principal, 240), 0, 153,
                    new Color(8, 13, 25, 218)));
            g.fillRoundRect(308, 25, 664, 153, 21, 21);
            g.setColor(alpha(secondaire, 197));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(312, 29, 656, 145, 19, 19);
            String titre = nomCarte;
            int pixel = titre.length() > 8 ? 7 : 9;
            int largeur = titre.length() * 6 * pixel - pixel;
            int x = (LARGEUR - largeur) / 2;
            dessinerTextePixel(g, titre, x + 4, 54 + 4, pixel,
                    new Color(5, 7, 12, 222));
            dessinerTextePixel(g, titre, x, 54, pixel, lumiere);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
            g.setColor(alpha(lumiere, 220));
            String ligne = noms.length + " JOUEURS  •  1 ARENE";
            int largeurLigne = g.getFontMetrics().stringWidth(ligne);
            g.drawString(ligne, (LARGEUR - largeurLigne) / 2, 165);
        }

        private void dessinerCarteJoueur(Graphics2D g, int i, double t) {
            final int W = 238, H = 437;
            // Cartes biseautees et individuelles (laissant 216 px au centre).
            Polygon face = new Polygon(
                    new int[]{19, W - 19, W, W, W - 19, 19, 0, 0},
                    new int[]{0, 0, 19, H - 19, H, H, H - 19, 19}, 8);
            g.setColor(new Color(0, 0, 0, 100));
            g.translate(4, 7);
            g.fillPolygon(face);
            g.translate(-4, -7);
            g.setPaint(new GradientPaint(0, 0,
                    new Color(principal.getRed(), principal.getGreen(),
                            principal.getBlue(), 243), 0, H,
                    new Color(6, 13, 22, 251)));
            g.fillPolygon(face);

            g.setColor(alpha(secondaire, 240));
            g.setStroke(new BasicStroke(4));
            g.drawPolygon(face);
            g.setColor(alpha(lumiere, 100));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(11, 33, W - 22, H - 49, 7, 7);

            // Etiquette joueur : texte parfaitement centre et lisible.
            int etiquetteX = 57;
            int etiquetteY = 14;
            int etiquetteW = 124;
            int etiquetteH = 44;
            int tailleEtiquette = 4;

            g.setColor(alpha(secondaire, 225));
            g.fillRoundRect(etiquetteX, etiquetteY,
                    etiquetteW, etiquetteH, 10, 10);
            g.setColor(alpha(lumiere, 170));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(etiquetteX, etiquetteY,
                    etiquetteW, etiquetteH, 10, 10);

            String etiquette = "J" + numerosJoueurs[i];
            int largeurEtiquette = etiquette.length() * 6 * tailleEtiquette
                    - tailleEtiquette;
            // La methode dessinerTextePixel trace les glyphes de y + 2*taille
            // a y + 9*taille : ce decalage centre leur hauteur de 7 pixels.
            int texteX = etiquetteX + (etiquetteW - largeurEtiquette) / 2;
            int texteY = etiquetteY + (etiquetteH - 7 * tailleEtiquette) / 2
                    - 2 * tailleEtiquette;
            dessinerTextePixel(g, etiquette, texteX + 1,
                    texteY + 1, tailleEtiquette, new Color(0, 0, 0, 150));
            dessinerTextePixel(g, etiquette, texteX,
                    texteY, tailleEtiquette, new Color(15, 23, 34));

            // Halo et liseres adaptes a la carte.
            g.setPaint(new GradientPaint(0, 70, alpha(lumiere, 47),
                    0, 340, alpha(lumiere, 2)));
            g.fillRoundRect(18, 72, W - 36, 293, 8, 8);
            g.setColor(alpha(secondaire, 100));
            g.drawLine(21, 362, W - 21, 362);
            double respiration = 1.0 + 0.025 * Math.sin(t * Math.PI * 6 + i);
            dessinerPortrait(g, portraits[i], noms[i], respiration);

            // Nom choisi reellement par le joueur.
            String nom = Normalizer.normalize(noms[i], Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
            int taille = nom.length() > 10 ? 2 : nom.length() > 7 ? 3 : 4;
            int largeur = nom.length() * 6 * taille - taille;
            if (largeur > 202) {
                taille = Math.max(1, (int) (202 / (double) (nom.length() * 6)));
                largeur = nom.length() * 6 * taille - taille;
            }
            dessinerTextePixel(g, nom,
                    (W - largeur) / 2 + 2, 383 + 2, taille,
                    new Color(1, 4, 8));
            dessinerTextePixel(g, nom,
                    (W - largeur) / 2, 383, taille, lumiere);
            g.setColor(alpha(secondaire, 150));
            g.fillRect(25, H - 17, W - 50, 3);
        }

        private void dessinerPortrait(Graphics2D g,
                                     BufferedImage image, String nom,
                                     double echelle) {
            if (image == null) {
                // Affichage lisible si un PNG est absent.
                g.setColor(alpha(lumiere, 90));
                g.fillOval(74, 163, 90, 90);
                g.setColor(lumiere);
                dessinerTextePixel(g, "?", 100, 170, 8, lumiere);
                return;
            }
            double rapport = Math.min(202.0 / image.getWidth(),
                    247.0 / image.getHeight()) * echelle;
            int w = (int) Math.round(image.getWidth() * rapport);
            int h = (int) Math.round(image.getHeight() * rapport);
            int x = (238 - w) / 2;
            int y = 349 - h;
            g.setColor(new Color(0, 0, 0, 65));
            g.fillOval(44, 340, 150, 17);
            // Garde le pixel art net quand la taille est augmentee.
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(image, x, y, w, h, null);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }

        /**
         * VS legerement vivant : respiration, reflets et arc lumineux tournant.
         * Deux emblemes reduits pour une partie a trois joueurs.
         */
        private void dessinerVS(Graphics2D g, double t,
                                int centreX, int centreY,
                                double echelle, double phase) {
            double apparition = doux((t - 0.20 - 0.03 * phase) / 0.22);
            if (apparition <= 0) return;
            Graphics2D v = (Graphics2D) g.create();
            try {
                v.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, (float) apparition));
                double rebondInitial = 0.12 * (1.0 - doux((t - 0.20) / 0.20));
                double respiration = 0.021 * Math.sin(t * Math.PI * 6.0 + phase);
                double taille = echelle * (1.0 + rebondInitial + respiration);
                v.translate(centreX, centreY);
                v.scale(taille, taille);

                double lueur = 0.5 + 0.5 * Math.sin(t * Math.PI * 5.0 + phase);
                v.setColor(alpha(secondaire, (int) (26 + lueur * 24)));
                v.fillOval(-105, -105, 210, 210);
                v.setStroke(new BasicStroke(5f));
                v.setColor(alpha(secondaire, (int) (130 + lueur * 50)));
                v.drawOval(-95, -95, 190, 190);
                v.setStroke(new BasicStroke(2.0f));
                v.setColor(alpha(lumiere, 90));
                v.drawOval(-84, -84, 168, 168);

                // Un bref trait lumineux tourne doucement autour de l'anneau.
                int angle = (int) Math.round(72.0 - t * 220.0 + 38.0 * phase);
                v.setColor(alpha(lumiere, (int) (110 + 65 * lueur)));
                v.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                v.drawArc(-95, -95, 190, 190, angle, 26);
                v.setStroke(new BasicStroke(1f));

                // Petit eclat orbitant, sans masquer le lettrage.
                double a = Math.toRadians(angle + 13);
                int etoileX = (int) Math.round(95 * Math.cos(a));
                int etoileY = (int) Math.round(-95 * Math.sin(a));
                v.setColor(alpha(lumiere, (int) (115 + 75 * lueur)));
                v.fillOval(etoileX - 4, etoileY - 4, 8, 8);

                dessinerTextePixel(v, "VS", -73 + 5, -69 + 5, 13,
                        new Color(3, 8, 17));
                dessinerTextePixel(v, "VS", -73, -69, 13, lumiere);
            } finally {
                v.dispose();
            }
        }

        private void dessinerBas(Graphics2D g, double t) {
            g.setColor(alpha(principal, 215));
            g.fillRoundRect(318, 668, 644, 43, 13, 13);
            g.setColor(alpha(secondaire, 152));
            g.drawRoundRect(318, 668, 644, 43, 13, 13);
            g.setColor(alpha(lumiere, 190));
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
            String phrase = "LE COMBAT VA COMMENCER";
            int sw = g.getFontMetrics().stringWidth(phrase);
            g.drawString(phrase, (LARGEUR - sw) / 2, 695);
            g.setColor(alpha(secondaire, 80));
            g.fillRoundRect(328, 715, 624, 4, 4, 4);
            g.setColor(secondaire);
            g.fillRoundRect(328, 715,
                    (int) Math.round(624 * borne(t)), 4, 4, 4);
        }

        private void decorerCarte(Graphics2D g, double t) {
            switch (carte) {
                case "CHATEAU": decorChateau(g, t); break;
                case "MARCHE": decorMarche(g, t); break;
                case "POTAGER": decorPotager(g, t); break;
                case "COLONIE": decorColonie(g, t); break;
                default: break;
            }
        }

        private void decorChateau(Graphics2D g, double t) {
            // Tours stylisees, bannières royales et petites braises dorees.
            for (int cote : new int[]{0, 1}) {
                int x = cote == 0 ? 19 : 1249;
                g.setColor(new Color(15, 10, 29, 170));
                g.fillRect(x, 130, 13, 445);
                g.setColor(alpha(secondaire, 180));
                g.fillRect(x, 137, 4, 425);
            }
            for (int i = 0; i < 14; i++) {
                int x = 70 + i * 88;
                int y = 165 + (int) (26 * Math.sin(i * 3.7 + t * 9));
                g.setColor(alpha(lumiere, 20 + i % 3 * 20));
                g.fillOval(x, y, 5, 5);
            }
            g.setColor(alpha(secondaire, 85));
            for (int k = 0; k < 8; k++) {
                g.fillRect(40 + k * 148, 170, 70, 3);
            }
        }

        private void decorMarche(Graphics2D g, double t) {
            // Guirlandes de petits fanions et confettis au-dessus des portraits.
            g.setStroke(new BasicStroke(2));
            g.setColor(alpha(lumiere, 165));
            g.drawLine(20, 174, 1260, 174);
            Color[] fanions = {new Color(251, 198, 96),
                new Color(125, 219, 203), new Color(250, 122, 104)};
            for (int i = 0; i < 30; i++) {
                int x = 36 + i * 42;
                int y = 172 + (int) (4 * Math.sin(t * 20 + i));
                g.setColor(alpha(fanions[i % 3], 225));
                Polygon tri = new Polygon(new int[]{x, x + 24, x + 12},
                        new int[]{y, y, y + 19}, 3);
                g.fillPolygon(tri);
                g.setColor(alpha(lumiere, 85));
                g.fillOval((i * 79 + 31) % 1280,
                        150 + (i * 131) % 490, 4, 4);
            }
        }

        private void decorPotager(Graphics2D g, double t) {
            // Tiges et feuilles autour des joueurs, pollen en suspension.
            for (int cote = 0; cote < 2; cote++) {
                int xb = cote == 0 ? 17 : 1262;
                g.setColor(alpha(secondaire, 130));
                g.setStroke(new BasicStroke(5));
                Path2D tige = new Path2D.Double();
                tige.moveTo(xb, 720);
                tige.curveTo(xb + (cote == 0 ? 55 : -55), 575,
                        xb + (cote == 0 ? -20 : 20), 400, xb, 224);
                g.draw(tige);
                for (int i = 0; i < 7; i++) {
                    int y = 254 + i * 68;
                    int dx = (int) (15 * Math.sin(t * 7 + i));
                    int x = xb + dx + (cote == 0 ? 12 : -32);
                    g.setColor(alpha(lumiere, 100 + i % 3 * 25));
                    g.fillOval(x, y, 35, 13);
                }
            }
            for (int i = 0; i < 34; i++) {
                int x = (i * 179 + 13) % 1270;
                int y = (int) ((i * 71 + 440 - 140 * t) % 710);
                g.setColor(alpha(lumiere, 50 + (i % 5) * 14));
                g.fillOval(x, y, 3 + i % 3, 3 + i % 3);
            }
        }

        private void decorColonie(Graphics2D g, double t) {
            // Grille radar et lignes de balayage holographiques.
            g.setColor(alpha(secondaire, 27));
            g.setStroke(new BasicStroke(1));
            for (int x = 25; x <= 1255; x += 46) {
                g.drawLine(x, 165, x, 666);
            }
            for (int y = 180; y <= 659; y += 36) {
                g.drawLine(20, y, 1260, y);
            }
            int ligne = 176 + (int) (470 * t);
            g.setColor(alpha(secondaire, 110));
            g.setStroke(new BasicStroke(3));
            g.drawLine(24, ligne, 1256, ligne);
            for (int k = 0; k < 8; k++) {
                int y = 178 + k * 62;
                g.setColor(alpha(lumiere, 65));
                g.drawLine(18, y, 45, y);
                g.drawLine(1235, y, 1262, y);
            }
        }

        private static void dessinerTextePixel(Graphics2D g, String texte,
                int x, int y, int taille, Color couleur) {
            g.setColor(couleur);
            for (int i = 0; i < texte.length(); i++) {
                char c = texte.charAt(i);
                String decompose = Normalizer.normalize(
                        String.valueOf(c), Normalizer.Form.NFD);
                char base = Character.toUpperCase(decompose.charAt(0));
                int[] lignes = pixelLettre(base);
                int px = x + i * 6 * taille;
                for (int ligne = 0; ligne < 7; ligne++) {
                    for (int col = 0; col < 5; col++) {
                        if ((lignes[ligne] & (1 << (4 - col))) != 0) {
                            g.fillRect(px + col * taille,
                                    y + (ligne + 2) * taille, taille, taille);
                        }
                    }
                }
                if (decompose.indexOf('\u0302') >= 0) {
                    g.fillRect(px + 2 * taille, y, taille, taille);
                    g.fillRect(px + taille, y + taille, taille, taille);
                    g.fillRect(px + 3 * taille, y + taille, taille, taille);
                } else if (decompose.indexOf('\u0301') >= 0) {
                    g.fillRect(px + 3 * taille, y, taille, taille);
                    g.fillRect(px + 2 * taille, y + taille, taille, taille);
                } else if (decompose.indexOf('\u0300') >= 0) {
                    g.fillRect(px + taille, y, taille, taille);
                    g.fillRect(px + 2 * taille, y + taille, taille, taille);
                }
            }
        }

        private static int[] pixelLettre(char c) {
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
                case '?': return new int[]{14,17,1,2,4,0,4};
                default: return new int[]{0,0,0,0,0,0,0};
            }
        }
    }

    /**
     * Test autonome : Run File => 4 joueurs sur Chateau.
     * Arguments optionnels : "2 marche", "3 colonie", "4 potager".
     */
    public static void main(String[] args) {
        int nombre = 4;
        if (args.length > 0 && args[0].matches("[234]")) {
            nombre = Integer.parseInt(args[0]);
        }
        String carte = args.length > 1
                ? "/ressources/Maps/" + args[1].toLowerCase(Locale.ROOT) + ".png"
                : "/ressources/Maps/chateau.png";
        final String chemin = carte;
        final int nombreFinal = nombre;
        SwingUtilities.invokeLater(() -> {
            String[] choix = Arrays.copyOf(
                    new String[]{"Carrot", "Nikku", "Tomato", "Potato"},
                    nombreFinal);
            int[] numeros = Arrays.copyOf(new int[]{1, 2, 3, 4}, nombreFinal);
            new FenetreVS(chemin, choix, numeros,
                    () -> new FenetreDeJeu(chemin).setVisible(true))
                    .setVisible(true);
        });
    }
}