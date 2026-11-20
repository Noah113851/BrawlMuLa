/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

public enum ModeDeJeu {
    DUEL(2, new int[]{0, 1}),                    // 1v1
    TRIO(3, new int[]{0, 1, 2}),                 // 1v1v1 : chacun pour soi
    DEUX_CONTRE_DEUX(4, new int[]{0, 0, 1, 1});
    public static ModeDeJeu courant = DUEL;   // mode choisi dans Selection// J1+J2 contre J3+J4

    private final int nbJoueurs;
    private final int[] equipes;   // equipes[i] = équipe du joueur i

    ModeDeJeu(int nbJoueurs, int[] equipes) {
        this.nbJoueurs = nbJoueurs;
        this.equipes = equipes;
    }

    public int getNbJoueurs()   { return nbJoueurs; }
    public int getEquipe(int i) { return equipes[i]; }
    public boolean parEquipes() { return this == DEUX_CONTRE_DEUX; }
    
    public static ModeDeJeu depuisNombre(int nb) {
    if (nb <= 2) return DUEL;
    if (nb == 3) return TRIO;
    return DEUX_CONTRE_DEUX;
    }
}
