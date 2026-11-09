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

            try (AudioInputStream audio =
                    AudioSystem.getAudioInputStream(url)) {

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

            try (AudioInputStream audio =
                    AudioSystem.getAudioInputStream(url)) {

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

        FloatControl controle = (FloatControl)
                son.getControl(FloatControl.Type.MASTER_GAIN);

        float valeur = Math.max(0.0001f, volume / 100.0f);

        float decibels = (float) (
                20 * Math.log10(valeur)
        );

        decibels = Math.max(
                controle.getMinimum(),
                Math.min(controle.getMaximum(), decibels)
        );

        controle.setValue(decibels);
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