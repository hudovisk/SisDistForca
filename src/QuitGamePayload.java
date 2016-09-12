/**
 * Created by rodrigoguimaraes on 2016-09-11.
 */
public class QuitGamePayload implements GamePayload{
    private boolean m_reallyQuit;

    public QuitGamePayload(Boolean reallyQuit) {
        m_reallyQuit = reallyQuit;
    }

    public boolean getReallyQuit() {
        return m_reallyQuit;
    }

    public void setReallyQuit(boolean reallyQuit) {
        this.m_reallyQuit = reallyQuit;
    }
}
