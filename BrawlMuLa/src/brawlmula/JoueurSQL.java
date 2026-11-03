/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

/**
 *
 * @author abriton
 */
import java.sql.*;

public class JoueurSQL {

    //Ok ! L'idée c'est que dans cette classe, tu implémentes TOUTES les actions posible avec la Table Joueur (sur le serveur distant)
    //Pour faire ça, déjà tu as besoin de pouvoir te connecter à la base de donnée, c'est pourquoi c'est judicieux de les mettre en 
    //attributs les choses dont t'as besoin pour te connecter.
    private String adresseBase;
    private String user;
    private String motdepasse;
    private Connection connexion; //lui c'est l'état de la connexion, autant en faire aussi un attribut.

    //Ici, on fait un constructeur qui va juste initialiser l'intermédiaire SQL
    public JoueurSQL() {
        this.adresseBase = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
        this.user = "etudiant";
        this.motdepasse = "YTDTvj9TR3CDYCmP";

        //Vous avez vu que, avant de faire une requête, il fallait se connecter à la BD, ce que je te propose c'est de te connecter/déco UNE seule fois, et pas à 
        //chaque fois que tu fais une requête : La connection à la BD prend du TEMPS, si tu fais plusieurs co/déco, ça va être long :)
        try {

            this.connexion = DriverManager.getConnection(this.adresseBase, this.user, this.motdepasse);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    //Je t'ai mis ici les 4 méthodes qui vont être importantes à coder, à toi de fustionner ça avec les bouts de code dans tes tests : 
    public void creerJoueur(Joueur J) {
        String sql = "INSERT INTO Joueur (nom, positionX, positionY, score) VALUES (?, ?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, J.getNom());
            requete.setInt(2, J.getPositionX());
            requete.setInt(3, J.getPositionY());
            requete.setInt(4, J.getScore());

            int nombreDAjouts = requete.executeUpdate();
            System.out.println("✅ " + nombreDAjouts + " joueur(s) ajouté(s).");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void modifierJoueur(Joueur J) {
        String sql = "UPDATE Joueur SET positionX = ?, positionY = ?, score = ? WHERE nom = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, J.getPositionX());
            requete.setInt(2, J.getPositionY());
            requete.setInt(3, J.getScore());
            requete.setString(4, J.getNom());

            int modifs = requete.executeUpdate();
            System.out.println("✅ " + modifs + " joueur(s) mis à jour.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void supprimerJoueur(Joueur J) {
        String sql = "DELETE FROM Joueur WHERE nom = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, J.getNom());

            int suppr = requete.executeUpdate();
            System.out.println("✅ " + suppr + " joueur(s) supprimé(s).");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void voirJoueur(Joueur J) {
        String sql = "SELECT * FROM Joueur WHERE nom = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, J.getNom());

            try (ResultSet resultat = requete.executeQuery()) {
                while (resultat.next()) {
                    System.out.println("Joueur : " + resultat.getString("nom")
                            + " | X: " + resultat.getInt("positionX")
                            + " | Y: " + resultat.getInt("positionY")
                            + " | Score: " + resultat.getInt("score"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void closeTable() {
        try {
            if (this.connexion != null && !this.connexion.isClosed()) {
                this.connexion.close();
                System.out.println("✅ Connexion à la BDD fermée proprement.");
            }
        } catch (SQLException ex) {
            System.err.println("❌ Erreur lors de la fermeture de la connexion :");
            ex.printStackTrace();
        }
    }
}

//Si tu as une autre table, tu dois créer une autre classe similaire à celle-ci ! A présent, ton collègue qui travaille sur le moteur pourra
   //facilement utiliser tes méthodes pour mettre à jour la BDD ! En utilisant les méthodes que tu as crée pour lui :)
