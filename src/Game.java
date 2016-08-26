import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.PublicKey;

/**
 * Created by Hudo on 21/08/2016.
 */
public class Game implements ISocketActions {

    public static final String GROUP_ADDR = "228.5.6.7";
    public static final int PORT = 6789;
    private SocketListenner socket;

    public static Player g_currentPlayer;

    private BufferedReader m_br = new BufferedReader(new InputStreamReader(System.in));

    /*TEMPORARY TEST VARIABLES FOR RSA TEST */
    PublicKey otherPlayerPubK;
    Boolean otherPlayerReceivedConnect = false;
    Boolean sentOK = false;
    /*TEMPORARY TEST VARIABLES FOR RSA TEST */

    @Override
    public void gamePacketReceived(GamePacket packet) {
        Player player = packet.getPlayer();

        switch (packet.getAction())
        {
            case CONNECTED:
                System.out.println("Player received connect: " + player.getNickname());
                otherPlayerPubK = player.getPublicKey();
                if(!sentOK)
                {
                    //sendOK
                    GamePacket connectionPacket = new GamePacket();
                    connectionPacket.setPlayer(g_currentPlayer);
                    connectionPacket.setAction(GamePacket.Actions.CONNECT_OK);
                    try
                    {
                        socket.sendGamePacket(connectionPacket);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                break;
            case CONNECT_OK:
                System.out.println("Player received confirmation " + player.getNickname());
                otherPlayerReceivedConnect = true;
                break;
            case WORD_GUESS:
                System.out.println("The message is " + packet.getCurrentWord() + " and the decrypted message is " + RSAEncryptDecrypt.decrypt(packet.getEncryptedSign(), otherPlayerPubK));
                break;
        }

    }

    public void run() throws IOException {
        System.out.print("Enter nickname: ");
        String nickname = m_br.readLine();
//        String nickname = "hudovisk";
        System.out.print("Enter passphrase: ");
        String passphrase = m_br.readLine();
//        String passphrase = "123";

        //Initializing keys
        RSAEncryptDecrypt myRSA = new RSAEncryptDecrypt(nickname);
        myRSA.createKeys();

        //NOTE: FOR THIS TO WORK IN MAC OS X PLEASE USE -Djava.net.preferIPv4Stack=true.
        //SOURCE: https://github.com/bluestreak01/questdb/issues/23
        socket = new SocketListenner(PORT, InetAddress.getByName(GROUP_ADDR));
        socket.setSocketActionsListenner(this);
        new Thread(socket).start();

        g_currentPlayer = new Player(nickname);
        g_currentPlayer.setPublicKey(myRSA.getPublicKey());

        GamePacket connectionPacket = new GamePacket();
        connectionPacket.setPlayer(g_currentPlayer);
        connectionPacket.setAction(GamePacket.Actions.CONNECTED);

        try {
            while(!otherPlayerReceivedConnect && !sentOK)
            {
                Thread.sleep(1000);
                if(!otherPlayerReceivedConnect)
                {
                    socket.sendGamePacket(connectionPacket);
                }

            }
        }  catch (InterruptedException e) {
            e.printStackTrace();
        }

        //SEND A ENCRYPTED MESSAGE WITH ITS OWN NAME
        GamePacket encriptedName = new GamePacket();
        encriptedName.setPlayer(g_currentPlayer);
        encriptedName.setAction(GamePacket.Actions.WORD_GUESS);
        encriptedName.setCurrentWord(g_currentPlayer.getNickname());
        encriptedName.setEncryptedSign(myRSA.encrypt(g_currentPlayer.getNickname(), myRSA.getPrivateKey()));
        while(true)
        {
            try
            {
                Thread.sleep(1000);
                socket.sendGamePacket(encriptedName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Game game = new Game();
        game.run();
    }

}
