/**
 * Created by Hudo on 04/09/2016.
 */

//Payload of a guess. used for both letter and word guesses.
public class GuessPayload implements GamePayload {
    //String containing the guess
    private String m_guess;

    public GuessPayload(String guess) {
        m_guess = guess;
    }

    /*GETTERS AND SETTERS*/
    public String getGuess() {
        return m_guess;
    }

    public void setGuess(String guess) {
        m_guess = guess;
    }
}
