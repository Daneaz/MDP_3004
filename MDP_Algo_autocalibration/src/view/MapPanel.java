package view;

import model.physical.Cell;
import model.physical.Grid;
import model.physical.Robot;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

import static constant.MapConstant.*;
import static constant.RobotConstant.*;

/**
 * Map UI
 */
public class MapPanel extends JPanel implements Observer {

    private Grid mGrid;
    private Robot mRobot;

    MapPanel(Grid grid, Robot robot) {
        mGrid = grid;
        mRobot = robot;
        initializeMap();
    }

    private void initializeMap() {
        setPreferredSize(new Dimension(CELL_SIZE * MAP_COLUMNS, CELL_SIZE * MAP_ROWS));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        /* paint map */
        Cell[][] cells = mGrid.getCell();
        for (int x = 0; x < MAP_COLUMNS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                /* draw cells */
                if (Grid.isInStartingZone(x, y))
                    g2d.setColor(Color.YELLOW);
                else if (Grid.isInEndingZone(x, y))
                    g2d.setColor(Color.BLUE);
                else if (cells[x][y].getExplored()) {
                    if (cells[x][y].getIsObstacle())
                        g2d.setColor(Color.BLACK);
                    else
                        g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(Color.LIGHT_GRAY);
                }
                g2d.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                /* draw border */
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        /* paint robot */
        g2d.setColor(Color.BLACK);
        g2d.fillOval(mRobot.getPositionX() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                mRobot.getPositionY() * CELL_SIZE + PAINT_PIXEL_OFFSET,
               CELL_SIZE * SIZE_OF_ROBOT - 2 * PAINT_PIXEL_OFFSET,
                CELL_SIZE * SIZE_OF_ROBOT - 2 * PAINT_PIXEL_OFFSET);

        /* paint robot heading */
        g2d.setColor(Color.WHITE);
        if (mRobot.getDirection() == NORTH) {
            g2d.fillOval((mRobot.getPositionX() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                    mRobot.getPositionY() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                    SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        } else if (mRobot.getDirection() == SOUTH) {
            g2d.fillOval((mRobot.getPositionX() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                    (mRobot.getPositionY() + 2) * CELL_SIZE + CELL_SIZE - SIZE_OF_DIRECTION_PIXEL - PAINT_PIXEL_OFFSET,
                    SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        } else if (mRobot.getDirection() == WEST) {
            g2d.fillOval(mRobot.getPositionX() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                    (mRobot.getPositionY() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                    SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        } else if (mRobot.getDirection() == EAST) {
            g2d.fillOval((mRobot.getPositionX() + 2) * CELL_SIZE + CELL_SIZE - SIZE_OF_DIRECTION_PIXEL - PAINT_PIXEL_OFFSET,
                    (mRobot.getPositionY() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                    SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        //System.out.println("Map updated, repainting");
        this.repaint();
    }
}
