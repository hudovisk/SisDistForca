/**
 * Created by Hudo on 21/08/2016.
 */
//Interface to handle GamePackets according to the actions.
public interface ISocketActions {

    void gamePacketReceived(GamePacket packet);
}
