/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

/**
 *
 * @author Shyturo
 */
import java.net.URL;
import javax.sound.sampled.*;

public class Musiques_sons {

    private static Clip clip;
    private static int volume = 100;

    // Charger les sons depuis les ressources du projet
    private static URL trouverSon(String chemin) {

        URL url = Musiques_sons.class.getResource(chemin);

        if (url == null) {
            System.err.println(
                    "Fichier audio introuvable : " + chemin
            );
        }

        return url;
    }

    // Jouer une musique en boucle
    public static void playLoop(String chemin) {

        stop();

        try {
            URL url = trouverSon(chemin);

            if (url == null) {
                return;
            }

            try (AudioInputStream audio
                    = AudioSystem.getAudioInputStream(url)) {

                clip = AudioSystem.getClip();
                clip.open(audio);

                appliquerVolume(clip);

                clip.setFramePosition(0);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }

        } catch (Exception e) {
            System.err.println(
                    "Erreur musique : " + e.getMessage()
            );
        }
    }

    // Jouer un effet sonore
    public static void jouerSon(String chemin) {

        try {
            URL url = trouverSon(chemin);

            if (url == null) {
                return;
            }

            try (AudioInputStream audio
                    = AudioSystem.getAudioInputStream(url)) {

                Clip effet = AudioSystem.getClip();
                effet.open(audio);

                appliquerVolume(effet);

                // Libérer le son après sa lecture
                effet.addLineListener(event -> {

                    if (event.getType() == LineEvent.Type.STOP) {
                        effet.close();
                    }
                });

                effet.start();
            }

        } catch (Exception e) {
            System.err.println(
                    "Erreur effet sonore : " + e.getMessage()
            );
        }
    }

    // Régler le volume
    public static void reglerVolume(int volumePourcent) {

        volume = Math.max(
                0,
                Math.min(100, volumePourcent)
        );

        if (clip != null) {
            appliquerVolume(clip);
        }
    }

    private static void appliquerVolume(Clip son) {

        if (son == null || !son.isControlSupported(
                FloatControl.Type.MASTER_GAIN)) {
            return;
        }

        FloatControl controle = (FloatControl) son.getControl(FloatControl.Type.MASTER_GAIN);

        float valeur = Math.max(0.0001f, volume / 100.0f);

        float decibels = (float) (20 * Math.log10(valeur));

        decibels = Math.max(
                controle.getMinimum(),
                Math.min(controle.getMaximum(), decibels)
        );

        controle.setValue(decibels);
    }

    public static void jouerMusiqueCarte(String cheminCarte) {

        if (cheminCarte == null) {
            System.err.println("Aucune carte sélectionnée.");
            return;
        }

        String nom = cheminCarte.replace('\\', '/')
                .toLowerCase(java.util.Locale.ROOT);

        String musique;

        if (nom.endsWith("chateau.png")) {

            musique = "chateau_theme.wav";

        } else if (nom.endsWith("marche.png")) {

            musique = "marche_theme.wav";

        } else if (nom.endsWith("colonie.png")) {

            musique = "colonie_theme.wav";

        } else if (nom.endsWith("potager.png")) {

            musique = "potager_theme.wav";

        } else {

            System.err.println(
                    "Carte inconnue : " + cheminCarte
            );
            return;
        }

        String cheminMusique
                = "/ressources/Musiques-sons/" + musique;

        System.out.println(
                "Lancement musique : " + cheminMusique
        );

        // Arrête la musique précédente et lance la nouvelle
        playLoop(cheminMusique);
    }

    public static void jouerMusiqueIntroCarte(String cheminCarte) {

        if (cheminCarte == null) {
            return;
        }

        String carte = cheminCarte.replace('\\', '/')
                .toLowerCase(java.util.Locale.ROOT);

        String musique;

        if (carte.endsWith("chateau.png")) {
            musique = "intro_chateau.wav";

        } else if (carte.endsWith("marche.png")) {
            musique = "intro_marche.wav";

        } else if (carte.endsWith("colonie.png")) {
            musique = "intro_colonie.wav";

        } else if (carte.endsWith("potager.png")) {
            musique = "intro_potager.wav";

        } else {
            System.err.println("Carte inconnue : " + cheminCarte);
            return;
        }

        playLoop("/ressources/Musiques-sons/" + musique);
    }

// Musique pendant l'affichage des joueurs
    public static void jouerMusiqueVersus() {

        playLoop(
                "/ressources/Musiques-sons/versus_theme.wav"
        );
    }

// Voix annonçant le début du combat
    public static void annoncerFight() {

        jouerSon(
                "/ressources/Musiques-sons/fight.wav"
        );
    }

    // Arrêter la musique
    public static void stop() {

        if (clip != null) {

            clip.stop();
            clip.close();
            clip = null;
        }
    }
}
