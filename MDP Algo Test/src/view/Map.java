package view;

import javax.imageio.ImageIO;
import javax.swing.*;

import static constant.MapConstant.*;
import static constant.RobotConstant.*;
import model.physical.Cell;
import model.physical.Grid;
import model.physical.Robot;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class Map extends JPanel implements Observer {
	private Grid mapGrid;
	private Robot mapRobot;
	
	BufferedImage imgNorth, imgSouth, imgEast, imgWest;
		
	public Map (Grid grid, Robot robot) {
		mapGrid = grid;
		mapRobot = robot;
		initialMap();		
	}
	
	public void initialMap() {
		setPreferredSize(new Dimension(CELL_SIZE * MAP_COLUMNS, CELL_SIZE * MAP_ROWS));
	}
	
	@Override
	public void paintComponent (Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		
		Cell[][] cells = mapGrid.getCell();
        for (int x = 0; x < MAP_COLUMNS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                if (Grid.isInStartingZone(x, y))
                    g.setColor(STARTZONE);
                else if (Grid.isInEndingZone(x, y))
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
                
                g.setColor(new Color(152,152,152));
                g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        
        try {
			imgNorth = ImageIO.read(new File("robot_north.png"));
			imgSouth = ImageIO.read(new File("robot_south.png"));
			imgEast = ImageIO.read(new File("robot_east.png"));
			imgWest = ImageIO.read(new File("robot_west.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        switch (mapRobot.getDirection()) {
        case (NORTH): 
        		g.drawImage(imgNorth, mapRobot.getPositionX() * CELL_SIZE, mapRobot.getPositionY() * CELL_SIZE, 3 * CELL_SIZE, 3 * CELL_SIZE, this);
        		break;

        case (SOUTH): 
        		g.drawImage(imgSouth, mapRobot.getPositionX() * CELL_SIZE, mapRobot.getPositionY() * CELL_SIZE, 3 * CELL_SIZE, 3 * CELL_SIZE, this);
        	
        		break;
        case (WEST): 
        		g.drawImage(imgWest, mapRobot.getPositionX() * CELL_SIZE, mapRobot.getPositionY() * CELL_SIZE, 3 * CELL_SIZE, 3 * CELL_SIZE, this);
        		break;
        case (EAST):
        		g.drawImage(imgEast, mapRobot.getPositionX() * CELL_SIZE, mapRobot.getPositionY() * CELL_SIZE, 3 * CELL_SIZE, 3 * CELL_SIZE, this);
        		break;
        }
    }
	
	@Override
	public void update(Observable o, Object arg) {
		this.repaint();		
	}
}