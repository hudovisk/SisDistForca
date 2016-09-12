import java.io.Serializable;
import java.security.PrivateKey;

/**
 * Created by Hudo on 21/08/2016.
 */
public class GamePacket implements Serializable {

    public enum Actions {
        CONNECTED,
        NEXT_TURN,
        NEXT_GM,
        QUIT_GAME,
        LETTER_GUESS,
        UPDATE_WORD,
        UPDATE_SCORE, WORD_GUESS
    };

    private Player m_player;
    private Actions m_action;
    private byte[] m_encryptedSign;

    private GamePayload m_payload;


    public void sign(PrivateKey key) {
        m_encryptedSign = RSAEncryptDecrypt.encrypt(m_player.getNickname(), key);
    }

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
