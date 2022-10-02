package com.codingame.game.engine;

public class Action
{
  public enum AType {
    BASIC_SINGLE,
    BASIC_DOUBLE,
    BASIC_TRANSPOSE,
    IMPASSE_SINGLE,
    IMPASSE_DOUBLE,
  }

  public AType type;
  public int fromxy;
  public int toxy;
  public int crownfromxy = -1;
  public int crowntoxy = -1;
  public boolean bearoff = false;

  public static Action newBasicSingle(int fromxy, int toxy)
  {
    Action a = new Action();
    a.type = AType.BASIC_SINGLE;
    a.fromxy = fromxy;
    a.toxy = toxy;
    return a;
  }

  public static Action newBasicDouble(int fromxy, int toxy, boolean bearoff)
  {
    Action a = new Action();
    a.type = AType.BASIC_DOUBLE;
    a.fromxy = fromxy;
    a.toxy = toxy;
    a.bearoff = bearoff;
    return a;
  }

  public static Action newBasicTranspose(int fromxy, int toxy, boolean bearoff)
  {
    Action a = new Action();
    a.type = AType.BASIC_TRANSPOSE;
    a.fromxy = fromxy;
    a.toxy = toxy;
    a.bearoff = bearoff;
    return a;
  }

  public static Action newImpasse(int fromxy, boolean fromsingle)
  {
    Action a = new Action();
    a.type = fromsingle ? AType.IMPASSE_SINGLE : AType.IMPASSE_DOUBLE;
    a.fromxy = fromxy;
    a.toxy = -1;
    return a;
  }

  public Action copy()
  {
    Action a = new Action();
    a.type = type;
    a.fromxy = fromxy;
    a.toxy = toxy;
    a.crownfromxy = crownfromxy;
    a.crowntoxy = crowntoxy;
    a.bearoff = bearoff;
    return a;
  }

  @Override
  public String toString()
  {
    String s = "";
    if (type == AType.IMPASSE_SINGLE || type == AType.IMPASSE_DOUBLE) s = GameState.toStr(fromxy);
    else s = GameState.toStr(fromxy)+ GameState.toStr(toxy);

    if (crownfromxy != -1) s += GameState.toStr(crownfromxy);

    return s;
  }
}
