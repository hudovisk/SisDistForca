import java.io.Serializable;

/**
 * Created by Hudo on 21/08/2016.
 */
public class Player implements Serializable {
    private String nickname;

    public Player(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
