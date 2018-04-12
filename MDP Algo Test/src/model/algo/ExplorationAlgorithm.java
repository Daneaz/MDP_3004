package model.algo;


import model.algo.Algorithm;
import model.physical.Grid;
import model.physical.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;


import static constant.CommunicationConstant.*;
import static constant.MapConstant.*;

import static constant.RobotConstant.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import constant.RobotConstant;

public class ExplorationAlgorithm implements Algorithm {

	private boolean trustExplored = false;
	private boolean uTurnFlagForNoTrustExplored = false;
	private boolean uTurn = false;
	private int minutes = 50;
	private int seconds = 20;
	private int timeLimit = ((minutes *60) + seconds)*1000;
	// A variable to store the start time
	long startTime;
	private boolean uTurnHalt = false;
	private String robotMovementString = "";
	private int robotSpeed;
	private static final int START_X_POSITION = 0;
    private static final int START_Y_POSITION = 17;
	private List<Integer> robotStartPosAndWaypoints;
	private static String timeStr;
	
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
            
            // Set the start time when received the starting command from android
            startTime = System.nanoTime(); 
        }
        
        // SELECT EITHER ONE OF THE METHODS TO RUN ALGORITHMS.
        runExplorationAlgorithm(realRun, robot, grid);

        // CALIBRATION AFTER EXPLORATION
        calibrateAndTurn(realRun, robot);

        // GENERATE MAP DESCRIPTOR, SEND TO ANDROID
        System.out.println("Final Exploration MDFSTRINGS: ");
        System.out.println("MDFString1: " + grid.generateMapDescriptor1());
        System.out.println("MDFString2: " + grid.generateMapDescriptor2());
        //SocketMgr.getInstance().sendMessage(CALL_ANDROID, MessageMgr.generateFinalDescriptor(part1, part2));
		
	}

	private void calibrateAndTurn(boolean realRun, Robot robot) {
		if (realRun) {
				// set the flag to disable updating of map
				robot.setFastestPathCalibration(true);
	            while (robot.getDirection() != SOUTH) {
				//while (robot.getDirection() != NORTH) {
	                robot.turn(LEFT);
	                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	                robot.sense(realRun);
	            }
	            
	            /*SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	            flushCalibration();
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	            robot.sense(realRun);
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	            flushCalibration();
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	            robot.sense(realRun);*/
	            
	            // Send the calibration command to Arduino
	            try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "P");
	            flushCalibration();
	            
	            // set the flag to enable updating of map
	            robot.setFastestPathCalibration(false);
	        }
		
	}

	private void runExplorationAlgorithm(boolean realRun, Robot robot, Grid grid) {
		// flag to keep track if robot has entered startZone
		boolean inStartZone = false;
		// flag to keep track if robot has entered endZone
		boolean inEndZone = false;
		
		// Sense the surrounding at the starting point
		robot.sense(realRun);
		
		// This is the exploration algorithm, as long as it is not in startZone or endZone
		// it will keep looping here
		while(!inStartZone || !inEndZone) {
			// Checks if a turn is needed
			boolean turn = leftHugging(realRun, robot, grid);
			
			// A turn has been made 
			if (turn) {
				// Actual Run
				if (realRun) {
					// uTurnHalt is used to detect that a U turn has been interfered
					// No need to sense the surrounding if the U turn has been interfered
		            /*if(uTurnHalt) {
		            	uTurnHalt = false;
		            } else {*/
		            	// Sense the surrounding since a turn has been made
		            	// Store the sensor readings at the same time
		            	robot.sense(realRun, true);
		            //}
				}
				// Sense for simulator
				else {
					robot.sense(realRun);
				}
			}
			
			// Move forward
			if (realRun) {
				handleMoveForward(grid, robot, realRun);
			}
			// Simulator
			else {
				robotMovementString+="M";
				System.out.println("----------------------Moving Forward----------------------");
				System.out.println(robotMovementString);
				
				// show the robot move forward on the simulator
				robot.move();
				
				stepTaken();
				
				// Sense the surrounding
				robot.sense(realRun);
			}
            
            // checks if robot enters the endZone
            if(Grid.isInEndingZone(robot.getPositionX(), robot.getPositionY())) {
            	// sets the endZone flag to true if robot enters the endZone
            	inEndZone = true;
            }
            
            // checks if robot enters the startZone
            if(Grid.isInStartingZone(robot.getPositionX() +2, robot.getPositionY()) && inEndZone) {
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
                        String compressed = Algorithm.compressExplorationPath(returnActions, proxyRobot, false);
                    	//String compressed = Algorithm.compressExplorationCalibrationPath(returnActions, proxyRobot);
                        // Send the actions string to Arduino
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressed);
                    }
                    
                    /*System.out.println("Fastest Path Algorithm back to Start Zone Calculated. Executing actions now...");
                    System.out.println(returnActions.toString());*/
                    
                    // Simulate the robot moving back to startZone step by step
                    for (String action : returnActions) {
                        if (action.equals("M")) {
                            robot.move();
                            robotMovementString += "m";
                        } else if (action.equals("L")) {
                            robot.turn(LEFT);
                            robotMovementString += "l";
                        } else if (action.equals("R")) {
                            robot.turn(RIGHT);
                            robotMovementString += "r";
                        } else if (action.equals("U")) {
                            robot.turn(RIGHT);
                            robotMovementString += "r";
                            robot.turn(RIGHT);
                            robotMovementString += "r";
                        }
                        
                        // Only give delay for simulator 
                        if(!realRun) {
                        	stepTaken();
                        }
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
            }  
            // A loop is detected
            else if(checkForCycle()) {
            	// only tries to break the loop if it is not fully explored
            	if (grid.checkPercentageExplored() < 100) { 
            		executeLoopBreaker(robot, grid, realRun);
            		
            		// Checks if robot is in startZone,
                    // Set the startZone flag to true only if robot has entered the endZone before
                    if (Grid.isInStartingZone(robot.getPositionX() + 2, robot.getPositionY()) && inEndZone) {
                        inStartZone = true;
                    }
                }
            }
            // Head back to start zone when time limit reached
            else if(realRun && getElapsedTime(startTime, System.nanoTime()) >= timeLimit) {
            	System.out.println("Time limit reached. Ending the exploration!");
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
                        String compressed = Algorithm.compressExplorationPath(returnActions, proxyRobot,false);
                    	//String compressed = Algorithm.compressExplorationCalibrationPath(returnActions, proxyRobot);
                        // Send the actions string to Arduino
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressed);
                    }
                    
                    /*System.out.println("Fastest Path Algorithm back to Start Zone Calculated. Executing actions now...");
                    System.out.println(returnActions.toString());*/
                    
                    // Simulate the robot moving back to startZone step by step
                    for (String action : returnActions) {
                        if (action.equals("M")) {
                            robot.move();
                            robotMovementString += "m";
                        } else if (action.equals("L")) {
                            robot.turn(LEFT);
                            robotMovementString += "l";
                        } else if (action.equals("R")) {
                            robot.turn(RIGHT);
                            robotMovementString += "r";
                        } else if (action.equals("U")) {
                            robot.turn(RIGHT);
                            robotMovementString += "r";
                            robot.turn(RIGHT);
                            robotMovementString += "r";
                        }
                        
                        // Only give delay for simulator 
                        if(!realRun) {
                        	stepTaken();
                        }
                        // Update Android that there is a move or turn
                        // sendAndroid(grid, robot, realRun);
                    }
                    //refreshMap(robot);
                }
                else {
                    System.out.println("FASTEST PATH CANNOT BE FOUND!");
                }

                // Checks if robot is in startZone,
                // Set the startZone flag to true only if robot has entered the endZone before
                if (Grid.isInStartingZone(robot.getPositionX() + 2, robot.getPositionY()) && inEndZone) {
                    inStartZone = true;
                    // Even if the robot has not passed through the endZone just set it to true since time exceeded
                    inEndZone = true;
                }
            }
		}
	        
        // Start Second exploration if robot is back to starting zone but the exploration is not 100% and it is not over the time limit
        if(grid.checkPercentageExplored() < 100.0) {
        	boolean runSecond = false;
        	if(realRun) {
        		if(getElapsedTime(startTime, System.nanoTime()) < (timeLimit-1000)) {
        			runSecond = true;
        		}
        	} else {
        		runSecond = true;
        	}
        	
        	if(runSecond) {
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
                	if (grid.checkPercentageExplored() == 100) { 
                        break;
                    }
                    for (int x = MAP_COLUMNS - 1; x >= 0; x--) {
                    	//check for unexplored cells and if neighbors are reachable.
                        if (!grid.getIsExplored(x, y) &&
    				            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
    				                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
    				                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
                        	
                        	if (grid.checkPercentageExplored() == 100) { 
                                break;
                            }
                        	
                            boolean isInStartPoint = true;
                            while (isInStartPoint) { 
                            	// set isInStartPoint to false to state that the robot is not at starting point.
                            	isInStartPoint = false; 
                            	
                            	if(checkForCycle()) {
                            		// only tries to break the loop if it is not fully explored
                                	if (grid.checkPercentageExplored() < 100) { 
                                		executeLoopBreaker(robot, grid, realRun);
                                		
                                		// Checks if robot is in startZone,
                                        // Set the startZone flag to true only if robot has entered the endZone before
                                        if (Grid.isInStartingZone(robot.getPositionX() + 2, robot.getPositionY()) && inEndZone) {
                                            inStartZone = true;
                                        }
                                    }
                                }
                            	
                            	// Checks if a turn is needed
                                boolean turned = leftHugging(realRun, robot, grid);
                                
                                // A turn has been made
                                if (turned) {
                                    // Actual Run
                                    if (realRun) {                                	
                                    	// uTurnHalt is used to detect that a U turn has been interfered
                    					// No need to sense the surrounding if the U turn has been interfered
                    		            /*if(uTurnHalt) {
                    		            	uTurnHalt = false;
                    		            } else {*/
                    		            	// Sense the surrounding since a turn has been made
                    		            	robot.sense(realRun, true);
//                    		            }
                                    } 
                                    // Simulator Run
                                    else {
                                    	robot.sense(realRun);
                                    }
                                }
                                
                                // Move forward
                    			if (realRun) {
                    				handleMoveForward(grid, robot, realRun);
                    			}
                    			// Simulator
                    			else {
                    				robotMovementString+="M";
                    				System.out.println("----------------------Moving Forward----------------------");
                    				System.out.println(robotMovementString);
                    				
                    				// show the robot move forward on the simulator
                    				robot.move();
                    				
                    				stepTaken();
                    				
                    				// Sense the surrounding
                    				robot.sense(realRun);
                    			}
                                
                                // breaks out of the while loop if map has been fully explored
                                if (grid.checkPercentageExplored() == 100) { 
                                    break;
                                }
                                // Head back to start zone when time limit reached
                                else if(realRun && getElapsedTime(startTime, System.nanoTime()) >= timeLimit) {
                                    break;
                                }
                                
                                while (grid.getIsExplored(robot.getPositionX(), robot.getPositionY()) != firstRunExplored.getIsExplored(robot.getPositionX(), robot.getPositionY())) {
                                	// checks if a turn is needed
                                    turned = leftHugging(realRun, robot, grid);
                                    
                                    // A turn has been made
                                    if (turned) {
                                    	// Actual Run
                                    	if (realRun) {                                		
                                    		// uTurnHalt is used to detect that a U turn has been interfered
                        					// No need to sense the surrounding if the U turn has been interfered
                        		            /*if(uTurnHalt) {
                        		            	uTurnHalt = false;
                        		            } else {*/
                        		            	// Sense the surrounding since a turn has been made
                        		            	robot.sense(realRun, true);
                        		            //}
                                    	}
                                    	// Simulator Run
                                    	else {
                                    		// Sense the surrounding since a turn has been made
                                    		robot.sense(realRun);
                                    	}
                                    }

                                    // Move forward
                        			if (realRun) {
                        				handleMoveForward(grid, robot, realRun);
                        			}
                        			// Simulator
                        			else {
                        				robotMovementString+="M";
                        				System.out.println("----------------------Moving Forward----------------------");
                        				System.out.println(robotMovementString);
                        				
                        				// show the robot move forward on the simulator
                        				robot.move();
                        				
                        				stepTaken();
                        				
                        				// Sense the surrounding
                        				robot.sense(realRun);
                        			}
                                    
                        			// breaks out of the while loop is map has been fully explored
                                	if (grid.checkPercentageExplored() == 100) { 
                                        break;
                                    }
                                    // Check if there is a Cycle
                                	else if(checkForCycle()) {
                                    	// only tries to break the loop if it is not fully explored
                                    	if (grid.checkPercentageExplored() < 100) { 
                                    		executeLoopBreaker(robot, grid, realRun);
                                    		
                                    		// Checks if robot is in startZone,
                                            // Set the startZone flag to true only if robot has entered the endZone before
                                            if (Grid.isInStartingZone(robot.getPositionX() + 2, robot.getPositionY()) && inEndZone) {
                                                inStartZone = true;
                                            }
                                        }
                                    }
                                	// Head back to start zone when time limit reached
                                    else if(realRun && getElapsedTime(startTime, System.nanoTime()) >= timeLimit) {
                                        break;
                                    }
                                }
                                if (grid.checkPercentageExplored() == 100) { 
                                    break;
                                }
                                // Head back to start zone when time limit reached
                                else if(realRun && getElapsedTime(startTime, System.nanoTime()) >= timeLimit) {
                                    break;
                                }
                            }
                            if (grid.checkPercentageExplored() == 100) { 
                                break;
                            } 
                            // Head back to start zone when time limit reached
                            else if(realRun && getElapsedTime(startTime, System.nanoTime()) >= timeLimit) {
                                break;
                            }
                        }
                        if (grid.checkPercentageExplored() == 100) { 
                            break;
                        }
                        // Head back to start zone when time limit reached
                        else if(realRun && getElapsedTime(startTime, System.nanoTime()) >= timeLimit) {
                            break;
                        }
                    }
                    if (grid.checkPercentageExplored() == 100) { 
                        break;
                    }
                    // Head back to start zone when time limit reached
                    else if(realRun && getElapsedTime(startTime, System.nanoTime()) >= timeLimit) {
                    	System.out.println("Time limit reached. Ending the exploration!");
                        break;
                    }
                }
                
                // Run the Fastest Path Algorithm back to start zone after completing the exploration
                if(!Grid.isInStartingZone(robot.getPositionX()+2, robot.getPositionY()+2)) {
                	// Create a proxyRobot for the calculation so that it will not affect the original robot
                	Robot proxyRobot = new Robot(grid, new ArrayList<>());
                    
                	// Initialize the proxyRobot with the current conditions of the original robot
                	proxyRobot.setDirection(robot.getDirection());
                    proxyRobot.setPositionX(robot.getPositionX());
                    proxyRobot.setPositionY(robot.getPositionY());
                 
//                    System.out.println("Running A* Algorithm to go back to the Start Zone...");
                    List<String> returnActions = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), START_X_POSITION, START_Y_POSITION, grid, proxyRobot);

                    // If the fastest path can be calculated, process it
                    if (returnActions != null) {
                    	/*System.out.println("Fastest Path Algorithm back to Start Zone Calculated. Executing actions now...");
                        System.out.println(returnActions.toString());*/
                        
                        // Initialize the proxyRobot back to the original robot current conditions
                        proxyRobot.setPositionX(robot.getPositionX());
                        proxyRobot.setPositionY(robot.getPositionY());
                        proxyRobot.setDirection(robot.getDirection());
                            
                        if (realRun) {
                        	// Get the actions string to send to Arduino
                            String compressedPath = Algorithm.compressExplorationPath(returnActions, proxyRobot, false);
                            //String compressedPath = Algorithm.compressExplorationCalibrationPath(returnActions, proxyRobot);
                            // Send the actions string to Arduino
                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressedPath);
                        }
                        
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
                            // Only give delay for simulator 
                            if(!realRun) {
                            	stepTaken();
                            }
                        }
                        
                        //refreshMap(robot);
                        // Update Android that there is a move or turn
                        // sendAndroid(grid, robot, realRun);	
                        
//                        grid.clearClosedSet();
                    }
                    else {
                        System.out.println("FASTEST PATH CANNOT BE FOUND!!");
                    }
                }
        	}
        }
        
        if(realRun) {
        	refreshMap(robot);
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	// Inform Android Tablet to stop the Exploration Timer
        	SocketMgr.getInstance().sendMessage(CALL_ANDROID, "exploreComplete");
            
        	// Need to give a delay in between so that raspberry pi can send the messages separately
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
            // Update Android when the exploration is completed
            sendAndroid(grid, robot, realRun);
        }
/*        grid.clearClosedSet();
        refreshMap(robot);
        System.out.println("Whole Maze Robot Movement: " + robotMovementString);*/
        System.out.println("EXPLORATION COMPLETED!");
        if(realRun) {
        	getElapsedTime(startTime, System.nanoTime());
        	System.out.println(timeStr);
        }
        System.out.println("AREA EXPLORED: " + grid.checkPercentageExplored() + "%!");
	}


	private boolean checkUnexploredCell(boolean realRun, Grid grid, Robot robot, int x, int y) {
        Robot proxyRobot = new Robot(grid, new ArrayList<>());
        proxyRobot.setDirection(robot.getDirection());
        proxyRobot.setPositionX(robot.getPositionX());
        proxyRobot.setPositionY(robot.getPositionY());

        List<String> returnActions = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), x, y, grid, proxyRobot);
        
        if (returnActions != null) {
            //System.out.println("Algorithm is done, executing actions now");
            //System.out.println(returnActions.toString());
            //System.out.println("Nearest Cell found: x:" + x + ", y:" + y);
            
            List<String> processedActions = processActions(returnActions);
            
            if(realRun) {
            	for (String action : processedActions) {
            		 SocketMgr.getInstance().sendMessage(CALL_ARDUINO, action);
                     updateRobotOnMap(action, robot);
                     robot.sense(realRun);
                }
            	
            	// Refresh the map after all the actions have been executed
            	//refreshMap(robot);
                 	
                // Update Android after all the actions have been executed
        		sendAndroid(grid, robot, realRun);
            }
            // for simulator
            else {
            	for (String action : returnActions) {
                    if (action.equals("M")) {
                        	robot.move();
                        	robotMovementString += "m";
                    } 
                    else if (action.equals("L")) {
                    		robot.turn(LEFT);
                    		robotMovementString += "l";
                    } 
                    else if (action.equals("R")) {
                        	robot.turn(RIGHT);
                        	robotMovementString += "r";
                    } 
                    else if (action.equals("U")) {
                        	robot.turn(RIGHT);
                        	robotMovementString += "r";
                            robot.turn(RIGHT);
                            robotMovementString += "r";
                    }
                    
                    // Delay for simulator
                    stepTaken();
                }
            }
            
            /*for (String action : returnActions) {
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
                
                // Only give delay for simulator 
                if(!realRun) {
                	stepTaken();
                }
            }
            
            // Refresh the map after all the actions have been executed
            if(realRun)
             	stepTaken();
             	
            // Update Android after all the actions have been executed
    		sendAndroid(grid, robot, realRun);*/
            
            /*grid.getCell()[x][y].setIsFastestPath(false);
            grid.clearClosedSet();*/
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
		if(trustExplored) {
			if(robot.isObstacleInfront() && robot.isObstacleOnLeftSide() && !robot.isObstacleOnRightSide()) {
				// Create a proxyRobot for the calculation so that it will not affect the original robot
		    	Robot proxyRobot = new Robot(grid, new ArrayList<>());
		        
		    	// Initialize the proxyRobot with the current conditions of the original robot
		    	proxyRobot.setDirection(robot.getDirection());
		        proxyRobot.setPositionX(robot.getPositionX());
		        proxyRobot.setPositionY(robot.getPositionY());
		        
		        proxyRobot.turn(RIGHT);
				if(check1StepUturn(grid, proxyRobot)) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					robotMovementString+="U";
					robot.turn(RIGHT);
					robot.turn(RIGHT);
				} else {
					if (realRun) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	                }
	                robotMovementString+="R";
	                robot.turn(RIGHT);
	                // Only give delay for simulator
	                if(!realRun) {
	                	stepTaken();
	                }
				}
                return true;
			}
			// Checks if need to do U-Turn in front
			else if(checkUTurnAhead(grid, robot)) {
				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
				robotMovementString+="U";
				robot.turn(RIGHT);
				robot.turn(RIGHT);
				
				return true;
			} else if(robot.isObstacleInfront() && robot.isObstacleOnLeftSide2() && robot.isObstacleOnRightSide2()) {
				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
				robotMovementString+="U";
				robot.turn(RIGHT);
				robot.turn(RIGHT);
				
				return true;
			} else if(robot.isObstacleInfront()) {
				if(robot.isObstacleOnLeftSide() && robot.isObstacleOnRightSide()) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					robotMovementString+="U";
					robot.turn(RIGHT);
					robot.turn(RIGHT);
					
					return true;
				}
				else if(robot.isObstacleOnLeftSide()) {
					/*System.out.println("---------------------Making a Right Turn-------------------");
	                */if (realRun) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	                }
	                robotMovementString+="R";
	                robot.turn(RIGHT);
	                // Only give delay for simulator
	                if(!realRun) {
	                	stepTaken();
	                }
	                
	                return true;
				} else {
//					System.out.println("---------------------Making a Left Turn--------------------");
	                if (realRun) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	                }
	                robotMovementString+="L";
	                robot.turn(LEFT);

	                // Only give delay for simulator 
	                if(!realRun) {
	                	stepTaken();
	                }
	                
	                return true;
				}
			} else if(!robot.isObstacleOnLeftSide()) {
//				System.out.println("-------------------------Making a Left Turn--------------------");
	            if (realRun) {
	            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	            }
	            robotMovementString+="L";
	            robot.turn(LEFT);
	            // Only give delay for simulator 
	            if(!realRun) {
	            	stepTaken();
	            }
	            
	            // Update Android that there is a turn
	            /*sendAndroid(grid, robot, realRun);
	            
	            System.out.println(robotMovementString);*/
	            return true;
			}
		} else {
			if(robot.isObstacleInfront() && robot.isObstacleOnLeftSide() && !robot.isObstacleOnRightSide()) {
				// Create a proxyRobot for the calculation so that it will not affect the original robot
		    	Robot proxyRobot = new Robot(grid, new ArrayList<>());
		        
		    	// Initialize the proxyRobot with the current conditions of the original robot
		    	proxyRobot.setDirection(robot.getDirection());
		        proxyRobot.setPositionX(robot.getPositionX());
		        proxyRobot.setPositionY(robot.getPositionY());
		        
		        proxyRobot.turn(RIGHT);
				if(check1StepUturn(grid, proxyRobot)) {
					/*SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					robotMovementString+="U";*/
					
					// U-Turn the robot for the simulator first
					robot.turn(RIGHT);
					robot.turn(RIGHT);
					
					// After U-Turn calculate the number of steps to move forward and move
					uTurnFlagForNoTrustExplored = true;

					int numberOfSteps = checkFrontExplored2(grid, robot);
					// numberOfSteps: 0
					if(numberOfSteps == 0) {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
						robotMovementString+="U";
						System.out.println("Cannot move forward. Obstacles in front");
					} else if(numberOfSteps == 1) {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
						robotMovementString+="U";
					}
					// numberOfSteps: 2-9
					else if(numberOfSteps > 1 && numberOfSteps < 10) {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM0" + String.valueOf(numberOfSteps));
						robotMovementString+="U";
					}
					// numberOfSteps: 10-17
					else {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM" + String.valueOf(numberOfSteps));
						robotMovementString+="U";
					}
					
					// Update the simulator
					if(numberOfSteps > 1) {
						for(int i=0; i<numberOfSteps; i++) {
							robotMovementString+="M";
							robot.move();
						}
					}
				} else {
					if (realRun) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	                }
	                robotMovementString+="R";
	                robot.turn(RIGHT);
	                // Only give delay for simulator
	                if(!realRun) {
	                	stepTaken();
	                }
				}
                return true;
			}
			// Checks if need to do U-Turn in front
			else if(checkUTurnAhead(grid, robot)) {
				/*SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
				robotMovementString+="U";*/
				
				// U-Turn the robot for the simulator first
				robot.turn(RIGHT);
				robot.turn(RIGHT);
				
				// After U-Turn calculate the number of steps to move forward and move
				uTurnFlagForNoTrustExplored = true;

				int numberOfSteps = checkFrontExplored2(grid, robot);
				// numberOfSteps: 0
				if(numberOfSteps == 0) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					robotMovementString+="U";
					System.out.println("Cannot move forward. Obstacles in front");
				} else if(numberOfSteps == 1) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					robotMovementString+="U";
				}
				// numberOfSteps: 2-9
				else if(numberOfSteps > 1 && numberOfSteps < 10) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM0" + String.valueOf(numberOfSteps));
					robotMovementString+="U";
				}
				// numberOfSteps: 10-17
				else {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM" + String.valueOf(numberOfSteps));
					robotMovementString+="U";
				}
				
				// Update the simulator
				if(numberOfSteps > 1) {
					for(int i=0; i<numberOfSteps; i++) {
						robotMovementString+="M";
						robot.move();
					}
				}
				
				return true;
			} else if(robot.isObstacleInfront() && robot.isObstacleOnLeftSide2() && robot.isObstacleOnRightSide2()) {
				/*SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
				robotMovementString+="U";*/
				
				// U-Turn the robot for the simulator first
				robot.turn(RIGHT);
				robot.turn(RIGHT);
				
				// After U-Turn calculate the number of steps to move forward and move
				uTurnFlagForNoTrustExplored = true;

				int numberOfSteps = checkFrontExplored2(grid, robot);
				// numberOfSteps: 0
				if(numberOfSteps == 0) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					robotMovementString+="U";
					System.out.println("Cannot move forward. Obstacles in front");
				} else if(numberOfSteps == 1) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
					robotMovementString+="U";
				}
				// numberOfSteps: 2-9
				else if(numberOfSteps > 1 && numberOfSteps < 10) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM0" + String.valueOf(numberOfSteps));
					robotMovementString+="U";
				}
				// numberOfSteps: 10-17
				else {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM" + String.valueOf(numberOfSteps));
					robotMovementString+="U";
				}
				
				// Update the simulator
				if(numberOfSteps > 1) {
					for(int i=0; i<numberOfSteps; i++) {
						robotMovementString+="M";
						robot.move();
					}
				}
				
				return true;
			} else if(robot.isObstacleInfront()) {
				if(robot.isObstacleOnLeftSide() && robot.isObstacleOnRightSide()) {
					/*System.out.println("---------------------Making a U-Turn--------------------");*/
					if (realRun) {
						/*SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
						robotMovementString+="U";*/
						
						// U-Turn the robot for the simulator first
						robot.turn(RIGHT);
						robot.turn(RIGHT);
						
						// After U-Turn calculate the number of steps to move forward and move
						uTurnFlagForNoTrustExplored = true;

						int numberOfSteps = checkFrontExplored2(grid, robot);
						// numberOfSteps: 0
						if(numberOfSteps == 0) {
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
							robotMovementString+="U";
							System.out.println("Cannot move forward. Obstacles in front");
						} else if(numberOfSteps == 1) {
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
							robotMovementString+="U";
						}
						// numberOfSteps: 2-9
						else if(numberOfSteps > 1 && numberOfSteps < 10) {
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM0" + String.valueOf(numberOfSteps));
							robotMovementString+="U";
						}
						// numberOfSteps: 10-17
						else {
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZUM" + String.valueOf(numberOfSteps));
							robotMovementString+="U";
						}
						
						// Update the simulator
						if(numberOfSteps > 1) {
							for(int i=0; i<numberOfSteps; i++) {
								robotMovementString+="M";
								robot.move();
							}
						}
							
					} else {
						robotMovementString+="RR";
						robot.turn(RIGHT);
						robot.turn(RIGHT);
						// Only give delay for simulator
						stepTaken();
					}
				} else if(robot.isObstacleOnLeftSide()) {
					/*System.out.println("---------------------Making a Right Turn-------------------");
	                */if (realRun) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	                }
	                robotMovementString+="R";
	                robot.turn(RIGHT);
	                // Only give delay for simulator
	                if(!realRun) {
	                	stepTaken();
	                }
				} else {
//					System.out.println("---------------------Making a Left Turn--------------------");
	                if (realRun) {
	                	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	                }
	                robotMovementString+="L";
	                robot.turn(LEFT);

	                // Only give delay for simulator 
	                if(!realRun) {
	                	stepTaken();
	                }

				}
				
				// Update Android that there is a turn
//				sendAndroid(grid, robot, realRun);
				
//				System.out.println(robotMovementString);
				return true;
			}
			else if(!robot.isObstacleOnLeftSide()) {
//				System.out.println("-------------------------Making a Left Turn--------------------");
	            if (realRun) {
	            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	            }
	            robotMovementString+="L";
	            robot.turn(LEFT);
	            // Only give delay for simulator 
	            if(!realRun) {
	            	stepTaken();
	            }
	            
	            // Update Android that there is a turn
	            /*sendAndroid(grid, robot, realRun);
	            
	            System.out.println(robotMovementString);*/
	            return true;
			}
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
		//int pattern1Count = 0;
		String pattern = "LMLMLMLM";
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
	
	private boolean checkZoneForUnexplored(int zoneNum, boolean realRun, Grid grid, Robot robot, boolean failed) {
		switch(zoneNum) {
			case 0:
				// To prevent the robot to move towards the center scan the area nearer to the wall first
				// If there is no reachable cells near to the wall then there is no choice but to move towards the center
				
				if(!failed) {
					// check for any unexplored cells in zone 0, x: 0-3, y: 10-19
					for (int x = 0; x <= ((int) (MAP_COLUMNS/2)); x++) {
						for (int y = (MAP_ROWS/2); y < MAP_ROWS; y++) {
			            	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
			            		//grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
		                    
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            }
					}
				} else {
					// Unexplored FAILED! Check Explored instead
					// check for any reachable explored cells in zone 0, x: 0-3, y: 10-19
					for (int x = 0; x <= ((int) (MAP_COLUMNS/2)); x++) {
						for (int y = (MAP_ROWS/2); y < MAP_ROWS; y++) {
			            	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            }
					}
				}
				break;
			case 1:
				// To prevent the robot to move towards the center scan the area nearer to the wall first
				// If there is no reachable cells near to the wall then there is no choice but to move towards the center
				
				if(!failed) {
					//check for any unexplored cells in zone 1, x: 0-3, y: 0-9
					for (int x = 0; x <= ((int) (MAP_COLUMNS/4)); x++) {
						for (int y = (MAP_ROWS/2) - 1; y >= 0; y--) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
	    			
	    			//check for any unexplored cells in zone 1, x: 4-7, y: 0-9
					for (int y = 0; y <= (MAP_ROWS/2) - 1; y++) {
						for (int x = (((int) (MAP_COLUMNS/4))) + 1; x <= ((int) (MAP_COLUMNS/2)); x++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
				} else {
					// Unexplored FAILED! Check Explored instead
					//check for any reachable explored cells in zone 1, x: 0-3, y: 0-9
					for (int y = 0; y <= (MAP_ROWS/2) - 1; y++) {
						for (int x = 0; x <= ((int) (MAP_COLUMNS/4)); x++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
	    			
	    			//check for any reachable explored cells in zone 1, x: 4-7, y: 0-9
					for (int y = 0; y <= (MAP_ROWS/2) - 1; y++) {
						for (int x = (((int) (MAP_COLUMNS/4))) + 1; x <= ((int) (MAP_COLUMNS/2)); x++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
			            	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
					}
				}
				break;
			case 2:
				// To prevent the robot to move towards the center scan the area nearer to the wall first
				// If there is no reachable cells near to the wall then there is no choice but to move towards the center
				
				if(!failed) {
					//check for any unexplored cells in zone 2, x: 8-14, y: 0-4
					for (int y = 0; y <= (MAP_ROWS/5); y++) {
						for (int x = ((int) Math.ceil(MAP_COLUMNS/2)) + 1 ; x < MAP_COLUMNS; x++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
	                        /*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
					
					//check for any unexplored cells in zone 2, x: 11-14, y: 5-9
					for (int x = MAP_COLUMNS - 1; x >= (((int) Math.ceil(MAP_COLUMNS/2)) + 1); x--) {
						for (int y = (MAP_ROWS/4); y < (MAP_ROWS/2); y++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
		                    }
			            	
	                        /*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
				} else {
					// Unexplored FAILED! Check Explored instead
					//check for any reachable explored cells in zone 2, x: 8-14, y: 0-4
					for (int x = ((int) Math.ceil(MAP_COLUMNS/2)) + 1 ; x < MAP_COLUMNS; x++) {
						for (int y = 0; y <= (MAP_ROWS/5); y++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }			            	
	                        /*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
					
					//check for any reachable explored cells in zone 2, x: 11-14, y: 5-9
					for (int x = MAP_COLUMNS - 1; x >= (((int) Math.ceil(MAP_COLUMNS/2)) + 1); x--) {
						for (int y = (MAP_ROWS/4); y < (MAP_ROWS/2); y++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
	                        /*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
				}
				break;
			case 3:
				// To prevent the robot to move towards the center scan the area nearer to the wall first
				// If there is no reachable cells near to the wall then there is no choice but to move towards the center
				
				if(!failed) {
					//check for any unexplored cells in zone 3, x: 11-14, y: 10-19
					for (int x = MAP_COLUMNS - 1; x >= MAP_COLUMNS - (((int) Math.ceil(MAP_COLUMNS/4)) + 1); x--) {
						for (int y = (MAP_ROWS/2); y < MAP_ROWS; y++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
					
					//check for any unexplored cells in zone 3, x: 8-10, y: 15-19
					for (int y = (MAP_ROWS - 1); y >= ((MAP_ROWS/4)*3); y--) {
						for (int x = MAP_COLUMNS - (((int) Math.ceil(MAP_COLUMNS/4)) + 2); x >= (((int) Math.ceil(MAP_COLUMNS/2)) + 1); x--) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
					
					//check for any unexplored cells in zone 3, x: 8-10, y: 10-14
					for (int x = MAP_COLUMNS - (((int) Math.ceil(MAP_COLUMNS/4)) + 2); x >= (((int) Math.ceil(MAP_COLUMNS/2)) + 1); x--) {
						for (int y = (MAP_ROWS/2); y < ((MAP_ROWS/4)*3); y++) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
	                    	//check for unexplored cells and if neighbors are reachable.
		                    if (!grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
				} else {
					// Unexplored FAILED! Check Explored instead
					//check for any reachable explored cells in zone 3, x: 11-14, y: 10-19
					for (int y = (MAP_ROWS/2); y < MAP_ROWS; y++) {
						for (int x = MAP_COLUMNS - 1; x >= MAP_COLUMNS - (((int) Math.ceil(MAP_COLUMNS/4)) + 1); x--) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
					
					//check for any reachable explored cells in zone 3, x: 8-10, y: 15-19
					for (int y = (MAP_ROWS - 1); y >= ((MAP_ROWS/4)*3); y--) {
						for (int x = MAP_COLUMNS - (((int) Math.ceil(MAP_COLUMNS/4)) + 2); x >= (((int) Math.ceil(MAP_COLUMNS/2)) + 1); x--) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
			            	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
					
					//check for any reachable explored cells in zone 3, x: 8-10, y: 10-14
					for (int y = (MAP_ROWS/2); y < ((MAP_ROWS/4)*3); y++) {
						for (int x = MAP_COLUMNS - (((int) Math.ceil(MAP_COLUMNS/4)) + 2); x >= (((int) Math.ceil(MAP_COLUMNS/2)) + 1); x--) {
	                    	/*grid.getCell()[x][y].setisChecking(true);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    	
	                    	//check for unexplored cells and if neighbors are reachable.
		                    if (grid.getIsExplored(x, y) &&
		                            ((checkUnexploredCell(realRun, grid, robot, x + 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y + 1)
		                                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)))) {
//			            		grid.getCell()[x][y].setisChecking(false);
			            		return true;
	                        }
			            	
			            	/*grid.getCell()[x][y].setisChecking(false);
			            	robot.turn(RIGHT);
			            	robot.turn(LEFT);*/
	                    }
	        		}
				}
				break;
				default:
		}
		
		return false;
	}
	
	private boolean trustSensorAndBreakLoop(Robot robot, Grid grid, boolean realRun) {
		// Turn Right
		if(realRun)
			SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
        robot.turn(RIGHT);
        robotMovementString += "r";
        robot.sense(realRun, true);
        
        // Case 1
        if(robot.getFrontCenter() < 2) {
        	// Turn Right
        	if(realRun)
        		SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
            robot.turn(RIGHT);
            robotMovementString += "r";
            robot.sense(realRun, true);
            
            int countFreeSpaceOnLeft = 0;
            
            if(robot.getLeftFront() > 1) {
            	countFreeSpaceOnLeft++;
            }
            
            int prevLeftFront = 0;
            boolean turned = false;
            boolean moved = false;
            
            // Keep moving forward/turn until there are 3 free space on the left 
            while(countFreeSpaceOnLeft < 3) {
            	if(!robot.isObstacleInfront()) {
            		// Move forward
            		if(realRun)
            			SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                    robot.move();
                    robotMovementString += "m";
                    robot.sense(realRun, true);
                    moved = true;
            	} 
            	// Obstacle in front, make a turn
            	else {
            		turned = leftHugging(realRun, robot, grid);
        			
        			// A turn has been made 
        			if (turned) {
        				// reset the count since the robot direction has been changed
                		countFreeSpaceOnLeft = 0;
                		
        				// Actual Run
        				if (realRun) {
        					// uTurnHalt is used to detect that a U turn has been interfered
        					// No need to sense the surrounding if the U turn has been interfered
        		            if(uTurnHalt) {
        		            	uTurnHalt = false;
        		            } else {
        		            	// Sense the surrounding since a turn has been made
        		            	robot.sense(realRun, true);
        		            }
        				}
        				// Sense for simulator
        				else {
        					robot.sense(realRun, true);
        				}
        				
        				prevLeftFront = robot.getFrontLeft();
        			}
            	}
            	
            	if(!turned && moved) {
            		if(robot.getLeftFront() > 1) {
                    	countFreeSpaceOnLeft++;
                    } else {
                    	// reset the count since it is not consecutive anymore
                    	countFreeSpaceOnLeft = 0;
                    }
            		moved = false;
            	} else if(turned && moved) {
            		countFreeSpaceOnLeft = 0;
            		if(robot.getLeftFront() > 1) {
                    	countFreeSpaceOnLeft++;
                    }
                	
                	if(robot.getLeftBack() > 1) {
                        countFreeSpaceOnLeft++;
                    }
                		
                	if(prevLeftFront > 1) {
                        countFreeSpaceOnLeft++;
                    }
                	
                	turned = false;
                	moved = false;
            	} else if(!turned && !moved) {
            		System.out.println("Something went wrong... Stuck in (Case 1) while loop cannot move forward or make a turn.");
            		return false;
            	}
            }
            
            // Now there are 3 consecutive free spaces on the left, turn left and move forward to break the loop
            // Turn Left
            if(realRun)
            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
            robot.turn(LEFT);
            robotMovementString += "l";
            robot.sense(realRun);
            
        	// Move forward
            if(realRun)
            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
        	robot.move();
            robotMovementString += "m";
            robot.sense(realRun);
            
//            System.out.println("trustSensorAndBreakLoop() Case 1 finished executing.");
            return true;
        } 
        // Case 2
        else if(robot.getLeftFront() < 2) {
        	int countFreeSpaceOnLeft = 0;
            
            int prevLeftFront = 0;
            boolean turned = false;
            boolean moved = false;
            
            // Keep moving forward/turn until there are 3 free space on the left 
            while(countFreeSpaceOnLeft < 3) {
            	if(!robot.isObstacleInfront()) {
            		// Move forward
            		if(realRun)
            			SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                    robot.move();
                    robotMovementString += "m";
                    robot.sense(realRun, true);
                    moved = true;
            	} 
            	// Obstacle in front, make a turn
            	else {
            		turned = leftHugging(realRun, robot, grid);
        			
        			// A turn has been made 
        			if (turned) {
        				// reset the count since the robot direction has been changed
                		countFreeSpaceOnLeft = 0;
                		
        				// Actual Run
        				if (realRun) {
        					// uTurnHalt is used to detect that a U turn has been interfered
        					// No need to sense the surrounding if the U turn has been interfered
        		            if(uTurnHalt) {
        		            	uTurnHalt = false;
        		            } else {
        		            	// Sense the surrounding since a turn has been made
        		            	robot.sense(realRun, true);
        		            }
        				}
        				// Sense for simulator
        				else {
        					robot.sense(realRun, true);
        				}
        				
        				prevLeftFront = robot.getFrontLeft();
        			}
            	}
            	
            	if(!turned && moved) {
            		if(robot.getLeftFront() > 1) {
                    	countFreeSpaceOnLeft++;
                    } else {
                    	// reset the count since it is not consecutive anymore
                    	countFreeSpaceOnLeft = 0;
                    }
            		moved = false;
            	} else if(turned && moved) {
            		countFreeSpaceOnLeft = 0;
            		if(robot.getLeftFront() > 1) {
                    	countFreeSpaceOnLeft++;
                    }
                	
                	if(robot.getLeftBack() > 1) {
                        countFreeSpaceOnLeft++;
                    }
                		
                	if(prevLeftFront > 1) {
                        countFreeSpaceOnLeft++;
                    }
                	
                	turned = false;
                	moved = false;
            	} else if(!turned && !moved) {
            		System.out.println("Something went wrong... Stuck in (Case 2) while loop cannot move forward or make a turn.");
            		return false;
            	}
            }
            
            // Now there are 3 consecutive free spaces on the left, turn left and move forward to break the loop
            // Turn Left
            if(realRun)
            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
            robot.turn(LEFT);
            robotMovementString += "l";
            robot.sense(realRun);
            
        	// Move forward
            if(realRun)
            	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
        	robot.move();
            robotMovementString += "m";
            robot.sense(realRun);
            
//            System.out.println("trustSensorAndBreakLoop() Case 2 finished executing.");
            return true;
        }
        // Case 3
        else if(robot.getFrontLeft() < 2) {
        	int prevFrontLeft = robot.getFrontLeft();
        	int prevFrontRight = robot.getFrontRight();
        	int prevFrontCenter = robot.getFrontCenter();
        	
        	// Turn Right
        	if(realRun)
        		SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
            robot.turn(RIGHT);
            robotMovementString += "r";
            robot.sense(realRun, true);
            
            if(robot.getLeftBack() != prevFrontLeft) {
            	System.out.println("Sensor Reading for Previous Front Left and Current Left Back does not match.\n" 
            			+ "Previous Front Left->" + prevFrontLeft + "\nCurrent Left Back->" + robot.getLeftBack());
            	return false;
            } else {
            	if(robot.getLeftFront() != prevFrontRight) {
            		System.out.println("Sensor Reading for Previous Front Right and Current Left Front does not match.\n" 
                			+ "Previous Front Right->" + prevFrontRight + "\nCurrent Left Front->" + robot.getLeftFront());
            		return false;
            	} else {
                    int countFreeSpaceOnLeft = 0;
                    
                    if(robot.getLeftFront() > 1) {
                    	countFreeSpaceOnLeft++;
                    }
                	
                	if(robot.getLeftBack() > 1) {
                        countFreeSpaceOnLeft++;
                    }
                		
                	if(prevFrontCenter > 1) {
                        countFreeSpaceOnLeft++;
                    }
                	
                	int prevLeftFront = 0;
                    boolean turned = false;
                    boolean moved = false;
                    
                    // Keep moving forward/turn until there are 3 free space on the left 
                    while(countFreeSpaceOnLeft < 3) {
                    	if(!robot.isObstacleInfront()) {
                    		// Move forward
                    		if(realRun)
                    			SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                            robot.move();
                            robotMovementString += "m";
                            robot.sense(realRun, true);
                            moved = true;
                    	} 
                    	// Obstacle in front, make a turn
                    	else {
                    		turned = leftHugging(realRun, robot, grid);
                			
                			// A turn has been made 
                			if (turned) {
                				// reset the count since the robot direction has been changed
                        		countFreeSpaceOnLeft = 0;
                        		
                				// Actual Run
                				if (realRun) {
                					// uTurnHalt is used to detect that a U turn has been interfered
                					// No need to sense the surrounding if the U turn has been interfered
                		            if(uTurnHalt) {
                		            	uTurnHalt = false;
                		            } else {
                		            	// Sense the surrounding since a turn has been made
                		            	robot.sense(realRun, true);
                		            }
                				}
                				// Sense for simulator
                				else {
                					robot.sense(realRun, true);
                				}
                				
                				prevLeftFront = robot.getFrontLeft();
                			}
                    	}
                    	
                    	if(!turned && moved) {
                    		if(robot.getLeftFront() > 1) {
                            	countFreeSpaceOnLeft++;
                            } else {
                            	// reset the count since it is not consecutive anymore
                            	countFreeSpaceOnLeft = 0;
                            }
                    		moved = false;
                    	} else if(turned && moved) {
                    		countFreeSpaceOnLeft = 0;
                    		if(robot.getLeftFront() > 1) {
                            	countFreeSpaceOnLeft++;
                            }
                        	
                        	if(robot.getLeftBack() > 1) {
                                countFreeSpaceOnLeft++;
                            }
                        		
                        	if(prevLeftFront > 1) {
                                countFreeSpaceOnLeft++;
                            }
                        	
                        	turned = false;
                        	moved = false;
                    	} else if(!turned && !moved) {
                    		System.out.println("Something went wrong... Stuck in (Case 3) while loop cannot move forward or make a turn.");
                    		return false;
                    	}
                    }
                    
                    // Now there are 3 consecutive free spaces on the left, turn left and move forward to break the loop
                    // Turn Left
                    if(realRun)
                    	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                    robot.turn(LEFT);
                    robotMovementString += "l";
                    robot.sense(realRun);
                    
                	// Move forward
                    if(realRun)
                    	SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                	robot.move();
                    robotMovementString += "m";
                    robot.sense(realRun);
                    
//                    System.out.println("trustSensorAndBreakLoop() Case 3 finished executing.");
                    return true;
            	}
            }
        } else {
        	System.out.println("Not in any of the 3 scenarios.\nLast sensor readings: FL->" + robot.getFrontLeft() + ", FC->" 
        			+ robot.getFrontCenter() + ", FR->" + robot.getFrontRight() + ", LF->" + robot.getLeftFront() + ", LB->" 
        			+ robot.getLeftBack() + "\nRobot x:" + robot.getPositionX() + ", Robot y:" + robot.getPositionY() 
        			+ ", Robot Direction:" + robot.getDirection());
        	return false;
        }
	}
	
	private List<String> processActions(List<String> returnActions) {
		List<String> processedActions = new ArrayList<>();
		int moveCount = 0;
		int moveLimit = 20;
		
		for(int i=0; i < returnActions.size(); i++) {
			if(i < returnActions.size()-3) {
				if(returnActions.get(i).equals("M")) {
					moveCount++;
					
					// Move Limit reached
					if(moveCount == moveLimit) {
						// reset the moveCount
						moveCount = 0;
						processedActions.add("ZM0" + String.valueOf(moveLimit));
					} 
					// Checks if the next available element is M, 
					// if it is not M then add the current moveCount to the List
					else if(((i+1) < returnActions.size()) && !returnActions.get(i+1).equals("M")) {
						processedActions.add("ZM0" + String.valueOf(moveCount));
						moveCount = 0;
					}
				} else if(returnActions.get(i).equals("U")){
					processedActions.add("R");
					processedActions.add("R");
				} else {
					processedActions.add(returnActions.get(i));
				}
			} else {
				if(returnActions.get(i).equals("M")) {
					processedActions.add("ZM01");
				} else if(returnActions.get(i).equals("U")){
					processedActions.add("R");
					processedActions.add("R");
				} else {
					processedActions.add(returnActions.get(i));
				}
			}
			
		}
		
		return processedActions;
	}
	
	private void updateRobotOnMap(String action, Robot robot) {
		if(action.startsWith("ZM")) {
			String moveAmount = action.substring(2);
			int moveNum = Integer.parseInt(moveAmount);
			
			for(int i=0; i<moveNum; i++) {
				robot.move();
				robotMovementString += "m";
			}
		} else if(action.equals("R")) {
			robot.turn(RIGHT);
			robotMovementString += "r";
		} else if(action.equals("L")) {
			robot.turn(LEFT);
			robotMovementString += "l";
		}
	}
	
	private void refreshMap(Robot robot) {
		robot.turn(RIGHT);
		robot.turn(LEFT);
	}
	
	private static int getElapsedTime(final long start, final long end) {
		int time = 0;
		timeStr = "";
		DecimalFormat df = new DecimalFormat("#.##");
		double difference = (end - start) / 1000000000;
		int minHourDay = 0;
		
		// seconds
		if(difference > 60) {
			minHourDay++;
			// minutes
			difference /= 60;
		}
		
		switch(minHourDay) {
			case 1:	timeStr += (int) (difference) + "m";
					time += (int) (difference) *60;
					difference *= 60;
					difference %= 60;
			case 0: timeStr += df.format(difference) + "s";
					time += Integer.parseInt(df.format(difference));
		}
		
		
		/*if(str.equals("0s")) {
			System.out.println("<1s");
		} else {
			System.out.println(str);
		}*/
		
		return (time*1000);
	}
	
	private void executeLoopBreaker(Robot robot, Grid grid, boolean realRun) {
		// Using Nearest Neighbour to break the cycle
    	// Find out which zone the robot is in
    	int zoneNumber = getExploringZone(robot.getPositionX(), robot.getPositionY());
    	//System.out.println("Zone number is " + zoneNumber);
    	
    	boolean resultFound = false;
    	int count = 0;
    	// Loop through the 4 zones to look for unexplored cell starting from the zone the robot is in
    	while(!resultFound && count < 4) {
    		resultFound = checkZoneForUnexplored(zoneNumber, realRun, grid, robot, false);
    		if(resultFound) {
    			if(realRun) {
    				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
    			}
    			robot.turn(RIGHT);
    			// Only give delay for simulator 
                if(!realRun) {
                	stepTaken();
                }
    			robot.sense(realRun);
    			break;
    		}
    		
    		count++;
    		zoneNumber = (zoneNumber+1)%4;
    	}
    	
    	/*if(!resultFound) {
    		System.out.println("Nearest Neighbour Algorithm fails to find nearest cell.\nExecuting First Reachable Unexplored Algorithm");
    		resultFound = startFirstReachableUnexploredAlgo(realRun, grid, robot);
    		
    		if(!resultFound)
    			System.out.println("First Reachable Unexplored Algoritm fails to find a reachable unexplored path.");
    	}*/
    	
    	if(!resultFound) {
    		System.out.println("Nearest Neighbour Algorithm fails to find nearest cell.\nExecuting Trust Sensor And Break Loop Algorithm...");
    		resultFound = trustSensorAndBreakLoop(robot, grid, realRun);
    		
    		if(!resultFound) {
    			System.out.println("Trust Sensor And Break Loop Algorithm fails to break the loop.");
    			zoneNumber = getExploringZone(robot.getPositionX(), robot.getPositionY());
    			resultFound = checkZoneForUnexplored((zoneNumber+1)%4, realRun, grid, robot, true);
    			if(!resultFound) {
    				System.out.println("Nearest Reachable Explored Algorithm Failed");
    				
    				// Fastest Path back to Starting Point
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
                            String compressed = Algorithm.compressExplorationPath(returnActions, proxyRobot, false);
                        	//String compressed = Algorithm.compressExplorationCalibrationPath(returnActions, proxyRobot);
                            // Send the actions string to Arduino
                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressed);
                        }
                        
                        /*System.out.println("Fastest Path Algorithm back to Start Zone Calculated. Executing actions now...");
                        System.out.println(returnActions.toString());*/
                        
                        // Simulate the robot moving back to startZone step by step
                        for (String action : returnActions) {
                            if (action.equals("M")) {
                                robot.move();
                                robotMovementString += "m";
                            } else if (action.equals("L")) {
                                robot.turn(LEFT);
                                robotMovementString += "l";
                            } else if (action.equals("R")) {
                                robot.turn(RIGHT);
                                robotMovementString += "r";
                            } else if (action.equals("U")) {
                                robot.turn(RIGHT);
                                robotMovementString += "r";
                                robot.turn(RIGHT);
                                robotMovementString += "r";
                            }
                            
                            // Only give delay for simulator 
                            if(!realRun) {
                            	stepTaken();
                            }
                            // Update Android that there is a move or turn
                            // sendAndroid(grid, robot, realRun);
                        }
                        //refreshMap(robot);
                    }
                    else {
                        System.out.println("FASTEST PATH CANNOT BE FOUND!");
                    }
    			} else {
    				System.out.println("Nearest Reachable Explored Algorithm broke the loop");
    			}
    		} //else
    			System.out.println("Trust Sensor And Break Loop Algorithm broke the loop.");
    	} else {
    		System.out.println("Nearest Neighbour Algorithm breaks the loop");
    	}
	}
	
	private int calculateForward(boolean realRun, Robot robot, Grid grid) {
        int direction = robot.getDirection();
        int robotCurrentX = robot.getPositionX();
        int robotCurrentY = robot.getPositionY();
        
        int moveCount = 0;
        
        while(!checkObstacleInfront(grid, robotCurrentX, robotCurrentY, direction) && (!checkAbleToTurn(grid, robotCurrentX, robotCurrentY, direction))) {
        	moveCount++;
        	if(direction == NORTH) {
        		// Update the new robot Y position after move forward by 1
        		if(robotCurrentY == 1) {
        			break;
        		} else {
        			robotCurrentY--;
        			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == SOUTH) {
        		// Update the new robot Y position after move forward by 1
        		if(robotCurrentY == 18) {
        			break;
        		} else {
        			robotCurrentY++;
        			if(isInEndingZone(robotCurrentX, robotCurrentY+2)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == EAST) {
        		// Update the new robot X position after move forward by 1
        		if(robotCurrentX == 13) {
        			break;
        		} else {
        			robotCurrentX++;
        			if(isInEndingZone(robotCurrentX+2, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == WEST) {
        		// Update the new robot X position after move forward by 1
        		if(robotCurrentX == 1) {
        			break;
        		} else {
        			robotCurrentX--;
        			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	}
        }
        
        if(uTurn) {
        	uTurn = false;
        	moveCount--;
        }
        
		return moveCount;
	}
	
	// returns true if there is an obstacle in front of the robot
	public boolean checkObstacleInfront(Grid grid, int positionX, int positionY, int direction) { // DIRECTLY IN FRONT OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (direction == NORTH) {
            	if(grid.isOutOfArena(positionX + i, positionY - 1)) {
            		return true;
            	} else if(grid.getIsExplored(positionX + i, positionY - 1)) {
            		if (grid.getIsObstacle(positionX + i, positionY - 1)) {
                        return true;
                    }
            	} else {
            		return true;
            	}
            }
            else if (direction == SOUTH) {
            	if(grid.isOutOfArena(positionX + i, positionY + 3)) {
            		return true;
            	} else if(grid.getIsExplored(positionX + i, positionY + 3)) {
            		if (grid.getIsObstacle(positionX + i, positionY + 3)) {
                        return true;
                    }
            	} else {
            		return true;
            	}
            }
            else if (direction == EAST) {
            	if(grid.isOutOfArena(positionX + 3, positionY + i)) {
            		return true;
            	} else if(grid.getIsExplored(positionX + 3, positionY + i)) {
            		if (grid.getIsObstacle(positionX + 3, positionY + i)) {
                        return true;
                    }
            	} else {
            		return true;
            	}            
            }
            else if (direction == WEST) {
            	if(grid.isOutOfArena(positionX - 1, positionY + i)) {
            		return true;
            	} else if(grid.getIsExplored(positionX - 1, positionY + i)) {
            		if (grid.getIsObstacle(positionX - 1, positionY + i)) {
                        return true;
                    }
            	} else {
            		return true;
            	}
            }
        }
        
        // No obstacle in front
        if (direction == NORTH) {
//        	for(int i=-1; i>-3; i--) {
//        		if (grid.getIsExplored(positionX + i, positionY - 1)) {
//        			if(grid.getIsObstacle(positionX + i, positionY - 1))
//        			{
//        				break;
//        			}
//        			
//                }
//        		else {
//        			return true;
//        		}
//        	}
        		
        	for(int i=3; i<8; i++) {
        		if(grid.isOutOfArena(positionX + i, positionY -1)) {
            		return true;
            	} else if (grid.getIsExplored(positionX + i, positionY - 1)) {
        			if(grid.getIsObstacle(positionX + i, positionY - 1))
        			{
//        				break;
        				return false;
        			}
                }
        		else {
        			return true;
        		}
        	}
        }
        else if (direction == SOUTH) {
//        	for(int i=3; i<5; i++) {
//        		if (grid.getIsExplored(positionX + i, positionY + 3)) {
//        			if(grid.getIsObstacle(positionX + i, positionY + 3))
//        			{
//        				break;
//        			}
//        			
//                }
//        		else {
//        			return true;
//        		}
//        	}
        		
        	for(int i=-1; i>-6; i--) {
        		if(grid.isOutOfArena(positionX + i, positionY + 3)) {
            		return true;
            	} else if (grid.getIsExplored(positionX + i, positionY + 3)) {
        			if(grid.getIsObstacle(positionX + i, positionY + 3))
        			{
//        				break;
        				return false;
        			}
                }
        		else {
        			return true;
        		}
        	}
        }
        else if (direction == EAST) {
//        	for(int i=-1; i>-3; i--) {
//        		if (grid.getIsExplored(positionX + 3, positionY + i)) {
//        			if(grid.getIsObstacle(positionX + 3, positionY + i))
//        			{
//        				break;
//        			}
//        			
//                }
//        		else {
//        			return true;
//        		}
//        	}
        		
        	for(int i=3; i<8; i++) {
        		
        		if(grid.isOutOfArena(positionX + 3, positionY + i)) {
            		return true;
            	} else if (grid.getIsExplored(positionX + 3, positionY + i)) {
        			if(grid.getIsObstacle(positionX + 3, positionY + i))
        			{
//        				break;
        				return false;
        			}
                }
        		else {
        			return true;
        		}
        	}
        }
        else if (direction == WEST) {
//        	for(int i=3; i<5; i++) {
//        		if (grid.getIsExplored(positionX - 1, positionY + i)) {
//        			if(grid.getIsObstacle(positionX - 1, positionY + i))
//        			{
//        				break;
//        			}
//        			
//                }
//        		else {
//        			return true;
//        		}
//        	}
        		
        	for(int i=-1; i>-6; i--) {
        		if(grid.isOutOfArena(positionX - 1, positionY + i)) {
            		return true;
            	} else if (grid.getIsExplored(positionX - 1, positionY + i)) {
        			if(grid.getIsObstacle(positionX - 1, positionY + i))
        			{
//        				break;
        				return false;
        			}
                }
        		else {
        			return true;
        		}
        	}
        }
    
        return false;
    }
	
	// returns true if there is an obstacle in front of the robot
		public boolean checkObstacleInfront2(Grid grid, int positionX, int positionY, int direction) { // DIRECTLY IN FRONT OF ROBOT
	        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
	            if (direction == NORTH) {
	            	if(grid.isOutOfArena(positionX + i, positionY - 1)) {
	            		return true;
	            	} else if(grid.getIsExplored(positionX + i, positionY - 1)) {
	            		if (grid.getIsObstacle(positionX + i, positionY - 1)) {
	                        return true;
	                    }
	            	} else {
	            		return true;
	            	}
	            }
	            else if (direction == SOUTH) {
	            	if(grid.isOutOfArena(positionX + i, positionY + 3)) {
	            		return true;
	            	} else if(grid.getIsExplored(positionX + i, positionY + 3)) {
	            		if (grid.getIsObstacle(positionX + i, positionY + 3)) {
	                        return true;
	                    }
	            	} else {
	            		return true;
	            	}
	            }
	            else if (direction == EAST) {
	            	if(grid.isOutOfArena(positionX + 3, positionY + i)) {
	            		return true;
	            	} else if(grid.getIsExplored(positionX + 3, positionY + i)) {
	            		if (grid.getIsObstacle(positionX + 3, positionY + i)) {
	                        return true;
	                    }
	            	} else {
	            		return true;
	            	}            
	            }
	            else if (direction == WEST) {
	            	if(grid.isOutOfArena(positionX - 1, positionY + i)) {
	            		return true;
	            	} else if(grid.getIsExplored(positionX - 1, positionY + i)) {
	            		if (grid.getIsObstacle(positionX - 1, positionY + i)) {
	                        return true;
	                    }
	            	} else {
	            		return true;
	            	}
	            }
	        }
	        
	        return false;
	    }
	
	// return true if it is able to turn
	private boolean checkAbleToTurn(Grid grid, int robotCurrentX, int robotCurrentY, int direction) {
		// Create a proxyRobot for the calculation so that it will not affect the original robot
        Robot proxyRobot = new Robot(grid, new ArrayList<>());
        
        // Initialize the proxyRobot with the current conditions of the original robot
        proxyRobot.setPositionX(robotCurrentX);
        proxyRobot.setPositionY(robotCurrentY);
        proxyRobot.setDirection(direction);
        
        
		if(proxyRobot.isObstacleInfront()) {
			if(proxyRobot.isObstacleOnLeftSide() && proxyRobot.isObstacleOnRightSide()) {
				// Able to make U-Turn
				uTurn = true;
				return true;
			} else if(proxyRobot.isObstacleOnLeftSide()) {
				// Able to make a Right Turn
				return true;
			} else {
				// Able to make a Left Turn
				return true;
			}
		} else if(!proxyRobot.isObstacleOnLeftSide()) {
			// Able to make a Left Turn
            return true;
		}
		
		return false;
	}
	
	private boolean checkUTurnAhead(Grid grid, Robot robot) {
		// Create a proxyRobot for the calculation so that it will not affect the original robot
        Robot proxyRobot = new Robot(grid, new ArrayList<>());
        
        int currentXPos = robot.getPositionX();
        int currentYPos = robot.getPositionY();
        
        if(robot.isObstacleInfront()) {
        	return false;
        } else {
        	// Simulate that the robot has moved 1 step forward, check if a U Turn is possible
            if(robot.getDirection() == NORTH) {
        		// Update the new robot Y position after move forward by 1
        		currentYPos--;
        	} else if(robot.getDirection() == SOUTH) {
        		// Update the new robot Y position after move forward by 1
        		currentYPos++;
        	} else if(robot.getDirection() == EAST) {
        		// Update the new robot X position after move forward by 1
        		currentXPos++;
        	} else if(robot.getDirection() == WEST) {
        		// Update the new robot X position after move forward by 1
        		currentXPos--;
        	}
        }
        
        
        // If after moving forward and robot is not out of Arena then we check if it is able to U-Turn
        if(robot.getDirection() == EAST || robot.getDirection() == SOUTH)
        {
        	if(!isOutOfArena(currentXPos, currentYPos)) {
            	// Initialize the proxyRobot with the conditions whereby it has moved 1 step forward
                proxyRobot.setPositionX(currentXPos);
                proxyRobot.setPositionY(currentYPos);
                proxyRobot.setDirection(robot.getDirection());
                
                if(checkRightRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection()) && checkLeftRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection())) {
                	if(proxyRobot.isObstacleInfront()) {
            			if(proxyRobot.isObstacleOnLeftSide() && proxyRobot.isObstacleOnRightSide()) {
            				// Able to make U-Turn
            				return true;
            			}
                    } else {
                    	if(robot.isObstacleInfront()) {
                        	return false;
                        } else {
                        	// Simulate that the robot has moved 1 step forward, check if a U Turn is possible
                        	if(robot.getDirection() == SOUTH) {
                        		// Update the new robot Y position after move forward by 1
                        		currentYPos++;
                        	} else if(robot.getDirection() == EAST) {
                        		// Update the new robot X position after move forward by 1
                        		currentXPos++;
                        	}
                        }
                        
                        // If after moving forward and robot is not out of Arena then we check if it is able to U-Turn
                    	if(!isOutOfArena(currentXPos, currentYPos)) {
                        	// Initialize the proxyRobot with the conditions whereby it has moved 1 step forward
                            proxyRobot.setPositionX(currentXPos);
                            proxyRobot.setPositionY(currentYPos);
                            proxyRobot.setDirection(robot.getDirection());
                            
                            if(checkRightRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection()) && checkLeftRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection())) {
                            	if(proxyRobot.isObstacleInfront()) {
                        			if(proxyRobot.isObstacleOnLeftSide() && proxyRobot.isObstacleOnRightSide()) {
                        				// Able to make U-Turn
                        				return true;
                        			}
                                }
                            }
                    	}	
                    }
                }
            }
        }
        else
        {
        	if(!grid.isOutOfArena(currentXPos, currentYPos)) {
            	// Initialize the proxyRobot with the conditions whereby it has moved 1 step forward
                proxyRobot.setPositionX(currentXPos);
                proxyRobot.setPositionY(currentYPos);
                proxyRobot.setDirection(robot.getDirection());
                
                if(checkRightRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection()) && checkLeftRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection())) {
                	if(proxyRobot.isObstacleInfront()) {
            			if(proxyRobot.isObstacleOnLeftSide() && proxyRobot.isObstacleOnRightSide()) {
            				// Able to make U-Turn
            				return true;
            			}
                    } else {
                    	
                    	if(robot.isObstacleInfront()) {
                        	return false;
                        } else {
                        	// Simulate that the robot has moved 1 step forward, check if a U Turn is possible
                            if(robot.getDirection() == NORTH) {
                        		// Update the new robot Y position after move forward by 1
                        		currentYPos--;
                        	} else if(robot.getDirection() == WEST) {
                        		// Update the new robot X position after move forward by 1
                        		currentXPos--;
                        	}
                        }
                        
                        // If after moving forward and robot is not out of Arena then we check if it is able to U-Turn
                        if(!grid.isOutOfArena(currentXPos, currentYPos)) {
                        	// Initialize the proxyRobot with the conditions whereby it has moved 1 step forward
                            proxyRobot.setPositionX(currentXPos);
                            proxyRobot.setPositionY(currentYPos);
                            proxyRobot.setDirection(robot.getDirection());
                            
                            if(checkRightRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection()) && checkLeftRangeExplored(grid, proxyRobot.getPositionX(), proxyRobot.getPositionY(), proxyRobot.getDirection())) {
                            	if(proxyRobot.isObstacleInfront()) {
                        			if(proxyRobot.isObstacleOnLeftSide() && proxyRobot.isObstacleOnRightSide()) {
                        				// Able to make U-Turn
                        				return true;
                        			}
                                }
                            }
                    	}	
                    }
                }
            }
        }
        
		return false;
	}
	
	public boolean isOutOfArena(int x, int y) {
        return x < 0 || y < 0 || x >= 13 || y >= 18;
    }
	
	private boolean rightSideNotFullyExplored(Grid grid, Robot robot) {
		
		int count = 0;
		
		for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (robot.getDirection() == NORTH) {
                if (grid.isOutOfArena(robot.getPositionX() + 3, robot.getPositionY() + i) || grid.getIsExplored(robot.getPositionX() + 3, robot.getPositionY() + i)) {
                	count++;
                }
            }
            else if (robot.getDirection() == SOUTH) {
                if (grid.isOutOfArena(robot.getPositionX() - 1, robot.getPositionY() + i) || grid.getIsExplored(robot.getPositionX() - 1, robot.getPositionY() + i)) {
                	count++;
                }
            }
            else if (robot.getDirection() == EAST) {
                if (grid.isOutOfArena(robot.getPositionX() + i, robot.getPositionY() + 3) || grid.getIsExplored(robot.getPositionX() + i, robot.getPositionY() + 3)) {
                	count++;
                }
            }
            else if (robot.getDirection() == WEST) {
                if (grid.isOutOfArena(robot.getPositionX() + i, robot.getPositionY() - 1) || grid.getIsExplored(robot.getPositionX() + i, robot.getPositionY() - 1)) {
                	count++;
                }
            }
        }
		
		if(count == 3) {
			return false;
		}
		
		return true;
	}
	
	private boolean leftSideNotFullyExplored(Grid grid, Robot robot) {
		int count = 0;
		
		for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (robot.getDirection() == NORTH) {
                if (grid.isOutOfArena(robot.getPositionX() -1, robot.getPositionY() + i) || grid.getIsExplored(robot.getPositionX() -1, robot.getPositionY() + i)) {
                	count++;
                }
            }
            else if (robot.getDirection() == SOUTH) {
                if (grid.isOutOfArena(robot.getPositionX() +3, robot.getPositionY() + i) || grid.getIsExplored(robot.getPositionX() +3, robot.getPositionY() + i)) {
                	count++;
                }
            }
            else if (robot.getDirection() == EAST) {
                if (grid.isOutOfArena(robot.getPositionX() + i, robot.getPositionY() -1) || grid.getIsExplored(robot.getPositionX() + i, robot.getPositionY() -1)) {
                	count++;
                }
            }
            else if (robot.getDirection() == WEST) {
                if (grid.isOutOfArena(robot.getPositionX() + i, robot.getPositionY() +3) || grid.getIsExplored(robot.getPositionX() + i, robot.getPositionY() +3)) {
                	count++;
                }
            }
        }
		
		if(count == 3) {
			return false;
		}
		
		return true;
	}
	
	public boolean isInEndingZone(int x, int y){
		return (y < ZONE_SIZE) && (y >= 0) && (x < MAP_COLUMNS) && (x >= MAP_COLUMNS - ZONE_SIZE);
                
	}
	
	private void handleMoveForward(Grid grid, Robot robot, boolean realRun) {
		if(trustExplored) {
			/*// Checks if need to do U-Turn in front
			if(checkUTurnAhead(grid, robot)) {
				SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
				robot.turn(RIGHT);
				robot.turn(RIGHT);
				
				int numberOfSteps = checkFrontExplored(grid, robot);
				// numberOfSteps: 0
				if(numberOfSteps == 0) {
					justTurned = false;
					System.out.println("Cannot move forward. Obstacles in front");
				} 
				// numberOfSteps: 1-9
				else if(numberOfSteps < 10) {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZM0" + String.valueOf(numberOfSteps));
					justTurned = false;
				}
				// numberOfSteps: 10-17
				else {
					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZM" + String.valueOf(numberOfSteps));
					justTurned = false;
				}
				
				// Update the simulator
				if(numberOfSteps != 0) {
					for(int i=0; i<numberOfSteps; i++) {
						robotMovementString+="M";
						robot.move();
					}
				}
				
				// Sense the surrounding
				robot.sense(realRun, true);
				
				// Update Android when there is a move forward
				sendAndroid(grid, robot, realRun);
//				handleMoveForward(grid, robot, realRun);
				
//				if(robot.isObstacleOnRightSide()) {
//					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
//					robot.turn(LEFT);
//					robot.sense(realRun, true);
//				} else if(robot.isObstacleOnLeftSide()) {
//					SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
//					robot.turn(RIGHT);
//					robot.sense(realRun, true);
//				}
			}*/ 
			// No need to do U-Turn in front, go ahead and move forward
			//else {
//				if(justTurned) {
					int numberOfSteps = checkFrontExplored(grid, robot);
					// numberOfSteps: 0
					if(numberOfSteps == 0) {
						System.out.println("Cannot move forward. Obstacles in front");
					} 
					// numberOfSteps: 1-9
					else if(numberOfSteps < 10) {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZM0" + String.valueOf(numberOfSteps));
					}
					// numberOfSteps: 10-17
					else {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZM" + String.valueOf(numberOfSteps));
					}
					
					// Update the simulator
					if(numberOfSteps != 0) {
						for(int i=0; i<numberOfSteps; i++) {
							robotMovementString+="M";
							robot.move();
						}
					}
					
					// Sense the surrounding
					robot.sense(realRun, true);
					
					// Update Android when there is a move forward
					sendAndroid(grid, robot, realRun);
					
					// Checks if need to do U-Turn in front
					/*if(checkUTurnAhead(grid, robot)) {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
						robot.turn(RIGHT);
						robot.turn(RIGHT);
						robot.sense(realRun, true);
						
						numberOfSteps = 0;
						numberOfSteps = checkFrontExplored(grid, robot);
						// numberOfSteps: 0
						if(numberOfSteps == 0) {
							System.out.println("Cannot move forward. Obstacles in front");
						} 
						// numberOfSteps: 1-9
						else if(numberOfSteps < 10) {
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZM0" + String.valueOf(numberOfSteps));
						}
						// numberOfSteps: 10-17
						else {
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "ZM" + String.valueOf(numberOfSteps));
						}
						
						// Update the simulator
						if(numberOfSteps != 0) {
							for(int i=0; i<numberOfSteps; i++) {
								robotMovementString+="M";
								robot.move();
							}
						}
						
						// Sense the surrounding
						robot.sense(realRun, true);
						
						// Update Android when there is a move forward
						sendAndroid(grid, robot, realRun);
//						handleMoveForward(grid, robot, realRun);
						
//						if(robot.isObstacleOnRightSide()) {
//							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
//							robot.turn(LEFT);
//							robot.sense(realRun, true);
//						} else if(robot.isObstacleOnLeftSide()) {
//							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
//							robot.turn(RIGHT);
//							robot.sense(realRun, true);
//						}
					}*/

					
//					if(numberOfSteps > 1) {
//						if(!isInEndingZone(robot.getPositionX(), robot.getPositionY())) {
//							int zoneNumber = getExploringZone(robot.getPositionX(), robot.getPositionY());
//							if(zoneNumber == 2 || zoneNumber == 3) {
//								if(robot.getDirection() != EAST) {
//									if(rightSideNotFullyExplored(grid, robot)) {
//										SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
//										robot.turn(RIGHT);
//										robot.sense(realRun);
//										
//										SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
//										robot.turn(LEFT);
//										robot.sense(realRun, true);
//										
//									}
//								}
//							} else {
//								if(rightSideNotFullyExplored(grid, robot)) {
//									SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
//									robot.turn(RIGHT);
//									robot.sense(realRun);
//									
//									SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
//									robot.turn(LEFT);
//									robot.sense(realRun, true);
//									
//								}
//							}
//						}
//					}
				/*} else {
					// Sensor readings show there is obstacles in front of robot; DO NOT MOVE FORWARD
					if(robot.getFrontCenter() < 2 || robot.getFrontLeft() < 2 || robot.getFrontRight() < 2) {
						// Simulator shows there is no obstacle in front
						if(!robot.isObstacleInfront()) {
							System.out.println("Sensor readings conflict with simulator. Robot is prevented from moving forward.");
							System.out.println("Sensor readings: FL: " + robot.getFrontLeft() + ", FC: " + robot.getFrontCenter() + ", FR: " + robot.getFrontRight());
							// Sense again
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
							robot.turn(RIGHT);
							robot.sense(realRun);
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
							robot.turn(LEFT);
							robot.sense(realRun, true);
						}
					}
					// Sensor readings show there is NO obstacles in front of robot
					else {
						// Simulator shows there is obstacle(s) in front
						if(robot.isObstacleInfront()) {
							System.out.println("Sensor readings conflict with simulator. Robot is prevented from moving forward.");
							System.out.println("Sensor readings: FL: " + robot.getFrontLeft() + ", FC: " + robot.getFrontCenter() + ", FR: " + robot.getFrontRight());
							
							// Sense again to correct the front using the left sensors
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
							robot.turn(RIGHT);
							robot.sense(realRun);
							// Sense again to correct the front using the front sensors
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
							robot.turn(LEFT);
							robot.sense(realRun, true);
						}
						// No conflict move forward
						else {
							SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
							//robot.setDetectNextMove(false);
							robotMovementString+="M";
							System.out.println("----------------------Moving Forward----------------------");
							System.out.println(robotMovementString);
							
							// show the robot move forward on the simulator
							robot.move();
							
							

							// Sense the surrounding
							robot.sense(realRun, true);
							
							
							// Update Android when there is a move forward
							sendAndroid(grid, robot, realRun);
							
						}
					}
				}*/
			//}
		} 
		// I do not trust the area I have already explored. Only move 1 step at a time
		else {
			if(uTurnFlagForNoTrustExplored) {
				uTurnFlagForNoTrustExplored = false;
			} else {
				// Sensor readings show there is obstacles in front of robot; DO NOT MOVE FORWARD
				if(robot.getFrontCenter() < 2 || robot.getFrontLeft() < 2 || robot.getFrontRight() < 2) {
					// Simulator shows there is no obstacle in front
					if(!robot.isObstacleInfront()) {
						/*System.out.println("Sensor readings conflict with simulator. Robot is prevented from moving forward.");
						System.out.println("Sensor readings: FL: " + robot.getFrontLeft() + ", FC: " + robot.getFrontCenter() + ", FR: " + robot.getFrontRight());*/
						// Sense again
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
						robot.turn(RIGHT);
						robot.sense(realRun);
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
						robot.turn(LEFT);
						robot.sense(realRun, true);
					}
				}
				// Sensor readings show there is NO obstacles in front of robot
				else {
					// Simulator shows there is obstacle(s) in front
					if(robot.isObstacleInfront()) {
						/*System.out.println("Sensor readings conflict with simulator. Robot is prevented from moving forward.");
						System.out.println("Sensor readings: FL: " + robot.getFrontLeft() + ", FC: " + robot.getFrontCenter() + ", FR: " + robot.getFrontRight());*/
						
						// Sense again to correct the front using the left sensors
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
						robot.turn(RIGHT);
						robot.sense(realRun);
						// Sense again to correct the front using the front sensors
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
						robot.turn(LEFT);
						robot.sense(realRun, true);
					}
					// No conflict move forward
					else {
						SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
						//robot.setDetectNextMove(false);
						robotMovementString+="M";
						/*System.out.println("----------------------Moving Forward----------------------");
						System.out.println(robotMovementString);*/
						
						// show the robot move forward on the simulator
						robot.move();
						
						// Sense the surrounding
						robot.sense(realRun, true);
						
						// Update Android when there is a move forward
						sendAndroid(grid, robot, realRun);
						
						
					}
				}
			}
		}		
	}
	
	public int checkFrontExplored(Grid grid, Robot robot) {
		int direction = robot.getDirection();
        int robotCurrentX = robot.getPositionX();
        int robotCurrentY = robot.getPositionY();
        
        int moveCount = 1;
        
        if(direction == NORTH) {
    		// Update the new robot Y position after move forward by 1
    		if(robotCurrentY == 1) {
    			return moveCount;
    		} else {
    			robotCurrentY--;
//    			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	} else if(direction == SOUTH) {
    		// Update the new robot Y position after move forward by 1
    		if(robotCurrentY == 18) {
    			return moveCount;
    		} else {
    			robotCurrentY++;
//    			if(isInEndingZone(robotCurrentX, robotCurrentY+2)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	} else if(direction == EAST) {
    		// Update the new robot X position after move forward by 1
    		if(robotCurrentX == 13) {
    			return moveCount;
    		} else {
    			robotCurrentX++;
//    			if(isInEndingZone(robotCurrentX+2, robotCurrentY)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	} else if(direction == WEST) {
    		// Update the new robot X position after move forward by 1
    		if(robotCurrentX == 1) {
    			return moveCount;
    		} else {
    			robotCurrentX--;
//    			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	}
        
        while(!checkObstacleInfront(grid, robotCurrentX, robotCurrentY, direction) && (!checkAbleToTurn(grid, robotCurrentX, robotCurrentY, direction)) && checkLeftRangeExplored(grid, robotCurrentX, robotCurrentY, direction) && checkRightRangeExplored(grid, robotCurrentX, robotCurrentY, direction)) {
        	
        	moveCount++;
        	if(direction == NORTH) {
        		// Update the new robot Y position after move forward by 1
        		if(robotCurrentY == 1) {
        			break;
        		} else {
        			robotCurrentY--;
        			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == SOUTH) {
        		// Update the new robot Y position after move forward by 1
        		if(robotCurrentY == 18) {
        			break;
        		} else {
        			robotCurrentY++;
        			if(isInEndingZone(robotCurrentX, robotCurrentY+2)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == EAST) {
        		// Update the new robot X position after move forward by 1
        		if(robotCurrentX == 13) {
        			break;
        		} else {
        			robotCurrentX++;
        			if(isInEndingZone(robotCurrentX+2, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == WEST) {
        		// Update the new robot X position after move forward by 1
        		if(robotCurrentX == 1) {
        			break;
        		} else {
        			robotCurrentX--;
        			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	}
        }
        
//        if(uTurn) {
//        	uTurn = false;
//        	moveCount--;
//        }
        
		return moveCount;
	}
	
	public int checkFrontExplored2(Grid grid, Robot robot) {
		int direction = robot.getDirection();
        int robotCurrentX = robot.getPositionX();
        int robotCurrentY = robot.getPositionY();
        
        int moveCount = 1;
        
        if(direction == NORTH) {
    		// Update the new robot Y position after move forward by 1
    		if(robotCurrentY == 1) {
    			return moveCount;
    		} else {
    			robotCurrentY--;
//    			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	} else if(direction == SOUTH) {
    		// Update the new robot Y position after move forward by 1
    		if(robotCurrentY == 18) {
    			return moveCount;
    		} else {
    			robotCurrentY++;
//    			if(isInEndingZone(robotCurrentX, robotCurrentY+2)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	} else if(direction == EAST) {
    		// Update the new robot X position after move forward by 1
    		if(robotCurrentX == 13) {
    			return moveCount;
    		} else {
    			robotCurrentX++;
//    			if(isInEndingZone(robotCurrentX+2, robotCurrentY)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	} else if(direction == WEST) {
    		// Update the new robot X position after move forward by 1
    		if(robotCurrentX == 1) {
    			return moveCount;
    		} else {
    			robotCurrentX--;
//    			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
//            		moveCount = moveCount +2;
//            		return moveCount;
//            	}
    		}
    	}
        
        while(!checkObstacleInfront2(grid, robotCurrentX, robotCurrentY, direction) && (!checkAbleToTurn(grid, robotCurrentX, robotCurrentY, direction)) && checkLeftRangeExplored(grid, robotCurrentX, robotCurrentY, direction) && checkRightRangeExplored(grid, robotCurrentX, robotCurrentY, direction)) {
        	
        	moveCount++;
        	if(direction == NORTH) {
        		// Update the new robot Y position after move forward by 1
        		if(robotCurrentY == 1) {
        			break;
        		} else {
        			robotCurrentY--;
        			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == SOUTH) {
        		// Update the new robot Y position after move forward by 1
        		if(robotCurrentY == 18) {
        			break;
        		} else {
        			robotCurrentY++;
        			if(isInEndingZone(robotCurrentX, robotCurrentY+2)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == EAST) {
        		// Update the new robot X position after move forward by 1
        		if(robotCurrentX == 13) {
        			break;
        		} else {
        			robotCurrentX++;
        			if(isInEndingZone(robotCurrentX+2, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	} else if(direction == WEST) {
        		// Update the new robot X position after move forward by 1
        		if(robotCurrentX == 1) {
        			break;
        		} else {
        			robotCurrentX--;
        			if(isInEndingZone(robotCurrentX, robotCurrentY)) {
                		moveCount = moveCount +2;
                		break;
                	}
        		}
        	}
        }
        
//        if(uTurn) {
//        	uTurn = false;
//        	moveCount--;
//        }
        
		return moveCount;
	}
	
	private boolean checkLeftRangeExplored(Grid grid, int robotXPos, int robotYPos, int direction) {
		if(direction == NORTH) {
			for(int x=-1; x>-3; x--) {
				if(!grid.isOutOfArena(robotXPos+x, robotYPos)) {
					if(!grid.getIsExplored(robotXPos+x, robotYPos)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos+x, robotYPos)) {
						return true;
					}
				} else {
					return true;
				}
			}
		} else if(direction == SOUTH) {
			for(int x=3; x<5; x++) {
				if(!grid.isOutOfArena(robotXPos+x, robotYPos+2)) {
					if(!grid.getIsExplored(robotXPos+x, robotYPos+2)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos+x, robotYPos+2)) {
						return true;
					}
				} else {
					return true;
				}
			}
		} else if(direction == EAST) {
			for(int y=-1; y>-3; y--) {
				if(!grid.isOutOfArena(robotXPos+2, robotYPos+y)) {
					if(!grid.getIsExplored(robotXPos+2, robotYPos+y)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos+2, robotYPos+y)) {
						return true;
					}
				} else {
					return true;
				}
			}
		} else if(direction == WEST) {
			for(int y=3; y<5; y++) {
				if(!grid.isOutOfArena(robotXPos, robotYPos+y)) {
					if(!grid.getIsExplored(robotXPos, robotYPos+y)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos, robotYPos+y)) {
						return true;
					}
				} else {
					return true;
				}
			}
		}
		
		return true;
	}
	
	private boolean checkRightRangeExplored(Grid grid, int robotXPos, int robotYPos, int direction) {
		if(direction == NORTH) {
			for(int x=3; x<8; x++) {
				if(!grid.isOutOfArena(robotXPos+x, robotYPos)) {
					if(!grid.getIsExplored(robotXPos+x, robotYPos)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos+x, robotYPos)) {
						return true;
					}
				} else {
					return true;
				}
			}
		} else if(direction == SOUTH) {
			for(int x=-1; x>-6; x--) {
				if(!grid.isOutOfArena(robotXPos+x, robotYPos+2)) {
					if(!grid.getIsExplored(robotXPos+x, robotYPos+2)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos+x, robotYPos+2)) {
						return true;
					}
				} else {
					return true;
				}
			}
		} else if(direction == EAST) {
			for(int y=3; y<8; y++) {
				if(!grid.isOutOfArena(robotXPos+2, robotYPos+y)) {
					if(!grid.getIsExplored(robotXPos+2, robotYPos+y)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos+2, robotYPos+y)) {
						return true;
					}
				} else {
					return true;
				}
			}
		} else if(direction == WEST) {
			for(int y=-1; y>-6; y--) {
				if(!grid.isOutOfArena(robotXPos, robotYPos+y)) {
					if(!grid.getIsExplored(robotXPos, robotYPos+y)) {
						return false;
					} else if(grid.getIsObstacle(robotXPos, robotYPos+y)) {
						return true;
					}
				} else {
					return true;
				}
			}
		}
		
		return true;
	}
	
	// return true if it needs a U turn after 1 step
	private boolean check1StepUturn(Grid grid, Robot robot) {
		robot.move();
		
		if(robot.isObstacleInfront() && robot.isObstacleOnLeftSide() && robot.isObstacleOnRightSide()) {
			return true;
		}
		return false;
	}
}