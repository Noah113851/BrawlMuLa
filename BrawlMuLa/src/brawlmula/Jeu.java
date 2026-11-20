package brawlmula;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import static brawlmula.Avatar.HAUTEUR_AVATAR;
import static brawlmula.Avatar.LARGEUR_AVATAR;

/**
 * Physique de l'avatar local et rendu des combattants dans la boucle a 40 ms.
 */
public class Jeu {

    private static final double DISTANCE_TELEPORTATION = 180;

    public final Avatar avatar;
    private final PanneauCombat panneauCombat;
    private final Avatar[] avatars = new Avatar[4];
    private final boolean[] avatarsVisibles = new boolean[avatars.length];
    private final Reseau reseau;
    private final int indexLocal;
    private final JoueurSQL sql;
    private final ModeDeJeu mode;
    private long derniereChute = 0L;

    /**
     * Compatibilite : FenetreDeJeu() continue d'ouvrir le Chateau.
     */
    public Jeu() {
        this("/ressources/Maps/chateau.png");
    }

    /**
     * Une meme physique, mais un decor et des plateformes selon la carte.
     */
    public Jeu(String cheminCarte) {
        this("Carrot", null, null, null, cheminCarte, null, 1);
    }

    public Jeu(String j1, String j2, String j3, String j4,
            String cheminCarte, Reseau reseau, int monNumeroJoueur) {
        if (monNumeroJoueur < 1 || monNumeroJoueur > avatars.length) {
            throw new IllegalArgumentException("Numéro de joueur invalide : " + monNumeroJoueur);
        }
        this.reseau = reseau;
        indexLocal = monNumeroJoueur - 1;
        String[] personnages = {j1, j2, j3, j4};
        for (int index = 0; index < avatars.length; index++) {
            if (reseau != null || index == indexLocal) {
                avatars[index] = new Avatar(personnages[index]);
            }
        }
        avatarsVisibles[indexLocal] = true;
        panneauCombat = new PanneauCombat(cheminCarte, avatars, avatarsVisibles);
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);
       sql = new JoueurSQL();
mode = ModeDeJeu.courant;
for (int i = 1; i <= mode.getNbJoueurs(); i++) {
    sql.initialiserPv(i);
}
panneauCombat.setHud(new BarreVieHUD(sql, mode));

        avatar = avatars[indexLocal];
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        placerAvatarSurPlateformePrincipale();
        if (reseau != null) {
            reseau.demarrerSynchronisation(monNumeroJoueur, (int) avatar.x, (int) avatar.y);

            reseau.demarrerEcouteAttaques(numero -> {

                int index = numero - 1;

                if (index >= 0
                        && index < avatars.length
                        && index != indexLocal
                        && avatars[index] != null) {

                    avatars[index].afficherAttaqueDistante();
                }
            });
            reseau.demarrerEcouteVer(msg -> {
                String[] parts = msg.split(":");
                if (parts.length == 3 && "VER_SPAWN".equals(parts[0])) {
                    if (indexLocal != 0) {
                        int vx = Integer.parseInt(parts[1]);
                        int vy = Integer.parseInt(parts[2]);
                        panneauCombat.getVerDeTerre().synchroniser(true, vx, vy);
                    }
                } else if (parts.length == 2 && "VER_PRIS".equals(parts[0])) {
                    if (indexLocal != 0) {
                        int idx = Integer.parseInt(parts[1]) - 1;
                        panneauCombat.getVerDeTerre().ramasser();
                        if (idx >= 0 && idx < avatars.length && avatars[idx] != null) {
                            avatars[idx].activerAttaqueSpeciale();
                        }
                    }
                } else if (parts.length == 2 && "VER_UTILISE".equals(parts[0])) {
                    int idx = Integer.parseInt(parts[1]) - 1;
                    if (idx != indexLocal && idx >= 0 && idx < avatars.length && avatars[idx] != null) {
                        avatars[idx].setAttaqueSpecialeActive(false);
                    }
                }
            });
        }
    }

    /**
     * Apparition initiale sur le support principal.
     */
    private void placerAvatarSurPlateformePrincipale() {
        if (reseau != null || estCarteFlottante()) {
            Plateforme principale = chercherPlateformePrincipale();
            double fraction = reseau == null ? 0.5 : (indexLocal + 0.5) / avatars.length;
            avatar.x = principale.getX() + principale.getLargeur() * fraction - LARGEUR_AVATAR / 2.0;
            avatar.y = principale.getY() - HAUTEUR_AVATAR;
            return;
        }
        // Conserver l'apparition du Chateau et des autres cartes.
        double solLePlusHaut = Double.POSITIVE_INFINITY;
        double avatarX = avatar.x;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && avatarX + LARGEUR_AVATAR > p.getX()
                    && avatarX < p.getX() + p.getLargeur()) {
                solLePlusHaut = Math.min(solLePlusHaut, p.getY());
            }
        }
        if (Double.isFinite(solLePlusHaut)) {
            avatar.y = solLePlusHaut - HAUTEUR_AVATAR;
        }
    }

    /**
     * Le Marche, le Potager et la Colonie ont une plateforme principale
     * flottante.
     */
    private boolean estCarteFlottante() {
        String nom = panneauCombat.getConfiguration().getNom();
        return "MARCHE".equals(nom)
                || "POTAGER".equals(nom)
                || "COLONIE".equals(nom);
    }

    /**
     * Plus grande plateforme solide de la carte.
     */
    private Plateforme chercherPlateformePrincipale() {
        Plateforme principale = null;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && (principale == null || p.getLargeur() > principale.getLargeur())) {
                principale = p;
            }
        }
        if (principale == null) {
            throw new IllegalStateException(
                    "Aucune plateforme principale solide sur cette carte.");
        }
        return principale;
    }

    public void miseAJour() {
        // Un seul Timer pilote les plateformes et le personnage local (40 ms par tick).
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        avatar.miseAJour();

        // Sur les cartes flottantes, remettre l'avatar sur le plateau principal.
        if (avatar.consommerChute()) {
    long maintenant = System.currentTimeMillis();
    if (maintenant - derniereChute > 1500) {
        sql.retirerPv(indexLocal + 1, 15);
        derniereChute = maintenant;
    }
    Plateforme principale = chercherPlateformePrincipale();
    avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
    avatar.reapparaitreSur(
            principale.getX() + principale.getLargeur() / 2.0 - LARGEUR_AVATAR / 2.0,
            principale.getY() - HAUTEUR_AVATAR - 30);
}

        if (reseau != null) {
            reseau.envoyerMaPosition((int) avatar.x, (int) avatar.y);
            actualiserAvatarsDistants();
        }
        VerDeTerre ver = panneauCombat.getVerDeTerre();
        if (reseau == null || indexLocal == 0) {
            // 1. Apparition aléatoire sur une plateforme active toutes les 12s
            if (!ver.isActif() && (System.currentTimeMillis() - dernierSpawnVer >= DELAI_SPAWN_MS)) {
                ver.apparaitreAleatoirement(panneauCombat.getGestionnairePlateformes().getPlateformes());
                dernierSpawnVer = System.currentTimeMillis();
            }

            // 2. Vérification du ramassage par l'un des 4 joueurs
            if (ver.isActif()) {
                for (int i = 0; i < avatars.length; i++) {
                    Avatar joueur = avatars[i];
                    if (joueur != null && avatarsVisibles[i]) {
                        if (ver.getHitbox().intersects(joueur.x, joueur.y, LARGEUR_AVATAR, HAUTEUR_AVATAR)) {
                            joueur.activerAttaqueSpeciale();
                            ver.ramasser();
                            dernierSpawnVer = System.currentTimeMillis();
                            break;
                        }
                    }
                }
            }
        }
    }

    private void actualiserAvatarsDistants() {

        for (Reseau.JoueurData etat
                : reseau.lireTousLesJoueurs()) {

            int index = etat.emplacement() - 1;

            if (index == indexLocal
                    || index < 0
                    || index >= avatars.length) {
                continue;
            }

            Avatar distant = avatars[index];

            if (distant == null) {
                continue;
            }

            if (!avatarsVisibles[index]) {

                distant.x = etat.x();
                distant.y = etat.y();

                distant.actualiserAnimationDistante(0, 0);

                avatarsVisibles[index] = true;
                continue;
            }

            double ancienX = distant.x;
            double ancienY = distant.y;

            double dx = etat.x() - distant.x;
            double dy = etat.y() - distant.y;

            double distance = Math.hypot(dx, dy);

            double fraction
                    = distance < 1
                    || distance >= DISTANCE_TELEPORTATION
                            ? 1.0 : 0.5;

            distant.x += fraction * dx;
            distant.y += fraction * dy;

            distant.actualiserAnimationDistante(
                    distant.x - ancienX,
                    distant.y - ancienY
            );
        }
    }

    public void rendu(Graphics2D contexte) {
        panneauCombat.paint(contexte);
    }

   public void fermer() {
    if (reseau != null) {
        reseau.fermer();
    }
    sql.closeTable();
}

    public PanneauCombat getPanneauCombat() {
        return panneauCombat;
    }

public void attaquer() {

    if (avatar.attaquer()) {

        // Synchroniser l'attaque avec les autres joueurs
        if (reseau != null) {
            reseau.envoyerAttaque();
        }

        // Détection des adversaires touchés
        Rectangle zone = avatar.getZoneAttaque();
        for (int i = 0; i < avatars.length; i++) {
            if (i == indexLocal || avatars[i] == null || !avatarsVisibles[i]) {
                continue;
            }
            // pas de tir ami en 2v2
            if (mode.parEquipes() && mode.getEquipe(i) == mode.getEquipe(indexLocal)) {
                continue;
            }
            if (zone.intersects(avatars[i].getHitbox())) {
                sql.retirerPv(i + 1, sql.lireDegats(indexLocal + 1));
            }
        }
    }
}

    public void gererToucheAppuyee(int codeTouche) {

        // Conserver uniquement les déplacements et le saut
        avatar.gererToucheAppuyee(codeTouche);
    }

    public void gererToucheRelachee(int codeTouche) {
        avatar.gererToucheRelachee(codeTouche);
    }
    private final VerDeTerre verDeTerre = new VerDeTerre();
    private long dernierSpawnVer = System.currentTimeMillis();
    private static final long DELAI_SPAWN_MS = 12000; // Spawn toutes les 12 secondes

    public VerDeTerre getVerDeTerre() {
        return verDeTerre;
    }
}


//package brawlmula;
//
//import java.awt.Color;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import javax.imageio.ImageIO;
//
//public class Jeu {
//
//    public Avatar avatar;
//    private BufferedImage fond;
//
//    public Jeu(String nomPerso, String cheminCarte) {
//        try {
//            fond = ImageIO.read(getClass().getResource(cheminCarte));
//        } catch (Exception ex) {
//            System.err.println("Fond introuvable : " + cheminCarte);
//        }
//        avatar = new Avatar(nomPerso, 100);
//    }
//
//    public void miseAJour() {
//        avatar.miseAJour();
//    }
//
//    public void rendu(Graphics2D g) {
//        if (fond != null) {
//            g.drawImage(fond, 0, 0, 960, 540, null);
//        } else {
//            g.setColor(Color.BLACK);
//            g.fillRect(0, 0, 960, 540);
//        }
//        avatar.rendu(g);
//    }
//}