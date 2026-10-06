/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sql;

/**
 *
 * @author abriton
 */

import java.sql.*;
import outils.OutilsJDBC;
import moteur.Joueur;

public class JoueurSQL {
    
    //Ok ! L'idée c'est que dans cette classe, tu implémentes TOUTES les actions posible avec la Table Joueur (sur le serveur distant)
    //Pour faire ça, déjà tu as besoin de pouvoir te connecter à la base de donnée, c'est pourquoi c'est judicieux de les mettre en 
    //attributs les choses dont t'as besoin pour te connecter.
    private String adresseBase;
    private String user;
    private String motdepasse;
    private Connection connexion; //lui c'est l'état de la connexion, autant en faire aussi un attribut.
    
    
    //Ici, on fait un constructeur qui va juste initialiser l'intermédiaire SQL
    public JoueurSQL(){
        this.adresseBase = "jdbc:mariadb://nemrod.ens2m.fr:3306/2024_2025_s1_vs2_tp2_fish_up";
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
    public void creerJoueur(Joueur J){
       //Voilà un exemple (va utiliser INSERT dans sa requête SQL), admettons qu'on a un Joueur J caractérisé par : son nom, son score, sa position X, sa position Y. On va ajouter cerre
	//ligne à notre table JOUEUR !
        try {
            PreparedStatement requete = connexion.prepareStatement("INSERT INTO Joueur VALUES (?, ?, ?, ?)");
            requete.setString(1, J.getNom());
            requete.setInt(2, J.getScore());
            requete.setInt(3, J.getPositionX());
            requete.setBoolean(4, getPositionY());
            System.out.println(requete);
            int nombreDAjouts = requete.executeUpdate();
            System.out.println(nombreDAjouts + " enregistrement(s) ajoute(s)");

            requete.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }
    
     public void modifierJoueur(Joueur J){
       
        try {

            //TODO (va utiliser UPDATE dans sa requête SQL)
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
     
     public void supprimerJoueur(Joueur J){
       
        try {

            //TODO (va utiliser DELETE dans sa requête SQL)

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
     
     public void voirJoueur(Joueur J){
       //TODO (va utiliser SELECT dans sa requête SQL)
       //Un autre exemple car je suis gentille. Là je récupère toutes les infos du joueur J, de nom J.getNom()
        try {

            PreparedStatement requete = connexion.prepareStatement("SELECT * FROM Joueur WHERE nom = ?");
            requete.setString(1, J.getNom());
            System.out.println(requete);
            ResultSet resultat = requete.executeQuery();
            OutilsJDBC.afficherResultSet(resultat);

            requete.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }
    
    public void closeTable(){
       //On a lancé la connexion dans le Constructeur, il faut fermer donc la connexion quand tout est fini. Dans le jeu, il y a de fortes chance que tu le fasses quand tu supprimes tes joueurs
	// à priori quand le jeu est terminé. 
        try {

            this.connexion.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }
    
}
    
   //Si tu as une autre table, tu dois créer une autre classe similaire à celle-ci ! A présent, ton collègue qui travaille sur le moteur pourra
   //facilement utiliser tes méthodes pour mettre à jour la BDD ! En utilisant les méthodes que tu as crée pour lui :)
