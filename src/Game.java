import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Comparator;

/*
 * This class implements the ISocketActions class, and it is
 * where most of the hard work of the processes happen.
 */
public class Game implements ISocketActions {

    //CONSTANTS
    public static final String GROUP_ADDR = "228.5.6.7";
    public static final int PORT = 6789;
    public static final int MAX_TIMEOUTS = 3;
    public static final int MAX_ERRORS = 5;
    public static final int TIMEOUT_TIME = 60;

    //This refers to the player of the current process
    public static Player g_currentPlayer;

    //Used when player is the gameMaster. Index that refers to the player that is currently playing.
    private int m_guessingPlayerIndex = 0;

    //Index of the current gameMaster.
    private int m_gameMasterIndex = 0;

    private SocketListenner m_socket;

    private KeyPair m_playerKeyPair;

    //True when enough players are connected and ready to play.
    private boolean m_gameReady = false;

    //Reader initialization, to ask for information from the standard input.
    private BufferedReader m_br = new BufferedReader(new InputStreamReader(System.in));

    //List of connected players on the game. Includes the current player too.
    private List<Player> m_connectedPlayers = new ArrayList<>();

    //Variables used by the gameMaster to check which letters have been already guessed,
    //which word should be guessed and how is the currently guessed word.
    private String m_lettersGuessed = "";
    private String m_wordToGuess = null;
    private String m_partlyGuessedWord = "";

    //Booleans to demarcate the current phases of the game.
    private boolean m_letterGuessed = false;
    private boolean m_wordGuessed = false;
    private boolean m_wordGenerated = false;

    //Variables to check for player timeOut.
    private long timeoutInitialTime = 0;
    private boolean waitingForGamePacket = false;

    /**
     * @return an array of integers that represents the scores of all the players in order.
     */
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

    /**
     * @return an array of integers that represents the number of errors of all the players in order.
     */
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

    /**
     * Given a Player object that was received through a message, find the object in the connected
     * players list that relates to the received player and return it.
     * @param player - received through a message
     * @return player on the connected list that is the same as the received player
     */
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

    /**
     * Sends a GamePacket with the updated Scores for all players
     */
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

    /**
     * Warns all the player that the game had a problem and the process should quit.
     */
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

    /**
     * Sends a GamePacket with the currently guessed part of the word that should be guessed.
     */
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

    /**
     * Receives a player and adds it to the connected list if it passes all criteria.
     * @param player - player that should be added to the connected player list.
     */
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
            Collections.sort(m_connectedPlayers, new Comparator<Player>() {
                public int compare(Player o1, Player o2) {
                    return o1.getRndOrder() - o2.getRndOrder();
                }
            });
//            m_connectedPlayers.sort(new Comparator<Player>() {
//                public int compare(Player o1, Player o2) {
//                    return o1.getRndOrder() - o2.getRndOrder();
//                }
//            });
            printConnectedPlayersList();
        }
    }

    /**
     * Function called when a LETTER_GUESS game packet is received. Checks if it is a valid
     * packet and then processes the play.
     * @param player - player that send the letter
     * @param letter - letter guess
     * @return false if a valid player skipped its turn, true otherwise
     */
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
                //Checks if letter was already guessed
                boolean letterAlreadyGuessed = false;
                if(m_lettersGuessed.contains(String.valueOf(letter)))
                {
                    System.out.println("You are trying a letter that has already been tried. You will get an error for that.");
                    letterAlreadyGuessed = true;
                }
                if(!letterAlreadyGuessed)
                    m_lettersGuessed += letter + " ";
                if (!letterAlreadyGuessed && m_wordToGuess.contains(String.valueOf(letter))) {
                    //Guessed letter correctly
                    System.out.println(player.getNickname() + " got '" + letter + "' right. ");
                    for (int i = 0; i < m_wordToGuess.length(); i++) {
                        if (m_wordToGuess.charAt(i) == letter) {
                            m_partlyGuessedWord = m_partlyGuessedWord.substring(0, i) + letter + m_partlyGuessedWord.substring(i + 1, m_wordToGuess.length());
                        }
                    }
                    System.out.println("m_wordToGuess = " + m_wordToGuess + " and partlyGuessed = " + m_partlyGuessedWord);
                } else {
                    //Wrong letter guess.
                    Player playerThatGotWrong = findPlayer(player);
                    playerThatGotWrong.incrementErrorsThisTurn();
                    System.out.println(player.getNickname() + " got '" + letter + "' wrong.");
                }

                sendUpdateScorePakcet();

                sendUpdateWordPacket();

            }
        }
        return true;
    }

    /**
     * Function called when a WORD_GUESS game packet is received. Checks if it is a valid
     * packet and then processes the play.
     * @param player - player that send the letter
     * @param word - word guess
     */
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
                    //Word guessed correctly, the game has been won by guessingPlayer.
                    findPlayer(player).incrementScore();
                    System.out.println("Player " + player.getNickname() + " won the game by getting the word " + m_wordToGuess + " correctly.");

                    resetTurn();

                    sendUpdateScorePakcet();

                    sendNextGM();
                } else {
                    //Wrong word guess, proceed to the next turn.
                    System.out.println("Too bad, wrong guess.");
                    sendNextTurn();
                }
            }
        }
    }

    /**
     * Function called when GameMaster 1 is notifying GameMaster 2 that a
     * game has just ended and that GameMaster 2 should now begin a new
     * game.
     */
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

    /**
     * Resets all game variables to their default values
     * when a new game is about to begin.
     */
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

    /**
     * Notify the players that a turn just ended and that the next
     * player should guess the letter now.
     */
    private void sendNextTurn() {
        calcNextGuessingIndex();

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

    /**
     * Calculates the next player that should guess the letter, considering
     * that the GameMaster does not play and that players that
     * exceeded the max number of errors should also not play.
     */
    private void calcNextGuessingIndex() {
        int tries = 0;
        do {
            m_guessingPlayerIndex = (m_guessingPlayerIndex + 1) % m_connectedPlayers.size();
            if(tries++ > m_connectedPlayers.size()) return;
        } while(m_guessingPlayerIndex == m_gameMasterIndex ||
                m_connectedPlayers.get(m_guessingPlayerIndex).getErrorsThisTurn() >= MAX_ERRORS);
    }

    /**
     * Handles the packet received from other players according to their inner Actions.
     * @param packet - packet received from other players.
     */
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

                //Handles turn skipping
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
                    System.out.println("Closing the game, player disconnected due excess of timeouts!!");
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

                    calcNextGuessingIndex();

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

    /**
     * Function called when a timeOut has occurred. Finds the player
     * that has timed out, increases its current timeOut counter
     * and check if the player that has timeout'ed hasn't maxed
     * the timeout allowance.
     */
    private void handleTimeout()
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

    /**
     * Main thread of the player process. Handles most
     * game logic.
     * @throws IOException
     */
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
        //Creates a new SocketListenner that needs PORT and GROUP_ADDR to create the MultiCastSocket object
        m_socket = new SocketListenner(PORT, InetAddress.getByName(GROUP_ADDR));
        //Sets the SocketActionsListenner to this class, so the gamePacketReceived method of this class is called whenever
        //a new message is received.
        m_socket.setSocketActionsListenner(this);
        //Starts the socket run method.
        new Thread(m_socket).start();

        while(true)
        {
            try
            {
                Thread.sleep(500);
                //If the game is not ready, not enough players have connected yet, so send the connection packets.
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
                //ENOUGH PLAYERS ARE CONNECTED, GAME SHOULD RUN
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

                    // My turn to guess
                    if(m_connectedPlayers.get(m_guessingPlayerIndex) == g_currentPlayer &&
                            !m_partlyGuessedWord.isEmpty()) {
                        if(!m_letterGuessed) {
                            System.out.print("Guess the letter(empty to pass your turn): ");
                            String input = m_br.readLine();
                            char guessedLetter;
                            if(input.isEmpty())
                            {
                                //char that represents the turn skipping
                                guessedLetter = '0';
                            }
                            else
                            {
                                guessedLetter = input.charAt(0);
                            }

                            //CHECK IF A TIMEOUT HAS NOT OCCURRED AND YOU LOST YOUR TURN
                            if(m_connectedPlayers.get(m_guessingPlayerIndex) == g_currentPlayer)
                            {
                                sendGuessedLetter(guessedLetter);
                                m_letterGuessed = true;
                            }
                            else
                            {
                                System.out.println("Guess was not sent. You(or the connection) took to long and your turn timed out.");
                            }
                        }else if(!m_wordGuessed) {
                            System.out.print("Guess the word(empty to pass your turn): ");
                            String guessedWord = m_br.readLine();

                            //CHECK IF A TIMEOUT HAS NOT OCCURRED AND YOU LOST YOUR TURN
                            if(m_connectedPlayers.get(m_guessingPlayerIndex) == g_currentPlayer)
                            {
                                sendGuessedWord(guessedWord);
                                m_wordGuessed = true;
                            }
                            else
                            {
                                System.out.println("Guess was not sent. You(or the connection) took to long and your turn timed out.");
                            }
                        }
                    }
                    // A new game has just started and I'm the GameMaster. My turn to generate a new word.
                    if(m_connectedPlayers.get(m_gameMasterIndex) == g_currentPlayer) {
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

    /**
     * Send connection packet.
     */
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

    /**
     * Send guessed word packet.
     * @param guessedWord - guess
     */
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

    /**
     * Send guessed letter packet.
     * @param guessedLetter - guess
     */
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

    /**
     * Prints the connected player list, with their scores and errors on the current turn.
     */
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

    /**
     * Generates a word for the new game and assing the relevant variables.
     */
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

    /**
     * On the start of a new process, executes the game.run method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Game game = new Game();
        game.run();
    }

}
