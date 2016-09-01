import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

public class Game implements ISocketActions {

    public static final String GROUP_ADDR = "228.5.6.7";
    public static final int PORT = 6789;
    private SocketListenner socket;

    public static Player g_currentPlayer;
    private KeyPair playerKeyPair;

    private boolean m_gameReady = false;

    private BufferedReader m_br = new BufferedReader(new InputStreamReader(System.in));

    private TreeSet<Player> m_connectedPlayers = new TreeSet<>((o1, o2) -> o1.getRndOrder() - o2.getRndOrder());
    private int m_wordGeneratorPlayerIndex = 0;

    @Override
    public void gamePacketReceived(GamePacket packet) {
        Player player = packet.getPlayer();

        switch (packet.getAction())
        {
            case CONNECTED:
                if(player.getRndOrder() == g_currentPlayer.getRndOrder())
                {
                    //Conflict - Update rnd order and send connect again
                    g_currentPlayer.setRndOrder(ThreadLocalRandom.current().nextInt());
                } else {
                    // Does not add if the player already exist!
                    if(!m_connectedPlayers.add(player)) {
                        System.out.println("Player already connected: " + player.getNickname());
                    }
                }
                break;
            case WORD_GUESS:
                System.out.println("The message is " + packet.getCurrentWord() + " and the decrypted message is " + RSAEncryptDecrypt.decrypt(packet.getEncryptedSign(), player.getPublicKey()));
                break;
        }

    }

    public void run() throws IOException {
        System.out.print("Enter nickname: ");
        String nickname = m_br.readLine();

        //Initializing keys
        playerKeyPair = RSAEncryptDecrypt.createKeys();

        //Generate random number that defines the order in the connected list.
        int rnd = ThreadLocalRandom.current().nextInt();

        g_currentPlayer = new Player(nickname, rnd);
        g_currentPlayer.setPublicKey(playerKeyPair.getPublic());

        //NOTE: FOR THIS TO WORK IN MAC OS X PLEASE USE -Djava.net.preferIPv4Stack=true.
        //SOURCE: https://github.com/bluestreak01/questdb/issues/23
        socket = new SocketListenner(PORT, InetAddress.getByName(GROUP_ADDR));
        socket.setSocketActionsListenner(this);
        new Thread(socket).start();

        GamePacket encriptedName = new GamePacket();
        //SEND A ENCRYPTED MESSAGE WITH ITS OWN NAME
        while(true)
        {
            try
            {
                Thread.sleep(5000);
                System.out.println("Connected players: " + m_connectedPlayers.size());
                if(!m_gameReady) {
                    if(m_connectedPlayers.size() < 2) {
                        encriptedName.setPlayer(g_currentPlayer);
                        encriptedName.setAction(GamePacket.Actions.CONNECTED);
                        encriptedName.setCurrentWord(g_currentPlayer.getNickname());
                        encriptedName.setEncryptedSign(RSAEncryptDecrypt.encrypt(g_currentPlayer.getNickname(), playerKeyPair.getPrivate()));
                        socket.sendGamePacket(encriptedName);
                    }else {
                        m_gameReady = true;

                        m_connectedPlayers.add(g_currentPlayer);

                        // Send the last connect packet
                        encriptedName.setPlayer(g_currentPlayer);
                        encriptedName.setAction(GamePacket.Actions.CONNECTED);
                        encriptedName.setCurrentWord(g_currentPlayer.getNickname());
                        encriptedName.setEncryptedSign(RSAEncryptDecrypt.encrypt(g_currentPlayer.getNickname(), playerKeyPair.getPrivate()));
                        socket.sendGamePacket(encriptedName);
                    }
                } else {
                    System.out.println("Game is ready. Player list:");
                    printConnectedPlayersList();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printConnectedPlayersList() {
        int index = 0;
        for(Player p : m_connectedPlayers) {
            if(index == m_wordGeneratorPlayerIndex) {
                System.out.println(">" + p.getNickname() + " " + p.getRndOrder());
            } else {
                System.out.println(p.getNickname() + " " + p.getRndOrder());
            }
            index++;
        }
    }

    public static void main(String[] args) throws IOException {
        Game game = new Game();
        game.run();
    }

}
