import java.io.Serializable;
import java.security.PrivateKey;

/**
 * Created by Hudo on 21/08/2016.
 */

/**
 * This class is the GamePacket to be sent and received by the processes.
 */
public class GamePacket implements Serializable {

    //Each GamePacket has one action of this set of possible actions.
    public enum Actions {
        CONNECTED, //Represents a player introducing himself
        NEXT_TURN, //GM notifies a player that its his turn to play
        NEXT_GM, //GM notifies next GM that its his turn to manage the games
        QUIT_GAME, //GM notifies players that the game should quit
        LETTER_GUESS, //Player guesses a letter
        UPDATE_WORD, //GM sends the currently guessed part of the word
        UPDATE_SCORE, //GM sends the updated scores of all the players
        WORD_GUESS //Player guesses a word
    };

    //Player that is sending the message
    private Player m_player;
    //Action of the current packet
    private Actions m_action;
    //Player nickname signed by the private key of the player that sent the message
    private byte[] m_encryptedSign;
    //Payload of the message
    private GamePayload m_payload;

    //Player nickname signed by the private key of the player.
    public void sign(PrivateKey key) {
        m_encryptedSign = RSAEncryptDecrypt.encrypt(m_player.getNickname(), key);
    }

    /*GETTERS AND SETTERS*/
    public GamePayload getPayload() {
        return m_payload;
    }

    public void setPayload(GamePayload payload) {
        m_payload = payload;
    }

    public byte[] getEncryptedSign() {
        return m_encryptedSign;
    }

    public void setEncryptedSign(byte[] encryptedSign) {
        this.m_encryptedSign = encryptedSign;
    }

    public Player getPlayer() {
        return m_player;
    }

    public void setPlayer(Player player) {
        m_player = player;
    }

    public Actions getAction() {
        return m_action;
    }

    public void setAction(Actions action) {
        m_action = action;
    }
}
