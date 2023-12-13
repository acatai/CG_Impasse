package com.codingame.game.engine;

import com.codingame.game.Player;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.module.entities.Sprite;

import java.util.ArrayList;
import java.util.Arrays;

import static com.codingame.game.engine.Constants.*;

public class GameState
{
  public static Viewer viewer;
  public static GameManager manager;

  public ArrayList<Integer>[] singles = new ArrayList[]{new ArrayList<Integer>(), new ArrayList<Integer>()};
  public ArrayList<Integer>[] doubles = new ArrayList[]{new ArrayList<Integer>(), new ArrayList<Integer>()};

  public final int[] occupied = new int[WIDTH*HEIGHT];
  public final int[] score = new int[] {0, 0};

  public GameState() {}

  public static GameState initial()
  {
    GameState s = new GameState();
    s.singles[0] = new ArrayList<>(Arrays.asList(toXY(0, 0), toXY(3, 1), toXY(4, 0), toXY(7, 1)));
    s.singles[1] = new ArrayList<>(Arrays.asList(toXY(0, 6), toXY(3, 7), toXY(4, 6), toXY(7, 7)));
    s.doubles[0] = new ArrayList<>(Arrays.asList(toXY(1, 7), toXY(2, 6), toXY(5, 7), toXY(6, 6)));
    s.doubles[1] = new ArrayList<>(Arrays.asList(toXY(1, 1), toXY(2, 0), toXY(5, 1), toXY(6, 0)));
    return s;
  }

  public GameState copy()
  {
    GameState s = new GameState();
    for (int p=0; p <2; p++) s.singles[p] = new ArrayList<>(this.singles[p]);
    for (int p=0; p <2; p++) s.doubles[p] = new ArrayList<>(this.doubles[p]);
    return s;
  }

  private void computeOccupied()
  {
    for (int xy=0; xy < WIDTH*HEIGHT; xy++) occupied[xy] = -1;
    for (int p=0; p <2; p++) for(Integer xy: singles[p]) occupied[xy] = p;
    for (int p=0; p <2; p++) for(Integer xy: doubles[p]) occupied[xy] = 2+p;
  }

  public ArrayList<Action> generateLegalActions(int player)
  {
    computeOccupied();
    ArrayList<Action> actions = generateLegalBasicMoves(player);
    if (actions.size()==0) actions = generateLegalImpasses(player);

    return actions;
  }

  private ArrayList<Action> generateLegalBasicMoves(int player)
  {
    ArrayList<Action> actions = new ArrayList<>();

    // Single Slides
    for (Integer xy:singles[player])
    {
      for (int dir=0; dir < 2; dir++)
      {
        for (Integer txy: FORWARDS[xy][player][dir])
        {
          if (occupied[txy] != -1) break;
          Action a = Action.newBasicSingle(xy, txy);
          actions.addAll(generateLegalCrownsMove(player, a, xy, needCrown(player,txy)?txy:-1));
        }
      }
    }

    // Double Slides
    for (Integer xy:doubles[player])
    {
      for (int dir=0; dir < 2; dir++)
      {
        for (Integer txy: BACKWARDS[xy][player][dir])
        {
          if (occupied[txy] != -1) break;
          Action a = Action.newBasicDouble(xy, txy, needBearOff(player, txy));
          actions.addAll(generateLegalCrownsMove(player, a, -1, a.bearoff?txy:-1));
        }
      }
    }

    // Transposes
    for (Integer xy:doubles[player])
    {
      for (Integer txy:TRANSPOSES[xy][player])
      {
        if (occupied[txy] != player) continue;
        Action a = Action.newBasicTranspose(xy, txy, needBearOff(player, txy));
        actions.addAll(generateLegalCrownsMove(player, a, a.bearoff?-1: txy, xy));
      }
    }

    return actions;
  }

  private ArrayList<Action> generateLegalImpasses(int player)
  {
    ArrayList<Action> actions = new ArrayList<>();

    // From Singles
    for (Integer xy:singles[player])
    {
      Action a = Action.newImpasse(xy, true);
      actions.add(a);
    }

    // From Doubles
    for (Integer xy:doubles[player])
    {
      Action a = Action.newImpasse(xy, false);
      actions.addAll(generateLegalCrownsMove(player, a, -1, xy));
    }

    return actions;
  }


  private boolean needCrown(int player, int xy)
  {
    return (player==0 && xy/WIDTH == 7) || (player==1 && xy/WIDTH==0);
  }
  private boolean needBearOff(int player, int xy)
  {
    return (player==0 && xy/WIDTH == 0) || (player==1 && xy/WIDTH==7);
  }

  private ArrayList<Action> generateLegalCrownsMove(int player, Action action, Integer noxy, Integer newsinglexy)
  {
    ArrayList<Action> actions = new ArrayList<>();
    ArrayList<Integer> localsingles = new ArrayList<>(singles[player]);
    localsingles.remove(noxy);
    if (newsinglexy != -1) localsingles.add(newsinglexy);

    for (Integer toxy:localsingles)
    {
      if (!needCrown(player, toxy)) continue;

      for (Integer fromxy:localsingles)
      {
        if (toxy==fromxy) continue;
        Action a = action.copy();
        a.crownfromxy = fromxy;
        a.crowntoxy = toxy;
        actions.add(a);
      }
    }

    if (actions.size()==0) actions.add(action);

    return actions;
  }

  public void applyAction(Player player, Action action)
  {
    int p = player.getIndex();

    double d1 = 0.0;
    if (action.type== Action.AType.IMPASSE_DOUBLE || action.type== Action.AType.IMPASSE_SINGLE) d1 = REMOVE_TIME * viewer.computePixelHomeDistance(action.fromxy, p);
    else d1 = MOVE_TIME * viewer.computePixelBoardDistance(action.fromxy, action.toxy);
    double d2 = 0.0;
    if (action.bearoff) d2 = REMOVE_TIME * viewer.computePixelHomeDistance(action.toxy, p);
    double d3 = 0.0;
    if (action.crownfromxy != -1) d3 = CROWN_TIME * viewer.computePixelBoardDistance(action.crownfromxy, action.crowntoxy);

    manager.setFrameDuration((int)(d1+d2+d3));
    double commitTime0 = 0.0;
    double commitTime1 = d1 / (d1+d2+d3);
    double commitTime2 = (d1+d2) / (d1+d2+d3);
    double commitTime3 = 1.0;

    if (action.type== Action.AType.BASIC_SINGLE)
    {
      Sprite s = viewer.bottoms.get(action.fromxy);
      viewer.setActionsHighlight(0, action.fromxy, commitTime0, 0);
      viewer.setActionsHighlight(1, action.toxy, commitTime0, 0);

      singles[p].remove((Integer)action.fromxy);
      singles[p].add(action.toxy);

      viewer.bottoms.remove(action.fromxy);
      viewer.bottoms.put(action.toxy, s);

      s.setZIndex(s.getZIndex()+10);
      viewer.graphics.commitEntityState(commitTime0, s);
      viewer.setXY(s, action.toxy);
      s.setZIndex(s.getZIndex()-10);
      viewer.graphics.commitEntityState(commitTime1, s);

      manager.addToGameSummary(player.getNicknameToken() + " performed a single slide from " + toStr(action.fromxy)+" to "+toStr(action.toxy)+".");
    }

    if (action.type== Action.AType.BASIC_DOUBLE)
    {
      Sprite s1 = viewer.bottoms.get(action.fromxy);
      Sprite s2 = viewer.crowns.get(action.fromxy);
      viewer.setActionsHighlight(0, action.fromxy, commitTime0, 0);
      viewer.setActionsHighlight(1, action.toxy, commitTime0, 0);

      doubles[p].remove((Integer)action.fromxy);
      doubles[p].add(action.toxy);

      viewer.bottoms.remove(action.fromxy);
      viewer.bottoms.put(action.toxy, s1);
      viewer.crowns.remove(action.fromxy);
      viewer.crowns.put(action.toxy, s2);

      for (Sprite s: new Sprite[] {s1, s2})
      {
        s.setZIndex(s.getZIndex()+10);
        viewer.graphics.commitEntityState(commitTime0, s);
        viewer.setXY(s, action.toxy);
        s.setZIndex(s.getZIndex()-10);
        viewer.graphics.commitEntityState(commitTime1, s);
      }

      manager.addToGameSummary(player.getNicknameToken() + " performed a double slide from " + toStr(action.fromxy)+" to "+toStr(action.toxy)+".");
    }

    if (action.type== Action.AType.BASIC_TRANSPOSE)
    {
      Sprite s = viewer.crowns.get(action.fromxy);
      viewer.setActionsHighlight(0, action.fromxy, commitTime0, 0);
      viewer.setActionsHighlight(1, action.toxy, commitTime0, 0);

      doubles[p].remove((Integer)action.fromxy);
      singles[p].add(action.fromxy);
      singles[p].remove((Integer)action.toxy);
      doubles[p].add(action.toxy);

      viewer.crowns.remove(action.fromxy);
      viewer.crowns.put(action.toxy, s);
      viewer.tooltipModule.setTooltipText(viewer.bottoms.get(action.fromxy), PLAYER_NAMES[p]+" Single");
      viewer.tooltipModule.setTooltipText(viewer.bottoms.get(action.toxy), PLAYER_NAMES[p]+" Double");

      //s.setZIndex(s.getZIndex()+10);
      viewer.graphics.commitEntityState(commitTime0, s);
      viewer.setXY(s, action.toxy);
      //s.setZIndex(s.getZIndex()-10);
      viewer.graphics.commitEntityState(commitTime1, s);

      manager.addToGameSummary(player.getNicknameToken() + " performed a transposition from " + toStr(action.fromxy)+" to "+toStr(action.toxy)+".");
    }

    if (action.type== Action.AType.IMPASSE_SINGLE)
    {
      Sprite s = viewer.bottoms.get(action.fromxy);
      viewer.setActionsHighlight(0, action.fromxy, commitTime0, 2);
      viewer.setActionsHighlight(1, -1, commitTime0, 0);

      singles[p].remove((Integer)action.fromxy);

      viewer.bottoms.remove(action.fromxy);

      s.setZIndex(s.getZIndex()+10);
      viewer.graphics.commitEntityState(commitTime0, s);
      viewer.sendHome(s, p);
      s.setVisible(false);
      viewer.graphics.commitEntityState(commitTime1, s);
      increaseScore(p, commitTime1);

      manager.addToGameSummary(player.getNicknameToken() + " is at an \"impasse\" and removes a single on " + toStr(action.fromxy)+".");
    }

    if (action.type== Action.AType.IMPASSE_DOUBLE)
    {
      Sprite s = viewer.crowns.get(action.fromxy);
      viewer.setActionsHighlight(0, action.fromxy, commitTime0, 2);
      viewer.setActionsHighlight(1, -1, commitTime0, 0);

      doubles[p].remove((Integer)action.fromxy);
      singles[p].add(action.fromxy);

      viewer.crowns.remove(action.fromxy);
      viewer.tooltipModule.setTooltipText(viewer.bottoms.get(action.fromxy), PLAYER_NAMES[p]+" Single");

      s.setZIndex(s.getZIndex()+10);
      viewer.graphics.commitEntityState(commitTime0, s);
      viewer.sendHome(s, p);
      s.setVisible(false);
      viewer.graphics.commitEntityState(commitTime1, s);
      increaseScore(p, commitTime1);

      manager.addToGameSummary(player.getNicknameToken() + " is at an \"impasse\" and removes a crown from a double on " + toStr(action.fromxy)+".");
    }

    if (action.bearoff)
    {
      Sprite s = viewer.crowns.get(action.toxy);
      viewer.setActionsHighlight(0, -1, commitTime1, 0);
      viewer.setActionsHighlight(1, action.toxy, commitTime1, 2);

      doubles[p].remove((Integer)action.toxy);
      singles[p].add(action.toxy);

      viewer.crowns.remove(action.toxy);
      viewer.tooltipModule.setTooltipText(viewer.bottoms.get(action.toxy), PLAYER_NAMES[p]+" Single");

      s.setZIndex(s.getZIndex()+10);
      viewer.graphics.commitEntityState(commitTime1, s);
      viewer.sendHome(s, p);
      s.setVisible(false);
      viewer.graphics.commitEntityState(commitTime2, s);
      increaseScore(p, commitTime2);

      manager.addToGameSummary(player.getNicknameToken() + " \"bear off\" a crown from a double on " + toStr(action.toxy)+".");
    }

    if (action.crownfromxy != -1)
    {
      Sprite s = viewer.bottoms.get(action.crownfromxy);
      viewer.setActionsHighlight(0, action.crownfromxy, commitTime2, 1);
      viewer.setActionsHighlight(1, action.crowntoxy, commitTime2, 1);

      singles[p].remove((Integer)action.crownfromxy);
      singles[p].remove((Integer)action.crowntoxy);
      doubles[p].add(action.crowntoxy);

      viewer.bottoms.remove(action.crownfromxy);
      viewer.crowns.put(action.crowntoxy, s);
      viewer.tooltipModule.setTooltipText(s, "");
      viewer.tooltipModule.setTooltipText(viewer.bottoms.get(action.crowntoxy), PLAYER_NAMES[p]+" Double");

      s.setZIndex(s.getZIndex()+10);
      viewer.graphics.commitEntityState(commitTime2, s);
      viewer.setXY(s, action.crowntoxy);
      s.setZIndex(s.getZIndex()-10+1);
      s.setImage((p+2)+".png");
      viewer.graphics.commitEntityState(commitTime3, s);

      manager.addToGameSummary(""+player.getNicknameToken() + " crowned a single on " + toStr(action.crowntoxy)+" using a single from "+toStr(action.crownfromxy)+".");
    }
  }

  public void increaseScore(int player, double t)
  {
    score[player]++;
    viewer.playerScore[player].setText(score[player]+"");
    viewer.graphics.commitEntityState(t, viewer.playerScore[player]);
    viewer.gameManager.addTooltip(viewer.gameManager.getPlayer(player), viewer.gameManager.getPlayer(player).getNicknameToken() + " scored");
  }

  public static int toXY(int x, int y)
  {
    return y*WIDTH+x;
  }

  public static String toStr(int x, int y)
  {
    return ((char)(97 + x))+ "" + (y+1);
  }

  public static String toStr(int xy)
  {
    return ((char)(97 + xy%WIDTH))+ "" + ((xy/WIDTH)+1);
  }
}
