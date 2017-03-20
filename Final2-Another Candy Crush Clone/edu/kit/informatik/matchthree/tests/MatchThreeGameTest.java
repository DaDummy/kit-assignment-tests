package edu.kit.informatik.matchthree.tests;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import edu.kit.informatik.matchthree.MatchThreeBoard;
import edu.kit.informatik.matchthree.MatchThreeGame;
import edu.kit.informatik.matchthree.MaximumDeltaMatcher;
import edu.kit.informatik.matchthree.MoveFactoryImplementation;
import edu.kit.informatik.matchthree.framework.Delta;
import edu.kit.informatik.matchthree.framework.FillingStrategy;
import edu.kit.informatik.matchthree.framework.Position;
import edu.kit.informatik.matchthree.framework.RandomStrategy;
import edu.kit.informatik.matchthree.framework.Token;
import edu.kit.informatik.matchthree.framework.exceptions.BoardDimensionException;
import edu.kit.informatik.matchthree.framework.interfaces.Board;
import edu.kit.informatik.matchthree.framework.interfaces.Game;
import edu.kit.informatik.matchthree.framework.interfaces.Matcher;
import edu.kit.informatik.matchthree.framework.interfaces.Move;

/**
 * Tests for {@link MatchThreeGame}
 *
 * @author Luke Brocke
 */
public class MatchThreeGameTest {
    // A FillingStrategy will be set for the board, the MatchThreeGame constructor MUST NOT do that
    // see https://ilias.studium.kit.edu/ilias.php?ref_id=583580&cmdClass=ilobjforumgui&thr_pk=85450&cmd=viewThread&cmdNode=75:r6&baseClass=ilrepositorygui
    
    /**
     * {@link Game#acceptMove(Move)} should throw a {@link BoardDimensionException} if the given
     *   {@link Move} cannot be applied
     */
    @Test (expected = BoardDimensionException.class)
    public void moveAcceptExceptionTest() {
        Board board = new MatchThreeBoard(Token.set("AB"), 2, 2);
        board.setFillingStrategy(new RandomStrategy());
        
        MoveFactoryImplementation factory = new MoveFactoryImplementation();
        Move flipRight = factory.flipRight(Position.at(1, 0));
        
        Matcher matcher = new MaximumDeltaMatcher(new HashSet<>(Arrays.asList(Delta.dxy(0, 1))));
        
        MatchThreeGame game = new MatchThreeGame(board, matcher);
        game.acceptMove(flipRight);
    }
    
    /**
     * Valid test for {@link Game#acceptMove(Move)}
     */
    @Test
    public void moveAcceptTest() {
        Board board = new MatchThreeBoard(Token.set("AB"), 2, 2);
        board.setFillingStrategy(new RandomStrategy());
        
        MoveFactoryImplementation factory = new MoveFactoryImplementation();
        Move flipRight = factory.flipRight(Position.at(0, 0));
        
        Matcher matcher = new MaximumDeltaMatcher(new HashSet<>(Arrays.asList(Delta.dxy(0, 1))));
        
        MatchThreeGame game = new MatchThreeGame(board, matcher);
        game.acceptMove(flipRight);
    }
    
    /**
     * Initial score of a new {@link MatchThreeGame} is 0.
     *
     * As sb pointed out, the score could be > 0 after initialization because
     *   some matches could have already been found after the random fill
     */
    @Ignore
    @Test
    public void initializeScoreTest() {
        Board board = new MatchThreeBoard(Token.set("AB"), 5, 5);
        board.setFillingStrategy(new RandomStrategy());
        Matcher matcher = new MaximumDeltaMatcher(new HashSet<>(Arrays.asList(Delta.dxy(0, 1))));
        
        MatchThreeGame game = new MatchThreeGame(board, matcher);
        game.initializeBoardAndStart();
        
        assertEquals(0, game.getScore());
    }
    
    /**
     * {@link Board} should get filled entirely on initialization.
     */
    @Test
    public void initializeFillBoardTest() {
        Board board = new MatchThreeBoard(Token.set("AB"), 5, 5);
        board.setFillingStrategy(new RandomStrategy());
        Matcher matcher = new MaximumDeltaMatcher(new HashSet<>(Arrays.asList(Delta.dxy(0, 1))));
        
        MatchThreeGame game = new MatchThreeGame(board, matcher);
        game.initializeBoardAndStart();
        
        assertTrue(TestUtils.boardIsFilled(board));
    }

    /**
     * This test asserts that matches with less than 3 positions are not
     * evaluated
     * 
     * @author NicoWeidmann
     */
    @Test
    public void matchFilterTest() {

        // this board contains matches with a size below 3
        Board board = new MatchThreeBoard(Token.set("AE"), "AAAA;AAAA;AAAA;AAAA");

        // if this token is found on the board, a match was evaluated
        final Token evilToken = new Token('E');

        board.setFillingStrategy(new FillingStrategy() {

            // fills every empty position with the evil token
            @Override
            public void fill(Board board) {
                for (int x = 0; x < board.getColumnCount(); ++x) {
                    for (int y = 0; y < board.getRowCount(); ++y) {
                        if (board.getTokenAt(Position.at(x, y)) == null) {
                            board.setTokenAt(Position.at(x, y), evilToken);
                        }
                    }
                }
            }
        });

        // a matcher that returns three hardcoded matches on first call, then
        // returns no matches (to prevent infinite loops)
        final class FakeMatcher implements Matcher {

            private boolean called = false;

            @Override
            public Set<Set<Position>> matchAll(Board board, Set<Position> initial) {
                return match(board, Position.at(0, 0));
            }

            @Override
            public Set<Set<Position>> match(Board board, Position initial) {

                if (called) {
                    // no matches
                    return new HashSet<>();
                } else {
                    return new HashSet<>(Arrays.asList(new HashSet<>(Arrays.asList(Position.at(0, 0))),
                            new HashSet<>(Arrays.asList(Position.at(0, 0), Position.at(0, 1))),
                            new HashSet<>(/* empty */)));
                }
            }
        }
        ;

        MatchThreeGame game = new MatchThreeGame(board, new FakeMatcher());
        game.initializeBoardAndStart();

        // score must be null, as no matches should be evaluated
        assertEquals(game.getScore(), 0);

        // resetting matcher
        game.setMatcher(new FakeMatcher());

        game.acceptMove((new MoveFactoryImplementation()).flipRight(Position.at(0, 1)));

        // score must still be null
        assertEquals(game.getScore(), 0);

        assertTrue("The evil token was found! The game evaluated a match of size <3.",
                !board.toTokenString().contains(evilToken.toString()));
    }
}
