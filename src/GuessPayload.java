/**
 * Created by Hudo on 04/09/2016.
 */
public class GuessPayload implements GamePayload {
    private String m_guess;

    public GuessPayload(String guess) {
        m_guess = guess;
    }

    public String getGuess() {
        return m_guess;
    }

    public void setGuess(String guess) {
        m_guess = guess;
    }
}
