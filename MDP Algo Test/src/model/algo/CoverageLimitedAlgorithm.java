package model.algo;

import static constant.RobotConstant.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import model.algo.Algorithm;
import model.physical.Cell;
import model.physical.Grid;
import model.physical.Robot;
import model.util.SocketMgr;

public class CoverageLimitedAlgorithm implements Algorithm {

    private int sleepDuration;
    private static final int START_X = 0;
    private static final int START_Y = 17;

    public CoverageLimitedAlgorithm(int speed){
        sleepDuration = 1000 / speed;
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
        int coveragePercentage = 0;
        do{
            try{
                String input = JOptionPane.showInputDialog(null, "Please enter the exploration percentage:", "Enter Percentage", JOptionPane.INFORMATION_MESSAGE);
                if(input.equals(JOptionPane.CANCEL_OPTION)){
                    break;
                }else{
                    coveragePercentage = Integer.parseInt(input);
                }
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Please enter an integer more than 0!", "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }while(coveragePercentage == 0);
        coverageLimitedAlgorithm(grid, robot, coveragePercentage, realRun);
        
        grid.generateMapDescriptor1();
        grid.generateMapDescriptor2();
    }

    private void coverageLimitedAlgorithm(Grid grid, Robot robot, int coveragePercentage, boolean realRun) {

        while (grid.checkPercentageExplored() < coveragePercentage) {
            Cell position = new Cell(robot.getPositionX(), robot.getPositionY());
            robot.sense(realRun);
            if (robot.isObstacleInfront()) {
                if (robot.isObstacleOnRightSide() && robot.isObstacleOnLeftSide()) {
                    System.out.println("OBSTACLE DETECTED! (ALL 3 SIDES) U-TURNING");
                    robot.turn(RIGHT);
                    stepTaken();
                    robot.turn(RIGHT);
                    stepTaken();
                } else if (robot.isObstacleOnLeftSide()) {
                    System.out.println("OBSTACLE DETECTED! (FRONT + LEFT) TURNING RIGHT");
                    robot.turn(RIGHT);
                    stepTaken();
                } else {
                    System.out.println("OBSTACLE DETECTED! (FRONT) TURNING LEFT");
                    robot.turn(LEFT);
                    stepTaken();
                }
                robot.sense(realRun);
                System.out.println("-----------------------------------------------");
            } else if (!robot.isObstacleOnLeftSide()) {
                System.out.println("NO OBSTACLES ON THE LEFT! TURNING LEFT");
                robot.turn(LEFT);
                stepTaken();
                robot.sense(realRun);
                System.out.println("-----------------------------------------------");
            }
            robot.move();
            stepTaken();
        }

        Robot fakeRobot = new Robot(grid, new ArrayList<>());
        fakeRobot.setPositionX(robot.getPositionX());
        fakeRobot.setPositionY(robot.getPositionY());
        fakeRobot.setDirection(robot.getDirection());
        List<String> returnPath = Algorithm.startAstarSearch(robot.getPositionX(), robot.getPositionY(), START_X, START_Y, grid, fakeRobot);

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
        
        System.out.println("EXPLORATION COMPLETED!");
        System.out.println("AREA EXPLORED: " + grid.checkPercentageExplored() + "%!");
    }

    private void stepTaken(){
        /*
            MAKE IT MOVE SLOWLY SO CAN SEE STEP BY STEP MOVEMENT
             */
        try {
            Thread.sleep(sleepDuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	

	
}
