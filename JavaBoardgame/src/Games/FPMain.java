package Games;
/**
 * @author Daniel Ackerman 23104834
 * @version 0.1.0, 3/5/2017
 */
public class FPMain {

    public static void main(String[] args)  {

        AbstractGameFactory agf = new TicTacToeFactory();
        Game TTTGame = agf.createGame(agf);
        TTTGame.runGame();
    }

}
