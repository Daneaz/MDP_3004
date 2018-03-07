import controller.*;
import model.physical.Grid;
import model.physical.Robot;
import model.physical.Sensor;
import model.util.SocketMgr;
import view.Simulator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static constant.RobotConstant.*;

/**
 * Entry of the application
 */
public class AppRunner {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // models
            Grid grid = new Grid();
            //setObstaclesMap(grid);
            //Sensor sensor4 = new Sensor(0, 0, 2, LEFT, 5); //left front
           // Sensor sensor5 = new Sensor(0, 1, 2, LEFT, 5);//left back
            Sensor sensor4 = new Sensor(0, 1, 2, LEFT, 3); //left front
            Sensor sensor5 = new Sensor(0, 0, 2, LEFT, 3);//left back
            Sensor sensor1 = new Sensor(0, 0, 2, MIDDLE, 5); //front left
            Sensor sensor3 = new Sensor(2, 0, 2, MIDDLE, 5); //front right
            Sensor sensor2 = new Sensor(1, 0, 2, MIDDLE, 3); //front middle
            Sensor sensor6 = new Sensor(1, 0, 6, RIGHT, 1);
            List<Sensor> sensors = new ArrayList<>();
            sensors.add(sensor1);
            sensors.add(sensor2);
            sensors.add(sensor3);
            sensors.add(sensor4);
            sensors.add(sensor5);
            sensors.add(sensor6);
            Robot robot = new Robot(grid, sensors);

            // view
            Simulator simulator = new Simulator(grid, robot);

            // controller
            new ExplorationButtonListener(simulator, grid, robot);
            new FastestPathButtonListener(simulator, grid, robot);
            new LoadMapButtonListener(simulator, grid, robot);
            new RealRunButtonListener(simulator, grid, robot);
            new RealRunCheckBoxListener(simulator);
            new TimeLimitedButtonListener(simulator, grid, robot);
            new CoverageLimitedButtonListener(simulator, grid, robot);

            simulator.setVisible(true);
            System.out.println("Simulator started.");
        });
    }
}