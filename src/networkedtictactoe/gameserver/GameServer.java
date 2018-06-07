package networkedtictactoe.gameserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import networkedtictactoe.GameStatus;
import networkedtictactoe.MessageType;

/**
 *
 * @author igor
 */
public class GameServer {
    private static final int PLAYER_COUNT_MAX = 2;
    private static final int BOARD_SIZE = 3;
    private int port;
    private ServerSocket serverSocket;
    private int numPlayers = 0;
    private ServerSideConnection playerO;
    private ServerSideConnection playerX;
    private String[][] board = {
        { " ", " ", " " },
        { " ", " ", " " },
        { " ", " ", " " }
    };
    private int movePlayerO, movePlayerX;
    private GameStatus gameStatus;
    private int x1, y1, x2, y2;
    
    public GameServer(int port){
        System.out.println("---Game server---");
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }
    
    public void acceptConnections(){
        System.out.println("Waiting for players...");
        try{
            while(numPlayers < PLAYER_COUNT_MAX){
                Socket socket = this.serverSocket.accept();
                ++numPlayers;
                System.out.println("Player #" + numPlayers + " connected");
                ServerSideConnection ssc = new ServerSideConnection(socket,
                        numPlayers);
                if(numPlayers == 1){
                    playerO = ssc;
                    gameStatus = GameStatus.WAINTING_FOR_OPPONENT;
                } else if(numPlayers == 2) {
                    playerX = ssc;
                    playerO.sendSecondPlayerReadiness();
                    gameStatus = GameStatus.O_TO_PLAY;
                    System.out.println("Opponent connected");
                    playerO.sendGameStatus(gameStatus);
                    playerX.sendGameStatus(gameStatus);

                }
                Thread thread = new Thread(ssc);
                thread.start();
            }
            System.out.println("Two players connected.No longer accepting "
                    + "connections.");
        }
        catch(IOException ex){
            Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                    null, ex);    
        }
    }
    
    public void printBoard(){
        System.out.println("------");
        for(int i = 0; i < board.length; ++i){
            for(int j = 0; j < board[i].length; ++j){
                System.out.print(board[i][j] + "|");
            }
            System.out.println("\n------");
        }
    }
    
    private int countFreeSpots(){
        int count = 0;
        for(int i = 0; i < BOARD_SIZE; ++i){
            for(int j = 0; j < BOARD_SIZE; ++j){
                if(board[i][j].equals(" ")){
                    ++count;
                }
            }
        }
        return count;
    }
    
    private void checkGameStatus(){
        System.out.println("Checking game status");
        //Check rows
        outer:
        for(int i = 0; i < board.length; ++i){
            String first = board[i][0];
            if(first.isEmpty()) continue;
            for(int j = 1; j < board[i].length; ++j){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = 0;
            y1 = i;
            x2 = BOARD_SIZE - 1;
            y2 = y1;
            return;
        }
        
        //Check columns
        outer:
        for(int j = 0; j < BOARD_SIZE; ++j){
            String first = board[0][j];
            if(first.isEmpty()) continue;
            for(int i = 1; i < BOARD_SIZE; ++i){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = j;
            y1 = 0;
            x2 = x1;
            y2 = BOARD_SIZE - 1;
            return;
        }
        
        //Check main diagonal
        String first = board[0][0];
        boolean isMainDiagonalFilled = true;
        if(first.isEmpty()){
            isMainDiagonalFilled = false;
        } else {
            for(int i = 1; i < BOARD_SIZE; ++i){
                first = board[0][0];
                if(!board[i][i].equalsIgnoreCase(first)){
                    isMainDiagonalFilled = false;
                    break;
                }
            }
        }
        
        if(isMainDiagonalFilled){
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = 0;
            y1 = 0;
            x2 = BOARD_SIZE - 1;
            y2 = BOARD_SIZE - 1;
            return;
        }
        
        //Check secondary diagonal
        boolean isSecondaryDiagonalFilled = true;
        first = board[0][BOARD_SIZE - 1];
        if(first.isEmpty()){
            isSecondaryDiagonalFilled = false;
        } else {
            for(int i = 1; i < BOARD_SIZE; ++i){
                if(!board[i][BOARD_SIZE - 1 - i].equalsIgnoreCase(first)){
                    isSecondaryDiagonalFilled = false;
                    break;
                }
            }
        }
        
        if(isSecondaryDiagonalFilled){
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = BOARD_SIZE - 1;
            y1 = 0;
            x2 = 0;
            y2 = BOARD_SIZE - 1;
            return;
        }
        if(countFreeSpots() == 0)
            gameStatus = GameStatus.TIE;
    }
    
    private class ServerSideConnection implements Runnable {
        private Socket socket;
        private int playerId;
        private boolean isCircle;
        private DataInputStream dis;
        private DataOutputStream dos;
        private boolean hasClosedConnection = false;
        
        public ServerSideConnection(Socket socket, int playerID){
            try {
                System.out.println("--Server side connection for player #"
                        + playerID + "---");
                this.socket = socket;
                this.playerId = playerID;
                this.isCircle = (playerID == 1);
                this.dis = new DataInputStream(this.socket.getInputStream());
                this.dos = new DataOutputStream(this.socket.getOutputStream());
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }

        @Override
        public void run() {
            try {
                this.dos.writeInt(MessageType.IS_CIRCLE.ordinal());
                this.dos.writeBoolean(this.isCircle);
                this.dos.flush();
                System.out.println("Sending is circle from server " + isCircle);
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
            
            while(true){
                checkGameStatus();
                try{
                    receiveMessage();
                } catch(IOException ex){
                    
                }
                if(gameStatus.isGameOver()){
                    System.out.println("---GAME OVER---");
                    sendServerShutdown();
                    break;
                }
            }
            //closeConnection();
        }
        
        private void sendServerShutdown(){
            try {
                this.dos.writeInt(MessageType.SERVER_SHUTDOWN.ordinal());
                this.dos.writeBoolean(true);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        private void receiveMessage() throws IOException{
            try {
                int code = this.dis.readInt();
                MessageType type = MessageType.values()[code];
                switch(type){
                    case MOVE:
                        System.out.println("Server side connection for player #"
                            + playerId);
                        if(this.isCircle){
                            movePlayerO = this.dis.readInt();
                            System.out.println("received movePlayerO = "
                                    + movePlayerO);
                            int y = movePlayerO / BOARD_SIZE;
                            int x = movePlayerO % BOARD_SIZE;
                            System.out.println("x = " + x + " y = " + y);
                            board[y][x] = "O";
                            printBoard();
                            if(playerX != null){
                                playerX.sendMove(movePlayerO);
                            }
                        } else {
                            movePlayerX = this.dis.readInt();
                            System.out.println("received movePlayerX = "
                                    + movePlayerX);
                            int y = movePlayerX / BOARD_SIZE;
                            int x = movePlayerX % BOARD_SIZE;
                            System.out.println("x = " + x + " y = " + y);
                            board[y][x] = "X";
                            printBoard();
                            if(playerO != null)
                            playerO.sendMove(movePlayerX);
                        }
                        checkGameStatus();
                        if(gameStatus.isGameOver()){
                            sendSpotCoordinates();
                        } else {
                            if(gameStatus == GameStatus.X_TO_PLAY){
                                System.out.println("Changing turn to O");
                                gameStatus = GameStatus.O_TO_PLAY;
                            } else if(gameStatus == GameStatus.O_TO_PLAY){
                                gameStatus = GameStatus.X_TO_PLAY;
                                System.out.println("Changing turn to X");
                            }
                        }
                        if(playerO != null)
                            playerO.sendGameStatus(gameStatus);
                        if(playerX != null)
                            playerX.sendGameStatus(gameStatus);
                        break;
                    case DISCONNECTION :
                        if(this.dis.readBoolean()){
                            gameStatus = GameStatus.OPPONENT_DISCONNECTED;
                            if(isCircle){
                                System.out.println("Closing Player O window");
                                System.out.println("Checking if player X closed connection");
                                System.out.println("playerX.hasClosedConnection = " + playerO.hasClosedConnection);

                                if(playerX != null && !playerX.hasClosedConnection){
                                    playerX.sendGameStatus(gameStatus);
                                }
                                playerO.closeConnection();
                                playerO.hasClosedConnection = true;
                                System.out.println("playerO.hasClosedConnection = " + playerO.hasClosedConnection);
                            } else {
                                System.out.println("Closing Player X window");
                                System.out.println("Checking if player O closed connection");
                                System.out.println("playerX.hasClosedConnection = " + playerX.hasClosedConnection);
                                if(playerO != null && !playerO.hasClosedConnection){
                                    playerO.sendGameStatus(gameStatus);                                    
                                }
                                System.out.println("");
                                playerX.closeConnection();
                                playerX.hasClosedConnection = true;
                                System.out.println("playerX.hasClosedConnection = " + playerX.hasClosedConnection);
                            }
                            hasClosedConnection = true;
                        }
                        break;
                }
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        private void sendMove(int move) throws IOException{
            try {
                System.out.println("Sending move from server " + move);
                this.dos.writeInt(MessageType.MOVE.ordinal());
                System.out.println("Writing move " + move);
                this.dos.writeInt(move);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        private void sendGameStatus(GameStatus status) throws IOException{
            System.out.println("Sending game status " + status);
            try {
                System.out.println("status.ordinal() = " + status.ordinal());
                this.dos.writeInt(MessageType.GAME_STATUS.ordinal());
                this.dos.writeInt(status.ordinal());
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        private void sendSecondPlayerReadiness() throws IOException{
            try {
                this.dos.writeInt(MessageType.OPPONENT_READINESS.ordinal());
                this.dos.writeBoolean(true);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        public void sendSpotCoordinates() throws IOException{
            try {
                this.dos.writeInt(MessageType.WINNIG_LINE_SPOTS.ordinal());
                this.dos.writeInt(x1);
                this.dos.writeInt(y1);
                this.dos.writeInt(x2);
                this.dos.writeInt(y2);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        public void closeConnection(){
            try {
                this.socket.close();
                System.out.println("Player #" + playerId + " closed connection.");
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
    }
    
    public static void main(String[] args) {
        GameServer server = new GameServer(55555);
        server.acceptConnections();
    }
}
