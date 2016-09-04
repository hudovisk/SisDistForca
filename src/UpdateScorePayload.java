/**
 * Created by Hudo on 04/09/2016.
 */
public class UpdateScorePayload implements GamePayload {
    private int[] m_currentScores;
    private int[] m_currentErrors;

    public UpdateScorePayload(int[] currentScores, int[] currentErrors) {
        m_currentScores = currentScores;
        m_currentErrors = currentErrors;
    }

    public int[] getCurrentScores() {
        return m_currentScores;
    }

    public void setCurrentScores(int[] currentScores) {
        m_currentScores = currentScores;
    }

    public int[] getCurrentErrors() {
        return m_currentErrors;
    }

    public void setCurrentErrors(int[] currentErrors) {
        m_currentErrors = currentErrors;
    }
}
