package model.algo;

import static constant.RobotConstant.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import model.physical.Grid;
import model.physical.Robot;
import model.util.SocketMgr;

public class TimeLimitedAlgorithm implements Algorithm  {

	private static final int START_X_POSITION = 0;
    private static final int START_Y_POSITION = 17;
    private int robotSpeed;
    
    public TimeLimitedAlgorithm(int speed){
        robotSpeed = 1000 / speed;
    }
    
	@Override
	public void start(boolean realRun, Robot robot, Grid grid) {
		
		 	grid.reset();
	        robot.resetRobot();
	        
	        if (realRun) {
	            String msg = SocketMgr.getInstance().receiveMessage(false);
	            while (!msg.equals("exs")) {
	                msg = SocketMgr.getInstance().receiveMessage(false);
	            }
	        }
	        
	        int sec = -1;
	        int min = -1;
	        
	        do{
	            try{
	                String minInput = JOptionPane.showInputDialog(null, "Please enter Time limit :", "Enter Time Limit (Minutes)", JOptionPane.INFORMATION_MESSAGE);
	                if(minInput.equals(JOptionPane.CANCEL_OPTION)){
	                    break;
	                }
	                else{
	                    min = Integer.parseInt(minInput);
	                }
	            }
	            catch(NumberFormatException exception){
	                JOptionPane.showMessageDialog(null, "PLEASE RE-ENTER AN INTEGER!", "Error!", JOptionPane.ERROR_MESSAGE);
	            }
	        }
	        while(min < 0);
	        
	        
	        do{
	            try{
	                String secInput = JOptionPane.showInputDialog(null, "Please enter Time limit :", "Enter Time Limit (Seconds)", JOptionPane.INFORMATION_MESSAGE);
	                if(secInput.equals(JOptionPane.CANCEL_OPTION)){
	                    break;
	                }
	                else{
	                    sec = Integer.parseInt(secInput);
	                }
	            }
	            catch(NumberFormatException exception){
	                JOptionPane.showMessageDialog(null, "PLEASE RE-ENTER AN INTEGER!", "Error!", JOptionPane.ERROR_MESSAGE);
	            }
	        }
	        while(sec < 0);
	        
	        int time = sec + (min *60);
	        
	        
	        runTimeLimitedAlgorithm(realRun, robot, grid, time);
	        
	        grid.generateMapDescriptor1();
	        grid.generateMapDescriptor2();
	        
	        
	}

	private void runTimeLimitedAlgorithm(boolean realRun, Robot robot, Grid grid, int time) {
		
		System.out.println("The time limit is " + time + " seconds");
		
		boolean inStartZone = false;
		boolean inEndZone = false;
		int timeInMs = time * 1000;
		
		/* EDIT - FROM - HERE*/
		
        while (timeInMs > 0 && (!inEndZone || !inStartZone)) {
       
            robot.sense(realRun);
            if (robot.isObstacleInfront()) {
                if (robot.isObstacleOnRightSide() && robot.isObstacleOnLeftSide()) {
                    System.out.println("OBSTACLE DETECTED! (ALL 3 SIDES) U-TURNING");
                    robot.turn(RIGHT);
                    stepTaken();
                    robot.turn(RIGHT);
                    stepTaken();
                    timeInMs = timeInMs - (robotSpeed * 2);
                } else if (robot.isObstacleOnLeftSide()) {
                    System.out.println("OBSTACLE DETECTED! (FRONT + LEFT) TURNING RIGHT");
                    robot.turn(RIGHT);
                    stepTaken();
                    timeInMs = timeInMs - robotSpeed;
                } else {
                    System.out.println("OBSTACLE DETECTED! (FRONT) TURNING LEFT");
                    robot.turn(LEFT);
                    stepTaken();
                    timeInMs = timeInMs - robotSpeed;
                }
                robot.sense(realRun);
                System.out.println("-----------------------------------------------");
            } else if (!robot.isObstacleOnLeftSide()) {
                System.out.println("NO OBSTACLES ON THE LEFT! TURNING LEFT");
                robot.turn(LEFT);
                stepTaken();
                timeInMs = timeInMs - robotSpeed;
                robot.sense(realRun);
                System.out.println("-----------------------------------------------");
            }
            robot.move();
            stepTaken();
            timeInMs = timeInMs - robotSpeed;
            System.out.println("Time left: " + timeInMs + " ms");
            if(Grid.isInEndingZone(robot.getPositionX(), robot.getPositionY())){
            	inEndZone = true;
            }
            if(inEndZone && Grid.isInStartingZone(robot.getPositionX()+2, robot.getPositionY())){
            	inStartZone = true;
            }
        }
        
        Robot proxyRobot = new Robot(grid, new ArrayList<>());
        proxyRobot.setPositionX(robot.getPositionX());
        proxyRobot.setPositionY(robot.getPositionY());
        proxyRobot.setDirection(robot.getDirection());
        List<String> returnPath = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), START_X_POSITION, START_Y_POSITION, grid, proxyRobot);

        if (returnPath != null) {
            System.out.println("Algorithm finished, executing actions");
            System.out.println(returnPath.toString());

            for (String action : returnPath) {
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
        }else {
            System.out.println("Fastest path not found!");
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
