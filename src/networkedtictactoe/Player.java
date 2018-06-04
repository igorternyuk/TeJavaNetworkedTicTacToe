package networkedtictactoe;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author igor
 */
public class Player {
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;
    private static final int SPOT_SIZE = 160;
    private static final int SPOT_GAP = 10;
    private static final int FIELD_SIZE = 3;
    private JFrame window;
    private Canvas canvas;
    private JPanel connectionPanel;
    private JTextField textFieldHost;
    private JTextField textFieldPort;
    private JButton btnConnectToServer;
    private static final String TITLE_OF_PROGRAM = "NetworkedTicTacToe";
    
    
    public Player(){
        this.window = new JFrame(TITLE_OF_PROGRAM);
        this.canvas = new Canvas();
        this.connectionPanel = new JPanel(new GridLayout(1, 5));
        this.textFieldHost = new JTextField("127.0.0.1");
        this.textFieldPort = new JTextField("55555");
        this.btnConnectToServer = new JButton("Connect to server");
        setupGUI();
    }
    
    private void setupGUI(){
        this.window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.canvas.setSize((SPOT_SIZE + SPOT_GAP) * FIELD_SIZE,
                (SPOT_SIZE + SPOT_GAP) * FIELD_SIZE);
        Container container = this.window.getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS)); 
        this.connectionPanel.add(new JLabel("Server"));
        this.connectionPanel.add(this.textFieldHost);
        this.connectionPanel.add(new JLabel("Port:"));
        this.connectionPanel.add(this.textFieldPort);
        this.connectionPanel.add(this.btnConnectToServer);
        this.connectionPanel.setSize(WINDOW_WIDTH,
                WINDOW_HEIGHT - (SPOT_SIZE + SPOT_GAP) * FIELD_SIZE);
        container.add(this.canvas);
        container.add(this.connectionPanel);
        this.window.setResizable(false);
        this.window.setLocationRelativeTo(null);
        this.window.setVisible(true);
    }
    
    private void loadImages(){
        //BufferedImage 
    }
    
    private class Canvas extends JPanel implements MouseListener{
        
        public Canvas(){
            setFocusable(true);
            requestFocus();
            setBackground(Color.GREEN);
            addMouseListener(this);
        }
        
        @Override
        public void paintComponent(Graphics g){
            //super.paintComponent(g);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            System.out.println("Mouse release event");
            System.out.println("x = " + e.getX() + " y = " + e.getY());
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
