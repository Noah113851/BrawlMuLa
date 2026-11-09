/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Reseau {

    private static final String URL = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
    private static final String USER = "etudiant";
    private static final String PASSWORD = "YTDTvj9TR3CDYCmP";

    static final String DEBUT_COMBAT = "START_GAME";
    private static final int POSITION_INACTIVE = Avatar.LIMITE_CHUTE + 1;
    private static final int INTERVALLE_COMBAT_MS = 40;
    private static final String LIGNES_JOUEURS =
            "pseudo IN ('ChoixJ1', 'ChoixJ2', 'ChoixJ3', 'ChoixJ4')";
    private static final String COMBAT_ACTIF =
            "EXISTS (SELECT 1 FROM reseau_salon WHERE id = ? AND message = 'START_GAME')";

    private final ExecutorService messagesSortants = Executors.newSingleThreadExecutor(tache -> {
        Thread thread = new Thread(tache, "brawlmula-envoi-salon");
        thread.setDaemon(true);
        return thread;
    });
    private String monIdJoueur;
    private int idSalon;
    private volatile int idCombat;
    private volatile boolean ferme;
    private Thread ecouteSalon;
    private Thread synchronisationCombat;
    private volatile Position maPosition;
    private volatile List<JoueurData> positionsJoueurs = List.of();

    // Constructeur Hôte (Joueur 1)
    public Reseau(int port) {
        this.monIdJoueur = "P1";
    }

    // Constructeur Client (Joueurs 2, 3, 4)
    public Reseau(String ip, int port) {
        this.monIdJoueur = "P2";
    }

    private Connection getConnexion() throws SQLException {
        Properties options = new Properties();
        options.setProperty("user", USER);
        options.setProperty("password", PASSWORD);
        options.setProperty("connectTimeout", "2000");
        options.setProperty("socketTimeout", "2000");
        return DriverManager.getConnection(URL, options);
    }

    public void enregistrerHote() throws SQLException {
        try (Connection c = getConnexion();
             PreparedStatement st = c.prepareStatement("DELETE FROM reseau_salon")) {
            c.setAutoCommit(false);
            try {
                st.executeUpdate();
                insererMessage(c, "PRESENCE_P1");
                idSalon = verrouillerSalon(c);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public int inscrireClientEtObtenirNumero() throws SQLException {
        try (Connection c = getConnexion()) {
            c.setAutoCommit(false);
            try {
                idSalon = verrouillerSalon(c);
                verifierSalonOuvert(c);
                try (PreparedStatement st = c.prepareStatement(
                        "SELECT COUNT(*) FROM reseau_salon WHERE message LIKE 'PRESENCE_P%'");
                     ResultSet rs = st.executeQuery()) {
                    rs.next();
                    int numero = rs.getInt(1) + 1;
                    if (numero > 4) {
                        throw new SQLException("Le salon est complet.");
                    }
                    monIdJoueur = "P" + numero;
                    insererMessage(c, "PRESENCE_" + monIdJoueur);
                    c.commit();
                    return numero;
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // Sérialise les inscriptions, les choix et le départ sur la ligne de l'hôte.
    private int verrouillerSalon(Connection c) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "SELECT id FROM reseau_salon WHERE emetteur = 'P1' AND message = 'PRESENCE_P1' FOR UPDATE");
             ResultSet rs = st.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("Aucun salon disponible.");
            }
            return rs.getInt("id");
        }
    }

    private void verifierSalonOuvert(Connection c) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "SELECT id FROM reseau_salon WHERE message = 'START_GAME'");
             ResultSet rs = st.executeQuery()) {
            if (rs.next()) {
                throw new SQLException("Le combat a déjà commencé.");
            }
        }
    }

    private void insererMessage(Connection c, String msg) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "INSERT INTO reseau_salon (emetteur, message) VALUES (?, ?)")) {
            st.setString(1, monIdJoueur);
            st.setString(2, msg);
            st.executeUpdate();
        }
    }

    public void envoyerMessage(String msg) {
        messagesSortants.execute(() -> {
            try {
                publierMessage(msg);
            } catch (SQLException e) {
                System.err.println("Erreur envoi réseau BDD : " + e.getMessage());
            }
        });
    }

    public void lancerCombat(Consumer<String> signalerErreur) {
        messagesSortants.execute(() -> {
            try {
                publierMessage(DEBUT_COMBAT);
            } catch (SQLException e) {
                EventQueue.invokeLater(() -> {
                    if (!ferme) {
                        signalerErreur.accept(e.getMessage());
                    }
                });
            }
        });
    }

    private void publierMessage(String msg) throws SQLException {
        try (Connection c = getConnexion()) {
            c.setAutoCommit(false);
            try {
                if (verrouillerSalon(c) != idSalon) {
                    throw new SQLException("Ce salon a été fermé.");
                }
                verifierSalonOuvert(c);
                if (DEBUT_COMBAT.equals(msg)) {
                    if (!"P1".equals(monIdJoueur)) {
                        throw new SQLException("Seul l’hôte peut lancer le combat.");
                    }
                    verifierLignesJoueurs(c);
                    // Les slots restent invisibles jusqu'à leur première position réelle.
                    try (PreparedStatement st = c.prepareStatement(
                            "UPDATE Joueur SET x = 0, y = ? WHERE " + LIGNES_JOUEURS)) {
                        st.setInt(1, POSITION_INACTIVE);
                        st.executeUpdate();
                    }
                }
                insererMessage(c, msg);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void verifierLignesJoueurs(Connection c) throws SQLException {
        // Une ancienne table peut avoir un emplacement vide ou différent du numéro du joueur.
        // Le pseudo ChoixJ1..ChoixJ4 identifie la ligne utilisée par le relais.
        try (PreparedStatement st = c.prepareStatement(
                "SELECT CONCAT('ChoixJ', RIGHT(s.message, 1)) AS pseudo_attendu, "
                + "COUNT(j.pseudo) AS nombre FROM reseau_salon s "
                + "LEFT JOIN Joueur j ON j.pseudo = CONCAT('ChoixJ', RIGHT(s.message, 1)) "
                + "WHERE s.message IN ('PRESENCE_P1', 'PRESENCE_P2', 'PRESENCE_P3', 'PRESENCE_P4') "
                + "GROUP BY s.message HAVING COUNT(j.pseudo) <> 1");
             ResultSet rs = st.executeQuery()) {
            if (rs.next()) {
                throw new SQLException("La table Joueur doit contenir exactement une ligne pour "
                        + rs.getString("pseudo_attendu") + " (" + rs.getInt("nombre")
                        + " trouvée(s)). Vérifiez les lignes de la BDD avant de relancer le combat.");
            }
        }
    }

    public void demarrerEcoute(Consumer<String> gestionnaireMessage) {
        ecouteSalon = new Thread(() -> ecouterSalon(gestionnaireMessage), "brawlmula-ecoute-salon");
        ecouteSalon.setDaemon(true);
        ecouteSalon.start();
    }

    private void ecouterSalon(Consumer<String> gestionnaireMessage) {
        int dernierIdLu = idSalon;
        try {
            while (!ferme) {
                try (Connection c = getConnexion();
                     PreparedStatement st = c.prepareStatement(
                             "SELECT id, message FROM reseau_salon WHERE id > ? "
                             + "AND EXISTS (SELECT 1 FROM reseau_salon WHERE id = ?) ORDER BY id")) {
                    st.setInt(1, dernierIdLu);
                    st.setInt(2, idSalon);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next() && !ferme) {
                            dernierIdLu = rs.getInt("id");
                            String message = rs.getString("message");
                            if (DEBUT_COMBAT.equals(message)) {
                                idCombat = dernierIdLu;
                            }
                            EventQueue.invokeLater(() -> {
                                if (!ferme) {
                                    gestionnaireMessage.accept(message);
                                }
                            });
                            if (idCombat != 0) {
                                return;
                            }
                        }
                    }
                } catch (SQLException e) {
                    if (!ferme) {
                        System.err.println("Erreur écoute réseau BDD : " + e.getMessage());
                    }
                }
                Thread.sleep(150);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void demarrerSynchronisation(int numeroJoueur, int x, int y) {
        envoyerMaPosition(x, y);
        synchronisationCombat = new Thread(() -> synchroniserPositions(numeroJoueur), "brawlmula-positions");
        synchronisationCombat.setDaemon(true);
        synchronisationCombat.start();
    }

    private void synchroniserPositions(int numeroJoueur) {
        try {
            while (!ferme) {
                try (Connection c = getConnexion();
                     PreparedStatement envoyer = c.prepareStatement(
                             "UPDATE Joueur SET x = ?, y = ? WHERE pseudo = ? AND " + COMBAT_ACTIF);
                     PreparedStatement lire = c.prepareStatement(
                             "SELECT RIGHT(TRIM(pseudo), 1) AS numero_joueur, x, y FROM Joueur WHERE y <> ? AND "
                             + LIGNES_JOUEURS + " AND " + COMBAT_ACTIF)) {
                    envoyer.setString(3, "ChoixJ" + numeroJoueur);
                    envoyer.setInt(4, idCombat);
                    lire.setInt(1, POSITION_INACTIVE);
                    lire.setInt(2, idCombat);
                    Position dernierePositionEnvoyee = null;
                    while (!ferme) {
                        Position position = maPosition;
                        if (!position.equals(dernierePositionEnvoyee)) {
                            envoyer.setInt(1, position.x());
                            envoyer.setInt(2, position.y());
                            envoyer.executeUpdate();
                            dernierePositionEnvoyee = position;
                        }
                        List<JoueurData> joueurs = new ArrayList<>(4);
                        try (ResultSet rs = lire.executeQuery()) {
                            while (rs.next()) {
                                joueurs.add(new JoueurData(rs.getInt("numero_joueur"),
                                        rs.getInt("x"), rs.getInt("y")));
                            }
                        }
                        positionsJoueurs = List.copyOf(joueurs);
                        Thread.sleep(INTERVALLE_COMBAT_MS);
                    }
                } catch (SQLException e) {
                    if (!ferme) {
                        System.err.println("Erreur synchro positions : " + e.getMessage());
                        Thread.sleep(500);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void envoyerMaPosition(int x, int y) {
        maPosition = new Position(x, y);
    }

    public List<JoueurData> lireTousLesJoueurs() {
        return positionsJoueurs;
    }

    public void fermer() {
        ferme = true;
        messagesSortants.shutdownNow();
        if (ecouteSalon != null) {
            ecouteSalon.interrupt();
        }
        if (synchronisationCombat != null) {
            synchronisationCombat.interrupt();
        }
    }

    private record Position(int x, int y) { }

    public record JoueurData(int emplacement, int x, int y) { }
}