package model.algo;


import model.algo.Algorithm;
import model.physical.Grid;
import model.physical.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;


import static constant.CommunicationConstant.*;
import static constant.MapConstant.*;

import static constant.RobotConstant.*;


import java.util.ArrayList;
import java.util.List;

import constant.RobotConstant;

public class ExplorationAlgorithm implements Algorithm {

	private boolean uTurnHalt = false;
	private String robotMovementString = "";
	private int robotSpeed;
	private static final int START_X_POSITION = 0;
    private static final int START_Y_POSITION = 17;
	private static final int CALIBRATION_LIMIT = 3;
	private List<Integer> robotStartPosAndWaypoints;
	
	public ExplorationAlgorithm(int speed){
		robotSpeed = 1000/speed;
	}
	
	
	public ExplorationAlgorithm(int speed, List<Integer> robotStartPosAndWaypoints){
    	this.robotStartPosAndWaypoints = robotStartPosAndWaypoints;
        robotSpeed = 1000 / speed;
    }
	
	@Override
	public void start(boolean realRun, Robot robot, Grid grid){
		grid.reset();
		robot.resetRobot();
        if (realRun) {
        	robot.setPositionX(robotStartPosAndWaypoints.get(0));
            robot.setPositionY(robotStartPosAndWaypoints.get(1));
            switch(robotStartPosAndWaypoints.get(2)) {
            	case 0: robot.setDirection(RobotConstant.NORTH);
            		break;
            	case 90: robot.setDirection(RobotConstant.EAST);
            		break;
            	case 180: robot.setDirection(RobotConstant.SOUTH);
        			break;
            	case 270: robot.setDirection(RobotConstant.WEST);
    				break;
            }
            grid.clearAllObstacle();
            SocketMgr.getInstance().sendMessage(CALL_ANDROID, "exr");
            String msg = SocketMgr.getInstance().receiveMessage(false);
            while (!msg.equals("exs")) {
                msg = SocketMgr.getInstance().receiveMessage(false);
            }
        }
        
        //System.out.println("EXPLORATION ENDED!");
        
        // SELECT EITHER ONE OF THE METHODS TO RUN ALGORITHMS.
        runExplorationAlgorithm(realRun, robot, grid);

        // CALIBRATION AFTER EXPLORATION
        calibrateAndTurn(realRun, robot);

        // GENERATE MAP DESCRIPTOR, SEND TO ANDROID
        String part1 = grid.generateMapDescriptor1();
        String part2 = grid.generateMapDescriptor2();
        //SocketMgr.getInstance().sendMessage(CALL_ANDROID, MessageMgr.generateFinalDescriptor(part1, part2));
		
	}

	private void calibrateAndTurn(boolean realRun, Robot robot) {
		if (realRun) {
	            while (robot.getDirection() != SOUTH) {
	                robot.turn(LEFT);
	                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	                robot.sense(realRun);
	            }
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	            flushCalibration();
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	            robot.sense(realRun);
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	            flushCalibration();
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	            robot.sense(realRun);
	        }
		
	}

	private void runExplorationAlgorithm(boolean realRun, Robot robot, Grid grid) {
		// flag to keep track if robot has entered startZone
		boolean inStartZone = false;
		// flag to keep track if robot has entered endZone
		boolean inEndZone = false;
		
		// calibrationCount is used to keep track of how many steps the robot has not calibrated so far
		int calibrationCount = 0;
		
		// isCalibrated is used to check if a calibration occurs
		boolean isCalibrated = false;
		
		// Sense the surrounding at the starting point
		robot.sense(realRun);
		
		// This is the exploration algorithm, as long as it is not in startZone or endZone
		// it will keep looping here
		while(!inStartZone || !inEndZone) {
			// Checks if a turn is needed
			boolean turn = leftHugging(realRun, robot, grid);
			
			// A turn has been made 
			if (turn) {
				// CALIBRATION
				if (realRun) {
					isCalibrated = false;
					calibrationCount++;
					// Sense the surrounding since a turn has been made
		            if(uTurnHalt) {
		            	uTurnHalt = false;
		            } else {
		            	robot.sense(realRun);
		            }
					
		            
		            // special case for U turn; if after U-Turn and the left has no obstacles
		            // Turn left, this needs to be added in because the right sensor is not accurate
		            /*if(robotMovementString.substring(robotMovementString.length()-1).equalsIgnoreCase("U") && !robot.isObstacleOnLeftSide()) {
		            	// Turn Left
		            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
		            	robot.turn(LEFT);
		                stepTaken();
		                // Sense the surrounding after the left turn
		                robot.sense(realRun);
		                // Update Android
		                sendAndroid(grid, robot, realRun);
		            }*/
		            
		            // checks if robot is able to calibrate
	                if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
	                    isCalibrated = true;
	                    calibrationCount = 0;
	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
	                    isCalibrated = true;
	                    calibrationCount = 0;
	                } else if(calibrationCount >= CALIBRATION_LIMIT && robot.isObstacleOnRightSideForCalibration()) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
	                    isCalibrated = true;
	                    calibrationCount = 0;
	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateFront()) {
	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	                    isCalibrated = true;
	                    calibrationCount = 0;
	                }  
	                
	                if(isCalibrated) {
	                	// Arduino will send an "OK" message after calibration
	                	// flush the message
						flushCalibration();
			        }
				} else {
					robot.sense(realRun);
				}
			}
			
			// Move a step forward
			if (realRun) {
				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
				robot.setDetectNextMove(false);
			}
			
			robotMovementString+="M";
			System.out.println("----------------------Moving Forward----------------------");
			System.out.println(robotMovementString);
			robot.move();
			stepTaken();
			// Update Android that there is a move forward
			sendAndroid(grid, robot, realRun);
			
            if (realRun) {
            	isCalibrated = false;
                calibrationCount++;
                // sense the surrounding after making the forward move
	            robot.sense(realRun);
	            
	            // checks if robot is able to calibrate
                if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                    isCalibrated = true;
                    calibrationCount = 0;
                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                    isCalibrated = true;
                    calibrationCount = 0;
                } else if(calibrationCount >= CALIBRATION_LIMIT && robot.isObstacleOnRightSideForCalibration()) {
                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                    isCalibrated = true;
                    calibrationCount = 0;
                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateFront()) {
                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                    isCalibrated = true;
                    calibrationCount = 0;
                }
            	
            	if(isCalibrated) {
            		// Arduino will send an "OK" message after calibration
                	// flush the message
                	flushCalibration();	
                }
            } else {
            	robot.sense(realRun);
            }
            
            // checks if robot enters the endZone
            if(Grid.isInEndingZone(robot.getPositionX(), robot.getPositionY())) {
            	// sets the endZone flag to true if robot enters the endZone
            	inEndZone = true;
            }
            
            // checks if robot enters the startZone
            if(Grid.isInStartingZone(robot.getPositionX() +2, robot.getPositionY()) && inEndZone){
            	// sets the endZone flag to true if robot enters the endZone
            	inStartZone = true;
            }

            // if the map has been fully explored and robot is not in the startZone,
            // run the fastest path Algorithm from the current robot position to the startZone
            if(!inStartZone && grid.checkPercentageExplored() == 100) {
            	// Create a proxyRobot for the calculation so that it will not affect the original robot
                Robot proxyRobot = new Robot(grid, new ArrayList<>());
                
                // Initialize the proxyRobot with the current conditions of the original robot
                proxyRobot.setDirection(robot.getDirection());
                proxyRobot.setPositionX(robot.getPositionX());
                proxyRobot.setPositionY(robot.getPositionY());
                
                // Run the A* Algorithm to get the list of actions needed to head back to the startZone
                List<String> returnActions = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), START_X_POSITION, START_Y_POSITION, grid, proxyRobot);                
                
                // If the fastest path can be calculated, process it
                if (returnActions != null) {
                	// Initialize the proxyRobot back to the original robot current conditions
                    proxyRobot.setPositionX(robot.getPositionX());
                    proxyRobot.setPositionY(robot.getPositionY());
                    proxyRobot.setDirection(robot.getDirection());
                    
                    if (realRun) {
                    	// Get the actions string to send to Arduino
                        String compressed = Algorithm.compressExplorationPath(returnActions, proxyRobot);
                        // Send the actions string to Arduino
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressed);
                    }
                    
                    System.out.println("Fastest Path Algorithm back to Start Zone Calculated. Executing actions now...");
                    System.out.println(returnActions.toString());
                    
                    // Simulate the robot moving back to startZone step by step
                    for (String action : returnActions) {
                        if (action.equals("M")) {
                            robot.move();
                        } else if (action.equals("L")) {
                            robot.turn(LEFT);
                        } else if (action.equals("R")) {
                            robot.turn(RIGHT);
                        } else if (action.equals("U")) {
                            robot.turn(LEFT);
                            robot.turn(LEFT);
                        }                        
                        stepTaken();
                        // Update Android that there is a move or turn
                        sendAndroid(grid, robot, realRun);
                    }
                }
                else {
                    System.out.println("FASTEST PATH CANNOT BE FOUND!");
                }

                // Checks if robot is in startZone,
                // Set the startZone flag to true only if robot has entered the endZone before
                if (Grid.isInStartingZone(robot.getPositionX() + 2, robot.getPositionY()) && inEndZone) {
                    inStartZone = true;
                }
            } else if(checkForCycle()) {
            	// only make the robot move forward when there is no obstacles in front
            	/*if(!robot.isObstacleInfront()) {
            		// Move a step forward
        			if (realRun) {
        				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
        				robot.setDetectNextMove(false);
        			}
        			// add small "m" so that i can see where a cycle has been detected
        			robotMovementString+="m";
                    robot.move();
                    stepTaken();
                    // Update Android that there is a move forward
                    sendAndroid(grid, robot, realRun);
                    
                    if (realRun) {
                    	isCalibrated = false;
                        calibrationCount++;
                        // sense the surrounding after making the forward move
        	            robot.sense(realRun);
        	            
        	            // Checks if robot is able to Calibrate
                    	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                            isCalibrated = true;
                            calibrationCount = 0;
                        } else if (robot.ableToCalibrateFront()) {
                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                            isCalibrated = true;
                            calibrationCount = 0;
                        } else if(robot.isObstacleOnRightSideForCalibration()) {
    	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
    	                    isCalibrated = true;
    	                    calibrationCount = 0;
    	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                            isCalibrated = true;
                            calibrationCount = 0;
                        }
                    	
                    	if(isCalibrated) {
                    		// Arduino will send an "OK" message after calibration
                        	// flush the message
                        	flushCalibration();	
                        }
                    } else {
                    	robot.sense(realRun);
                    }
                    
                    if(!robot.isObstacleInfront()) {
                		// Move a step forward
            			if (realRun) {
            				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
            				robot.setDetectNextMove(false);
            			}
            			// add small "m" so that i can see where a cycle has been detected
            			robotMovementString+="m";
                        robot.move();
                        stepTaken();
                        // Update Android that there is a move forward
                        sendAndroid(grid, robot, realRun);
                        
                        if (realRun) {
                        	isCalibrated = false;
                            calibrationCount++;
                            // sense the surrounding after making the forward move
            	            robot.sense(realRun);
            	            
            	            // Checks if robot is able to Calibrate
                        	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                isCalibrated = true;
                                calibrationCount = 0;
                            } else if (robot.ableToCalibrateFront()) {
                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                isCalibrated = true;
                                calibrationCount = 0;
                            } else if(robot.isObstacleOnRightSideForCalibration()) {
        	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
        	                    isCalibrated = true;
        	                    calibrationCount = 0;
        	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                isCalibrated = true;
                                calibrationCount = 0;
                            }
                        	
                        	if(isCalibrated) {
                        		// Arduino will send an "OK" message after calibration
                            	// flush the message
                            	flushCalibration();	
                            }
                        } else {
                        	robot.sense(realRun);
                        }
                	}
                    
                    if(!robot.isObstacleInfront()) {
                		// Move a step forward
            			if (realRun) {
            				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
            				robot.setDetectNextMove(false);
            			}
            			// add small "m" so that i can see where a cycle has been detected
            			robotMovementString+="m";
                        robot.move();
                        stepTaken();
                        // Update Android that there is a move forward
                        sendAndroid(grid, robot, realRun);
                        
                        if (realRun) {
                        	isCalibrated = false;
                            calibrationCount++;
                            // sense the surrounding after making the forward move
            	            robot.sense(realRun);
            	            
            	            // Checks if robot is able to Calibrate
                        	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                isCalibrated = true;
                                calibrationCount = 0;
                            } else if (robot.ableToCalibrateFront()) {
                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                isCalibrated = true;
                                calibrationCount = 0;
                            } else if(robot.isObstacleOnRightSideForCalibration()) {
        	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
        	                    isCalibrated = true;
        	                    calibrationCount = 0;
        	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                isCalibrated = true;
                                calibrationCount = 0;
                            }
                        	
                        	if(isCalibrated) {
                        		// Arduino will send an "OK" message after calibration
                            	// flush the message
                            	flushCalibration();	
                            }
                        } else {
                        	robot.sense(realRun);
                        }
                	}
            	}*/
            	
            	// Using Nearest Neighbour to break the cycle
            	// Find out which zone the robot is in
            	int zoneNumber = getExploringZone(robot.getPositionX(), robot.getPositionY());
            	
            	boolean resultFound = false;
            	// Loop through the 4 zones to look for unexplored cell starting from the zone the robot is in
            	for(int i = zoneNumber; i <= ((zoneNumber+3)%4); i++) {
            		resultFound = checkZoneForUnexplored(i, realRun, grid, robot);
            		if(resultFound) {
            			SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
            			robot.turn(RIGHT);
            			robot.sense(realRun);
            			break;
            		}
            	}
            	
            	if(!resultFound) {
            		System.out.println("Nearest Neighbour Algorithm fails to find nearest cell.\nExecuting First Reachable Unexplored Algorithm");
            		resultFound = startFirstReachableUnexploredAlgo(realRun, grid, robot);
            		
            		if(!resultFound)
            			System.out.println("First Reachable Unexplored Algoritm fails to find a reachable unexplored path.");
            	}
            		
            }
		}
	        
        // Start Second exploration if robot is back to starting zone but the exploration is not 100%
        if(grid.checkPercentageExplored() < 100.0) {
        	// Duplicate the current grid
            Grid firstRunExplored = new Grid();
            for (int x = 0; x < MAP_COLUMNS; x++) {
                for (int y = 0; y < MAP_ROWS; y++) {    	
                	firstRunExplored.setIsObstacle(x, y, grid.getIsObstacle(x, y));
                	firstRunExplored.setIsExplored(x, y, grid.getIsExplored(x, y));
                }
            }
            
            System.out.println("NOT FULLY EXPLORED, RUNNING SECOND EXPLORATION!");
            
            //checking for reachable cells in the arena.
            for (int y = MAP_ROWS - 1; y >= 0; y--) {
                for (int x = MAP_COLUMNS - 1; x >= 0; x--) {
                	//check for unexplored cells and if neighbors are reachable.
                    if (!grid.getIsExplored(x, y) &&
				            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
				                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
				                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
				                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
                    	
                        boolean isInStartPoint = true;
                        while (isInStartPoint) { 
                        	// set isInStartPoint to false to state that the robot is not at starting point.
                        	isInStartPoint = false; 
                        	
                        	if(checkForCycle()) {
                            	// only make the robot move forward when there is no obstacles in front
                            	/*if(!robot.isObstacleInfront()) {
                            		// Move a step forward
                        			if (realRun) {
                        				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                        				robot.setDetectNextMove(false);
                        			}
                        			// add small "m" so that i can see where a cycle has been detected
                        			robotMovementString+="m";
                                    robot.move();
                                    stepTaken();
                                    // Update Android that there is a move forward
                                    sendAndroid(grid, robot, realRun);
                                    
                                    if (realRun) {
                                    	isCalibrated = false;
                                        calibrationCount++;
                                        // sense the surrounding after making the forward move
                        	            robot.sense(realRun);
                        	            
                        	            // Checks if robot is able to Calibrate
                                    	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                            isCalibrated = true;
                                            calibrationCount = 0;
                                        } else if (robot.ableToCalibrateFront()) {
                                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                            isCalibrated = true;
                                            calibrationCount = 0;
                                        } else if(robot.isObstacleOnRightSideForCalibration()) {
                    	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                    	                    isCalibrated = true;
                    	                    calibrationCount = 0;
                    	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                            isCalibrated = true;
                                            calibrationCount = 0;
                                        }
                                    	
                                    	if(isCalibrated) {
                                    		// Arduino will send an "OK" message after calibration
                                        	// flush the message
                                        	flushCalibration();	
                                        }
                                    } else {
                                    	robot.sense(realRun);
                                    }
                                    
                                    if(!robot.isObstacleInfront()) {
                                		// Move a step forward
                            			if (realRun) {
                            				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                            				robot.setDetectNextMove(false);
                            			}
                            			// add small "m" so that i can see where a cycle has been detected
                            			robotMovementString+="m";
                                        robot.move();
                                        stepTaken();
                                        // Update Android that there is a move forward
                                        sendAndroid(grid, robot, realRun);
                                        
                                        if (realRun) {
                                        	isCalibrated = false;
                                            calibrationCount++;
                                            // sense the surrounding after making the forward move
                            	            robot.sense(realRun);
                            	            
                            	            // Checks if robot is able to Calibrate
                                        	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            } else if (robot.ableToCalibrateFront()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            } else if(robot.isObstacleOnRightSideForCalibration()) {
                        	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                        	                    isCalibrated = true;
                        	                    calibrationCount = 0;
                        	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            }
                                        	
                                        	if(isCalibrated) {
                                        		// Arduino will send an "OK" message after calibration
                                            	// flush the message
                                            	flushCalibration();	
                                            }
                                        } else {
                                        	robot.sense(realRun);
                                        }
                                	}
                                    
                                    if(!robot.isObstacleInfront()) {
                                		// Move a step forward
                            			if (realRun) {
                            				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                            				robot.setDetectNextMove(false);
                            			}
                            			// add small "m" so that i can see where a cycle has been detected
                            			robotMovementString+="m";
                                        robot.move();
                                        stepTaken();
                                        // Update Android that there is a move forward
                                        sendAndroid(grid, robot, realRun);
                                        
                                        if (realRun) {
                                        	isCalibrated = false;
                                            calibrationCount++;
                                            // sense the surrounding after making the forward move
                            	            robot.sense(realRun);
                            	            
                            	            // Checks if robot is able to Calibrate
                                        	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            } else if (robot.ableToCalibrateFront()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            } else if(robot.isObstacleOnRightSideForCalibration()) {
                        	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                        	                    isCalibrated = true;
                        	                    calibrationCount = 0;
                        	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            }
                                        	
                                        	if(isCalibrated) {
                                        		// Arduino will send an "OK" message after calibration
                                            	// flush the message
                                            	flushCalibration();	
                                            }
                                        } else {
                                        	robot.sense(realRun);
                                        }
                                	}
                            	}*/
                            	
                            	// Using Nearest Neighbour to break the cycle
                            	// Find out which zone the robot is in
                            	int zoneNumber = getExploringZone(robot.getPositionX(), robot.getPositionY());
                            	boolean resultFound = false;
                            	// Loop through the 4 zones to look for unexplored cell starting from the zone the robot is in
                            	for(int i = zoneNumber; i <= ((zoneNumber+3)%4); i++) {
                            		resultFound = checkZoneForUnexplored(i, realRun, grid, robot);
                            		if(resultFound) {
                            			SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                            			robot.turn(RIGHT);
                            			robot.sense(realRun);
                            			break;
                            		}
                            	}
                            	
                            	if(!resultFound) {
                            		System.out.println("Nearest Neighbour Algorithm fails to find nearest cell.\nExecuting First Reachable Unexplored Algorithm");
                            		resultFound = startFirstReachableUnexploredAlgo(realRun, grid, robot);
                            		
                            		if(!resultFound)
                            			System.out.println("First Reachable Unexplored Algoritm fails to find a reachable unexplored path.");
                            	}
                            }
                        	
                        	// Checks if a turn is needed
                            boolean turned = leftHugging(realRun, robot, grid);
                            
                            // A turn has been made
                            if (turned) {
                                // CALIBRATION
                                if (realRun) {
                                	isCalibrated = false;
                                	calibrationCount++;
                                    
                                	if(uTurnHalt) {
                		            	uTurnHalt = false;
                		            } else {
                		            	// Sense the surrounding since a turn has been made
                		            	robot.sense(realRun);
                		            }
                    		        
                    		        // special case for U turn; if after U-Turn and the left has no obstacles
                		            // Turn left, this needs to be added in because the right sensor is not accurate
                		            /*if(robotMovementString.substring(robotMovementString.length()-1).equalsIgnoreCase("U") && !robot.isObstacleOnLeftSide()) {
                		            	// Turn Left
                		            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                		            	robot.turn(LEFT);
                		                stepTaken();
                		                // Sense the surrounding after the left turn
                		                robot.sense(realRun);
                		                // Update Android
                		                sendAndroid(grid, robot, realRun);
                		            }*/
                    		            
                                	// checks if robot is able to calibrate
                	                if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                } else if(calibrationCount >= CALIBRATION_LIMIT && robot.isObstacleOnRightSideForCalibration()) {
                	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateFront()) {
                	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                }
                                    	
                                	if(isCalibrated) {
                                		// Arduino will send an "OK" message after calibration
                                		// flush the message
                                		flushCalibration();
                                	}
                                } else {
                                	robot.sense(realRun);
                                }
                            }
                            
                            // Move a step forward
                            if (realRun) {
                            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                            	robot.setDetectNextMove(false);
                            }
                            
                            robotMovementString+="M";
                            System.out.println("----------------------Moving Forward----------------------");
            				System.out.println(robotMovementString);
                            robot.move();
                            stepTaken();
                                
                            // Update Android that there is a move forward
                            sendAndroid(grid, robot, realRun);
                            
                            
                            // CALIBRATION
                            if (realRun) {
                            	isCalibrated = false;
                            	calibrationCount++;
                            	// sense the surrounding after making the forward move
                            	robot.sense(realRun);
                            	
                            	// checks if robot is able to calibrate
            	                if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
            	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
            	                    isCalibrated = true;
            	                    calibrationCount = 0;
            	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
            	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
            	                    isCalibrated = true;
            	                    calibrationCount = 0;
            	                } else if(calibrationCount >= CALIBRATION_LIMIT && robot.isObstacleOnRightSideForCalibration()) {
            	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
            	                    isCalibrated = true;
            	                    calibrationCount = 0;
            	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateFront()) {
            	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
            	                    isCalibrated = true;
            	                    calibrationCount = 0;
            	                }
                            	
                            	if(isCalibrated) {
                            		// Arduino will send an "OK" message after calibration
                                	// flush the message
                                	flushCalibration();
                                }
                            } else {
                            	robot.sense(realRun);
                            }
                            
                            // breaks out of the while loop is map has been fully explored
                            if (grid.checkPercentageExplored() == 100) { 
                                break;
                            }

                            
                            while (grid.getIsExplored(robot.getPositionX(), robot.getPositionY()) != firstRunExplored.getIsExplored(robot.getPositionX(), robot.getPositionY())) {
                                
                            	// breaks out of the while loop is map has been fully explored
                            	if (grid.checkPercentageExplored() == 100) { 
                                    break;
                                }
                                
                            	// checks if a turn is needed
                                turned = leftHugging(realRun, robot, grid);
                                
                                // A turn has been made
                                if (turned) {
                                	// CALIBRATION
                                	if (realRun) {
                                		isCalibrated = false;
                                		calibrationCount++;
                                		
                                		if(uTurnHalt) {
                    		            	uTurnHalt = false;
                    		            } else {
                    		            	// Sense the surrounding since a turn has been made
                    		            	robot.sense(realRun);
                    		            }
                                		
                                		// special case for U turn; if after U-Turn and the left has no obstacles
                    		            // Turn left, this needs to be added in because the right sensor is not accurate
                    		            /*if(robotMovementString.substring(robotMovementString.length()-1).equalsIgnoreCase("U") && !robot.isObstacleOnLeftSide()) {
                    		            	// Turn Left
                    		            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                    		            	robot.turn(LEFT);
                    		                stepTaken();
                    		                // Sense the surrounding after the left turn
                    		                robot.sense(realRun);
                    		                // Update Android
                    		                sendAndroid(grid, robot, realRun);
                    		            }*/
                                		
                                		// checks if robot is able to calibrate
                    	                if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                    	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                    	                    isCalibrated = true;
                    	                    calibrationCount = 0;
                    	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                    	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                    	                    isCalibrated = true;
                    	                    calibrationCount = 0;
                    	                } else if(calibrationCount >= CALIBRATION_LIMIT && robot.isObstacleOnRightSideForCalibration()) {
                    	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                    	                    isCalibrated = true;
                    	                    calibrationCount = 0;
                    	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateFront()) {
                    	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                    	                    isCalibrated = true;
                    	                    calibrationCount = 0;
                    	                }
                                		
                                		if(isCalibrated) {
                                			// Arduino will send an "OK" message after calibration
                    	                	// flush the message
                                            flushCalibration();
                                        }
                                	} else {
                                		robot.sense(realRun);
                                	}
                                }

                                // Move a step forward
                                if (realRun) {
                                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                                	robot.setDetectNextMove(false);
                                }
                                
                                robotMovementString+="M";
                                System.out.println("----------------------Moving Forward----------------------");
                				System.out.println(robotMovementString);
                                robot.move();
                                stepTaken();

                                // Update Android that there is a move forward
                                sendAndroid(grid, robot, realRun);
                                
                                // CALIBRATION
                                if (realRun) {
                                	isCalibrated = false;
                                	calibrationCount++;
                                	robot.sense(realRun);
                                	
                                	// checks if robot is able to calibrate
                	                if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                } else if(calibrationCount >= CALIBRATION_LIMIT && robot.isObstacleOnRightSideForCalibration()) {
                	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateFront()) {
                	                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                	                    isCalibrated = true;
                	                    calibrationCount = 0;
                	                }
                                	
                                	if(isCalibrated) {
                                		// Arduino will send an "OK" message after calibration
                                    	// flush the message
                                        flushCalibration();
                                    }
                                } else {
                                	robot.sense(realRun);
                                }
                                
                                if(checkForCycle()) {
                                	/*if(!robot.isObstacleInfront()) {
                                		// Move a step forward if there is no obstacle in front
                            			if (realRun) {
                            				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                            				robot.setDetectNextMove(false);
                            			}
                            			// small "m" so that i can see when the cycle force move happens
                            			robotMovementString+="m";
                                        robot.move();
                                        stepTaken();
                                        // Update Android that there is a move forward
                                        sendAndroid(grid, robot, realRun);
                                        
                                        if (realRun) {
                                        	isCalibrated = false;
                                            calibrationCount++;
                                            // sense the surrounding after making the forward move
                            	            robot.sense(realRun);
                            	            
                            	            // Checks if robot is able to Calibrate
                                        	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            } else if (robot.ableToCalibrateFront()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            } else if(robot.isObstacleOnRightSideForCalibration()) {
                        	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                        	                    isCalibrated = true;
                        	                    calibrationCount = 0;
                        	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                                isCalibrated = true;
                                                calibrationCount = 0;
                                            }
                                        	
                                        	if(isCalibrated) {
                                        		// Arduino will send an "OK" message after calibration
                                            	// flush the message
                                            	flushCalibration();	
                                            }
                                        } else {
                                        	robot.sense(realRun);
                                        }
                                        
                                        if(!robot.isObstacleInfront()) {
                                    		// Move a step forward
                                			if (realRun) {
                                				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                                				robot.setDetectNextMove(false);
                                			}
                                			// add small "m" so that i can see where a cycle has been detected
                                			robotMovementString+="m";
                                            robot.move();
                                            stepTaken();
                                            // Update Android that there is a move forward
                                            sendAndroid(grid, robot, realRun);
                                            
                                            if (realRun) {
                                            	isCalibrated = false;
                                                calibrationCount++;
                                                // sense the surrounding after making the forward move
                                	            robot.sense(realRun);
                                	            
                                	            // Checks if robot is able to Calibrate
                                            	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                                    isCalibrated = true;
                                                    calibrationCount = 0;
                                                } else if (robot.ableToCalibrateFront()) {
                                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                                    isCalibrated = true;
                                                    calibrationCount = 0;
                                                } else if(robot.isObstacleOnRightSideForCalibration()) {
                            	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                            	                    isCalibrated = true;
                            	                    calibrationCount = 0;
                            	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                                    isCalibrated = true;
                                                    calibrationCount = 0;
                                                }
                                            	
                                            	if(isCalibrated) {
                                            		// Arduino will send an "OK" message after calibration
                                                	// flush the message
                                                	flushCalibration();	
                                                }
                                            } else {
                                            	robot.sense(realRun);
                                            }
                                    	}
                                        
                                        if(!robot.isObstacleInfront()) {
                                    		// Move a step forward
                                			if (realRun) {
                                				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                                				robot.setDetectNextMove(false);
                                			}
                                			// add small "m" so that i can see where a cycle has been detected
                                			robotMovementString+="m";
                                            robot.move();
                                            stepTaken();
                                            // Update Android that there is a move forward
                                            sendAndroid(grid, robot, realRun);
                                            
                                            if (realRun) {
                                            	isCalibrated = false;
                                                calibrationCount++;
                                                // sense the surrounding after making the forward move
                                	            robot.sense(realRun);
                                	            
                                	            // Checks if robot is able to Calibrate
                                            	if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "A");
                                                    isCalibrated = true;
                                                    calibrationCount = 0;
                                                } else if (robot.ableToCalibrateFront()) {
                                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                                    isCalibrated = true;
                                                    calibrationCount = 0;
                                                } else if(robot.isObstacleOnRightSideForCalibration()) {
                            	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "E");
                            	                    isCalibrated = true;
                            	                    calibrationCount = 0;
                            	                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "Q");
                                                    isCalibrated = true;
                                                    calibrationCount = 0;
                                                }
                                            	
                                            	if(isCalibrated) {
                                            		// Arduino will send an "OK" message after calibration
                                                	// flush the message
                                                	flushCalibration();	
                                                }
                                            } else {
                                            	robot.sense(realRun);
                                            }
                                    	}
                                	}*/
                                	
                                	// Using Nearest Neighbour to break the cycle
                                	// Find out which zone the robot is in
                                	int zoneNumber = getExploringZone(robot.getPositionX(), robot.getPositionY());
                                	boolean resultFound = false;
                                	// Loop through the 4 zones to look for unexplored cell starting from the zone the robot is in
                                	for(int i = zoneNumber; i <= ((zoneNumber+3)%4); i++) {
                                		resultFound = checkZoneForUnexplored(i, realRun, grid, robot);
                                		if(resultFound) {
                                			SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                                			robot.turn(RIGHT);
                                			robot.sense(realRun);
                                			break;
                                		}
                                	}
                                	
                                	if(!resultFound) {
                                		System.out.println("Nearest Neighbour Algorithm fails to find nearest cell.\nExecuting First Reachable Unexplored Algorithm");
                                		resultFound = startFirstReachableUnexploredAlgo(realRun, grid, robot);
                                		
                                		if(!resultFound)
                                			System.out.println("First Reachable Unexplored Algoritm fails to find a reachable unexplored path.");
                                	}
                                }
                            }
                        }
                    }
                }
            }
            
            // Run the Fastest Path Algorithm back to start zone after completing the exploration
            if(!Grid.isInStartingZone(robot.getPositionX()+2, robot.getPositionY()+2)){
            	// Create a proxyRobot for the calculation so that it will not affect the original robot
            	Robot proxyRobot = new Robot(grid, new ArrayList<>());
                
            	// Initialize the proxyRobot with the current conditions of the original robot
            	proxyRobot.setDirection(robot.getDirection());
                proxyRobot.setPositionX(robot.getPositionX());
                proxyRobot.setPositionY(robot.getPositionY());
             
                System.out.println("Running A* Algorithm to go back to the Start Zone...");
                List<String> returnActions = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), START_X_POSITION, START_Y_POSITION, grid, proxyRobot);

                // If the fastest path can be calculated, process it
                if (returnActions != null) {
                	System.out.println("Fastest Path Algorithm back to Start Zone Calculated. Executing actions now...");
                    System.out.println(returnActions.toString());
                    
                    if (realRun) {
                    	// Initialize the proxyRobot back to the original robot current conditions
                        proxyRobot.setPositionX(robot.getPositionX());
                        proxyRobot.setPositionY(robot.getPositionY());
                        proxyRobot.setDirection(robot.getDirection());
                        
                    	// Get the actions string to send to Arduino
                        String compressedPath = Algorithm.compressExplorationPath(returnActions, proxyRobot);
                        // Send the actions string to Arduino
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressedPath);
                    } else {
                    	// Simulate the robot moving back to startZone step by step
                        for (String action : returnActions) {
                            if (action.equals("M")) {
                            	robotMovementString += "m";
                                robot.move();
                            } else if (action.equals("L")) {
                            	robotMovementString += "l";
                                robot.turn(LEFT);
                            } else if (action.equals("R")) {
                            	robotMovementString += "r";
                                robot.turn(RIGHT);
                            } else if (action.equals("U")) {
                            	robotMovementString += "r";
                                robot.turn(RIGHT);
                                robotMovementString += "r";
                                robot.turn(RIGHT);
                            }
                            stepTaken();
                            // Update Android that there is a move or turn
                            sendAndroid(grid, robot, realRun);
                        }
                    }
                }
                else {
                    System.out.println("FASTEST PATH CANNOT BE FOUND!!");
                }
            }
        }
        
        if(realRun) {
        	// Inform Android Tablet to stop the Exploration Timer
        	SocketMgr.getInstance().sendMessage(CALL_ANDROID, "exploreComplete");
            
        	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
            // Update Android when the exploration is completed
            sendAndroid(grid, robot, realRun);
        }
        System.out.println("Whole Maze Robot Movement: " + robotMovementString);
        System.out.println("EXPLORATION COMPLETED!");
        System.out.println("AREA EXPLORED: " + grid.checkPercentageExplored() + "%!");
	}

	private boolean checkUnexploredCell(boolean realRun, Grid grid, Robot robot, int x, int y) {
        Robot proxyRobot = new Robot(grid, new ArrayList<>());
        proxyRobot.setDirection(robot.getDirection());
        proxyRobot.setPositionX(robot.getPositionX());
        proxyRobot.setPositionY(robot.getPositionY());

        List<String> returnActions = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), x, y, grid, proxyRobot);
        
        if (returnActions != null) {
            System.out.println("Algorithm is done, executing actions now");
            System.out.println(returnActions.toString());
            System.out.println("Nearest Cell found: x:" + x + ", y:" + y);
            for (String action : returnActions) {
                if (action.equals("M")) {
                    if (realRun) {
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                        robot.move();
                        robotMovementString += "m";
                        robot.sense(realRun);
                    } else {
                    	robot.move();
                    	robotMovementString += "m";
                    }
                } 
                else if (action.equals("L")) {
                	if (realRun) {
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                        robot.turn(LEFT);
                        robotMovementString += "l";
                        robot.sense(realRun); 
                	} else {
                		robot.turn(LEFT);
                		robotMovementString += "l";
                	}
                    
                } 
                else if (action.equals("R")) {
                	if (realRun) {
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                        robot.turn(RIGHT);
                        robotMovementString += "r";
                        robot.sense(realRun); 
                    } else {
                    	robot.turn(RIGHT);
                    	robotMovementString += "r";
                    }
                } 
                else if (action.equals("U")) {
                	if (realRun) {
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                        robot.turn(RIGHT);
                        robotMovementString += "r";
                        robot.sense(realRun); 
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                        robot.turn(RIGHT);
                        robotMovementString += "r";
                        robot.sense(realRun); 
                    } else {
                    	robot.turn(RIGHT);
                    	robotMovementString += "r";
                        robot.turn(RIGHT);
                        robotMovementString += "r";
                    }
                    
                }
                
                // Update Android that there is a move or turn
    			sendAndroid(grid, robot, realRun);
                
                stepTaken();
            }
            return true;
        } else {	
            return false;
        }
	}

	private void stepTaken() {
		try {
            Thread.sleep(robotSpeed);
        } catch (Exception except) {
            except.printStackTrace();
        }
	}

	private boolean leftHugging(boolean realRun, Robot robot, Grid grid) {
		if(robot.isObstacleInfront()) {
			if(robot.isObstacleOnLeftSide() && robot.isObstacleOnRightSide()){
				System.out.println("---------------------Making a U-Turn--------------------");
				if (realRun) {
					//SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
					robotMovementString+="R";
					robot.turn(RIGHT);
	                stepTaken();
					robot.sense(realRun);
					if(!robot.isObstacleInfront()) {
						uTurnHalt = true;
						return true;
					} else {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
						robotMovementString+="R";
						robot.turn(RIGHT);
		                stepTaken();
					}
				} else {
					robot.turn(RIGHT);
					robot.turn(RIGHT);
	                stepTaken();
				}
			} else if(robot.isObstacleOnLeftSide()){
				System.out.println("---------------------Making a Right Turn-------------------");
                if (realRun) {
                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                }
                robotMovementString+="R";
                robot.turn(RIGHT);
                stepTaken();
			} else {
				System.out.println("---------------------Making a Left Turn--------------------");
                if (realRun) {
                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                }
                robotMovementString+="L";
                robot.turn(LEFT);
                stepTaken();
			}
			
			// Update Android that there is a turn
			sendAndroid(grid, robot, realRun);
			return true;
		}
		else if(!robot.isObstacleOnLeftSide()) {
			System.out.println("-------------------------Making a Left Turn--------------------");
            if (realRun) {
            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
            }
            robotMovementString+="L";
            robot.turn(LEFT);
            stepTaken();
            System.out.println(robotMovementString);
            // Update Android that there is a turn
            sendAndroid(grid, robot, realRun);
            return true;
		}
		return false;
	}
	
	public void sendAndroid(Grid grid, Robot robot, boolean realRun) {
		if (realRun) {
			SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateMapDescriptor1(), grid.generateMapDescriptor2(), robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
		}
	}
	
	public void flushCalibration() {
		String calibrationData = SocketMgr.getInstance().receiveMessage(true);
        while (!calibrationData.equalsIgnoreCase("OK")) {
        	calibrationData = SocketMgr.getInstance().receiveMessage(false);
        }
        System.out.println("Data Flushed: " + calibrationData);
	}
	
	public boolean checkForCycle() {
		int patternCount = 0;
		int pattern1Count = 0;
		String pattern = "LMLMLM";
		//String pattern1 = "RMRMRMRM";
		
		// Only checks the pattern if the movement string is longer or same length as the pattern
		if(robotMovementString.length() >= pattern.length()) {
			for(int index = 0; index < pattern.length(); index++) {
				if(robotMovementString.substring(robotMovementString.length() - 1 - index, robotMovementString.length() - index).equals(pattern.substring(pattern.length() - 1 - index, pattern.length() - index))) {
					patternCount++;
				}
				
				/*if(robotMovementString.substring(robotMovementString.length() - 1 - index, robotMovementString.length() - index).equals(pattern1.substring(pattern1.length() - 1 - index, pattern1.length() - index))) {
					pattern1Count++;
				}*/
			}
		}
		
		//if(patternCount == pattern.length() || pattern1Count == pattern1.length()) {
		if(patternCount == pattern.length()) {
			System.out.println("Cycle Detected!");
			return true;
		}
		
		return false;
	}
	
	private int getExploringZone(int positionX, int positionY) {
		// x: 0-7, y: 10-19 
		if(positionX <= ((int) (MAP_COLUMNS/2)) && positionY >= (MAP_ROWS/2) && positionY < MAP_ROWS)
			return 0;
		// x: 0-7, y: 0-9
		else if(positionX <= ((int) (MAP_COLUMNS/2)) && positionY < (MAP_ROWS/2))
			return 1;
		// x: 8-14, y: 0-9
		else if(positionX >= ((int) Math.ceil(MAP_COLUMNS/2)) && positionX < MAP_COLUMNS && positionY < (MAP_ROWS/2))
			return 2;
		// x: 8-14, y: 10-19
		else if(positionX >= ((int) Math.ceil(MAP_COLUMNS/2)) && positionX < MAP_COLUMNS && positionY >= (MAP_ROWS/2) && positionY < MAP_ROWS)
			return 3;
		else
			return -1;
	}
	
	private boolean checkZoneForUnexplored(int zoneNum, boolean realRun, Grid grid, Robot robot) {
		switch(zoneNum) {
			case 0:
				// check for any unexplored cells in zone 0, x: 0-7, y: 10-19
				for (int y = (MAP_ROWS/2); y < MAP_ROWS; y++) {
		            for (int x = 0; x <= ((int) (MAP_COLUMNS/2)); x++) {
		            	//check for unexplored cells and if neighbors are reachable.
		                if (!grid.getIsExplored(x, y) &&
					            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
					                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
					                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
					                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
		                	return true;
		                }
		            }
				}
				break;
			case 1:
				//check for any unexplored cells in zone 1, x: 0-7, y: 0-9
    			for (int y = 0; y < (MAP_ROWS/2); y++) {
                    for (int x = 0; x <= ((int) (MAP_COLUMNS/2)); x++) {
                    	//check for unexplored cells and if neighbors are reachable.
                        if (!grid.getIsExplored(x, y) &&
    				            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
    				                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
                        	return true;
                        }
                    }
        		}
				break;
			case 2:
				//check for any unexplored cells in zone 2, x: 8-14, y: 0-9
				for (int y = 0; y < (MAP_ROWS/2); y++) {
                    for (int x = ((int) Math.ceil(MAP_COLUMNS/2)); x < MAP_COLUMNS; x++) {
                    	//check for unexplored cells and if neighbors are reachable.
                        if (!grid.getIsExplored(x, y) &&
    				            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
    				                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
                        	return true;
                        }
                    }
        		}
				break;
			case 3:
				//check for any unexplored cells in zone 3, x: 8-14, y: 10-19
				for (int y = (MAP_ROWS/2); y < MAP_ROWS; y++) {
                    for (int x = ((int) Math.ceil(MAP_COLUMNS/2)); x < MAP_COLUMNS; x++) {
                    	//check for unexplored cells and if neighbors are reachable.
                        if (!grid.getIsExplored(x, y) &&
    				            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
    				                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
                        	return true;
                        }
                    }
        		}
				break;
				default:
		}
		
		return false;
	}
	
	private boolean startFirstReachableUnexploredAlgo(boolean realRun, Grid grid, Robot robot) {
		//checking for reachable cells in the arena.
        for (int y = MAP_ROWS - 1; y >= 0; y--) {
            for (int x = MAP_COLUMNS - 1; x >= 0; x--) {
            	//check for unexplored cells and if neighbors are reachable.
                if (!grid.getIsExplored(x, y) &&
			            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
			                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
			                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
			                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
                	return true;
                }
            }
        }
        
		return false;
	}
	
}