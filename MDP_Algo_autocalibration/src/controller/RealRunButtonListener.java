package controller;

import model.algo.Algorithm;
import model.algo.ExplorationAlgorithm;
import model.algo.FastestPathAlgorithm;
//import model.algo.FastestPathAlgorithmRunner;
import model.physical.Grid;
import model.physical.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;
import view.Simulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;


public class RealRunButtonListener implements ActionListener {

    private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;

    public RealRunButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addRealRunButtonListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Physical run button pressed, real run: " + mView.getIsRealRun());
        if (mView.getIsRealRun()) {
            if (mView.getRobotSpeed() == 0) {
                JOptionPane.showMessageDialog(null, "Please set robot speed (X Steps per second)!", "Fastest path", JOptionPane.ERROR_MESSAGE);
            }
            mView.disableButtons();
            new PhysicalRunWorker().execute();
        }
    }

    class PhysicalRunWorker extends SwingWorker<Integer, Integer> {

        @Override
        protected Integer doInBackground() throws Exception {
        	// receive way point
            String msg = SocketMgr.getInstance().receiveMessage(false);
            /* robotStartPosAndWaypoints.get(0): Robot Start X-Coordinate,
             * robotStartPosAndWaypoints.get(1): Robot Start Y-Coordinate,
             * robotStartPosAndWaypoints.get(2): Robot Start Head Direction,
             * robotStartPosAndWaypoints.get(3): WayPoint X-Coordinate,
             * robotStartPosAndWaypoints.get(4): WayPoint Y-Coordinate,
             */
            List<Integer> robotStartPosAndWaypoints;
            
            while ((robotStartPosAndWaypoints = MessageMgr.parseMessage(msg)) == null) {
                msg = SocketMgr.getInstance().receiveMessage(false);
            }
            
            /*while(true) {
            	//while (msg == null) {
                msg = SocketMgr.getInstance().receiveMessage(false);
                //}
            	System.out.println(msg);
            }*/
            

            // do exploration
            Algorithm explorationRunner = new ExplorationAlgorithm(mView.getRobotSpeed(), robotStartPosAndWaypoints);
            explorationRunner.start(mView.getIsRealRun(), mRobot, mGrid);

            
            // do fastest path
            Algorithm fastestPathRunner = new FastestPathAlgorithm(mView.getRobotSpeed(),
            		robotStartPosAndWaypoints.get(3) - 1, robotStartPosAndWaypoints.get(4) - 1);
//            0, 17);
            fastestPathRunner.start(mView.getIsRealRun(), mRobot, mGrid);

            return 1;
        }

        @Override
        protected void done() {
            super.done();
            mView.enableButtons();
        }
    }
}
