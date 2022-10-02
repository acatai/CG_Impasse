package com.codingame.game.engine;

import java.util.ArrayList;
import java.util.HashMap;

/*
Game tags: Minimax, Bitboarding, Monte Carlo Tree Search, Neural network
 */
public class Constants
{
  public static int WIDTH = 8;
  public static int HEIGHT = 8;

  public static int TURNLIMIT = 250;
  public static int TIMELIMIT_INIT = 1000;
  public static int TIMELIMIT_TURN = 100;

  ///////////////////////////////////////////////////
  // VISUALIZATION
  ///////////////////////////////////////////////////
  public static int VIEWER_WIDTH = 1920;
  public static int VIEWER_HEIGHT = 1080;

  public static int MAX_MESSAGE_LENGTH = 15;

  public static final double MOVE_TIME = 3.0;
  public static final double CROWN_TIME = 0.8;
  public static final double REMOVE_TIME = 0.8;

  public static int VIEWER_RECTANGLE_SIZE = VIEWER_HEIGHT / -~HEIGHT;

  final static String[] PLAYER_NAMES = new String[] {"White","Black", };
  final static int[] PLAYERTEXTCOLORS = new int[] {0xffffff,0x000000, }; // different from config.ini
  final static int[] BOARDCOLORS = new int[] {0xb5d2a8,0x84b073, }; //{0xD2C2A8, 0xB09673}; // {0xF0D9B5, 0x946f51};
  final static int BACKGROUNDCOLOR = 0x184006; // 0x7F7F7F;
  final static int[] HIGHLIGHTCOLOR = new int[]{0xd7cd7e, 0xf2e041, 0xe0b91b}; // move, crown, remove // d7cd7e/d1c458

  public static int Z_UNIT = 10;
  public static int Z_CROWNUNIT = 11;

  ///////////////////////////////////////
  // OTHER CONSTANTS AND HELPER FUNCTIONS
  ///////////////////////////////////////
  public static final HashMap<Integer, String> UNIT_CHAR  = new HashMap<Integer, String>() {{ put(-1, "."); put(0, "w");put(1, "b");put(2, "W");put(3, "B"); }};
  public static final ArrayList<Integer>[][][] FORWARDS = new ArrayList[WIDTH*HEIGHT][2][2]; // xy -> player -> 0,1 -> [xy]
  public static final ArrayList<Integer>[][][] BACKWARDS = new ArrayList[WIDTH*HEIGHT][2][2]; // xy -> player -> 0,1 -> [xy]
  public static final ArrayList<Integer>[][] TRANSPOSES = new ArrayList[WIDTH*HEIGHT][2]; // xy -> player -> [xy]

  public static void pregenerateMovements()
  {
    int x1, y1;
    for (int x=0; x < WIDTH; x++)
    {
      for (int y=0; y < HEIGHT; y++)
      {
        TRANSPOSES[y*WIDTH+x][0] = new ArrayList<>();
        TRANSPOSES[y*WIDTH+x][1] = new ArrayList<>();

        ArrayList<Integer> list = new ArrayList<>();
        x1=x; y1=y;
        while (true)
        {
          x1--;
          y1++;
          if (x1 <0 || y1>=HEIGHT) break;
          list.add(y1*WIDTH+x1);
        }
        FORWARDS[y*WIDTH+x][0][0] = new ArrayList<>(list);
        BACKWARDS[y*WIDTH+x][1][0] = new ArrayList<>(list);
        if (list.size() > 0) TRANSPOSES[y*WIDTH+x][1].add(list.get(0));

        list = new ArrayList<>();
        x1=x; y1=y;
        while (true)
        {
          x1++;
          y1++;
          if (x1 >=WIDTH || y1>=HEIGHT) break;
          list.add(y1*WIDTH+x1);
        }
        FORWARDS[y*WIDTH+x][0][1] = new ArrayList<>(list);
        BACKWARDS[y*WIDTH+x][1][1] = new ArrayList<>(list);
        if (list.size() > 0) TRANSPOSES[y*WIDTH+x][1].add(list.get(0));

        list = new ArrayList<>();
        x1=x; y1=y;
        while (true)
        {
          x1--;
          y1--;
          if (x1 <0 || y1< 0) break;
          list.add(y1*WIDTH+x1);
        }
        FORWARDS[y*WIDTH+x][1][0] = new ArrayList<>(list);
        BACKWARDS[y*WIDTH+x][0][0] = new ArrayList<>(list);
        if (list.size() > 0) TRANSPOSES[y*WIDTH+x][0].add(list.get(0));

        list = new ArrayList<>();
        x1=x; y1=y;
        while (true)
        {
          x1++;
          y1--;
          if (x1 >=WIDTH || y1< 0) break;
          list.add(y1*WIDTH+x1);
        }
        FORWARDS[y*WIDTH+x][1][1] = new ArrayList<>(list);
        BACKWARDS[y*WIDTH+x][0][1] = new ArrayList<>(list);
        if (list.size() > 0) TRANSPOSES[y*WIDTH+x][0].add(list.get(0));
      }
    }
  }
}
