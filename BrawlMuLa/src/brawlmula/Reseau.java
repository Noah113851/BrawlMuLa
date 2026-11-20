/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.io.*;
import java.net.*;

/**
 *
 * @author ndufour
 */
public class Reseau {

    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isHost;

    // 1. Initialisation côté Hôte (Créer la partie)
    public Reseau(int port) throws IOException {
        this.isHost = true;
        this.serverSocket = new ServerSocket(port);
    }

    // Attente du deuxième joueur (bloquant, à exécuter dans un Thread séparé)
    public void attendreClient() throws IOException {
        this.socket = serverSocket.accept();
        initialiserFlux();
    }

    // 2. Initialisation côté Client (Rejoindre la partie)
    public Reseau(String ipHote, int port) throws IOException {
        this.isHost = false;
        this.socket = new Socket(ipHote, port);
        initialiserFlux();
    }

    private void initialiserFlux() throws IOException {
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Envoyer un message texte (ex: "CHOIX_J1:2" ou "POS:150,300")
    public void envoyerMessage(String msg) {
        if (writer != null) {
            writer.println(msg);
        }
    }

    // Lire le message suivant (bloquant)
    public String lireMessage() throws IOException {
        if (reader != null) {
            return reader.readLine();
        }
        return null;
    }

    public boolean isHost() {
        return isHost;
    }

    public void fermer() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

