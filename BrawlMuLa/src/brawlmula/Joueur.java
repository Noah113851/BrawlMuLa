/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

/**
 *
 * @author dcragnaz
 */
public class Joueur {

    private String nom;
    private int positionX;
    private int positionY;
    private int score;

    public Joueur(String nom, int positionX, int positionY, int score) {
        this.nom = nom;
        this.positionX = positionX;
        this.positionY = positionY;
        this.score = score;
    }

    public String getNom() {
        return nom;
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public int getScore() {
        return score;
    }
}
