package simulator;

import model.physical.Grid;
import model.physical.Robot;
import model.physical.Sensor;
import simulator.Simulator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static constant.RobotConstant.*;

/**
 * Entry of the application
 */
public class Launch {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // models
            Grid grid = new Grid();
            //setObstaclesMap(grid);
            Sensor sensor1 = new Sensor(2, 0, 0, LEFT, 3);
            Sensor sensor2 = new Sensor(2, 0, 2, LEFT, 3);
            Sensor sensor3 = new Sensor(2, 0, 0, MIDDLE, 5);
            Sensor sensor4 = new Sensor(2, 2, 0, MIDDLE, 5);
            Sensor sensor5 = new Sensor(2, 1, 0, MIDDLE, 3);
            Sensor sensor6 = new Sensor(6, 1, 0, RIGHT, 1);
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

            simulator.setVisible(true);
            System.out.println("Simulator started.");
        });
    }
}



/*
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launch extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setStage(primaryStage, new Simulator(primaryStage).getScene(), "MDP AY1718 S2 GROUP 13");
        primaryStage.sizeToScene();
        primaryStage.show();
    }

    public void setStage(final Stage primaryStage, Scene newScene, String title) {
        primaryStage.setTitle(title);
        primaryStage.setScene(newScene);
    }
}
*/