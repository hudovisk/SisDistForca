import java.io.Serializable;

/**
 * Created by Hudo on 21/08/2016.
 */
public class GamePacket implements Serializable {

    public enum Actions {
      CONNECTED
    };

    private Player m_player;
    private Actions m_action;

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
