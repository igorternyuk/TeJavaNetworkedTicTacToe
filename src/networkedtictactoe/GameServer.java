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
    private ServerSideConnection playerO, playerX;
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
                } else {
                    playerX = ssc;
                    playerO.sendSecondPlayerReadiness();
                    gameStatus = GameStatus.O_TO_PLAY;
                    System.out.println("Opponent connected");
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
        //Check rows
        outer:
        for(int i = 0; i < board.length; ++i){
            String first = board[i][0];
            for(int j = 1; j < board[i].length; ++j){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            gameStatus = first.equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
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
            for(int i = 1; i < BOARD_SIZE; ++i){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            gameStatus = first.equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
            /*firstSpot.x = j * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = firstSpot.x;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;*/
            return;
        }
        
        //Check main diagonal
        boolean isMainDiagonalFilled = true;
        for(int i = 1; i < BOARD_SIZE; ++i){
            String first = board[0][0];
            if(!board[i][i].equalsIgnoreCase(first)){
                isMainDiagonalFilled = false;
                break;
            }
        }
        
        if(isMainDiagonalFilled){
            gameStatus = board[0][0].equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
            /*firstSpot.x = SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = CANVAS_SIZE - SPOT_SIZE / 2;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;*/
            return;
        }
        
        //Check secondary diagonal
        boolean isSecondaryDiagonalFilled = true;
        for(int i = 1; i < BOARD_SIZE; ++i){
            String first = board[0][BOARD_SIZE - 1];
            if(!board[i][BOARD_SIZE - 1 - i].equalsIgnoreCase(first)){
                isSecondaryDiagonalFilled = false;
                break;
            }
        }
        
        if(isSecondaryDiagonalFilled){
            gameStatus = board[0][BOARD_SIZE - 1].equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
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
                this.socket = socket;
                this.playerId = playerID;
                this.isCircle = playerID == 1;
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
                this.dos.writeBoolean(this.isCircle);
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
            
            while(true){
                checkGameStatus();
                if(gameStatus.isGameOver()){
                    System.out.println("---GAME OVER---");
                    break;
                }
                
                try{
                    if(this.isCircle){
                        movePlayerO = this.dis.readInt();
                        int y = movePlayerO / BOARD_SIZE;
                        int x = movePlayerO % BOARD_SIZE;
                        board[y][x] = "O";
                        playerX.sendMove(movePlayerO);
                    } else {
                        movePlayerX = this.dis.readInt();
                        int y = movePlayerX / BOARD_SIZE;
                        int x = movePlayerX % BOARD_SIZE;
                        board[y][x] = "X";
                        playerO.sendMove(movePlayerX);
                        
                    }
                    checkGameStatus();
                    playerO.sendGameStatus(gameStatus);
                    playerX.sendGameStatus(gameStatus);
                } catch(IOException ex){
                    
                }
            }
            closeConnection();
        }
        
        public void sendMove(int move){
            try {
                this.dos.write(move);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public void sendGameStatus(GameStatus status){
            try {
                this.dos.writeInt(status.ordinal());
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public void sendSecondPlayerReadiness(){
            try {
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
