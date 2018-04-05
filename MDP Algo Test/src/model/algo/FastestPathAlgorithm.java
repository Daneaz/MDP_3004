package model.algo;

import static constant.CommunicationConstant.*;
import static constant.RobotConstant.*;

import java.util.ArrayList;
import java.util.List;

import model.algo.Algorithm;
import model.physical.Grid;
import model.physical.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;

public class FastestPathAlgorithm implements Algorithm {

	private int robotSpeed;
	private int wayPointPositionX;
	private int wayPointPositionY;
	
	private static final int START_X_POSITION = 0;
    private static final int START_Y_POSITION = 17;
    private static final int GOAL_X_POSITION = 12;
    private static final int GOAL_Y_POSITION = 0;

	
	public FastestPathAlgorithm(int speed) {
        robotSpeed = 1000 / speed;
    }
	
	public FastestPathAlgorithm(int speed, int x, int y) {
        wayPointPositionX = x;
        wayPointPositionY = y;
		robotSpeed = 1000 / speed;
    }

	@Override
	public void start(boolean realRun, Robot robot, Grid grid) {
		
		robot.resetRobot();
		
		if (realRun) {
			SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                    MessageMgr.generateMapDescriptorMsg(grid.generateMapDescriptor1(), grid.generateMapDescriptor2(), robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
            String msg = SocketMgr.getInstance().receiveMessage(false);
            while (!msg.equals("fps")) {
                msg = SocketMgr.getInstance().receiveMessage(false);
            }
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
        }
		
		int wayPointX, wayPointY;
		
		if(realRun){
			wayPointX = wayPointPositionX;
			wayPointY = wayPointPositionY;
		}
		else{
			wayPointX = 0;
			wayPointY = 17;
		}
		
		
		System.out.println("The waypoint is set to " + wayPointX+1 + "," + wayPointY+1);
		Robot proxyRobot = new Robot(new Grid(), new ArrayList<>());
		List<String> firstPath = Algorithm.startAstarSearch(START_X_POSITION, START_Y_POSITION, wayPointX, wayPointY, grid, proxyRobot);
        List<String> secondPath = Algorithm.startAstarSearch(wayPointX, wayPointY, GOAL_X_POSITION, GOAL_Y_POSITION, grid, proxyRobot);
        
        if ((firstPath != null) && (secondPath != null)) {
            firstPath.addAll(secondPath);
            String compressedPathForARDUINO = "";
            if (realRun) {	
                compressedPathForARDUINO = Algorithm.compressPath(firstPath);
                SocketMgr.getInstance().sendMessage(CALL_ARDUINO, compressedPathForARDUINO);
            }
            System.out.println("Fastest Path Algorithm calculated, printing and executing actions");
            System.out.println(firstPath.toString());
            
            System.out.print("Arduino actions: ");
            System.out.println(compressedPathForARDUINO);
            
            for (String action : firstPath) {
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
                
                if(!realRun)
                	stepTaken();
            }
            
            if (realRun) {
    			SocketMgr.getInstance().sendMessage(CALL_ANDROID,
                        MessageMgr.generateMapDescriptorMsg(grid.generateMapDescriptor1(), grid.generateMapDescriptor2(), robot.getCenterPositionX(), robot.getCenterPositionY(), robot.getDirection()));
            }
            
            /*grid.clearClosedSet();
            robot.turn(RIGHT);
            robot.turn(LEFT);*/
        } else {
        	System.out.println("Fastest path cannot be not found!");
        }
	}

	private void stepTaken() {
		try {
            Thread.sleep(robotSpeed);
        } catch (Exception except) {
            except.printStackTrace();
        }
	}	
}
