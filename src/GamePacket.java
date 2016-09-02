import java.io.Serializable;

/**
 * Created by Hudo on 21/08/2016.
 */
public class GamePacket implements Serializable {

    public enum Actions {
        CONNECTED,
        NEXT_TURN,
        NEXT_GM,
        LETTER_GUESS,
        UPDATE_WORD,
        WORD_GUESS
    };

    private Player m_player;
    private Actions m_action;
    private String m_currentWord;
    private int m_nonce;
    private byte[] m_encryptedSign;

    public byte[] getEncryptedSign() {
        return m_encryptedSign;
    }

    public void setEncryptedSign(byte[] encryptedSign) {
        this.m_encryptedSign = encryptedSign;
    }

    public String getCurrentWord() {
        return m_currentWord;
    }

    public void setCurrentWord(String currentWord) {
        this.m_currentWord = currentWord;
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
