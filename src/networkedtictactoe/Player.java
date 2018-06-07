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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private boolean isCircle = false;
    private int opponentMove = 0;
    private GameStatus gameStatus = GameStatus.WAINTING_FOR_OPPONENT;
    private Point firstSpot = new Point(-1,-1);
    private Point secondSpot = new Point(-1,-1);
    private ClientSideConnection clientSideConnection = null;
    
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
            this.btnConnectToServer.setEnabled(false);
        });
        this.window.add(BorderLayout.CENTER, this.canvas);
        this.window.add(BorderLayout.SOUTH, this.connectionPanel);
        this.window.setResizable(false);
        this.window.setLocationRelativeTo(null);
        this.window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e){
                int reply = JOptionPane.showConfirmDialog(window,
                        "Do you really want to exit?",
                        "Confirm exit, please",
                        JOptionPane.YES_NO_OPTION);
                if(reply == JOptionPane.YES_OPTION){
                    if(clientSideConnection != null){
                        clientSideConnection.sendCloseConenction();
                        clientSideConnection.closeConnection();
                    }
                System.exit(0);
                }
            }
        });
        this.window.setVisible(true);
    }
    
    public void connectToServer(String host, int port){
        clientSideConnection = new ClientSideConnection(host, port);
        startWaitingForMessages();
    }
    
    private class ClientSideConnection{
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        
        public ClientSideConnection(String host, int port){
            try {
                System.out.println("---Client side connection---");
                this.socket = new Socket(host, port);
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
                //isCircle = this.dis.readBoolean();
                System.out.println("Connected to server as "
                        + (isCircle ? " circle player" : " x player"));
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public void sendMove(int move){
            try {
                System.out.println("Client side connection of player "
                        + (isCircle ? "O" : "X"));
                System.out.println("Sending move = " + move);
                this.dos.writeInt(2);
                this.dos.writeInt(move);
                this.dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        public void sendCloseConenction(){
            try {
                System.out.println("Player " + (isCircle ? "O" : "X")
                        + " closes conection");
                this.dos.writeInt(5);
                this.dos.writeBoolean(true);
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        
        public void receiveMessage(){
            try {
                int code = this.dis.readInt();
                System.out.println("Received code: " + code);
                switch(code){
                    case 0:
                        receiveIsCircle();
                        break;
                    //Opponent connected
                    case 1:
                        receiveOpponentReadiness();
                        break;
                        
                    //Opponent move 
                    case 2:
                        receiveMove();
                        break;
                        
                    //Game status
                    case 3:
                        receiveGameStatus();
                        break;
                        
                    //First and second spots
                    case 4:
                        break;
                }
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        
        private void receiveIsCircle() throws IOException{
            isCircle = this.dis.readBoolean();
            System.out.println("Receiving isCircle " + isCircle);
        }
        
        private void receiveOpponentReadiness() throws IOException{
            if(isCircle){
                boolean isOpponentConnected = this.dis.readBoolean();
                System.out.println("Receiving opponent readiness");
                if(isOpponentConnected){
                    gameStatus = GameStatus.O_TO_PLAY;
                    canvas.repaint();
                }
            }
        }
        
        private void receiveMove() throws IOException{
            System.out.println("Receiving of the opponent move");
            System.out.println("this.dis.available() = " + this.dis.available());
            opponentMove = this.dis.readInt();
            
            System.out.println("Client side connection of player "
                        + (isCircle ? "O" : "X"));
            System.out.println("Received opponent move = " + opponentMove);
            makeMove(opponentMove, true);
            canvas.repaint();
        }
        
        private void receiveGameStatus() throws IOException {
            System.out.println("Receiving the new game status");
            int index = this.dis.readInt();
            System.out.println("index = " + index);
            gameStatus = GameStatus.values()[index];
            System.out.println("Returning game status = " + gameStatus);
            canvas.repaint();
        }
        
        private void closeConnection(){
            try {
                this.socket.close();
                System.out.println("Player " + (isCircle ? "O" : "X")
                        + " closed connection");
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
    }
    
    public void makeMove(int move, boolean isOpponentMove){
        System.out.println("Making move...");
        int y = move / BOARD_SIZE;
        int x = move % BOARD_SIZE;
        System.out.println("x = " + x + " y = " + y);
        if(isOpponentMove)
            this.board[y][x] = isCircle ? "X" : "O";
        else
            this.board[y][x] = isCircle ? "O" : "X";
    }
    
    public void printBoard(){
        for(int i = 0; i < board.length; ++i){
            for(int j = 0; j < board[i].length; ++j){
                System.out.print(board[i][j] + "-");
            }
            System.out.println("");
        }
    }
    
    public void startWaitingForMessages(){
        System.out.println("Waiting for a server message...");
        Thread thread = new Thread(() -> {
            while(!gameStatus.isGameOver()){
                System.out.println("Loop:Waiting for a server message...");
                clientSideConnection.receiveMessage();
                printBoard();
                this.canvas.repaint();
            }
        });
        thread.start();
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
        private Font fontMiddle = new Font("Tahoma", Font.BOLD, 42);
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
                    drawText(g, WAITING_FOR_OPPONENT_TEXT, fontMiddle,
                            Color.green);
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
                case OPPONENT_DISCONNECTED:
                    drawText(g, "Opponent disconnected", fontMiddle,
                            Color.red.darker().darker());
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
            int y = e.getY() / (SPOT_SIZE + SPOT_GAP);
            int x = e.getX() / (SPOT_SIZE + SPOT_GAP);
            System.out.println("isCircle = " + isCircle);
            System.out.println("Mouse released x = " + x + " y = " + y);
            System.out.println("gameStatus = " + gameStatus);
            if((isCircle && gameStatus == GameStatus.O_TO_PLAY)
               || (!isCircle && gameStatus == GameStatus.X_TO_PLAY)){
                System.out.println("board[y][x] = " + board[y][x]);
                if(board[y][x].equals(" ")){
                    int move = y * BOARD_SIZE + x;
                    System.out.println("sended move = " + move);
                    makeMove(move, false);
                    clientSideConnection.sendMove(move);
                }
            }
            this.repaint();
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
