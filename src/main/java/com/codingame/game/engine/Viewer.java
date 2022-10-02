package com.codingame.game.engine;

import com.codingame.game.Player;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.*;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;

import static com.codingame.game.engine.Constants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Viewer
{
    MultiplayerGameManager<Player> gameManager;
    GraphicEntityModule graphics;
    TooltipModule tooltipModule;
    public Rectangle[][] rectangles;
    Line[] lines;
    Rectangle[] actionsHighlight;

    Sprite[] playerPiece = new Sprite[2];
    RoundedRectangle[] playerFrame = new RoundedRectangle[2];
    public Text[] playerScore = new Text[2];
    public Text[] playerAction = new Text[2];
    public Text[] playerMessage = new Text[2];

    HashMap<Integer, Sprite> bottoms = new HashMap<>();
    HashMap<Integer, Sprite> crowns = new HashMap<>();

    int CIRCLE_RADIUS;
    int GAP;

    public Viewer(GraphicEntityModule graphics, MultiplayerGameManager<Player> gameManager, ToggleModule toggleModule, TooltipModule tooltipModule)
    {
        this.graphics = graphics;
        this.gameManager = gameManager;
        this.tooltipModule = tooltipModule;

        rectangles = new Rectangle[HEIGHT][WIDTH];
        lines = new Line[HEIGHT + WIDTH + 2];
        actionsHighlight = new Rectangle[3];

        this.graphics.createRectangle().setWidth(VIEWER_WIDTH).setHeight(VIEWER_HEIGHT).setFillColor(BACKGROUNDCOLOR);

        int START_X = VIEWER_WIDTH / 2 - VIEWER_RECTANGLE_SIZE * WIDTH / 2;
        int FONT_SIZE = VIEWER_RECTANGLE_SIZE / 3;

        for (int y = 0; y < HEIGHT; ++y) {
            int yG = HEIGHT - y - 1;
            for (int x = 0; x < WIDTH; ++x) {
                int xG = x;
                rectangles[x][y] = graphics.createRectangle().setZIndex(1).setWidth(VIEWER_RECTANGLE_SIZE).setHeight(VIEWER_RECTANGLE_SIZE).setX(START_X + xG * VIEWER_RECTANGLE_SIZE).setY((int) (VIEWER_RECTANGLE_SIZE / 2) + yG * VIEWER_RECTANGLE_SIZE - FONT_SIZE / 2);
                tooltipModule.setTooltipText(rectangles[x][y], GameState.toStr(x, y));
                if (x == 0) {
                    int length = -~y < 10 ? 1 : (int) (Math.log10(x) + 1);
                    graphics.createText(Integer.toString(-~y)).setX(rectangles[x][y].getX() - (int) (VIEWER_RECTANGLE_SIZE / 1.5) + (int) (FONT_SIZE / length * .5)).setY(rectangles[x][y].getY() + FONT_SIZE).setFontFamily("Verdana").setFontSize(FONT_SIZE).setFillColor(0xFEFEFE);
                }

                if (y == 0) {
                    graphics.createText(Character.toString((char) (97 + x))).setX(rectangles[x][y].getX() + (int) (FONT_SIZE * (1.25))).setY(rectangles[x][y].getY() + VIEWER_RECTANGLE_SIZE + FONT_SIZE / 4).setFontFamily("Verdana").setFontSize(FONT_SIZE).setFillColor(0xFEFEFE);
                }
                rectangles[x][y].setFillColor(BOARDCOLORS[(x + y + 1)  & 1]);
            }
        }

        for (int y = 0; y <= HEIGHT; ++y) {
            lines[y] = graphics.createLine().setFillColor(0x0).setLineWidth(3.0).setZIndex(3)
                    .setX(START_X).setX2(START_X + WIDTH * VIEWER_RECTANGLE_SIZE)
                    .setY((int)(VIEWER_RECTANGLE_SIZE / 2) + y * VIEWER_RECTANGLE_SIZE - FONT_SIZE / 2)
                    .setY2((int)(VIEWER_RECTANGLE_SIZE / 2) + y * VIEWER_RECTANGLE_SIZE - FONT_SIZE / 2);
        }

        for (int x = 0; x <= WIDTH; ++x) {
            lines[WIDTH + 1 + x] = graphics.createLine().setFillColor(0x0).setLineWidth(3.0).setZIndex(3)
                    .setX(START_X + x * VIEWER_RECTANGLE_SIZE).setX2(START_X + x * VIEWER_RECTANGLE_SIZE)
                    .setY((int)(VIEWER_RECTANGLE_SIZE / 2) - FONT_SIZE / 2)
                    .setY2((int)(VIEWER_RECTANGLE_SIZE / 2) + HEIGHT * VIEWER_RECTANGLE_SIZE - FONT_SIZE / 2);
        }

        CIRCLE_RADIUS = (int)(VIEWER_RECTANGLE_SIZE * .42);
        GAP = (VIEWER_RECTANGLE_SIZE - CIRCLE_RADIUS * 2) / 2;

        for (int i = 0; i < 3; ++i)
        {
            actionsHighlight[i] = graphics.createRectangle().setFillColor(HIGHLIGHTCOLOR[0]).setWidth(VIEWER_RECTANGLE_SIZE).setHeight(VIEWER_RECTANGLE_SIZE).setX(-VIEWER_RECTANGLE_SIZE).setY(-VIEWER_RECTANGLE_SIZE).setFillAlpha(i < 2 ? 0.75 : 0.45).setZIndex(2);
            toggleModule.displayOnToggleState(actionsHighlight[i], "debugToggle", true);
        }

        for (int p = 0; p < 2; ++p)
        {
            int HUDWIDTH = 340;
            int START_Y = p==1? rectangles[0][HEIGHT-1].getY() : rectangles[0][0].getY() + VIEWER_RECTANGLE_SIZE - 550;
            int START_PLAYER_X = p == 1 ? VIEWER_WIDTH - 30 - (HUDWIDTH) : 30;

            graphics.createRoundedRectangle().setHeight(550).setWidth(HUDWIDTH).setX(START_PLAYER_X).setY(START_Y).setFillColor(0xFFFFFF).setAlpha(0.15).setLineWidth(4);
            playerFrame[p] = graphics.createRoundedRectangle().setHeight(550).setWidth(HUDWIDTH).setX(START_PLAYER_X).setY(START_Y).setFillAlpha(0).setLineColor(p==0?0xffffff:0x000000).setLineAlpha(0.25).setLineWidth(14).setVisible(false);

            graphics.createText(gameManager.getPlayer(p).getNicknameToken()).setFontSize(42).setX(START_PLAYER_X + HUDWIDTH/2).setAnchorX(0.5).setY(START_Y + 25).setFillColor(PLAYERTEXTCOLORS[p]);
            graphics.createSprite().setImage(gameManager.getPlayer(p).getAvatarToken()).setX(START_PLAYER_X + HUDWIDTH/2).setY(START_Y + 100).setAnchorX(0.5).setBaseHeight(140).setBaseWidth(140);
            playerPiece[p] = graphics.createSprite().setImage(p +".png").setX(START_PLAYER_X + HUDWIDTH/2 - CIRCLE_RADIUS).setY(START_Y + 270).setBaseWidth(CIRCLE_RADIUS * 2).setBaseHeight(CIRCLE_RADIUS * 2);
            playerScore[p] = graphics.createText().setText("0").setFillColor(p==0?0x000000:0xffffff).setAnchorX(0.5).setAnchorY(0.5).setX(START_PLAYER_X + HUDWIDTH/2).setY(START_Y + 270 + CIRCLE_RADIUS).setFontSize(50).setFontWeight(Text.FontWeight.BOLD);
            playerAction[p] = graphics.createText().setText("").setFillColor(PLAYERTEXTCOLORS[p]).setAnchorX(0.5).setX(START_PLAYER_X + HUDWIDTH/2).setY(START_Y + 390).setFontSize(50);
            playerMessage[p] = graphics.createText().setText("").setFillColor(PLAYERTEXTCOLORS[p]).setAnchorX(0.5).setX(START_PLAYER_X + HUDWIDTH/2).setY(START_Y + 480).setFontSize(36);
        }

        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                graphics.commitEntityState(0.4, rectangles[x][y]);
            }
        }

        for (int i = 0; i < 3; ++i) {
            actionsHighlight[i].setX(-VIEWER_RECTANGLE_SIZE, Curve.IMMEDIATE);
        }
    }

    public void showState(GameState s)
    {
        bottoms.clear();
        crowns.clear();

        for (int p=0; p < 2; p++)
        {
            for (Integer xy : s.singles[p])
            {
                bottoms.put(xy, createUnit(p, xy, true));
            }

            for (Integer xy : s.doubles[p])
            {
                bottoms.put(xy, createUnit(p, xy, false));
                crowns.put(xy, createUnit(p+2, xy, false));
            }
        }
    }



    private Sprite createUnit(int p, int xy, boolean single)
    {
        Sprite s = graphics.createSprite()
                .setImage(p+ ".png")
                .setX(100)
                .setY(200)
                .setZIndex(p>1?Z_CROWNUNIT:Z_UNIT)
                .setBaseWidth(CIRCLE_RADIUS * 2)
                .setBaseHeight(CIRCLE_RADIUS * 2)
                .setX(rectangles[xy%WIDTH][xy/WIDTH].getX() + GAP)
                .setY(rectangles[xy%WIDTH][xy/WIDTH].getY() + GAP);
        if (p < 2) tooltipModule.setTooltipText(s, PLAYER_NAMES[p%2]+" " + (single?"Single":"Double"));
        return s;
    }

    public void setXY(Sprite sprite, int x, int y)
    {
        sprite.setX(rectangles[x][y].getX() + GAP).setY(rectangles[x][y].getY() + GAP);
    }
    public void setXY(Sprite sprite, int xy)
    {
        sprite.setX(rectangles[xy%WIDTH][xy/WIDTH].getX() + GAP).setY(rectangles[xy%WIDTH][xy/WIDTH].getY() + GAP);
    }

    public void sendHome(Sprite sprite, int player)
    {
        sprite.setX(playerPiece[player].getX()).setY(playerPiece[player].getY());
    }

    public double computePixelBoardDistance(int xy1, int xy2)
    {
        Rectangle r1 = rectangles[xy1%WIDTH][xy1/WIDTH];
        Rectangle r2 = rectangles[xy2%WIDTH][xy2/WIDTH];
        //return Math.pow(Math.max(Math.abs(r1.getX() - r2.getX()),Math.abs(r1.getY() - r2.getY())), 0.45);
        return Math.pow((r1.getX() - r2.getX()) * (r1.getX() - r2.getX()) + (r1.getY() - r2.getY()) * (r1.getY() - r2.getY()), 0.5);
    }

    public double computePixelHomeDistance(int xy1, int player)
    {
        Rectangle r1 = rectangles[xy1%WIDTH][xy1/WIDTH];
        Sprite r2 = playerPiece[player];
        //return Math.pow(Math.max(Math.abs(r1.getX() - r2.getX()),Math.abs(r1.getY() - r2.getY())), 0.45);
        return Math.pow((r1.getX() - r2.getX()) * (r1.getX() - r2.getX()) + (r1.getY() - r2.getY()) * (r1.getY() - r2.getY()), 0.5);
    }

    public void setActionsHighlight(int id, int xy, double t, int color)
    {
        if (xy == -1)
        {
            actionsHighlight[id].setVisible(false);
        }
        else
        {
            actionsHighlight[id].setX(rectangles[xy%WIDTH][xy/WIDTH].getX(), Curve.NONE).setY(rectangles[xy%WIDTH][xy/WIDTH].getY(), Curve.NONE).setFillColor(HIGHLIGHTCOLOR[color], Curve.NONE).setVisible(true);
        }
        graphics.commitEntityState(t, actionsHighlight[id]);
    }

    public void showFrame(int p)
    {
        playerFrame[p].setVisible(true);
        graphics.commitEntityState(0, playerFrame[p]);
        playerFrame[p^1].setVisible(false);
        graphics.commitEntityState(0, playerFrame[p^1]);
    }
}
