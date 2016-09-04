import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by Hudo on 21/08/2016.
 */
public class SocketListenner implements Runnable{

    private MulticastSocket m_socket;
    private boolean m_running;

    private int m_port;
    private InetAddress m_group;

    private ISocketActions m_socketActionsListenner;

    public SocketListenner(int port, InetAddress group) throws IOException {
        m_port = port;
        m_group = group;

        m_socket = new MulticastSocket(port);
        m_socket.setLoopbackMode(false);
        m_socket.joinGroup(group);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2000];

        m_running = true;
        while(m_running) {
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                m_socket.receive(receivedPacket);
                parsePacket(receivedPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendGamePacket(GamePacket gamePacket) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(gamePacket);
        oos.flush();

        byte[] objData = baos.toByteArray();

        System.out.println("objData.length = " + objData.length + " action: " + gamePacket.getAction());

        DatagramPacket packet = new DatagramPacket(objData, objData.length, m_group, m_port);
        m_socket.send(packet);
    }

    private void parsePacket(DatagramPacket receivedPacket) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(receivedPacket.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);

        try {
            GamePacket gamePacket = (GamePacket) ois.readObject();
            Player player = gamePacket.getPlayer();
            if(m_socketActionsListenner != null &&
                    !player.getNickname().equals(Game.g_currentPlayer.getNickname())) {
                String signature = RSAEncryptDecrypt.decrypt(gamePacket.getEncryptedSign(),
                        player.getPublicKey());
                if(signature.equals(player.getNickname())) {
                    m_socketActionsListenner.gamePacketReceived(gamePacket);
                } else {
                    System.out.println("Invalid signature received from: " + player.getNickname() +
                            ". Signature: " + signature);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return m_port;
    }

    public InetAddress getGroup() {
        return m_group;
    }

    public boolean isRunning() {
        return m_running;
    }

    public void setRunning(boolean running) {
        m_running = running;
    }

    public ISocketActions getSocketActionsListenner() {
        return m_socketActionsListenner;
    }

    public void setSocketActionsListenner(ISocketActions socketActionsListenner) {
        m_socketActionsListenner = socketActionsListenner;
    }
}
