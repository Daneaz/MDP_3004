package controller;

import model.algo.Algorithm;
import model.algo.FastestPathAlgorithm;
import model.physical.Grid;
import model.physical.Robot;
import view.Simulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Button listener
 */
public class FastestPathButtonListener implements ActionListener {

    private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;

    public FastestPathButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addFastestPathButtonListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Fastest path button pressed");
        if (mView.getRobotSpeed() == 0) {
            JOptionPane.showMessageDialog(null, "Please set robot speed! (X Steps per second)", "Fastest path", JOptionPane.ERROR_MESSAGE);
        }
        mView.disableButtons();
        new FastestPathWorker().execute();
    }

    class FastestPathWorker extends SwingWorker<Integer, Integer> {

        @Override
        protected Integer doInBackground() throws Exception {
            System.out.println("Worker started");
            Algorithm algorithmRunner = new FastestPathAlgorithm(mView.getRobotSpeed());
            algorithmRunner.start(mView.getIsRealRun(), mRobot, mGrid);
            return 1;
        }

        @Override
        protected void done() {
            super.done();
            System.out.println("Worker finished");
            mView.enableButtons();
        }
    }
}
