package brawlmula;

import java.awt.Graphics2D;
import java.util.List;

/**
 * Moteur de jeu gérant les 4 combattants simultanément sur le terrain.
 */
public class Jeu {

    // Tableau des 4 combattants présentés à l'écran
    public Avatar[] joueurs = new Avatar[4];
    public Avatar avatar; // Référence vers l'avatar local contrôlé par ce PC

    private final PanneauCombat panneauCombat;
    private static final double HAUTEUR_AVATAR = 100;
    private static final double LARGEUR_AVATAR = 100;

    private Reseau reseau;
    private int monNumeroJoueur = 1; // Slot attribué (1, 2, 3 ou 4)

    /** Constructeur par défaut (Château solo) */
    public Jeu() {
        this("/ressources/Maps/chateau.png");
    }

    /** Constructeur carte solo */
    public Jeu(String cheminCarte) {
        this("Carrot", "Nikku", "Tomato", "Potato", cheminCarte, null, 1);
    }

    /** Constructeur multijoueur complet appelé par FenetreDeJeu */
    public Jeu(String j1, String j2, String j3, String j4, String cheminCarte, Reseau reseau, int monNumeroJoueur) {
        this.reseau = reseau;
        this.monNumeroJoueur = monNumeroJoueur;

        panneauCombat = new PanneauCombat(cheminCarte);
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        String[] nomsAvatars = {j1, j2, j3, j4};

        // 1. Instanciation des 4 avatars avec leurs visuels respectifs
        for (int i = 0; i < 4; i++) {
            // Tente d'instancier avec le nom du personnage (sinon constructeur par défaut)
            try {
                joueurs[i] = new Avatar(nomsAvatars[i]);
            } catch (Exception e) {
                joueurs[i] = new Avatar();
            }
            joueurs[i].connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        }

        // 2. Définition de l'avatar local (contrôlé par le clavier de cette machine)
        int indexLocal = Math.max(0, Math.min(3, monNumeroJoueur - 1));
        avatar = joueurs[indexLocal];

        // 3. Placement initial des 4 combattants sur l'arène
        placerTousLesJoueurs();
    }

    private void placerTousLesJoueurs() {
        int[][] posInitiales = {{250, 450}, {950, 450}, {450, 450}, {750, 450}};
        for (int i = 0; i < 4; i++) {
            if (joueurs[i] != null) {
                joueurs[i].x = posInitiales[i][0];
                joueurs[i].y = posInitiales[i][1];
            }
        }
        if (avatar != null) {
            placerAvatarSurPlateformePrincipale(avatar);
        }
    }

    private void placerAvatarSurPlateformePrincipale(Avatar target) {
        if (target == null) return;
        boolean carteFlottante = estCarteFlottante();
        if (carteFlottante) {
            Plateforme principale = chercherPlateformePrincipaleFlottante();
            target.x = principale.getX() + (principale.getLargeur() - LARGEUR_AVATAR) / 2.0;
            target.y = principale.getY() - HAUTEUR_AVATAR;
            return;
        }
        double solLePlusHaut = Double.POSITIVE_INFINITY;
        double avatarX = target.x;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && avatarX + LARGEUR_AVATAR > p.getX()
                    && avatarX < p.getX() + p.getLargeur()) {
                solLePlusHaut = Math.min(solLePlusHaut, p.getY());
            }
        }
        if (Double.isFinite(solLePlusHaut)) {
            target.y = solLePlusHaut - HAUTEUR_AVATAR;
        }
    }

    private boolean estCarteFlottante() {
        String nom = panneauCombat.getConfiguration().getNom();
        return "MARCHE".equals(nom) || "POTAGER".equals(nom) || "COLONIE".equals(nom);
    }

    private Plateforme chercherPlateformePrincipaleFlottante() {
        Plateforme principale = null;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && (principale == null || p.getLargeur() > principale.getLargeur())) {
                principale = p;
            }
        }
        if (principale == null) {
            throw new IllegalStateException("Aucune plateforme principale solide sur cette carte.");
        }
        return principale;
    }

    /**
     * Boucle d'actualisation (Timer 40 ms)
     */
    public void miseAJour() {
        // A. Physique et déplacement du joueur local
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        if (avatar != null) {
            avatar.miseAJour();

            if (estCarteFlottante() && avatar.y > ConfigurationCarte.HAUTEUR) {
                reapparaitreSurPlateformePrincipale(avatar);
            }
        }

        // B. Échange de positions multijoueur via la BDD SQL
        if (reseau != null && avatar != null) {
            // 1. Envoyer la position (x, y) de son avatar local vers la BDD
            reseau.envoyerMaPosition(monNumeroJoueur, (int) avatar.x, (int) avatar.y);

            // 2. Récupérer et appliquer la position des 3 autres joueurs distants
            List<Reseau.JoueurData> etatsBDD = reseau.lireTousLesJoueurs();
            for (Reseau.JoueurData data : etatsBDD) {
                if (data.pseudo != null && data.pseudo.startsWith("ChoixJ")) {
                    try {
                        int numJ = Integer.parseInt(data.pseudo.replace("ChoixJ", ""));
                        int idx = numJ - 1;
                        
                        // Mettre à jour uniquement les joueurs distants (pas soi-même)
                        if (idx >= 0 && idx < 4 && idx != (monNumeroJoueur - 1)) {
                            if (joueurs[idx] != null) {
                                joueurs[idx].x = data.x;
                                joueurs[idx].y = data.y;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer si format d'entrée incorrect
                    }
                }
            }
        }
    }

    private void reapparaitreSurPlateformePrincipale(Avatar target) {
        if (target == null) return;
        Plateforme principale = chercherPlateformePrincipaleFlottante();
        target.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        target.x = principale.getX() + (principale.getLargeur() - LARGEUR_AVATAR) / 2.0;
        target.y = principale.getY() - HAUTEUR_AVATAR;
    }

    /**
     * Rendus graphiques : dessine le décor ET les 4 combattants
     */
    public void rendu(Graphics2D contexte) {
        // 1. Dessiner la carte et les plateformes
        panneauCombat.paint(contexte);

        // 2. Dessiner les 4 avatars sur le terrain
        for (int i = 0; i < 4; i++) {
            if (joueurs[i] != null) {
                joueurs[i].rendu(contexte);
            }
        }
    }

    public PanneauCombat getPanneauCombat() {
        return panneauCombat;
    }

    public void gererToucheAppuyee(int codeTouche) {
        if (avatar != null) {
            try { avatar.gererToucheAppuyee(codeTouche); } catch (Exception ignored) {}
        }
    }

    public void gererToucheRelachee(int codeTouche) {
        if (avatar != null) {
            try { avatar.gererToucheRelachee(codeTouche); } catch (Exception ignored) {}
        }
    }
}