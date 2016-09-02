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
    private String m_currentGuessedLetters;
    private int[] m_currentScores;
    private int[] m_currentErrors;
    private byte[] m_encryptedSign;

    public int[] getCurrentScores() { return m_currentScores; }
    public void setCurrentScore(int[] score) { m_currentScores = score; }

    public int[] getCurrentErrors() { return m_currentErrors; }
    public void setCurrentErrors(int[] errors) { m_currentErrors = errors; }

    public String getCurrentGuessedLetters() { return m_currentGuessedLetters; }
    public void setCurrentGuessedLetters(String letters) { m_currentGuessedLetters = letters; }

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
