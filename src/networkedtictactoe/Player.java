package networkedtictactoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author igor
 */
public class Player {
    private static final String TITLE_OF_PROGRAM = "NetworkedTicTacToe";
    private static final int SPOT_SIZE = 160;
    private static final int SPOT_GAP = 10;
    private static final int BOARD_SIZE = 3;
    private static final int CANVAS_SIZE = (SPOT_SIZE + SPOT_GAP) * BOARD_SIZE;
    private static final int CONNECTION_PANEL_HEIGHT = 40;
    private static final int WINDOW_WIDTH = CANVAS_SIZE - 7;
    private static final int WINDOW_HEIGHT = CANVAS_SIZE + CONNECTION_PANEL_HEIGHT;
    
    private JFrame window;
    private Canvas canvas;
    private JPanel connectionPanel;
    private JTextField textFieldHost;
    private JTextField textFieldPort;
    private JButton btnConnectToServer;
    private String[][] board = {
        { "X", "O", " " },
        { " ", "X", " " },
        { " ", " ", " " }
    };
    private boolean isCircle = false;
    private Point firstSpot = new Point(-1,-1);
    private Point secondSpot = new Point(-1,-1);
    private GameStatus gameStatus = GameStatus.PLAY;
    private boolean isYouTurn = false;
    private ClientSideConnection clientSideConnection;
    
    public Player(){
        this.window = new JFrame(TITLE_OF_PROGRAM);
        this.canvas = new Canvas();
        this.connectionPanel = new JPanel(new GridLayout(1, 5));
        this.textFieldHost = new JTextField("127.0.0.1");
        this.textFieldPort = new JTextField("55555");
        this.btnConnectToServer = new JButton("Connect");
        setupGUI();
    }
    
    public void connectToServer(String host, int port){
        clientSideConnection = new ClientSideConnection(host, port);
        if(isCircle){
            Thread thread = new Thread(() -> {
                System.out.println("Waiting for opponent...");
                if(clientSideConnection.receiveOpponentReadiness()){
                    isYouTurn = true;
                }
            });
            thread.start();
        }
    }
    
    private class ClientSideConnection{
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        
        public ClientSideConnection(String host, int port){
            try {
                this.socket = new Socket(host, port);
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
                isCircle = this.dis.readBoolean();
                System.out.println("Connected to server as "
                        + (isCircle ? " circle player" : " x player"));
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public void sendMove(String move){
            try {
                this.dos.writeUTF(move);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public String receiveMove(){
            String move = "";
            try {
                move = this.dis.readUTF();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
            return move;
        }
        
        public boolean receiveOpponentReadiness(){
            boolean isOpponentConnected = false;
            try {
                isOpponentConnected = this.dis.readBoolean();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
            return isOpponentConnected;
        }
        
        private void closeConnection(){
            try {
                this.socket.close();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
    }
    
    private void setupGUI(){
        this.window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.canvas.setSize(CANVAS_SIZE, CANVAS_SIZE);
        this.connectionPanel.add(new JLabel("Server:"));
        this.connectionPanel.add(this.textFieldHost);
        this.connectionPanel.add(new JLabel("Port:"));
        this.connectionPanel.add(this.textFieldPort);
        this.connectionPanel.add(this.btnConnectToServer);
        this.btnConnectToServer.addActionListener(e -> {
            String host = this.textFieldHost.getText();
            int port = 55555;
            try{
                port = Integer.parseInt(this.textFieldPort.getText());
            } catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(this.window, ex.getMessage(),
                        "Incorrect port", JOptionPane.WARNING_MESSAGE);
                Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                        null, ex);
            }
            
            if(port <= 1024 || port > 65535){
                JOptionPane.showMessageDialog(this.window,
                        "Port should be an integer between 1024 and 65535",
                        "Incorrect port", JOptionPane.WARNING_MESSAGE);
            }
            connectToServer(host, port);
        });
        this.window.add(BorderLayout.CENTER, this.canvas);
        this.window.add(BorderLayout.SOUTH, this.connectionPanel);
        this.window.setResizable(false);
        this.window.setLocationRelativeTo(null);
        this.window.setVisible(true);
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
    
    private GameStatus checkGameStatus(){
        GameStatus status = GameStatus.PLAY;
        
        //Check rows
        outer:
        for(int i = 0; i < board.length; ++i){
            String first = board[i][0];
            for(int j = 1; j < board[i].length; ++j){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            status = first.equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
            firstSpot.x = SPOT_SIZE / 2;
            firstSpot.y = i * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
            secondSpot.x = CANVAS_SIZE - SPOT_SIZE / 2;
            secondSpot.y = firstSpot.y;
            return status;
        }
        
        //Check columns
        outer:
        for(int j = 0; j < BOARD_SIZE; ++j){
            String first = board[0][j];
            for(int i = 1; i < BOARD_SIZE; ++i){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            status = first.equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
            firstSpot.x = j * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = firstSpot.x;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;
            return status;
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
            status = board[0][0].equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
            firstSpot.x = SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = CANVAS_SIZE - SPOT_SIZE / 2;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;
            return status;
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
            status = board[0][BOARD_SIZE - 1].equalsIgnoreCase("X")
                    ? GameStatus.X_WON
                    : GameStatus.O_WON;
            firstSpot.x = CANVAS_SIZE - SPOT_SIZE / 2;
            firstSpot.y = SPOT_SIZE / 2;
            secondSpot.x = SPOT_SIZE / 2;
            secondSpot.y = CANVAS_SIZE - SPOT_SIZE / 2;
            return status;
        }
        if(countFreeSpots() == 0)
            return GameStatus.TIE;
        
        return status;
    }
    
    private class Canvas extends JPanel implements MouseListener{
        private BufferedImage imgBoard;
        private BufferedImage imgRedX;
        private BufferedImage imgBlueX;
        private BufferedImage imgRedO;
        private BufferedImage imgBlueO;

        public Canvas(){
            setFocusable(true);
            requestFocus();
            setBackground(Color.white);
            addMouseListener(this);
            loadImages();
        }
        
        private void loadImages(){
            try {
                this.imgBoard = ImageIO.read(getClass().getResource("images/board.png"));
                this.imgRedX = ImageIO.read(getClass().getResource("images/redX.png"));
                this.imgBlueX = ImageIO.read(getClass().getResource("images/blueX.png"));
                this.imgRedO = ImageIO.read(getClass().getResource("images/redCircle.png"));
                this.imgBlueO = ImageIO.read(getClass().getResource("images/blueCircle.png"));
            } catch (IOException ex) {
                System.out.println("Could not load image: " + ex.getMessage());
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null,
                        ex);
            }
        }
        
        @Override
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            g.drawImage(imgBoard, 0, 0, null);
            for(int i = 0; i < board.length; ++i){
                for(int j = 0; j < board[i].length; ++j){
                    if("X".equalsIgnoreCase(board[i][j])){
                        if(isCircle)
                            g.drawImage(imgRedX, j * (SPOT_SIZE + SPOT_GAP),
                                        i * (SPOT_SIZE + SPOT_GAP), null);
                        else
                            g.drawImage(imgBlueX, j * (SPOT_SIZE + SPOT_GAP),
                                        i * (SPOT_SIZE + SPOT_GAP), null);
                    } else if("O".equalsIgnoreCase(board[i][j])){
                        if(isCircle)
                            g.drawImage(imgBlueO,  j * (SPOT_SIZE + SPOT_GAP),
                                        i * (SPOT_SIZE + SPOT_GAP), null);
                        else
                            g.drawImage(imgRedO,  j * (SPOT_SIZE + SPOT_GAP),
                                        i * (SPOT_SIZE + SPOT_GAP), null);
                    }
                }
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(isYouTurn){
                int x = e.getX() / (SPOT_SIZE + SPOT_GAP);
                int y = e.getY() / (SPOT_SIZE + SPOT_GAP);
                
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
        
    }
    
    public static void main(String[] args) {
        Player p = new Player();
    }
}
