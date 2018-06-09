package networkedtictactoe;

import java.io.Serializable;

/**
 *
 * @author igor
 */
public enum MessageType implements Serializable{
    PLAYER_TYPE,
    OPPONENT_READINESS,
    MOVE,
    MOVE_ACCEPTED,
    BOARD,
    GAME_STATUS,
    WINNIG_LINE_SPOTS,
    DISCONNECTION,
    SBOARD
}