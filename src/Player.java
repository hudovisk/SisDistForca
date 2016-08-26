import java.io.Serializable;
import java.security.PublicKey;

/**
 * Created by Hudo on 21/08/2016.
 */
public class Player implements Serializable {
    private String nickname;
    private PublicKey publicKey;

    public Player(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
