import java.io.Serializable;
import java.security.PublicKey;

/**
 * Created by Hudo on 21/08/2016.
 */
public class Player implements Serializable {
    private String nickname;
    private int rndOrder;
    private PublicKey publicKey;

    public Player(String nickname, int rndOrder) {
        this.nickname = nickname;
        this.rndOrder = rndOrder;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getRndOrder() {
        return rndOrder;
    }

    public void setRndOrder(int rndOrder) {
        this.rndOrder = rndOrder;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
