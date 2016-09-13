/**
 * Created by rodrigoguimaraes on 2016-09-11.
 */
public class QuitGamePayload implements GamePayload{
    //Boolean containg a variable to confirm that the quit action should be done. Unused for now.
    private boolean m_reallyQuit;


    public QuitGamePayload(Boolean reallyQuit) {
        m_reallyQuit = reallyQuit;
    }

    /*GETTERS AND SETTERS*/
    public boolean getReallyQuit() {
        return m_reallyQuit;
    }

    public void setReallyQuit(boolean reallyQuit) {
        this.m_reallyQuit = reallyQuit;
    }
}
