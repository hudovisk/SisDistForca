import java.io.Serializable;

/**
 * Created by Hudo on 21/08/2016.
 */
public class GamePacket implements Serializable {

    public enum Actions {
        CONNECTED,
        CONNECT_OK,
        LETTER_GUESS,
        UPDATE_WORD,
        WORD_GUESS
    };

    private Player m_player;
    private Actions m_action;
    private Character m_letterGuessed;
    private String m_currentWord;

    public byte[] getEncryptedSign() {
        return m_encryptedSign;
    }

    public void setEncryptedSign(byte[] encryptedSign) {
        this.m_encryptedSign = encryptedSign;
    }

    private byte[] m_encryptedSign;

    public Character getLetterGuessed() {
        return m_letterGuessed;
    }

    public void setLetterGuessed(Character letterGuessed) {
        this.m_letterGuessed = letterGuessed;
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
