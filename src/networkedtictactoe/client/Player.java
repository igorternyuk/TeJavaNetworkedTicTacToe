package networkedtictactoe.client;

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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import networkedtictactoe.Board;
import networkedtictactoe.GameStatus;
import networkedtictactoe.MessageType;
import networkedtictactoe.PlayerType;

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
    private static final int WINDOW_HEIGHT = CANVAS_SIZE + CONNECTION_PANEL_HEIGHT + 15;
    
    private JFrame window;
    private Canvas canvas;
    private JPanel connectionPanel;
    private JLabel lblGameStatus;
    private JTextField textFieldHost;
    private JTextField textFieldPort;
    private JButton btnConnectToServer;
    private Board board = new Board();
    private boolean isCircle = false;
    private PlayerType type = PlayerType.Circle;
    private Point opponentMove = null;
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
        this.lblGameStatus = new JLabel("Waiting for opponent...");
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
            if(connectToServer(host, port))
                this.btnConnectToServer.setEnabled(false);
        });
        this.window.add(BorderLayout.CENTER, this.canvas);
        this.window.add(BorderLayout.SOUTH, this.connectionPanel);
        this.window.add(BorderLayout.NORTH, this.lblGameStatus);
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
                        if(!clientSideConnection.socket.isClosed()){
                            clientSideConnection.sendCloseConenction();
                            clientSideConnection.closeConnection();
                        }
                    }
                System.exit(0);
                }
            }
        });
        this.window.setVisible(true);
    }
    
    public boolean connectToServer(String host, int port){
        try {
            clientSideConnection = new ClientSideConnection(host, port);
        } catch (Exception ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this.window, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        if(clientSideConnection != null){
            startWaitingForMessages();
            return true;
        } else {
            return false;
        }
    }
    
    private class ClientSideConnection implements Runnable{
        private Socket socket;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        
        public ClientSideConnection(String host, int port) throws Exception{
            try {
                System.out.println("---Client side connection---");
                this.socket = new Socket(host, port);
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.ois = new ObjectInputStream(socket.getInputStream());
                System.out.println("Connected to server as "
                        + type.getMoveSign() + " player");
                
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
            if(this.socket == null){
                System.out.println("Server not started");
                throw new Exception("Could not connect to server");
            }
        }
        
        public void sendMove(Point move) throws IOException{
            try {
                System.out.println("Client side connection of player "
                        + type.getMoveSign());
                System.out.println("Sending move = " + move);
                this.oos.writeObject(MessageType.MOVE);
                this.oos.writeObject(move);
                this.oos.flush();
            } catch (IOException ex) {
                System.out.println("IOException from ClientSideConnection"
                        + " sendMove(Point move)");
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        public void sendCloseConenction(){
            try {
                System.out.println("Player " + (isCircle ? "O" : "X")
                        + " closes conection");
                this.oos.writeObject(MessageType.DISCONNECTION);
                this.oos.writeBoolean(true);
                this.oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        
        public void receiveMessage() throws IOException, ClassNotFoundException{
            try {
                MessageType type = (MessageType)this.ois.readObject();
                System.out.println("Received code: " + type);
                switch(type){
                    case PLAYER_TYPE:
                       receivePlayerType();
                        break;
                    //Opponent connected
                    case OPPONENT_CONNECTED:
                        receiveOpponentReadiness();
                        break;
                        
                    case MOVE_ACCEPTED:
                        receiveMoveAccepted();
                        break;
                        
                    //Opponent move 
                    case MOVE:
                        //receiveMove();
                        break;
                        
                    case BOARD:
                        receiveBoard();
                        break;
                        
                    //Game status
                    case GAME_STATUS:
                        receiveGameStatus();
                        break;
                                              
                    case DISCONNECTION:
                        break;
                }
            } catch (IOException ex) {
                System.out.println("IOException from ClientSideConnection "
                        + "receiveMessage()");
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            } catch (ClassNotFoundException ex) {
                System.out.println("ClassNotFoundException from"
                        + " ClientSideConnection receiveMessage()");
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
                throw ex;
            }
        }
        
        private void receivePlayerType() throws IOException, ClassNotFoundException{
            type = (PlayerType)this.ois.readObject();
            System.out.println("Receiving player type " + type.getMoveSign());
        }
        
        private void receiveOpponentReadiness() throws IOException{
            if(type.equals(PlayerType.Circle)){
                boolean isOpponentConnected = this.ois.readBoolean();
                System.out.println("Receiving opponent readiness");
                if(isOpponentConnected){
                    gameStatus = GameStatus.O_TO_PLAY;
                    canvas.repaint();
                }
            }
        }
        
        private boolean receiveMoveAccepted() throws IOException{
            boolean moveAccepted = this.ois.readBoolean();
            System.out.println("Received moveAccepted " + moveAccepted);
            return moveAccepted;
        }
        
        private void receiveBoard() throws IOException, ClassNotFoundException{
            Board fromServer = (Board)this.ois.readObject();
            System.out.println("The board received from the server:");
            fromServer.print();
            board = fromServer;
            System.out.println("Our board:");
            if(board != null){
                board.print();
                canvas.repaint(); 
            }
        }
        
        private void receiveGameStatus() throws IOException, ClassNotFoundException {
            System.out.println("Receiving the new game status");
            gameStatus = (GameStatus)this.ois.readObject();
            if(gameStatus.isGameOver()){
                firstSpot = board.getFirstSpot();
                secondSpot = board.getSecondSpot();
                firstSpot.x = firstSpot.x * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
                firstSpot.y = firstSpot.y * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
                secondSpot.x = secondSpot.x * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
                secondSpot.y = secondSpot.y * (SPOT_SIZE + SPOT_GAP) + SPOT_SIZE / 2;
            }
            lblGameStatus.setText(gameStatus.getDescription());
            canvas.repaint();
        }
        
        private void closeConnection(){
            try {
                this.oos.close();
                this.ois.close();
                this.socket.close();
                System.out.println("Player " + type.getMoveSign()
                        + " closed connection");
            } catch (IOException ex) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }

        @Override
        public void run() {
            try{
                while(!gameStatus.isGameOver()){
                    clientSideConnection.receiveMessage();
                    if(board != null){
                        board.print();
                    }
                    canvas.repaint();
                }
            } catch(IOException | ClassNotFoundException ex){
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE,
                        null, ex);
            } finally{
                closeConnection();
            }
        }
    }
    
    public void startWaitingForMessages(){
        System.out.println("Waiting for a server message...");
        Thread thread = new Thread(clientSideConnection);
        thread.start();
    }
    
    private class Canvas extends JPanel implements MouseListener{
        private static final String YOU_WON_TEXT = "You won!!!";
        private static final String YOU_LOST_TEXT = "You lost!!!";
        private static final String TIE_TEXT = "It's tie!!!";
        private static final String WAITING_FOR_OPPONENT_TEXT
                = "Waiting for opponent...";
        private static final String OPPONENT_DISCONNECTED_TEXT
                = "Opponent disconnected";
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
            configureCanvas();
            loadImages();
        }
        
        private void configureCanvas(){
            setFocusable(true);
            requestFocus();
            setBackground(Color.white);
            addMouseListener(this);
        }
        
        private void loadImages(){
            try {
                this.imgBoard = ImageIO
                        .read(getClass().getResource("../images/board.png"));
                this.imgRedX = ImageIO
                        .read(getClass().getResource("../images/redX.png"));
                this.imgBlueX = ImageIO
                        .read(getClass().getResource("../images/blueX.png"));
                this.imgRedO = ImageIO
                        .read(getClass().getResource("../images/redCircle.png"));
                this.imgBlueO = ImageIO
                        .read(getClass().getResource("../images/blueCircle.png"));
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
            if(board == null) return;
            for(int i = 0; i < board.getBoardSize(); ++i){
                for(int j = 0; j < board.getBoardSize(); ++j){
                    if(PlayerType.Cross.getMoveSign()
                            .equalsIgnoreCase(board.valueAt(j, i))){
                        if(type.equals(PlayerType.Circle))
                            g.drawImage(imgRedX, j * (SPOT_SIZE + SPOT_GAP),
                                        i * (SPOT_SIZE + SPOT_GAP), null);
                        else if(type.equals(PlayerType.Cross))
                            g.drawImage(imgBlueX, j * (SPOT_SIZE + SPOT_GAP),
                                        i * (SPOT_SIZE + SPOT_GAP), null);
                    } else if(PlayerType.Circle.getMoveSign()
                            .equalsIgnoreCase(board.valueAt(j, i))){
                        if(type.equals(PlayerType.Circle))
                            g.drawImage(imgBlueO,  j * (SPOT_SIZE + SPOT_GAP),
                                        i * (SPOT_SIZE + SPOT_GAP), null);
                        else if(type.equals(PlayerType.Cross))
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
                    drawWinningLine(g, Color.green.darker().darker());
                    if(type.equals(PlayerType.Circle))
                        drawText(g, YOU_LOST_TEXT, fontLarge, colorYouLost);
                    else if(type.equals(PlayerType.Cross))
                        drawText(g, YOU_WON_TEXT, fontLarge, colorYouWon);
                    break;
                case O_WON:
                    drawBoard(g);
                    drawWinningLine(g, Color.green.darker().darker());
                    if(type.equals(PlayerType.Circle))
                        drawText(g, YOU_WON_TEXT, fontLarge, colorYouWon);
                    else if(type.equals(PlayerType.Cross))
                        drawText(g, YOU_LOST_TEXT, fontLarge, colorYouLost);
                    break;
                case TIE: 
                    drawBoard(g);
                    drawText(g, TIE_TEXT, fontLarge, Color.green);
                    break;
                case OPPONENT_DISCONNECTED:
                    drawText(g, OPPONENT_DISCONNECTED_TEXT, fontMiddle,
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
            if((type.equals(PlayerType.Circle)
                && gameStatus == GameStatus.O_TO_PLAY)
               || (type.equals(PlayerType.Cross)
                   && gameStatus == GameStatus.X_TO_PLAY)){
                Point move = new Point(x, y);
                System.out.println("sended move = " + move);
                try {
                    clientSideConnection.sendMove(move);
                    //board.tryToMove(move, type);
                } catch (IOException ex) {
                    Logger.getLogger(Player.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
            this.repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
        
    }
    
    public static void main(String[] args) {
        Player p = new Player();
    }
}
