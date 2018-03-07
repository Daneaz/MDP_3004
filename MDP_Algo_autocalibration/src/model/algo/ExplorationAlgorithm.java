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

	private int robotSpeed;
	private static final int START_X_POSITION = 0;
    private static final int START_Y_POSITION = 17;
	private static final int CALIBRATION_LIMIT = 5;
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
        
        System.out.println("EXPLORATION ENDED!");
        
        // SELECT EITHER ONE OF THE METHODS TO RUN ALGORITHMS.
        runExplorationAlgorithm(realRun, robot, grid);

        // CALIBRATION AFTER EXPLORATION
        //calibrateAndTurn(realRun, robot);

        // GENERATE MAP DESCRIPTOR, SEND TO ANDROID
        String part1 = grid.generateMapDescriptor1();
        String part2 = grid.generateMapDescriptor2();
        SocketMgr.getInstance().sendMessage(CALL_ANDROID, MessageMgr.generateFinalDescriptor(part1, part2));
		
	}

	private void calibrateAndTurn(boolean realRun, Robot robot) {
		// TODO Auto-generated method stub
		 if (realRun) {
	            while (robot.getDirection() != SOUTH) {
	                robot.turn(LEFT);
	                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	            }
	            //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	            //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	        }
		
	}

	private void runExplorationAlgorithm(boolean realRun, Robot robot, Grid grid) {
		// TODO Auto-generated method stub
		boolean inStartZone = false;
		boolean inEndZone = false;
		
		int calibrationCount = 0;
		
		robot.sense(realRun);
		
		if (realRun)
            SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                            robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
                            
		
		
		while(!inStartZone || !inEndZone){
			
			boolean turn = leftHugging(realRun, robot, grid);
			
			
			 /*if (turn) {
	                // CALIBRATION
	                if (realRun) {
	                	calibrationCount++;

	                    if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
	                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	                        calibrationCount = 0;
	                    } else if (robot.ableToCalibrateFront()) {
	                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	                        calibrationCount = 0;
	                    } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
	                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
	                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
	                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
	                        calibrationCount = 0;
	                    }
	                }

	                // SENSE AFTER CALIBRATION
	                senseAndUpdateAndroid(realRun, robot, grid);
	            }*/
			
			
			if (realRun)
                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
			
            robot.move();
            stepTaken();
            
            /*if (realRun) {
                calibrationCount++;
                
                if (robot.ableToCalibrateFront()) {
                    //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                    calibrationCount = 0;
                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                    //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                    calibrationCount = 0;
                }
            } */
            
            senseAndUpdateAndroid(realRun, robot, grid);
            
            if(Grid.isInEndingZone(robot.getPositionX(), robot.getPositionY())) { 
            	inEndZone = true;
            }
            
            if(Grid.isInStartingZone(robot.getPositionX() +2, robot.getPositionY()) && inEndZone){
            	inStartZone = true;
            }
            
            
            
            if(!inStartZone && grid.checkPercentageExplored() == 100){
                Robot proxyRobot = new Robot(grid, new ArrayList<>());                
                proxyRobot.setDirection(robot.getDirection());
                proxyRobot.setPositionX(robot.getPositionX());
                proxyRobot.setPositionY(robot.getPositionY());
                                
                List<String> returnActions = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), START_X_POSITION, START_Y_POSITION, grid, proxyRobot);                
                
                if (returnActions != null) {
                    System.out.println("Algorithm is done, executing actions now");
                    System.out.println(returnActions.toString());
                                        
                    if (realRun) {                    	
                    	proxyRobot.setDirection(robot.getDirection());
                		proxyRobot.setPositionX(robot.getPositionX());
                		proxyRobot.setPositionY(robot.getPositionY());                               
                		String compressed = Algorithm.compressExplorationPath(returnActions, proxyRobot);
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressed);
                    }

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
                        
                        if (realRun)
                            SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                                            robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
                        stepTaken();
                    }
                } 
                
                else {
                    System.out.println("FASTEST PATH CANNOT BE FOUND!");
                }

                if (Grid.isInStartingZone(robot.getPositionX() + 2, robot.getPositionY()) && inEndZone) {
                    inStartZone = true;
                }
            }
		}
		
		if (realRun)
            SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                            robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
		
		//Second exploration if robot is back to starting zone and exploration != to 100%
         
		 // Saving explored grid into a new grid to check and compare with new grid later 
        Grid firstRunExplored = new Grid();
        for (int x = 0; x < MAP_COLUMNS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {    	
            	firstRunExplored.setIsObstacle(x, y, grid.getIsObstacle(x, y));
            	firstRunExplored.setIsExplored(x, y, grid.getIsExplored(x, y));
            }
        }
        
        
        //checking for reachable cells in the arena.
        if(grid.checkPercentageExplored() < 100.0){ // CHECK FOR UNEXPLORED CELLS
            System.out.println("NOT FULLY EXPLORED, RUNNING SECOND EXPLORATION!");
            for (int y = MAP_ROWS - 1; y >= 0; y--) {
                for (int x = MAP_COLUMNS - 1; x >= 0; x--) {
                	
                	//check for unexplored cells and if neighbors are reachable.
                	
                    if (!grid.getIsExplored(x, y) &&
				            		((checkUnexploredCell(realRun, grid, robot, x, y + 1)
				                    || checkUnexploredCell(realRun, grid, robot, x, y - 1)
				                    || checkUnexploredCell(realRun, grid, robot, x + 1, y)
				                    || checkUnexploredCell(realRun, grid, robot, x - 1, y)))) {
                    	
                    	
                    	// set isInStartPoint to true so that lefthugging can be initiated.
                        boolean isInStartPoint = true;
                        
                        while (isInStartPoint) { 
                        	// set isInStartPoint back to false because robot is not in starting point.
                        	isInStartPoint = false; 
                            boolean turned = leftHugging(realRun, robot, grid);

                            /*if (turned) {
                                // CALIBRATION
                                if (realRun) {
                                	calibrationCount++;
                                    // IF CAN CALIBRATE FRONT, TAKE THE OPPORTUNITY
                                    // OTHERWISE CALIBRATE LEFT
                                    if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                        calibrationCount = 0;
                                    } else if (robot.ableToCalibrateFront()) {
                                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                        calibrationCount = 0;
                                    } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                                        calibrationCount = 0;
                                    }
                                }

                                senseAndUpdateAndroid(realRun, robot, grid);
                            }*/

                            if (realRun)
                                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                            robot.move();
                            stepTaken();

                            // CALIBRATION
                            /*if (realRun) {
                            	calibrationCount++;
                                // IF CAN CALIBRATE FRONT, TAKE THE OPPORTUNITY
                                // OTHERWISE CALIBRATE LEFT
                                if (robot.ableToCalibrateFront()) {
                                    //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                    calibrationCount = 0;
                                } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                                    //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                                    calibrationCount = 0;
                                }
                            }*/


                            senseAndUpdateAndroid(realRun, robot, grid);

                            if (grid.checkPercentageExplored() == 100) { 
                                break;
                            }

                            while (grid.getIsExplored(robot.getPositionX(), robot.getPositionY()) != firstRunExplored.getIsExplored(robot.getPositionX(), robot.getPositionY())) {
                                if (grid.checkPercentageExplored() == 100) { 
                                    break;
                                }
                                
                                //turned = leftHugging(realRun, robot, grid);
                                leftHugging(realRun, robot, grid);
                 	            /*if (turned) {
                                    // CALIBRATION
                                    if (realRun) {
                                    	calibrationCount++;
                                        // IF CAN CALIBRATE FRONT, TAKE THE OPPORTUNITY
                                        // OTHERWISE CALIBRATE LEFT
                                        if (robot.ableToCalibrateFront() && robot.ableToCalibrateLeft()) {
                                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                                            //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                                            //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                            calibrationCount = 0;
                                        } else if (robot.ableToCalibrateFront()) {
                                            //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                            calibrationCount = 0;
                                        } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                                            //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                            SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                                            calibrationCount = 0;
                                        }
                                    }

                                    // SENSE AFTER CALIBRATION
                                    senseAndUpdateAndroid(realRun, robot, grid);
                                }*/

                                // MOVE FORWARD
                                if (realRun)
                                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                                robot.move();
                                stepTaken();

                                // CALIBRATION
                                /*if (realRun) {
                                	calibrationCount++;
                                    // IF CAN CALIBRATE FRONT, TAKE THE OPPORTUNITY
                                    // OTHERWISE CALIBRATE LEFT
                                    if (robot.ableToCalibrateFront()) {
                                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                        calibrationCount = 0;
                                    } else if (calibrationCount >= CALIBRATION_LIMIT && robot.ableToCalibrateLeft()) {
                                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                                        //SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "C");
                                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                                        calibrationCount = 0;
                                    }
                                }*/

                                // SENSE AFTER CALIBRATION
                                senseAndUpdateAndroid(realRun, robot, grid);
                            }
                        }
                    }
                }
            }
           
            
            //Fastest path back to start zone after completing exploration
            if(!Grid.isInStartingZone(robot.getPositionX()+2, robot.getPositionY()+2)){
                Robot proxyRobot = new Robot(grid, new ArrayList<>());
                proxyRobot.setDirection(robot.getDirection());
                proxyRobot.setPositionX(robot.getPositionX());
                proxyRobot.setPositionY(robot.getPositionY());
             
                System.out.println("RUNNING A* SEARCH!");
                List<String> returnActions = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), START_X_POSITION, START_Y_POSITION, grid, proxyRobot);

                if (returnActions != null) {
                	
                    System.out.println(returnActions.toString());

                    if (realRun) {
                    	proxyRobot.setPositionX(robot.getPositionX());
                    	proxyRobot.setPositionY(robot.getPositionY());
                    	proxyRobot.setDirection(robot.getDirection());
                        String compressedPath = Algorithm.compressExplorationPath(returnActions, proxyRobot);
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressedPath);
                    } 
                    else {
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
                        }
                    }
                }
                else {
                    System.out.println("FASTEST PATH CANNOT BE FOUND!!");
                }
            }
        }
        
        if (realRun)
            SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                            robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
        SocketMgr.getInstance().sendMessage(CALL_ANDROID, "exploreComplete");
        
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

            for (String action : returnActions) {
                robot.sense(realRun);
                
                if (realRun){
                    SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                    robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
                }

                stepTaken();
                
                if (action.equals("M")) {
                    if (realRun)
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "M1");
                    robot.move();
                } 
                else if (action.equals("L")) {
                    if (realRun)
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                    robot.turn(LEFT);
                } 
                else if (action.equals("R")) {
                    if (realRun)
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                    robot.turn(RIGHT);
                } 
                else if (action.equals("U")) {
                    if (realRun)
                        SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
                    robot.turn(LEFT);
                    robot.turn(LEFT);
                }
            }
            return true;
        } 
        
        else {	
            return false;
        }
	}

	private void senseAndUpdateAndroid(boolean realRun, Robot robot, Grid grid) {
		robot.sense(realRun);
		if (realRun) {
            SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                            robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
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
		// TODO Auto-generated method stub
		if(robot.isObstacleInfront()){
			
			if(robot.isObstacleOnLeftSide() && robot.isObstacleOnRightSide()){
				System.out.println("---------------------Turning Back--------------------");
				if (realRun)
                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "U");
				robot.turn(LEFT);
				robot.turn(LEFT);
                stepTaken();
			}
			else if(robot.isObstacleOnLeftSide()){
				System.out.println("---------------------Turning right-------------------");
                if (realRun)
                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
                robot.turn(RIGHT);
                stepTaken();
			}
			else{
				System.out.println("---------------------Turning left--------------------");
                if (realRun)
                    SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
                robot.turn(LEFT);
                stepTaken();
			}
			
			
			// Inform Android of turn
            if(realRun)
            	SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                            robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
			
			return true;
		}
		else if(!robot.isObstacleOnLeftSide()){
			System.out.println("-------------------------Turning Left--------------------");
            if (realRun)
                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
            robot.turn(LEFT);
            stepTaken();
            
         // Inform Android of turn
            if(realRun)
            	SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(),
                            robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));

            return true;
		}
		
		return false;
	}
	
}


