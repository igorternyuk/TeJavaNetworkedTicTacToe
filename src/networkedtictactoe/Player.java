package networkedtictactoe;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
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
        { " ", " ", " " },
        { " ", " ", " " },
        { " ", " ", " " }
    };
    private boolean isCircle = true;
    private Point firstSpot = new Point(-1,-1);
    private Point secondSpot = new Point(-1,-1);
    private GameStatus gameStatus = GameStatus.WAINTING_FOR_OPPONENT;
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
                        "The port should be an integer between 1024 and 65535",
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
    
    public void connectToServer(String host, int port){
        clientSideConnection = new ClientSideConnection(host, port);
        if(isCircle){
            Thread thread = new Thread(() -> {
                System.out.println("Waiting for opponent...");
                if(clientSideConnection.receiveOpponentReadiness()){
                    gameStatus = GameStatus.O_TO_PLAY;
                }
            });
            thread.start();
        } else {
            gameStatus = GameStatus.O_TO_PLAY;
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
        
        public void sendMove(int move){
            try {
                this.dos.writeInt(move);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public int receiveMove(){
            int move = -1;
            try {
                move = this.dis.readInt();
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
        
        public GameStatus receiveGameStatus(){
            int index = 0;
            try {
                index = this.dis.readInt();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
            return GameStatus.values()[index];
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
    
    public void makeMove(int move){
        int y = move / BOARD_SIZE;
        int x = move % BOARD_SIZE;
        this.board[y][x] = isCircle ? "O" : "X";
    }
    
    public void startWaitingForOpponentMove(){
        
    }
    
    public void updateTurn(){
        /*
        int btnNum = clientSideConnection.receiveButtonNumber();
        messagesArea.setText("Your opponent clicked button #" + btnNum
                + ". Your turn.");
        opponentPoints += this.values[btnNum - 1];
        System.out.println("---Updating turn---");
        System.out.println("You are player #" + id);
        System.out.println("Turns made - " + turnsMade);
        if(id == 1 && turnsMade == maxTurns){
            System.out.println("Determining the winner for the first player");
            determineWinner();
        } else {
            buttonsEnabled = true;
        }
        toggleButtons();
        System.out.println("You enemy has " + opponentPoints + ".");
        */
    }
    
    public void calculatePoints(){
    }
    
    private class Canvas extends JPanel implements MouseListener{
        private static final String YOU_WON_TEXT = "You won!!!";
        private static final String YOU_LOST_TEXT = "You lost!!!";
        private static final String TIE_TEXT = "It's tie!!!";
        private static final String WAITING_FOR_OPPONENT_TEXT
                = "Waiting for opponent...";
        private BufferedImage imgBoard;
        private BufferedImage imgRedX;
        private BufferedImage imgBlueX;
        private BufferedImage imgRedO;
        private BufferedImage imgBlueO;
        private Font fontSmall = new Font("Tahoma", Font.BOLD, 36);
        private Font fontMiddle = new Font("Tahoma", Font.BOLD, 48);
        private Font fontLarge = new Font("Tahoma", Font.BOLD, 84);
        private Color colorYouWon = Color.green.darker().darker();
        private Color colorYouLost = new Color(255, 94, 0);

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
        
        private void drawText(Graphics g, String text, Font font, Color color){
            g.setFont(font);
            g.setColor(color);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g.drawString(text, (CANVAS_SIZE - textWidth) / 2, CANVAS_SIZE / 2);
        }
        
        private void drawBoard(Graphics g){
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
        
        private void drawWinningLine(Graphics g, Color color){
            Graphics2D g2 = (Graphics2D)g;
            g2.setStroke(new BasicStroke(10));
            g2.setColor(color);
            g2.drawLine(firstSpot.x, firstSpot.y, secondSpot.x, secondSpot.y);
        }
        
        @Override
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            switch(gameStatus){
                case WAINTING_FOR_OPPONENT:
                    drawText(g, WAITING_FOR_OPPONENT_TEXT, fontLarge, Color.green);
                    break;
                case X_TO_PLAY:
                case O_TO_PLAY:
                    drawBoard(g);
                    break;
                case X_WON:
                    drawBoard(g);
                    drawWinningLine(g, Color.red.darker().darker());
                    if(isCircle)
                        drawText(g, YOU_LOST_TEXT, fontLarge, colorYouLost);
                    else
                        drawText(g, YOU_WON_TEXT, fontLarge, colorYouWon);
                    break;
                case O_WON:
                    drawBoard(g);
                    drawWinningLine(g, Color.blue.darker().darker());
                    if(isCircle)
                        drawText(g, YOU_WON_TEXT, fontLarge, colorYouWon);
                    else
                        drawText(g, YOU_LOST_TEXT, fontLarge, colorYouLost);
                    break;
                case TIE: 
                    drawBoard(g);
                    drawText(g, TIE_TEXT, fontLarge, Color.green);
                    break;
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
            if((isCircle && gameStatus == GameStatus.O_TO_PLAY)
               || (!isCircle && gameStatus == GameStatus.X_TO_PLAY)){
                int x = e.getX() / (SPOT_SIZE + SPOT_GAP);
                int y = e.getY() / (SPOT_SIZE + SPOT_GAP);
                int move = y * BOARD_SIZE + x;
                makeMove(move);
                clientSideConnection.sendMove(move); 
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
