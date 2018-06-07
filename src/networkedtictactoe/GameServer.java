package networkedtictactoe;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        for(int i = 0; i < board.length; ++i){
            for(int j = 0; j < board[i].length; ++j){
                System.out.print(board[i][j] + "-");
            }
            System.out.println("");
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
            /*firstSpot.x = SPOT_SIZE / 2;
            firstSpot.y = i * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
            secondSpot.x = CANVAS_SIZE - SPOT_SIZE / 2;
            secondSpot.y = firstSpot.y;*/
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
            /*firstSpot.x = j * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = firstSpot.x;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;*/
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
            /*firstSpot.x = SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = CANVAS_SIZE - SPOT_SIZE / 2;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;*/
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
            /*firstSpot.x = CANVAS_SIZE - SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = SPOT_SIZE / 2;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;*/
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
                this.dos.writeInt(0);
                this.dos.writeBoolean(this.isCircle);
                this.dos.flush();
                System.out.println("Sending is circle from server " + isCircle);
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
            
            while(true){
                checkGameStatus();
                //System.out.println("gameStatus + " + gameStatus);
                if(gameStatus.isGameOver()){
                    System.out.println("---GAME OVER---");
                    break;
                }                
                try{
                    receiveMessage();
                    //TODO if game over send first and second spot coordinates
                } catch(IOException ex){
                    
                }
            }
            closeConnection();
        }
        
        public void receiveMessage() throws IOException{
            try {
                int code = this.dis.readInt();
                switch(code){
                    case 2:
                        System.out.println("Server side connection for player #"
                            + playerId);
                        if(this.isCircle){
                            movePlayerO = this.dis.readInt();
                            System.out.println("received movePlayerO = " + movePlayerO);
                            int y = movePlayerO / BOARD_SIZE;
                            int x = movePlayerO % BOARD_SIZE;
                            System.out.println("x = " + x + " y = " + y);
                            board[y][x] = "O";
                            printBoard();
                            playerX.sendMove(movePlayerO);
                        } else {
                            movePlayerX = this.dis.readInt();
                            System.out.println("received movePlayerX = " + movePlayerX);
                            int y = movePlayerX / BOARD_SIZE;
                            int x = movePlayerX % BOARD_SIZE;
                            System.out.println("x = " + x + " y = " + y);
                            board[y][x] = "X";
                            printBoard();
                            playerO.sendMove(movePlayerX);
                        }
                        checkGameStatus();
                        if(!gameStatus.isGameOver()){
                            if(gameStatus == GameStatus.X_TO_PLAY){
                                System.out.println("Changing turn to O");
                                gameStatus = GameStatus.O_TO_PLAY;
                            } else if(gameStatus == GameStatus.O_TO_PLAY){
                                gameStatus = GameStatus.X_TO_PLAY;
                                System.out.println("Changing turn to X");
                            }
                        }
                        playerO.sendGameStatus(gameStatus);
                        playerX.sendGameStatus(gameStatus);
                        break;
                    case 5:
                        if(this.dis.readBoolean()){
                            gameStatus = GameStatus.OPPONENT_DISCONNECTED;
                            if(isCircle){
                                playerX.sendGameStatus(gameStatus);
                            } else {
                                playerO.sendGameStatus(gameStatus);
                            }
                        }
                        break;
                }
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        public void sendMove(int move){
            try {
                System.out.println("Sending move from server " + move);
                int code = 2;
                this.dos.writeInt(code);
                System.out.println("Writing move " + move);
                this.dos.writeInt(move);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public void sendGameStatus(GameStatus status){
            System.out.println("Sending game status " + status);
            try {
                System.out.println("status.ordinal() = " + status.ordinal());
                int code = 3;
                this.dos.writeInt(code);
                this.dos.writeInt(status.ordinal());
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public void sendSecondPlayerReadiness(){
            try {
                this.dos.writeInt(1);
                this.dos.writeBoolean(true);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
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
