package controller;

import model.algo.Algorithm;
import model.algo.CoverageLimitedAlgorithm;
import model.physical.Grid;
import model.physical.Robot;
import view.Simulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Button listener
 */
public class CoverageLimitedButtonListener implements ActionListener {

    private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;

    public CoverageLimitedButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addCoverageLimitedButtonListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Coverage limited button pressed");
        if (mView.getRobotSpeed() == 0) {
            JOptionPane.showMessageDialog(null, "Please set robot speed (X Steps per second)!", "Fastest path", JOptionPane.ERROR_MESSAGE);
        }
        mView.disableButtons();
        new CoverageWorker().execute();
    }

    class CoverageWorker extends SwingWorker<Integer, Integer> {

        @Override
        protected Integer doInBackground() throws Exception {
            Algorithm algorithmRunner = new CoverageLimitedAlgorithm(mView.getRobotSpeed());
            algorithmRunner.start(mView.getIsRealRun(), mRobot, mGrid);
            return 1;
        }

        @Override
        protected void done() {
            super.done();
            mView.enableButtons();
        }
    }
}
