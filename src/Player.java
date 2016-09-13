import java.io.Serializable;
import java.security.PublicKey;

/**
 * Created by Hudo on 21/08/2016.
 */

/**
 * The player class is pretty much a data class that contains all the player information.
 */
public class Player implements Serializable {
    private String nickname;
    private int rndOrder;
    private PublicKey publicKey;
    private int score;
    private int errorsThisTurn;
    private int timeouts = 0;


    /*GETTERS, SETTERS AND INCREMENTS*/
    public int getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(int timeouts) {
        this.timeouts = timeouts;
    }

    public void incrementTimeouts()
    {
        this.timeouts++;
    }

    public int getScore()
    {
        return score;
    }

    public void setScore(int score)
    {
        this.score = score;
    }

    public void incrementScore()
    {
        score++;
    }

    public int getErrorsThisTurn()
    {
        return errorsThisTurn;
    }

    public void setErrorsThisTurn(int errorsThisTurn)
    {
        this.errorsThisTurn = errorsThisTurn;
    }

    public void incrementErrorsThisTurn() { errorsThisTurn++; }

    public Player(String nickname, int rndOrder) {
        this.nickname = nickname;
        this.rndOrder = rndOrder;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getRndOrder() {
        return rndOrder;
    }

    public void setRndOrder(int rndOrder) {
        this.rndOrder = rndOrder;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
