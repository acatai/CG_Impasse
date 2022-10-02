package com.codingame.game;

import com.codingame.game.engine.*;
import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.endscreen.EndScreenModule;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Random;
import java.util.Comparator;
import static com.codingame.game.engine.Constants.*;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphics;
    @Inject private ToggleModule toggleModule;
    @Inject TooltipModule tooltipModule;
    @Inject private EndScreenModule endScreenModule;

    Viewer viewer;
    Player currentPlayer;
    String lastAction = "null";
    Random rand;
    GameState mainState;

    @Override
    public void init()
    {
        rand = new Random(gameManager.getSeed());
        Constants.pregenerateMovements();
        viewer = new Viewer(graphics, gameManager, toggleModule, tooltipModule);
        gameManager.setMaxTurns(TURNLIMIT);
        gameManager.setFirstTurnMaxTime(TIMELIMIT_INIT);
        gameManager.setTurnMaxTime(TIMELIMIT_TURN);
        gameManager.setFrameDuration(1800);
        lastAction = "null";
        currentPlayer = gameManager.getPlayer(0);

        GameState.viewer = viewer;
        GameState.manager = gameManager;

        mainState = GameState.initial();
        viewer.showState(mainState);
    }

    @Override
    public void gameTurn(int turn)
    {
        int p = currentPlayer.getIndex();
        Player player = gameManager.getPlayer(p);
        ArrayList<Action> actions = mainState.generateLegalActions(p);
        viewer.showFrame(p);

        try
        {
            sendInputs(turn, player, actions);
            player.execute();

            String output = player.getOutputs().get(0);
            String comment = null;
            // Split comment from output.
            int spaceIndex = output.indexOf(' ');
            if (spaceIndex != -1) {
                comment = output.substring(spaceIndex + 1);
                if (comment.length() > MAX_MESSAGE_LENGTH)
                    comment = comment.substring(0, MAX_MESSAGE_LENGTH);
                output = output.substring(0, spaceIndex);
            }

            if (comment != null) viewer.playerMessage[p].setText(comment);
            else viewer.playerMessage[p].setText("");

            boolean found = false;
            if (output.equals("random"))
            {
                found = true;
                int i = rand.nextInt(actions.size());
                lastAction = actions.get(i).toString();
                mainState.applyAction(player, actions.get(i));
            }
            else
            {
                for (Action action : actions)
                {
                    String s = action.toString();
                    if (output.equals(s))
                    {
                        lastAction = s;
                        mainState.applyAction(player, action);
                        found = true;
                        break;
                    }
                }
            }
            if(!found) throw new InvalidActionException(String.format("Action \""+ output +"\"  was not valid!"));

            viewer.playerAction[player.getIndex()].setText(lastAction);

        } catch (AbstractPlayer.TimeoutException e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(player.getNicknameToken() + " did not output in time!"));
            player.deactivate(player.getNicknameToken() + " timeout.");
            player.setScore(-1);
            gameManager.endGame();
            return;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | InvalidActionException e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(player.getNicknameToken() + " made an invalid action!"));
            player.deactivate(player.getNicknameToken() + " made an invalid action.");
            player.setScore(-1);
            gameManager.endGame();
            return;
        }

        if (GameState.score[p]==12)
        {
            gameManager.getPlayer(p).setScore(GameState.score[p]);
            gameManager.getPlayer(p ^ 1).setScore(GameState.score[p ^ 1]);
            gameManager.endGame();
        }
        else
        {
            currentPlayer = gameManager.getPlayer(p ^ 1);
        }
    }

    void sendInputs(int turn, Player player, ArrayList<Action> actions)
    {
        // Color
        if (turn < 3) player.sendInputLine(player.getIndex() == 1 ? "b" : "w");

        // Board
        for(int y = HEIGHT-1; y >=0 ; y--)
        {
            String s = "";
            for (int x = 0; x < WIDTH; x++)
                s += UNIT_CHAR.get(GameState.occupied[GameState.toXY(x, y)]);
            player.sendInputLine(s);
        }
        // Last action
        player.sendInputLine(lastAction);
        // Number of actions and actions themselves
        actions.sort(Comparator.comparing(Action::toString));
        player.sendInputLine(Integer.toString(actions.size()));
        for (Action action : actions) player.sendInputLine(action.toString());
    }

    @Override
    public void onEnd()
    {
        int[] scores = { gameManager.getPlayer(0).getScore(), gameManager.getPlayer(1).getScore() };
        String[] text = new String[2];
        if(scores[0] > scores[1]) {
            gameManager.addToGameSummary(gameManager.formatSuccessMessage(gameManager.getPlayer(0).getNicknameToken() + " won!"));
            gameManager.addTooltip(gameManager.getPlayer(0), gameManager.getPlayer(0).getNicknameToken() + " won!");
            text[0] = "Won";
            text[1] = "Lost";
        } else if(scores[0] < scores[1]) {
            gameManager.addToGameSummary(gameManager.formatSuccessMessage(gameManager.getPlayer(1).getNicknameToken() + " won!"));
            gameManager.addTooltip(gameManager.getPlayer(1), gameManager.getPlayer(1).getNicknameToken() + " won!");
            text[0] = "Lost";
            text[1] = "Won";
        } else {
            gameManager.addToGameSummary(gameManager.formatErrorMessage("Game is drawn"));
            gameManager.addTooltip(gameManager.getPlayer(1), "Draw");
            text[0] = "Draw";
            text[1] = "Draw";
        }
        endScreenModule.setScores(scores, text);
   }
}
