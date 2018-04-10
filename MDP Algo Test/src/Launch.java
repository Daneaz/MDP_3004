import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.physical.Grid;
import model.physical.Robot;
import model.physical.Sensor;
import view.Simulator;

import static constant.RobotConstant.*;

public class Launch extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		Grid grid = new Grid();
	
        Sensor sensor1 = new Sensor(0, 0, 2, MIDDLE, 5); //front left
        Sensor sensor2 = new Sensor(1, 0, 2, MIDDLE, 4); //front middle
        Sensor sensor3 = new Sensor(2, 0, 2, MIDDLE, 4); //front right
        Sensor sensor4 = new Sensor(0, 0, 2, LEFT, 4);//left front
        Sensor sensor5 = new Sensor(0, 2, 2, LEFT, 4); //left back
        Sensor sensor6 = new Sensor(1, 0, 5, RIGHT, 1); //right center
        
        	List<Sensor> sensors = new ArrayList<>();
        	sensors.add(sensor1);
        	sensors.add(sensor2);
        	sensors.add(sensor3);
        	sensors.add(sensor4);
        	sensors.add(sensor5);
        	sensors.add(sensor6);
        	Robot robot = new Robot(grid, sensors);
		
		Simulator simulator = new Simulator(grid, robot);
		
		
		BorderPane root = new BorderPane();
		root.setTop(Simulator.titleBox());
		root.setCenter(Simulator.mainContainer());
		root.setRight(Simulator.buttons()); 
    		
    		Scene scene = new Scene(root);
    		
    		scene.setFill(Color.TRANSPARENT);
    		
    		primaryStage.setTitle("Simulator"); 
    		primaryStage.setScene(scene);
    		primaryStage.setResizable(false);
    		
    		primaryStage.show();
	}	
}
