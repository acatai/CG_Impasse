import java.util.*;

class Action
{
    public enum AType {
        BASIC_SINGLE,
        BASIC_DOUBLE,
        BASIC_TRANSPOSE,
        IMPASSE_SINGLE,
        IMPASSE_DOUBLE,
    }

    public Action.AType type;
    public int fromxy;
    public int toxy;
    public int crownfromxy = -1;
    public int crowntoxy = -1;
    public boolean bearoff = false;

    public static Action newBasicSingle(int fromxy, int toxy)
    {
        Action a = new Action();
        a.type = Action.AType.BASIC_SINGLE;
        a.fromxy = fromxy;
        a.toxy = toxy;
        return a;
    }

    public static Action newBasicDouble(int fromxy, int toxy, boolean bearoff)
    {
        Action a = new Action();
        a.type = Action.AType.BASIC_DOUBLE;
        a.fromxy = fromxy;
        a.toxy = toxy;
        a.bearoff = bearoff;
        return a;
    }

    public static Action newBasicTranspose(int fromxy, int toxy, boolean bearoff)
    {
        Action a = new Action();
        a.type = Action.AType.BASIC_TRANSPOSE;
        a.fromxy = fromxy;
        a.toxy = toxy;
        a.bearoff = bearoff;
        return a;
    }

    public static Action newImpasse(int fromxy, boolean fromsingle)
    {
        Action a = new Action();
        a.type = fromsingle ? Action.AType.IMPASSE_SINGLE : Action.AType.IMPASSE_DOUBLE;
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
        if (type == Action.AType.IMPASSE_SINGLE || type == Action.AType.IMPASSE_DOUBLE) s = GameState.toStr(fromxy);
        else s = GameState.toStr(fromxy)+ GameState.toStr(toxy);

        if (crownfromxy != -1) s += GameState.toStr(crownfromxy);

        return s;
    }
}


class GameState
{
    public ArrayList<Integer>[] singles = new ArrayList[]{new ArrayList<Integer>(), new ArrayList<Integer>()};
    public ArrayList<Integer>[] doubles = new ArrayList[]{new ArrayList<Integer>(), new ArrayList<Integer>()};

    public static final int[] occupied = new int[8*8];
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    public GameState() {}

    public static GameState empty()
    {
        GameState s = new GameState();
        s.singles[0] = new ArrayList<>();
        s.singles[1] = new ArrayList<>();
        s.doubles[0] = new ArrayList<>();
        s.doubles[1] = new ArrayList<>();
        return s;
    }

    public void addTo(char symbol, int x, int y)
    {
        switch (symbol)
        {
            case '.': return;
            case 'w': singles[0].add(toXY(x, y)); return;
            case 'W': doubles[0].add(toXY(x, y)); return;
            case 'b': singles[1].add(toXY(x, y)); return;
            case 'B': doubles[1].add(toXY(x, y)); return;
        }
    }

    public GameState copy()
    {
        GameState s = new GameState();
        for (int p=0; p <2; p++) s.singles[p] = new ArrayList<>(this.singles[p]);
        for (int p=0; p <2; p++) s.doubles[p] = new ArrayList<>(this.doubles[p]);
        return s;
    }

    private int distToCrown(int player, int xy)
    {
        int crownrow = player==0 ? 7 : 0;
        int y = xy/WIDTH;
        return Math.abs(crownrow - y);
    }
    private int distToBearOff(int player, int xy)
    {
        int bearrow = player==0 ? 0 : 7;
        int y = xy/WIDTH;
        return Math.abs(bearrow - y);
    }

    public int eval(int p)
    {
        if (terminal(p)) return 100000;

        int score = 0;
        int removed = 12 - singles[p].size() - 2 * doubles[p].size();
        score += 100 * removed;


        for (Integer xy:singles[p])
        {
            score -= distToCrown(p, xy);
        }

        for (Integer xy:doubles[p])
        {
            score -= distToBearOff(p, xy);
        }

        return score;
    }

    public boolean terminal(int p)
    {
        return singles[p].size() + doubles[p].size() == 0;
    }

    private void computeOccupied()
    {
        for (int xy=0; xy <WIDTH*HEIGHT; xy++) occupied[xy] = -1;
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

    public void applyAction(int p, Action action)
    {
        if (action.type== Action.AType.BASIC_SINGLE)
        {
            singles[p].remove((Integer)action.fromxy);
            singles[p].add(action.toxy);
        }

        if (action.type== Action.AType.BASIC_DOUBLE)
        {
            doubles[p].remove((Integer)action.fromxy);
            doubles[p].add(action.toxy);
        }

        if (action.type== Action.AType.BASIC_TRANSPOSE)
        {
            doubles[p].remove((Integer)action.fromxy);
            singles[p].add(action.fromxy);
            singles[p].remove((Integer)action.toxy);
            doubles[p].add(action.toxy);
        }

        if (action.type== Action.AType.IMPASSE_SINGLE)
        {
            singles[p].remove((Integer)action.fromxy);
        }

        if (action.type== Action.AType.IMPASSE_DOUBLE)
        {
            doubles[p].remove((Integer)action.fromxy);
            singles[p].add(action.fromxy);
        }

        if (action.bearoff)
        {
            doubles[p].remove((Integer)action.toxy);
            singles[p].add(action.toxy);
        }

        if (action.crownfromxy != -1)
        {
            singles[p].remove((Integer)action.crownfromxy);
            singles[p].remove((Integer)action.crowntoxy);
            doubles[p].add(action.crowntoxy);
        }
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



    public static int DEPTH = 4;
    public static double GAMMA = 0.82;

    public static double search(GameState s, int p, int depth)
    {
        if (depth==0 || s.terminal(p)) return s.eval(p);

        ArrayList<Action> actions = s.generateLegalActions(p);
        double bestscore = -10000000;
        for (Action a: actions)
        {
            GameState copy = s.copy();
            copy.applyAction(p, a);
            double score = copy.eval(p) + GAMMA * search(copy, p, depth-1);
            if (score > bestscore)
            {
                bestscore = score;
            }
        }
        return bestscore;
    }

}


class Player
{
    public static void main(String[] args)
    {
        Random RNG = new Random(36);
        Scanner in = new Scanner(System.in);

        GameState.pregenerateMovements();

        String color = in.next();
        int PLAYER = color.equals("w")?0:1;
        //System.err.println(PLAYER);
        while (true)
        {
            GameState STATE = GameState.empty();
            for (int y = 7; y >=0; y--)
            {
                char[] line = in.next().toCharArray();
                for (int x = 0; x < line.length; x++) STATE.addTo(line[x], x, y);
            }
            String last_action = in.next();
            int actionsNum = in.nextInt();
            for (int y = 0; y < actionsNum; ++y) in.next();

            ArrayList<Action> actions = STATE.generateLegalActions(PLAYER);

//            for(int y = GameState.HEIGHT-1; y >=0 ; y--)
//            {
//                String s = "";
//                for (int x = 0; x < GameState.WIDTH; x++) s += GameState.UNIT_CHAR.get(GameState.occupied[GameState.toXY(x, y)]);
//                System.err.println(s);
//            }
            //System.err.println("Current score: " + STATE.eval(PLAYER));

            Action bestaction = null;
            double bestscore = -10000000;
            for (Action action: actions)
            {
                GameState copy = STATE.copy();
                copy.applyAction(PLAYER, action);
                double score = copy.eval(PLAYER) + GameState.GAMMA * GameState.search(copy, PLAYER, GameState.DEPTH-2);
                if (score > bestscore)
                {
                    bestscore = score;
                    bestaction = action;
                }
            }
            //System.err.println("Best action score avg: " + bestscore/GameState.DEPTH);

            //System.out.println(String.format("%s %.2f/%d", bestaction.toString(), bestscore, GameState.DEPTH));
            System.out.println(bestaction.toString());
        }
    }
}
