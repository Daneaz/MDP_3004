package model.algo;

import model.physical.Cell;
import model.physical.Grid;
import model.physical.Robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static constant.MapConstant.MAP_COLUMNS;
import static constant.MapConstant.MAP_ROWS;
import static constant.RobotConstant.*;


/**
 * Interface for algorithms
 */
public interface Algorithm {
    int INFINITY = 1000000;
    void start(boolean realRun, Robot robot, Grid grid);
    
    
	static List<String> startAstarSearch(int startingPositionX, int startingPositionY, int endingPositionX, int endingPositionY,
			Grid grid, Robot proxyRobot) {
		StringBuilder stringBuilder = new StringBuilder();
	
		//actual value
		int[][] gValue;
		//heuristic value
		int[][] fValue;
		Cell[][] cells;
		HashMap<Cell, Cell> previousPosition;
		List<Cell> openSet;
		boolean[][] isSetClosed;
		
		gValue = new int[MAP_COLUMNS - 2][MAP_ROWS - 2];
		fValue = new int[MAP_COLUMNS - 2][MAP_ROWS - 2];
		cells = new Cell[MAP_COLUMNS - 2][MAP_ROWS - 2];
		previousPosition = new HashMap<>();
		openSet = new ArrayList<>();
		isSetClosed = new boolean[MAP_COLUMNS - 2][MAP_ROWS - 2];
		
		for (int x = 0; x < MAP_COLUMNS - 2; x++) {
			for (int y = 0; y < MAP_ROWS - 2; y++) {
            	gValue[x][y] = INFINITY;
            	fValue[x][y] = INFINITY;
            	cells[x][y] = new Cell(x, y);
            	isSetClosed[x][y] = false;    
            }
		}
        grid.clearClosedSet();
		System.out.println("-----A* Search for startX:" + startingPositionX + ", startY:" + startingPositionY + " to endX:" + endingPositionX + ", endY:" + endingPositionX + " -----");
		gValue[startingPositionX][startingPositionY] = 0;
		fValue[startingPositionX][startingPositionY] = estimateHowFarToGoal(startingPositionX, startingPositionY, endingPositionX, endingPositionY);
        //System.out.println("fValue for x:" + startingPositionX + ", y:" + startingPositionY + " is "+fValue[startingPositionX][startingPositionY]);
        cells[startingPositionX][startingPositionY].setDistance(fValue[startingPositionX][startingPositionY]);
        openSet.add(cells[startingPositionX][startingPositionY]);

        while (!openSet.isEmpty()) {
        	// get the Cell which has the least fValue from the openSet
            Cell currentCell = getCurrentCell(openSet, fValue);
            
            // if the currentCell is the desired Goal position return the Path string
            if (currentCell.getX() == endingPositionX && currentCell.getY() == endingPositionY) {
                System.out.println(stringBuilder.toString());
            	return reconstructPathToGoal(proxyRobot, currentCell, previousPosition);
            }

            // Remove the currentCell from the openSet
            openSet.remove(currentCell);
            
            // set the currentCell as part of the fastest Path by setting the closedSet to true
            isSetClosed[currentCell.getX()][currentCell.getY()] = true;
            grid.getCell()[currentCell.getX()][currentCell.getY()].setIsClosedSet(true);
            stringBuilder.append("x:" + currentCell.getX() + ", y:" + currentCell.getY() + " is added to closedSet\n");

            // Update the the fValues of all the neighbors of the CurrentCell
            for (Cell neighbor : generateNeighborCell(grid, currentCell, cells)) {
            	
            	// if the neighbour is already inside the closedSet. Ignore it.
                if (isSetClosed[neighbor.getX()][neighbor.getY()]){
                    continue;
                }

                // if neighbor is not inside the openSet. Add it into the openSet
                if (!openSet.contains(neighbor)){
                    openSet.add(neighbor);
                }

                // Since it is neighbor of the CurrentCell, the neighbor GValue is just +1 of the CurrentCell GValue 
                int tentativeGScore = gValue[currentCell.getX()][currentCell.getY()] + 1;
                
                // Get the previous Cell that currentCell came from
                Cell previousCell = previousPosition.get(currentCell);
                
                if (previousCell != null && previousCell.getX() != neighbor.getX() && previousCell.getY() != neighbor.getY()){
                    tentativeGScore += 1;
                }
                
                if (tentativeGScore >= gValue[neighbor.getX()][neighbor.getY()]){
                    continue;
                }

                // Update the neighbor GValue, FValue and at it to the previousPosition HashMap
                gValue[neighbor.getX()][neighbor.getY()] = tentativeGScore;
                fValue[neighbor.getX()][neighbor.getY()] = tentativeGScore + estimateHowFarToGoal(neighbor.getX(), neighbor.getY(), endingPositionX, endingPositionY);
                previousPosition.put(neighbor, currentCell);
            }
        }
        
		System.out.println("Fastest Path not found");
		return null;
	}


	static List<Cell> generateNeighborCell(Grid grid, Cell current, Cell[][] cells) {
		
		List<Cell> neighbors = new ArrayList<>();
        boolean north = true, south = true , west = true, east = true;
        
        int realX = current.getX() + 1, realY = current.getY() + 1;
        // check north
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(realX + i, realY - 2) ||
                grid.getIsObstacle(realX + i, realY - 2) ||
                !grid.getIsExplored(realX + i, realY - 2))
            	
            	north = false;
        }
        if (north)
            neighbors.add(cells[current.getX()][current.getY() - 1]);

        // check south
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(realX + i, realY + 2) ||
                grid.getIsObstacle(realX + i, realY + 2) ||
                !grid.getIsExplored(realX + i, realY + 2))
            	
            	south = false;
        }
        if (south)
            neighbors.add(cells[current.getX()][current.getY() + 1]);
        
        // check east
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(realX + 2, realY + i) ||
                grid.getIsObstacle(realX + 2, realY + i) ||
                !grid.getIsExplored(realX + 2, realY + i))
            	
            	east = false;
        }
        if (east)
            neighbors.add(cells[current.getX() + 1][current.getY()]);

        // check west
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(realX - 2, realY + i) ||
                grid.getIsObstacle(realX - 2, realY + i) ||
                !grid.getIsExplored(realX - 2, realY + i))
            	
            	west = false;
        }
        if (west)
            neighbors.add(cells[current.getX() - 1][current.getY()]);

        return neighbors;
	}


	static Cell getCurrentCell(List<Cell> openSet, int[][] fValue) {
		int minH = INFINITY;
		Cell mCell = null;     
		
        for (Cell cell : openSet) {
            if (fValue[cell.getX()][cell.getY()] < minH) {
            	minH = fValue[cell.getX()][cell.getY()];
                mCell = cell;
            }
        }
		return mCell;
	}


	static List<String> reconstructPathToGoal(Robot proxyRobot, Cell currentCell, HashMap<Cell, Cell> previousPosition) {

        List<Cell> path = new LinkedList<>();
        path.add(currentCell);
        
        while (previousPosition.get(currentCell) != null) {
        	currentCell = previousPosition.get(currentCell);
            path.add(0, currentCell);
        }
        
     // remove the starting cell
        path.remove(0); 
        
        // convert path to robot movement
        List<String> actions = new ArrayList<>();
        int calibrationCount = 0;
        for (Cell cell : path) {

            //calibrationCount++;
            //if (robot.ableToCalibrateFront()) {
            //    actions.add("C");
            //    calibrationCount = 0;
            //} else if (calibrationCount >= 5 && robot.ableToCalibrateLeft()) {
            //    actions.add("L");
            //    actions.add("C");
            //    actions.add("R");
            //    calibrationCount = 0;
            //}

            // see if we need to turn
            int nextDirection = 0;
            
            if (proxyRobot.getCenterPositionY() > cell.getY() + 1)
            	nextDirection = NORTH;
            else if (proxyRobot.getCenterPositionY() < cell.getY() + 1)
            	nextDirection = SOUTH;
            else if (proxyRobot.getCenterPositionX() < cell.getX() + 1)
            	nextDirection = EAST;
            else if (proxyRobot.getCenterPositionX() > cell.getX() + 1)
            	nextDirection = WEST;
            

            if (proxyRobot.getDirection() != nextDirection){
            	
            	if ((proxyRobot.getDirection() + 3) % 4 == nextDirection){
                     actions.add("L");
                     proxyRobot.turn(LEFT);
                 } 
            	else if ((proxyRobot.getDirection() + 1) % 4 == nextDirection){
                    actions.add("R");
                    proxyRobot.turn(RIGHT);
                }            
                else{
                    actions.add("U");
                    proxyRobot.turn(LEFT);
                    proxyRobot.turn(LEFT);
                }
            }
            actions.add("M");
            proxyRobot.move();
        }

        return actions;
	}


	static int estimateHowFarToGoal(int currentPositionX, int currentPositionY, int endingPositionX,
			int endingPositionY) {	
		
		int distance = Math.abs(endingPositionX - currentPositionX) + Math.abs(endingPositionY - currentPositionY);
        if (currentPositionX != endingPositionX && currentPositionY != endingPositionY){ 
            	distance += 1;
            }
		
		return distance;
	}


	static String compressExplorationPath(List<String> returnPath, Robot proxyRobot) {
		
        List<String> actionsIncludeCalibration = new ArrayList<>();
        
        for (String action : returnPath) {
        	actionsIncludeCalibration.add(action); // copy action to a new list
        	
            // execute action on proxy robot
        	
        	if (action.equals("M")) {
            	proxyRobot.move();
            }
        	else if (action.equals("L")) {
            	proxyRobot.turn(LEFT);
            } 
            else if (action.equals("R")) {
            	proxyRobot.turn(RIGHT);
            } 
            else if (action.equals("U")) {
            	proxyRobot.turn(RIGHT);
            	proxyRobot.turn(RIGHT);
            } 
            
            // check calibration
            if (proxyRobot.ableToCalibrateFront()) {
            	actionsIncludeCalibration.add("C");
            }
        }

        return compressPath(actionsIncludeCalibration);
	}


	/*static String compressPath(List<String> actionWithCalibration) {
        int moveCount = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("X");
        
        for (String action : actionWithCalibration){
            if ( action.equals("R") || action.equals("L") || action.equals("U") || action.equals("C")){
                if (moveCount != 0) {
                	
                	while(moveCount > 9){
                		
                		stringBuilder.append("M");
                    	stringBuilder.append(9);
                    	moveCount -= 9;
                	}
                	
                	stringBuilder.append("M");
                	stringBuilder.append(moveCount);
                    moveCount = 0;
                }
                stringBuilder.append(action);
            }
            
            else if (action.equals("M")){
            	moveCount++;
            }
        }
        if (moveCount != 0){
        	
        	while(moveCount > 9){
        		
        		stringBuilder.append("M");
            	stringBuilder.append(9);
            	moveCount -= 9;
        	}
        	stringBuilder.append("M");
        	stringBuilder.append(moveCount);
        }

        return stringBuilder.toString();
	}*/
	
	static String compressPath(List<String> actionWithCalibration) {
        int moveCount = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("X");
        
        for (String action : actionWithCalibration){
            if ( action.equals("R") || action.equals("L") || action.equals("U") || action.equals("C")){
                if (moveCount != 0) {
                	               	
                	if(moveCount > 9){
                		
                		int firstDigit = moveCount/10;
                		int remainder = moveCount - (firstDigit * 10);
                		
                		stringBuilder.append("M");
                    	stringBuilder.append(firstDigit);
                    	stringBuilder.append(remainder);
                    	moveCount = 0;
                	}
                	
                	else{
                	
		            	stringBuilder.append("M");
		            	stringBuilder.append(0);
		            	stringBuilder.append(moveCount);
		                moveCount = 0;
                	}
                	
                }
                
                
                stringBuilder.append(action);
            }
            
            else if (action.equals("M")){
            	moveCount++;
            }
        }
        
        
        if (moveCount != 0){
        	
        	if(moveCount > 9){
        		
        		int firstDigit = moveCount/10;
        		int remainder = moveCount - (firstDigit * 10);
        		
        		stringBuilder.append("M");
            	stringBuilder.append(firstDigit);
            	stringBuilder.append(remainder);
            	moveCount = 0;
        	}
        	
        	else{
        	
            	stringBuilder.append("M");
            	stringBuilder.append(0);
            	stringBuilder.append(moveCount);
                moveCount = 0;
        	}
        	
        	
        }

        return stringBuilder.toString();
	}
	
	static String explorationCompressPath(List<String> actionWithCalibration) {
        int moveCount = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Z");
        
        for (String action : actionWithCalibration){
            if ( action.equals("R") || action.equals("L") || action.equals("U") || action.equals("C")){
                if (moveCount != 0) {
                	               	
                	if(moveCount > 9){
                		
                		int firstDigit = moveCount/10;
                		int remainder = moveCount - (firstDigit * 10);
                		
                		stringBuilder.append("M");
                    	stringBuilder.append(firstDigit);
                    	stringBuilder.append(remainder);
                    	moveCount = 0;
                	}
                	
                	else{
                	
		            	stringBuilder.append("M");
		            	stringBuilder.append(0);
		            	stringBuilder.append(moveCount);
		                moveCount = 0;
                	}
                	
                }
                
                
                stringBuilder.append(action);
            }
            
            else if (action.equals("M")){
            	moveCount++;
            }
        }
        
        
        if (moveCount != 0){
        	
        	if(moveCount > 9){
        		
        		int firstDigit = moveCount/10;
        		int remainder = moveCount - (firstDigit * 10);
        		
        		stringBuilder.append("M");
            	stringBuilder.append(firstDigit);
            	stringBuilder.append(remainder);
            	moveCount = 0;
        	}
        	
        	else{
        	
            	stringBuilder.append("M");
            	stringBuilder.append(0);
            	stringBuilder.append(moveCount);
                moveCount = 0;
        	}
        	
        	
        }

        return stringBuilder.toString();
	}
	
	static String compressExplorationCalibrationPath(List<String> returnPath, Robot proxyRobot) {
		
        List<String> actionsIncludeCalibration = new ArrayList<>();
        
        for (String action : returnPath) {
        	actionsIncludeCalibration.add(action); // copy action to a new list
        	
            // execute action on proxy robot
        	
        	if (action.equals("M")) {
            	proxyRobot.move();
            }
        	else if (action.equals("L")) {
            	proxyRobot.turn(LEFT);
            } 
            else if (action.equals("R")) {
            	proxyRobot.turn(RIGHT);
            } 
            else if (action.equals("U")) {
            	proxyRobot.turn(RIGHT);
            	proxyRobot.turn(RIGHT);
            } 
            
            // check calibration
            if (proxyRobot.ableToCalibrateFront()) {
            	actionsIncludeCalibration.add("C");
            }
        }

        return explorationCompressPath(actionsIncludeCalibration);
	}
}
