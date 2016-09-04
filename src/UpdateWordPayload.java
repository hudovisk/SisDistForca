import java.io.Serializable;

/**
 * Created by Hudo on 04/09/2016.
 */
public class UpdateWordPayload implements GamePayload {
    private String m_currentWord;
    private String m_currentGuessedLetters;

    public UpdateWordPayload(String currentWord, String currentGuessedLetters) {
        m_currentWord = currentWord;
        m_currentGuessedLetters = currentGuessedLetters;
    }

    public String getCurrentWord() {
        return m_currentWord;
    }

    public void setCurrentWord(String currentWord) {
        m_currentWord = currentWord;
    }

    public String getCurrentGuessedLetters() {
        return m_currentGuessedLetters;
    }

    public void setCurrentGuessedLetters(String currentGuessedLetters) {
        m_currentGuessedLetters = currentGuessedLetters;
    }
}
