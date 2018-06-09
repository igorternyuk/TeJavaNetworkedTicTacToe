package networkedtictactoe.gameserver;

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import networkedtictactoe.Board;
import networkedtictactoe.GameResult;
import networkedtictactoe.GameStatus;
import networkedtictactoe.MessageType;
import networkedtictactoe.PlayerType;

/**
 *
 * @author igor
 */
public class GameServer {
    private static final int PLAYER_COUNT_MAX = 2;
    private static final int BOARD_SIZE = 3;
    private Board board;
    private int numPlayers = 0;
    private Point movePlayerO, movePlayerX;
    private GameStatus gameStatus = GameStatus.WAINTING_FOR_OPPONENT;
    
    private int port;
    private ServerSocket serverSocket;
    private ServerSideConnection playerO;
    private ServerSideConnection playerX;
    
    public GameServer(int port){
        System.out.println("---Game server---");
        this.board = new Board();
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
                System.out.println("Accepting loop...");
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
                System.out.println("Starting thread...");
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
    
    private void checkGameStatus(){
        GameResult result = this.board.checkGameResult();
        switch(result){
            case CIRCLES_WON:
                gameStatus = GameStatus.O_WON;
                break;
            case CROSSES_WON:
                gameStatus = GameStatus.X_WON;
                break;
            case TIE:
                gameStatus = GameStatus.TIE;
                break;
            case PLAYING:
                break;                     
        }
    }
    
    private class ServerSideConnection implements Runnable {
        private Socket socket;
        private int playerId;
        private PlayerType playerType;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private boolean hasClosedConnection = false;
        
        public ServerSideConnection(Socket socket, int playerID){
            try {
                System.out.println("--Server side connection for player #"
                        + playerID + "---");
                this.socket = socket;
                this.playerId = playerID;
                this.playerType = (playerID == 1)
                                ? PlayerType.Circle
                                : PlayerType.Cross;
                this.oos = new ObjectOutputStream(this.socket.getOutputStream());
                this.ois = new ObjectInputStream(this.socket.getInputStream());
                System.out.println("ClientSideConnection constructor finished its work");
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }

        @Override
        public void run() {
            System.out.println("Starting ServerSideConnection thread for "
                    + "player #" + playerId);
            try {
                sendPlayerType();
                sendBoard(board);
                while(!gameStatus.isGameOver()){
                    checkGameStatus();
                    receiveMessage();
                }
                System.out.println("---GAME OVER---");
            } catch (IOException ex) {
                System.out.println("IOException from ClientSideConnection run()");
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            } finally {
                closeConnection();
            }
        }
        
        
        private void receiveMessage() throws IOException{
            try {
                MessageType code = (MessageType)this.ois.readObject();
                switch(code){
                    case MOVE:
                        System.out.println("Server side connection for player #"
                            + playerId);
                        if(this.playerType.equals(PlayerType.Circle)){
                            movePlayerO = (Point)this.ois.readObject();
                            System.out.println("received movePlayerO = "
                                    + movePlayerO);
                            System.out.println("x = " + movePlayerO.x
                                    + " y = " + movePlayerO.y);
                            boolean isMoveAccepted = board.tryToMove(
                                    movePlayerO, this.playerType);
                            sendMoveAccepted();
                            if(isMoveAccepted){
                                if(playerX != null){
                                    playerX.sendMove(movePlayerO);
                                }
                            }
                        } else if(this.playerType.equals(PlayerType.Cross)) {
                            movePlayerX = (Point)this.ois.readObject();
                            System.out.println("received movePlayerX = "
                                    + movePlayerX);
                            System.out.println("x = " + movePlayerX.x
                                    + " y = " + movePlayerX.y);
                            boolean isMoveAccepted = board.tryToMove(
                                    movePlayerX, this.playerType);
                            sendMoveAccepted();
                            if(isMoveAccepted){
                                if(playerO != null){
                                    playerO.sendMove(movePlayerX);
                                }
                            }
                            
                        }
                        board.print();
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
                        if(playerO != null){
                            playerO.sendGameStatus(gameStatus);
                            playerO.sendBoard(board);
                        }
                        if(playerX != null){
                            playerX.sendGameStatus(gameStatus);
                            playerX.sendBoard(board);
                        }
                        break;
                    case DISCONNECTION :
                        if(this.ois.readBoolean()){
                            gameStatus = GameStatus.OPPONENT_DISCONNECTED;
                            if(this.playerType.equals(PlayerType.Circle)){
                                System.out.println("Closing Player O window");
                                System.out.println("Checking if player X closed connection");
                                System.out.println("playerX.hasClosedConnection = " + playerO.hasClosedConnection);

                                if(playerX != null && !playerX.hasClosedConnection){
                                    playerX.sendGameStatus(gameStatus);
                                }
                                playerO.hasClosedConnection = true;
                                System.out.println("playerO.hasClosedConnection = " + playerO.hasClosedConnection);
                            } else if(this.playerType.equals(PlayerType.Cross)){
                                System.out.println("Closing Player X window");
                                System.out.println("Checking if player O closed connection");
                                System.out.println("playerX.hasClosedConnection = " + playerX.hasClosedConnection);
                                if(playerO != null && !playerO.hasClosedConnection){
                                    playerO.sendGameStatus(gameStatus);                                    
                                }
                                System.out.println("");
                                //playerX.closeConnection();
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
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        private void sendPlayerType() throws IOException{
            this.oos.writeObject(MessageType.PLAYER_TYPE);
            this.oos.writeObject(this.playerType);
            this.oos.flush();
            System.out.println("Sending playerType from server "
                    + this.playerType);
        }
        
        private void sendMoveAccepted() throws IOException{
            try {
                this.oos.writeObject(MessageType.MOVE_ACCEPTED);
                this.oos.writeBoolean(true);
                this.oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        private void sendMove(Point move) throws IOException{
            try {
                System.out.println("Sending move from server " + move);
                this.oos.writeObject(MessageType.MOVE);
                System.out.println("Writing move " + move);
                this.oos.writeObject(move);
                this.oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        private void sendBoard(Board b){
            try {
                System.out.println("Sending board from server to player "
                        + playerType.getMoveSign());
                this.oos.writeObject(MessageType.BOARD);
                Board brd = new Board(b);
                System.out.println("brd:");
                brd.print();
                this.oos.writeObject(brd);
                this.oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        private void sendGameStatus(GameStatus status) throws IOException{
            System.out.println("Sending game status " + status);
            try {
                this.oos.writeObject(MessageType.GAME_STATUS);
                this.oos.writeObject(status);
                this.oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        private void sendSecondPlayerReadiness() throws IOException{
            try {
                this.oos.writeObject(MessageType.OPPONENT_READINESS);
                this.oos.writeBoolean(true);
                this.oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        public void sendSpotCoordinates() throws IOException{
            try {
                this.oos.writeObject(MessageType.WINNIG_LINE_SPOTS);
                this.oos.writeObject(board.getFirstSpot());
                this.oos.writeObject(board.getSecondSpot());
                this.oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        public void closeConnection(){
            try {
                this.ois.close();
                this.oos.close();
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
