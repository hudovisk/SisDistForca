import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Comparator;

public class Game implements ISocketActions {

    public static final String GROUP_ADDR = "228.5.6.7";
    public static final int PORT = 6789;
    public static Player g_currentPlayer;

    private int m_guessingPlayerIndex = 0;
    private int m_gameMasterIndex = 0;

    private SocketListenner m_socket;

    private KeyPair m_playerKeyPair;

    private boolean m_gameReady = false;

    private BufferedReader m_br = new BufferedReader(new InputStreamReader(System.in));

    private List<Player> m_connectedPlayers = new ArrayList<>();

    private String m_lettersGuessed = "";

    private String m_wordToGuess = null;
    private String m_partlyGuessedWord = "";

    private boolean m_letterGuessed = false;
    private boolean m_wordGuessed = false;
    private boolean m_wordGenerated = false;

    private int[] generateScoreList()
    {
        int[] scores = new int[m_connectedPlayers.size()];
        int i = 0;
        for (Player p : m_connectedPlayers)
        {
            scores[i++] = p.getScore();
        }
        return scores;
    }   

    private int[] generateCurrentErrorsList()
    {
        int[] errors = new int[m_connectedPlayers.size()];
        int i = 0;
        for (Player p : m_connectedPlayers)
        {
            errors[i++] = p.getErrorsThisTurn();
        }
        return errors;
    }

    private Player findPlayer(Player player)
    {
        for (Player p : m_connectedPlayers)
        {
            if (p.getNickname().equals(player.getNickname()))
            {
                return p;
            }
        }
        return null;
    }

    private void sendUpdateScorePakcet() {
        GamePacket updateScore = new GamePacket();
        updateScore.setPlayer(g_currentPlayer);
        updateScore.setAction(GamePacket.Actions.UPDATE_SCORE);
        updateScore.setPayload(new UpdateScorePayload(generateScoreList(), generateCurrentErrorsList()));
        updateScore.sign(m_playerKeyPair.getPrivate());
        try {
            m_socket.sendGamePacket(updateScore);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendQuitGamePacket()
    {
        GamePacket quitGame = new GamePacket();
        quitGame.setPlayer(g_currentPlayer);
        quitGame.setAction(GamePacket.Actions.QUIT_GAME);
        quitGame.setPayload(new QuitGamePayload(true));
        quitGame.sign(m_playerKeyPair.getPrivate());
        try {
            m_socket.sendGamePacket(quitGame);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUpdateWordPacket() {
        GamePacket updatedWord = new GamePacket();
        updatedWord.setPlayer(g_currentPlayer);
        updatedWord.setAction(GamePacket.Actions.UPDATE_WORD);
        updatedWord.setPayload(new UpdateWordPayload(m_partlyGuessedWord, m_lettersGuessed));
        updatedWord.sign(m_playerKeyPair.getPrivate());
        try {
            m_socket.sendGamePacket(updatedWord);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectPlayer(Player player) {
        if(player.getRndOrder() == g_currentPlayer.getRndOrder() &&
                !player.getNickname().equals(g_currentPlayer.getNickname()))
        {
            //Conflict - Update rnd order and send connect again
            g_currentPlayer.setRndOrder(ThreadLocalRandom.current().nextInt(0, 100));
        } else {
            // Does not add if the player already exist!
            Player connectedPlayer = findPlayer(player);
            if(connectedPlayer != null) {
                if(connectedPlayer.getRndOrder() != player.getRndOrder()) {
                    // Update player list with new rndOrder
                    m_connectedPlayers.remove(connectedPlayer);
                } else {
                    return;
                }
            }

            m_connectedPlayers.add(player);
            m_connectedPlayers.sort(new Comparator<Player>() {
                public int compare(Player o1, Player o2) {
                    return o1.getRndOrder() - o2.getRndOrder();
                }
            });
            printConnectedPlayersList();
        }
    }

    private boolean letterGuessed(Player player, char letter) {
        Player guessingPlayer = m_connectedPlayers.get(m_guessingPlayerIndex);

        if(m_connectedPlayers.get(m_gameMasterIndex) == g_currentPlayer) {
            if (player.getNickname().equals(guessingPlayer.getNickname())) {
                //Turn skipping
                if(letter == '0')
                {
                    sendUpdateScorePakcet();
                    sendUpdateWordPacket();
                    return false;
                }
                boolean letterAlreadyGuessed = false;
                if(m_lettersGuessed.contains(String.valueOf(letter)))
                {
                    System.out.println("You are trying a letter that has already been tried. You will get an error for that.");
                    letterAlreadyGuessed = true;
                }
                if(!letterAlreadyGuessed)
                    m_lettersGuessed += letter + " ";
                if (!letterAlreadyGuessed && m_wordToGuess.contains(String.valueOf(letter))) {
                    //Acertô Mizeravi!
                    System.out.println(player.getNickname() + " got '" + letter + "' right. ");
                    for (int i = 0; i < m_wordToGuess.length(); i++) {
                        if (m_wordToGuess.charAt(i) == letter) {
                            m_partlyGuessedWord = m_partlyGuessedWord.substring(0, i) + letter + m_partlyGuessedWord.substring(i + 1, m_wordToGuess.length());
                        }
                    }
                    System.out.println("m_wordToGuess = " + m_wordToGuess + " and partlyGuessed = " + m_partlyGuessedWord);
                } else {
                    Player playerThatGotWrong = findPlayer(player);
                    playerThatGotWrong.incrementErrorsThisTurn();
                    System.out.println(player.getNickname() + " got '" + letter + "' wrong.");
                    if(playerThatGotWrong.getErrorsThisTurn() >= MAX_ERRORS)
                    {
                        System.out.println("Exiting game because player " + playerThatGotWrong.getNickname() + " guessed too many wrong guesses.");
                        sendQuitGamePacket();
                        System.exit(0);
                    }

                }

                sendUpdateScorePakcet();

                sendUpdateWordPacket();

            }
        }
        return true;
    }

    private void wordGuessed(Player player, String word) {
        Player guessingPlayer = m_connectedPlayers.get(m_guessingPlayerIndex);

        if(m_connectedPlayers.get(m_gameMasterIndex) == g_currentPlayer) {
            if (player.getNickname().equals(guessingPlayer.getNickname())) {

                if(word.isEmpty())
                {
                    //Turn skip
                    sendNextTurn();
                }
                else if (word.equals(m_wordToGuess)) {
                    findPlayer(player).incrementScore();
                    System.out.println("Player " + player.getNickname() + " won the game by getting the word " + m_wordToGuess + " correctly.");

                    resetTurn();

                    sendUpdateScorePakcet();

                    sendNextGM();
                } else {
                    System.out.println("Too bad, wrong guess.");
                    sendNextTurn();
                }
            }
        }
    }

    private void sendNextGM() {
        m_gameMasterIndex = (m_gameMasterIndex + 1) % m_connectedPlayers.size();
        m_guessingPlayerIndex = (m_gameMasterIndex + 1) % m_connectedPlayers.size();

        GamePacket nextGM = new GamePacket();
        nextGM.setPlayer(g_currentPlayer);
        nextGM.setAction(GamePacket.Actions.NEXT_GM);
        nextGM.sign(m_playerKeyPair.getPrivate());

        try {
            m_socket.sendGamePacket(nextGM);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetTurn() {
        m_lettersGuessed = "";
        m_wordToGuess = "";
        m_partlyGuessedWord = "";

        m_letterGuessed = false;
        m_wordGuessed = false;

        m_wordGenerated = false;

        for(Player p : m_connectedPlayers) {
            p.setErrorsThisTurn(0);
        }
    }

    private void sendNextTurn() {
        do {
            m_guessingPlayerIndex = (m_guessingPlayerIndex + 1) % m_connectedPlayers.size();
        } while(m_guessingPlayerIndex == m_gameMasterIndex);

        GamePacket nextTurn = new GamePacket();
        nextTurn.setPlayer(g_currentPlayer);
        nextTurn.setAction(GamePacket.Actions.NEXT_TURN);
        nextTurn.sign(m_playerKeyPair.getPrivate());

        try {
            m_socket.sendGamePacket(nextTurn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //ENABLE WAIT FOR PACKET
        waitingForGamePacket = true;
        timeoutInitialTime = System.currentTimeMillis();
    }

    @Override
    public void gamePacketReceived(GamePacket packet) {
        Player player = packet.getPlayer();

        switch (packet.getAction())
        {
            case CONNECTED:
                connectPlayer(player);
                break;
            //ACTIONS PARSED BY GAME MASTER
            case LETTER_GUESS: {
                //DISABLE WAIT FOR PACKET
                waitingForGamePacket = false;
                //ZERO CONSECUTIVE TIMEOUTS
                m_connectedPlayers.get(m_guessingPlayerIndex).setTimeouts(0);

                GuessPayload payload = (GuessPayload) packet.getPayload();
                char letter = payload.getGuess().charAt(0);

                if(!letterGuessed(player, letter))
                {
                    sendNextTurn();
                    break;
                }
                //ENABLE WAIT FOR PACKET
                waitingForGamePacket = true;
                timeoutInitialTime = System.currentTimeMillis();
                break;
            }
            case WORD_GUESS: {
                //DISABLE WAIT FOR PACKET
                waitingForGamePacket = false;

                GuessPayload payload = (GuessPayload) packet.getPayload();
                String word = payload.getGuess();

                wordGuessed(player, word);
                break;
            }
            //ACTIONS PARSED BY PLAYERS
            case QUIT_GAME: {
                Player gameMasterPlayer = m_connectedPlayers.get(m_gameMasterIndex);
                if (player.getNickname().equals(gameMasterPlayer.getNickname())) {
                    System.exit(-1);
                }
            }
            case UPDATE_SCORE: {
                Player gameMasterPlayer = m_connectedPlayers.get(m_gameMasterIndex);
                if (player.getNickname().equals(gameMasterPlayer.getNickname())) {
                    UpdateScorePayload payload = (UpdateScorePayload) packet.getPayload();
                    int[] erros = payload.getCurrentErrors();
                    int[] scores = payload.getCurrentScores();
                    for (int i = 0; i < m_connectedPlayers.size(); i++) {
                        m_connectedPlayers.get(i).setScore(scores[i]);
                        m_connectedPlayers.get(i).setErrorsThisTurn(erros[i]);
                    }
                } else {
                    System.out.println("Received an invalid UPDATE_SCORE from player: " + player.getNickname());
                }
                break;
            }
            case UPDATE_WORD: {
                Player gameMasterPlayer = m_connectedPlayers.get(m_gameMasterIndex);
                if (player.getNickname().equals(gameMasterPlayer.getNickname())) {
                    UpdateWordPayload payload = (UpdateWordPayload) packet.getPayload();

                    System.out.println("The updated word is " + payload.getCurrentWord());
                    System.out.println("The letters guessed until now are: " + payload.getCurrentGuessedLetters());

                    m_partlyGuessedWord = payload.getCurrentWord();
                    m_lettersGuessed = payload.getCurrentGuessedLetters();

                    printConnectedPlayersList();
                } else {
                    System.out.println("Received an invalid UPDATE_WORD from player: " + player.getNickname());
                }
                break;
            }
            case NEXT_TURN: {
                Player gameMasterPlayer = m_connectedPlayers.get(m_gameMasterIndex);
                if(player.getNickname().equals(gameMasterPlayer.getNickname())) {

                    do {
                        m_guessingPlayerIndex = (m_guessingPlayerIndex + 1) % m_connectedPlayers.size();
                    } while(m_guessingPlayerIndex == m_gameMasterIndex);

                    m_letterGuessed = false;
                    m_wordGuessed = false;

                    printConnectedPlayersList();

                    System.out.println(m_connectedPlayers.get(m_guessingPlayerIndex).getNickname() + "'s turn.");
                } else {
                    System.out.println("Received an invalid NEXT_TURN from player: " + player.getNickname());
                }
                break;
            }
            case NEXT_GM:
                Player gameMasterPlayer = m_connectedPlayers.get(m_gameMasterIndex);
                if(player.getNickname().equals(gameMasterPlayer.getNickname())) {
                    m_gameMasterIndex = (m_gameMasterIndex + 1) % m_connectedPlayers.size();
                    m_guessingPlayerIndex = (m_gameMasterIndex + 1) % m_connectedPlayers.size();

                    resetTurn();

                    printConnectedPlayersList();

                    System.out.println(m_connectedPlayers.get(m_gameMasterIndex).getNickname() + "'s turn as GM.");
                    System.out.println(m_connectedPlayers.get(m_guessingPlayerIndex).getNickname() + "'s turn.");
                } else {
                    System.out.println("Received an invalid NEXT_GM from player: " + player.getNickname());
                }
                break;
        }

    }

    public int MAX_TIMEOUTS = 3;
    public int MAX_ERRORS = 5;

    public long timeoutInitialTime = 0;
    public final float TIMEOUT_TIME = 15;
    public boolean waitingForGamePacket = false;
    public void handleTimeout()
    {

        waitingForGamePacket = false;
        Player disconnectedPlayer = m_connectedPlayers.get(m_guessingPlayerIndex);
        disconnectedPlayer.incrementTimeouts();
        System.out.println("Player " + disconnectedPlayer.getNickname() + " timeout.");
        if(disconnectedPlayer.getTimeouts() >= MAX_TIMEOUTS)
        {
            System.out.println("Game ending due to multiple timeouts of player " + disconnectedPlayer.getNickname());
            sendQuitGamePacket();
            System.exit(-1);
        }
        sendNextTurn();
    }

    public void run() throws IOException {
        System.out.print("Enter nickname: ");
        String nickname = m_br.readLine();

        //Initializing keys
        m_playerKeyPair = RSAEncryptDecrypt.createKeys();

        //Generate random number that defines the order in the connected list.
        int rnd = ThreadLocalRandom.current().nextInt(0, 100);

        g_currentPlayer = new Player(nickname, rnd);
        g_currentPlayer.setPublicKey(m_playerKeyPair.getPublic());

        //NOTE: FOR THIS TO WORK IN MAC OS X PLEASE USE -Djava.net.preferIPv4Stack=true.
        //SOURCE: https://github.com/bluestreak01/questdb/issues/23
        m_socket = new SocketListenner(PORT, InetAddress.getByName(GROUP_ADDR));
        m_socket.setSocketActionsListenner(this);
        new Thread(m_socket).start();

        GamePacket encriptedName = new GamePacket();
        //SEND A ENCRYPTED MESSAGE WITH ITS OWN NAME
        while(true)
        {
            try
            {
                Thread.sleep(500);
                if(!m_gameReady)
                {
                    if(m_connectedPlayers.size() < 2) {
                        sendConnect();
                    }else {
                        m_gameReady = true;

                        connectPlayer(g_currentPlayer);

                        // Send the last connect packet
                        sendConnect();

                        m_gameMasterIndex = 0;
                        m_guessingPlayerIndex = 1;
                    }
                }
                else
                {

                    //TIMEOUT HANDLING CODE
                    if(waitingForGamePacket && m_connectedPlayers.get(m_gameMasterIndex) == g_currentPlayer)
                    {
                        System.out.print(".");
                        if(System.currentTimeMillis() - timeoutInitialTime > TIMEOUT_TIME*1000)
                        {
                            handleTimeout();
                        }
                    }



                    if(m_connectedPlayers.get(m_guessingPlayerIndex) == g_currentPlayer &&
                            !m_partlyGuessedWord.isEmpty()) { // My turn to guess
                        if(!m_letterGuessed) {
                            System.out.print("Guess the letter(empty to pass your turn): ");
                            String input = m_br.readLine();
                            char guessedLetter;
                            if(input.isEmpty())
                            {
                                guessedLetter = '0';
                            }
                            else
                            {
                                guessedLetter = input.charAt(0);
                            }
                            sendGuessedLetter(guessedLetter);
                            m_letterGuessed = true;
                        }else if(!m_wordGuessed) {
                            System.out.print("Guess the word(empty to pass your turn): ");
                            String guessedWord = m_br.readLine();

                            sendGuessedWord(guessedWord);
                            m_wordGuessed = true;
                        }
                    }
                    if(m_connectedPlayers.get(m_gameMasterIndex) == g_currentPlayer) { // My turn to generate
                        if(!m_wordGenerated) {
                            generateWord();
                            m_wordGenerated = true;
                            sendUpdateWordPacket();
                            //ENABLE WAIT FOR PACKET
                            waitingForGamePacket = true;
                            timeoutInitialTime = System.currentTimeMillis();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendConnect() {
        GamePacket packet = new GamePacket();
        packet.setPlayer(g_currentPlayer);
        packet.setAction(GamePacket.Actions.CONNECTED);
        packet.sign(m_playerKeyPair.getPrivate());

        try {
            m_socket.sendGamePacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendGuessedWord(String guessedWord) {
        GamePacket packet = new GamePacket();
        packet.setPlayer(g_currentPlayer);
        packet.setAction(GamePacket.Actions.WORD_GUESS);
        packet.setPayload(new GuessPayload(String.valueOf(guessedWord)));
        packet.sign(m_playerKeyPair.getPrivate());

        try {
            m_socket.sendGamePacket(packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void sendGuessedLetter(char guessedLetter) {
        GamePacket packet = new GamePacket();
        packet.setPlayer(g_currentPlayer);
        packet.setAction(GamePacket.Actions.LETTER_GUESS);
        packet.setPayload(new GuessPayload(String.valueOf(guessedLetter)));
        packet.sign(m_playerKeyPair.getPrivate());

        try {
            m_socket.sendGamePacket(packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void printConnectedPlayersList() {
        int index = 0;
        System.out.println("Player: " + g_currentPlayer.getNickname() + " ========================");
        for(Player p : m_connectedPlayers) {
            if(index == m_gameMasterIndex) {
                System.out.format("*%-9s Order: %3d Score: %3d Errors: %3d\n",
                        p.getNickname(),
                        p.getRndOrder(),
                        p.getScore(),
                        p.getErrorsThisTurn());
            } else if(index == m_guessingPlayerIndex){
                System.out.format(">%-9s Order: %3d Score: %3d Errors: %3d\n",
                        p.getNickname(),
                        p.getRndOrder(),
                        p.getScore(),
                        p.getErrorsThisTurn());
            } else {
                System.out.format("%-10s Order: %3d Score: %3d Errors: %3d\n",
                        p.getNickname(),
                        p.getRndOrder(),
                        p.getScore(),
                        p.getErrorsThisTurn());
            }
            index++;
        }
    }

    private void generateWord()
    {
        m_partlyGuessedWord = "";
        System.out.print("You're the new game master. Type the word for the new game: ");
        try {
            m_wordToGuess = m_br.readLine();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        for(int i = 0; i < m_wordToGuess.length(); i++)
        {
            m_partlyGuessedWord = m_partlyGuessedWord + "_";
        }
    }

    public static void main(String[] args) throws IOException {
        Game game = new Game();
        game.run();
    }

}
