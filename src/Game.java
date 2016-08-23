import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

/**
 * Created by Hudo on 21/08/2016.
 */
public class Game implements ISocketActions {

    public static final String GROUP_ADDR = "228.5.6.7";
    public static final int PORT = 6789;

    public static Player g_currentPlayer;

    private BufferedReader m_br = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public void gamePacketReceived(GamePacket packet) {
        Player player = packet.getPlayer();

        System.out.println("Player connected: " + player.getNickname());
    }

    public void run() throws IOException {
        System.out.print("Enter nickname: ");
        String nickname = m_br.readLine();
//        String nickname = "hudovisk";
        System.out.print("Enter passphrase: ");
        String passphrase = m_br.readLine();
//        String passphrase = "123";

        SocketListenner socket = new SocketListenner(PORT, InetAddress.getByName(GROUP_ADDR));
        socket.setSocketActionsListenner(this);
        new Thread(socket).start();

        g_currentPlayer = new Player(nickname);

        GamePacket connectionPacket = new GamePacket();
        connectionPacket.setPlayer(g_currentPlayer);
        connectionPacket.setAction(GamePacket.Actions.CONNECTED);

        try {
            while(true) {
                Thread.sleep(1000);
                socket.sendGamePacket(connectionPacket);
            }
        }  catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Game game = new Game();
        game.run();
    }

}
