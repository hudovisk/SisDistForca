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
    public static Player g_currentGameMaster = null;
    private KeyPair playerKeyPair;

    private boolean m_gameReady = false;

    private BufferedReader m_br = new BufferedReader(new InputStreamReader(System.in));

    private TreeSet<Player> m_connectedPlayers = new TreeSet<>((o1, o2) -> o1.getRndOrder() - o2.getRndOrder());
    private int m_wordGeneratorPlayerIndex = 0;
    private Player m_lastPlay = null;
    private String wordToGuess = null;
    private String partlyGuessedWord = "";
    private boolean m_anotherTurn = false;
    private boolean myTurn = false;
    private boolean m_isLetterGuessPhase = true;

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
            //ACTIONS PARSED BY GAME MASTER
            case LETTER_GUESS:
                if(g_currentGameMaster == g_currentPlayer)
                {
                    m_isLetterGuessPhase = false;
                    if (packet.getPlayer().getNickname().equals(higher(m_lastPlay).getNickname())) {
                        if (wordToGuess.contains(packet.getCurrentWord().substring(0, 1))) {
                            System.out.println(packet.getPlayer().getNickname() + " got " + packet.getCurrentWord() + " right. ");
                            for (int i = 0; i < wordToGuess.length(); i++) {
                                if (wordToGuess.charAt(i) == packet.getCurrentWord().charAt(0)) {
                                    partlyGuessedWord = partlyGuessedWord.substring(0, i) + packet.getCurrentWord().charAt(0) + partlyGuessedWord.substring(i + 1, wordToGuess.length());
                                }
                            }
                            System.out.println("wordToGuess = " + wordToGuess + " and partlyGuessed = " + partlyGuessedWord);
                        } else {
                            System.out.println(packet.getPlayer().getNickname() + " got " + packet.getCurrentWord() + " wrong.");
                        }

                        GamePacket updatedWord = new GamePacket();
                        updatedWord.setPlayer(g_currentPlayer);
                        updatedWord.setAction(GamePacket.Actions.UPDATE_WORD);
                        updatedWord.setCurrentWord(partlyGuessedWord);
                        updatedWord.setEncryptedSign(RSAEncryptDecrypt.encrypt(partlyGuessedWord, playerKeyPair.getPrivate()));
                        try {
                            socket.sendGamePacket(updatedWord);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case WORD_GUESS:
                if(g_currentGameMaster == g_currentPlayer) {
                    if (packet.getPlayer().getNickname().equals(higher(m_lastPlay).getNickname())) {
                        m_isLetterGuessPhase = true;
                        if (packet.getCurrentWord().equals(wordToGuess)) {
                            System.out.println("Player " + packet.getPlayer().getNickname() + " won the game by getting the word " + wordToGuess + " correctly.");
                            passTheGameToTheNextGameMaster();
                        } else {
                            System.out.println("Too bad, wrong guess.");
                            m_anotherTurn = true;
                        }
                    }
                }
                break;

            //ACTIONS PARSED BY PLAYERS
            case UPDATE_WORD:
                if(packet.getPlayer().getNickname().equals(g_currentGameMaster.getNickname()))
                {
                    System.out.println("The updated word is " + packet.getCurrentWord());
                }
                if(myTurn && packet.getPlayer().getNickname().equals(g_currentGameMaster.getNickname()))
                {
                    System.out.print("Guess the word: ");
                    try
                    {
                        String guessedWord = m_br.readLine();

                        GamePacket guessedWordPacket = new GamePacket();
                        guessedWordPacket.setPlayer(g_currentPlayer);
                        guessedWordPacket.setAction(GamePacket.Actions.WORD_GUESS);
                        guessedWordPacket.setCurrentWord(guessedWord);
                        guessedWordPacket.setEncryptedSign(RSAEncryptDecrypt.encrypt(guessedWord, playerKeyPair.getPrivate()));
                        socket.sendGamePacket(guessedWordPacket);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    myTurn = false;
                }
                break;
            case NEXT_TURN:
                if(packet.getPlayer().getNickname().equals(g_currentGameMaster.getNickname()))
                {
                    System.out.println("Game master notified me that " + packet.getCurrentWord() + " is the next to play.");
                    if(packet.getCurrentWord().equals(g_currentPlayer.getNickname())) {
                        //It's my turn
                        myTurn = true;
                        try {
                            String guessedLetter;
                            do {
                                System.out.print("Please input a single letter guess: ");
                                guessedLetter = m_br.readLine();
                                guessedLetter.trim();
                            }while(guessedLetter.length() != 1);
                            GamePacket guessLetter = new GamePacket();
                            guessLetter.setPlayer(g_currentPlayer);
                            guessLetter.setAction(GamePacket.Actions.LETTER_GUESS);
                            guessLetter.setCurrentWord(guessedLetter);
                            guessLetter.setEncryptedSign(RSAEncryptDecrypt.encrypt(guessedLetter, playerKeyPair.getPrivate()));
                            socket.sendGamePacket(guessLetter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case NEXT_GM:
                if(packet.getPlayer().getNickname().equals(g_currentGameMaster.getNickname())) {
                    if (packet.getCurrentWord().equals(g_currentPlayer.getNickname())) {
                        g_currentGameMaster = g_currentPlayer;
                        initializeTurnAsGM();
                    } else {
                        for (Player p : m_connectedPlayers) {
                            if (p.getNickname().equals(packet.getCurrentWord())) {
                                g_currentGameMaster = p;
                                break;
                            }
                        }
                    }
                }
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
                Thread.sleep(500);
                if(!m_gameReady)
                {
                    System.out.println("Connected players: " + m_connectedPlayers.size());
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

                        System.out.println("Player list:");
                        printConnectedPlayersList();
                    }
                }
                else
                {
                    if(g_currentGameMaster == null)
                    {
                        g_currentGameMaster = m_connectedPlayers.first();
                        System.out.println("Game Master Chosen: " + g_currentGameMaster.getNickname());

                        if(g_currentGameMaster == g_currentPlayer) {
                            initializeTurnAsGM();
                        }
                    }
                    else
                    {
                        if(g_currentGameMaster == g_currentPlayer && m_anotherTurn)
                        {
                            m_anotherTurn = false;
                            //Configure the last player that played correctly
                            m_lastPlay = higher(m_lastPlay);
                            if(higher(m_lastPlay) == g_currentGameMaster)
                            {
                                m_lastPlay = g_currentGameMaster;
                            }
                            //Choose the next player to play

                            String nextToPlay = higher(m_lastPlay).getNickname();
                            System.out.println("Letting " + nextToPlay + " know that he is the next to play.");
                            //Send packet to notify player that he should play
                            GamePacket notifyTurn = new GamePacket();
                            notifyTurn.setPlayer(g_currentPlayer);
                            notifyTurn.setAction(GamePacket.Actions.NEXT_TURN);
                            notifyTurn.setCurrentWord(nextToPlay);
                            notifyTurn.setEncryptedSign(RSAEncryptDecrypt.encrypt(nextToPlay, playerKeyPair.getPrivate()));
                            socket.sendGamePacket(notifyTurn);
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeTurnAsGM()
    {
        generate_word();
        m_anotherTurn = false;
        m_isLetterGuessPhase = true;

        //Configure the last player that played correctly
        m_lastPlay = g_currentPlayer;
        //Choose the next player to play
        String nextToPlay = higher(m_lastPlay).getNickname();
        System.out.println("Letting " + nextToPlay + " know that he is the next to play.");
        //Send packet to notify player that he should play
        GamePacket notifyTurn = new GamePacket();
        notifyTurn.setPlayer(g_currentPlayer);
        notifyTurn.setAction(GamePacket.Actions.NEXT_TURN);
        notifyTurn.setCurrentWord(nextToPlay);
        notifyTurn.setEncryptedSign(RSAEncryptDecrypt.encrypt(nextToPlay, playerKeyPair.getPrivate()));
        try
        {
            socket.sendGamePacket(notifyTurn);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Player higher(Player current)
    {
        Player next = m_connectedPlayers.higher(current);
        if(next == null)
            next = m_connectedPlayers.first();

        return next;
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

    private void passTheGameToTheNextGameMaster()
    {
        g_currentGameMaster = higher(g_currentGameMaster);

        String nextToBeGameMaster = g_currentGameMaster.getNickname();
        //Send packet to notify player that he should be the next game master
        GamePacket notifyTurnGM = new GamePacket();
        notifyTurnGM.setPlayer(g_currentPlayer);
        notifyTurnGM.setAction(GamePacket.Actions.NEXT_GM);
        notifyTurnGM.setCurrentWord(nextToBeGameMaster);
        notifyTurnGM.setEncryptedSign(RSAEncryptDecrypt.encrypt(nextToBeGameMaster, playerKeyPair.getPrivate()));
        try
        {
            socket.sendGamePacket(notifyTurnGM);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void generate_word()
    {
        partlyGuessedWord = "";
        System.out.print("You're the new game master. Type the word for the new game: ");
        try {
            wordToGuess = m_br.readLine();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        for(int i = 0; i < wordToGuess.length(); i++)
        {
            partlyGuessedWord = partlyGuessedWord + "_";
        }
    }

    public static void main(String[] args) throws IOException {
        Game game = new Game();
        game.run();
    }

}
