package controller;

import model.algo.Algorithm;
import model.algo.ExplorationAlgorithm;
import model.physical.Grid;
import model.physical.Robot;
import view.Simulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Button listener
 */
public class ExplorationButtonListener implements ActionListener {

    private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;

    public ExplorationButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addExplorationButtonListener(this);
        
        System.out.println("I AM IN EXPLORE BUTTON LISTENER CONSTRUCTOR");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Exploration button pressed");
        if (!mView.getIsRealRun()) {
            if (mView.getRobotSpeed() == 0) {
                JOptionPane.showMessageDialog(null, "Please set robot speed! (X Steps per second)", "Fastest path", JOptionPane.ERROR_MESSAGE);
            }
        }
        mView.disableButtons();
        new ExplorationWorker().execute();
    }

    class ExplorationWorker extends SwingWorker<Integer, Integer> {
    	
        @Override
        protected Integer doInBackground() throws Exception {
        	System.out.println("Exploration worker started!");
        	Algorithm algorithmRunner = new ExplorationAlgorithm(mView.getRobotSpeed());
        	System.out.println("Robot speed!!!");
            algorithmRunner.start(mView.getIsRealRun(), mRobot, mGrid);
            System.out.println("Robot START!!!");
            return 1;
        }

        @Override
        protected void done() {
            super.done();
            System.out.println("Exploration worker done!");
            mView.enableButtons();
        }
    }

}
