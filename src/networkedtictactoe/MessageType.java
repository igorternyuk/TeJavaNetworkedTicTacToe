package networkedtictactoe;

import java.io.Serializable;

/**
 *
 * @author igor
 */
public enum MessageType implements Serializable{
    PLAYER_TYPE,
    OPPONENT_CONNECTED,
    MOVE,
    MOVE_ACCEPTED,
    BOARD,
    GAME_STATUS,
    DISCONNECTION
}