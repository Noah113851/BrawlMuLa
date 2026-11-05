/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

public class Reseau {

    private static final String URL = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
    private static final String USER = "etudiant";
    private static final String PASSWORD = "YTDTvj9TR3CDYCmP";

    private final String role; // "HOST" ou "CLIENT"
    private int dernierIdLu = 0;
    private boolean enEcoute = false;

    // Constructeur pour l'Hôte
    public Reseau(int port) {
        this.role = "HOST";
        viderSalon(); // Nettoie les anciens messages de la session précédente
    }

    // Constructeur pour le Client
    public Reseau(String ip, int port) {
        this.role = "CLIENT";
        // Synchroniser au dernier ID existant pour ne pas relire les anciens messages
        this.dernierIdLu = getDernierIdActuel();
    }

    private Connection getConnexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Nettoyer la table lors de la création d'une partie
    private void viderSalon() {
        try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("DELETE FROM reseau_salon")) {
            st.executeUpdate();
            System.out.println("🧹 Salon réseau réinitialisé sur la BDD.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getDernierIdActuel() {
        try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("SELECT MAX(id) FROM reseau_salon"); ResultSet rs = st.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Envoyer un message dans le salon SQL
    public void envoyerMessage(String msg) {
        new Thread(() -> {
            try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("INSERT INTO reseau_salon (emetteur, message) VALUES (?, ?)")) {
                st.setString(1, role);
                st.setString(2, msg);
                st.executeUpdate();
            } catch (SQLException e) {
                System.err.println("❌ Erreur envoi réseau BDD : " + e.getMessage());
            }
        }).start();
    }

    // Attente simulée du client (pour garder la comptabilité avec Selection.java)
    public void attendreClient() {
        System.out.println("✅ Prêt à recevoir le Joueur 2 via BDD.");
    }

    // Boucle d'écoute des nouveaux messages (exécutée en tâche de fond)
    public void demarrerEcoute(Consumer<String> gestionnaireMessage) {
        enEcoute = true;
        new Thread(() -> {
            while (enEcoute) {
                try {
                    Thread.sleep(150); // Vérification toutes les 150ms

                    try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("SELECT id, emetteur, message FROM reseau_salon WHERE id > ? ORDER BY id ASC")) {
                        st.setInt(1, dernierIdLu);
                        ResultSet rs = st.executeQuery();

                        while (rs.next()) {
                            int id = rs.getInt("id");
                            String emetteur = rs.getString("emetteur");
                            String message = rs.getString("message");

                            this.dernierIdLu = id;

                            // Ne traiter que les messages envoyés par L'AUTRE joueur
                            if (!role.equals(emetteur)) {
                                java.awt.EventQueue.invokeLater(() -> gestionnaireMessage.accept(message));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur écoute réseau BDD : " + e.getMessage());
                }
            }
        }).start();
    }

    public void fermer() {
        enEcoute = false;
    }
}
