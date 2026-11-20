package brawlmula;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Intermediaire entre le jeu et la table Joueur.
 *
 * La table contient 4 lignes FIXES (une par place, emplacement 1 a 4).
 * Une place libre porte le pseudo "ChoixJ1", "ChoixJ2", ...
 * - rejoindre la partie  = remplacer "ChoixJn" par son pseudo (UPDATE)
 * - quitter la partie    = remettre "ChoixJn" (UPDATE)
 * Ainsi, il ne peut jamais y avoir plus de 4 joueurs.
 */
public class JoueurSQL {

    private static final int NB_PLACES = 4;

    private String adresseBase;
    private String user;
    private String motdepasse;
    private Connection connexion;   // ouverte UNE fois, fermee a la fin du jeu

    public JoueurSQL() {
        this.adresseBase = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
        this.user = "etudiant";
        this.motdepasse = "YTDTvj9TR3CDYCmP";   // <-- garde ta valeur actuelle

        try {
            this.connexion = DriverManager.getConnection(this.adresseBase, this.user, this.motdepasse);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    /** Pseudo qu'a une place quand elle est libre. */
    private String nomLibre(int emplacement) {
        return "ChoixJ" + emplacement;
    }

    /** Ecrit l'id d'avatar, ou NULL si le joueur n'a pas encore choisi (id <= 0). */
    private void definirAvatar(PreparedStatement requete, int index, int idAvatar) throws SQLException {
        if (idAvatar > 0) {
            requete.setInt(index, idAvatar);
        } else {
            requete.setNull(index, Types.INTEGER);
        }
    }

    /** Transforme la ligne courante d'un ResultSet en objet Joueur. */
    private Joueur extraireJoueur(ResultSet rs) throws SQLException {
        return new Joueur(
                rs.getString("pseudo"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("score"),
                rs.getInt("emplacement"),
                rs.getInt("id_avatar"));     // NULL en base -> 0 en Java
    }

    private int compter(String sql) {
        try (Statement s = connexion.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Les 4 actions de base (CRUD)
    // ------------------------------------------------------------------

    /**
     * "Cree" le joueur : prend SA place, a condition qu'elle soit encore libre.
     * Une seule requete UPDATE : si deux joueurs visent la meme place en meme temps,
     * un seul obtient true.
     */
    public boolean creerJoueur(Joueur J) {
        String sql = "UPDATE Joueur SET pseudo = ?, x = ?, y = ?, score = ?, id_avatar = ? "
                   + "WHERE emplacement = ? AND pseudo = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, J.getPseudo());
            requete.setInt(2, J.getX());
            requete.setInt(3, J.getY());
            requete.setInt(4, J.getScore());
            definirAvatar(requete, 5, J.getIdAvatar());
            requete.setInt(6, J.getEmplacement());
            requete.setString(7, nomLibre(J.getEmplacement()));

            int nombreDeModifs = requete.executeUpdate();
            System.out.println(nombreDeModifs + " enregistrement(s) modifie(s)");
            return nombreDeModifs == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Met a jour les infos du joueur (position, score, avatar, pret). */
    public void modifierJoueur(Joueur J) {
        String sql = "UPDATE Joueur SET x = ?, y = ?, score = ?, id_avatar = ?"
                   + "WHERE emplacement = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, J.getX());
            requete.setInt(2, J.getY());
            requete.setInt(3, J.getScore());
            definirAvatar(requete, 4, J.getIdAvatar());
            requete.setInt(6, J.getEmplacement());
            requete.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /** Libere la place du joueur (la ligne est remise a zero, pas supprimee). */
    public void supprimerJoueur(Joueur J) {
        String sql = "UPDATE Joueur SET pseudo = ?, x = 0, y = 0, score = 0, id_avatar = NULL, pret = 0 "
                   + "WHERE emplacement = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, nomLibre(J.getEmplacement()));
            requete.setInt(2, J.getEmplacement());
            requete.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /** Affiche dans la console les infos du joueur telles qu'elles sont en base. */
    public void voirJoueur(Joueur J) {
        Joueur enBase = lireJoueur(J.getEmplacement());
        if (enBase != null) {
            System.out.println(enBase);
        } else {
            System.out.println("Aucun joueur a l'emplacement " + J.getEmplacement());
        }
    }

    // ------------------------------------------------------------------
    // Methodes utiles pour le jeu multijoueur
    // ------------------------------------------------------------------

    /**
     * Cherche une place libre et la prend.
     * @return le Joueur cree (avec son emplacement), o
     * @param pseudou null si la partie est pleine.
     */
    public Joueur rejoindrePartie(String pseudo, int idAvatar) {
        for (int place = 1; place <= NB_PLACES; place++) {
            Joueur candidat = new Joueur(pseudo, 0, 0, 0, place, idAvatar);
            if (creerJoueur(candidat)) {
                return candidat;
            }
        }
        return null;
    }

    /** Lit un joueur par sa place (1 a 4), ou null s'il n'existe pas. */
    public Joueur lireJoueur(int emplacement) {
        String sql = "SELECT emplacement, pseudo, x, y, score, id_avatar, pret "
                   + "FROM Joueur WHERE emplacement = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, emplacement);
            try (ResultSet rs = requete.executeQuery()) {
                if (rs.next()) {
                    return extraireJoueur(rs);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /** Lit tous les joueurs presents (les places libres sont ignorees). */
    public List<Joueur> lireTousLesJoueurs() {
        List<Joueur> liste = new ArrayList<>();
        String sql = "SELECT emplacement, pseudo, x, y, score, id_avatar, pret FROM Joueur "
                   + "WHERE pseudo <> CONCAT('ChoixJ', emplacement) ORDER BY emplacement";
        try (PreparedStatement requete = connexion.prepareStatement(sql);
             ResultSet rs = requete.executeQuery()) {
            while (rs.next()) {
                liste.add(extraireJoueur(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return liste;
    }

    /** Nombre de places occupees (0 a 4). */
    public int nombreJoueursConnectes() {
        return compter("SELECT COUNT(*) FROM Joueur WHERE pseudo <> CONCAT('ChoixJ', emplacement)");
    }

    /** Nombre de joueurs qui ont valide leur choix. Quand il vaut 4, on lance la partie. */
    public int nombreJoueursPrets() {
        return compter("SELECT COUNT(*) FROM Joueur WHERE pret = 1");
    }

    /** Ferme la connexion : a appeler une seule fois, quand le jeu est termine. */
    public void closeTable() {
        try {
            if (this.connexion != null) {
                this.connexion.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
