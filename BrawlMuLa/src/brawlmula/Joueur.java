package brawlmula;

/**
 * Represente UN joueur dans le programme Java.
 * Un objet Joueur correspond a UNE ligne de la table Joueur (phpMyAdmin) :
 *
 *     pseudo | x | y | score | emplacement| id_avatar 
 */
public class Joueur {


    private String pseudo;
    private int x;
    private int y;
    private int score;
    private int emplacement;   // 1 a 4 (la "place" du joueur dans la partie)
    private int idAvatar;      // id dans la table Avatars (0 = pas encore choisi)
    

    public Joueur(String pseudo, int x, int y, int score, int emplacement, int idAvatar) {
        this.pseudo = pseudo;
        this.x = x;
        this.y = y;
        this.score = score;
        this.emplacement = emplacement;
        this.idAvatar = idAvatar;
    }

    // ----- Getters -----
    public String getPseudo()   { return pseudo; }
    public int getX()           { return x; }
    public int getY()           { return y; }
    public int getScore()       { return score; }
    public int getEmplacement() { return emplacement; }
    public int getIdAvatar()    { return idAvatar; }

    // ----- Setters (seulement pour ce qui change pendant le jeu) -----
    public void setX(int x)                { this.x = x; }
    public void setY(int y)                { this.y = y; }
    public void setScore(int score)        { this.score = score; }
    //public void setIdAvatar(int idAvatar)  { this.idAvatar = idAvatar; }

    @Override
    public String toString() {
        return "Joueur " + emplacement + " : " + pseudo
                + " (x=" + x + ", y=" + y + ", score=" + score
                + ", avatar=" + idAvatar + ")";
    }
}
