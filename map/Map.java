package map;

import javax.swing.*;

import static constant.MapConstant.*;
import static constant.RobotConstant.*;
import model.physical.Cell;
import model.physical.Grid;
import model.physical.Robot;

import java.awt.*;
import java.util.Observable;
import java.util.Observer;

public class Map extends JPanel implements Observer {
	private Grid mapGrid;
	private Robot mapRobot;
	
	public Map (Grid grid, Robot robot) {
		mapGrid = grid;
		mapRobot = robot;
		initialMap();		
	}

	public void initialMap() {
		setPreferredSize(new Dimension(CELL_SIZE * MAP_COLUMNS, CELL_SIZE * MAP_ROWS));
	}
	
	@Override
	public void paintComponent (Graphics g) {
				
		Cell[][] cells = mapGrid.getCell();
        for (int x = 0; x < MAP_COLUMNS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                if (Grid.isInStartingZone(x, y))
                    g.setColor(STARTZONE);
                else if (Grid.isInGoalZone(x, y))
                    g.setColor(GOALZONE);
                else if (cells[x][y].getExplored()) {
                    if (cells[x][y].getIsObstacle())
                        g.setColor(OBSTACLE);
                    else
                        g.setColor(EXPLORED);
                } else {
                    g.setColor(UNEXPLORED);
                }
                g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                
                g.setColor(Color.BLACK);
                g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        
        g.setColor(ROBOT);
        g.fillOval(mapRobot.getPositionX() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                mapRobot.getPositionY() * CELL_SIZE + PAINT_PIXEL_OFFSET,
               CELL_SIZE * SIZE_OF_ROBOT - 2 * PAINT_PIXEL_OFFSET,
                CELL_SIZE * SIZE_OF_ROBOT - 2 * PAINT_PIXEL_OFFSET);

        g.setColor(ROBOT_DIR);
        
        switch (mapRobot.getDirection()) {
        case (NORTH): g.fillOval((mapRobot.getPositionX() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                    mapRobot.getPositionY() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                    SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        break;
        case (SOUTH): g.fillOval((mapRobot.getPositionX() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                (mapRobot.getPositionY() + 2) * CELL_SIZE + CELL_SIZE - SIZE_OF_DIRECTION_PIXEL - PAINT_PIXEL_OFFSET,
                SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        break;
        case (WEST): g.fillOval(mapRobot.getPositionX() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                (mapRobot.getPositionY() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        break;
        case (EAST): g.fillOval((mapRobot.getPositionX() + 2) * CELL_SIZE + CELL_SIZE - SIZE_OF_DIRECTION_PIXEL - PAINT_PIXEL_OFFSET,
                (mapRobot.getPositionY() + 1) * CELL_SIZE + (CELL_SIZE - SIZE_OF_DIRECTION_PIXEL) / 2,
                SIZE_OF_DIRECTION_PIXEL, SIZE_OF_DIRECTION_PIXEL);
        break;
        }
    }
	
	@Override
	public void update(Observable o, Object arg) {
		this.repaint();		
	}
}









/*package map;


import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Map {
	
	final Stage primaryStage;
    private Rectangle[][] boardField = new Rectangle[15][20];

	public Map(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}
		
	public boolean isStartZone(int row, int col) {
		return (row >= 18 && row <= 20 && col >= 1 && col <= 3);
	}
	
	public boolean isGoalZone(int row, int col) {
		return (row >= 1 && row <= 3 && col >= 13 && col <= 15);
	}
	

    public void addBoardField(int pos1, int pos2, Rectangle rectangle) {
        boardField[pos1][pos2] = rectangle;
    }
    
    public Rectangle setUnexplored() {
    		Rectangle unexplored = new Rectangle(25, 25);
    		unexplored.setFill(Color.ANTIQUEWHITE);
    		unexplored.setStroke(Color.WHITE);
    		return unexplored;
    }
    
    public Rectangle setExplored() {
    		Rectangle explored = new Rectangle(25, 25);
    		explored.setFill(Color.GREENYELLOW);
    		explored.setStroke(Color.WHITE);
    		return explored;
    }
    
    public Rectangle setObstacle() {
    		Rectangle obstacle = new Rectangle(25, 25);
    		obstacle.setFill(Color.BLACK);
    		obstacle.setStroke(Color.WHITE);
    		return obstacle;
    }
    
    public Rectangle setStart() {
    		Rectangle start = new Rectangle(25, 25);
    		start.setFill(Color.GREEN);
    		start.setStroke(Color.WHITE);
    		return start;
    }
    
    public Rectangle setGoal() {
    		Rectangle goal = new Rectangle(25, 25);
    		goal.setFill(Color.MEDIUMVIOLETRED);
    		goal.setStroke(Color.WHITE);
    		return goal;
    }
}
*/
