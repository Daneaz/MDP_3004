package simulator;

import model.physical.Grid;
import model.physical.Robot;

import javax.swing.*;

import map.Map;

import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Observer;

/**
 * Simulator
 */

public class Simulator extends JFrame {

    // Swing components
	
	private JPanel mMapPanel;
	private JButton mExplorationButton;
	private JButton mFastestPathButton;
	private JButton mLoadMapButton;
	private JButton mTimeLimitedButton;
	private JButton mCoverageLimitedButton;
	private JButton mRealRunButton;
	private JCheckBox mRealRunCheckBox;
	private JFormattedTextField mRobotSpeedField;
	
	// model
	private Grid mSimulationGrid;
	private Robot mSimulationRobot;
	
	public Simulator(Grid grid, Robot robot) {
	    mSimulationGrid = grid;
	    mSimulationRobot = robot;
	    initializeUi();
	}
	
	private void initializeUi() {
	    // create components
	    mMapPanel = new Map(mSimulationGrid, mSimulationRobot);
	    mExplorationButton = new JButton("Exploration");
	    mFastestPathButton = new JButton("Fastest path");
	    mLoadMapButton = new JButton("Load map");
	    mTimeLimitedButton = new JButton("Time limited");
	    mCoverageLimitedButton = new JButton("Coverage limited");
	    mRealRunButton = new JButton("Physical run");
	    mRealRunCheckBox = new JCheckBox("Real run");
	    mRealRunCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
	    mRobotSpeedField = new JFormattedTextField(NumberFormat.getIntegerInstance());
	    mRobotSpeedField.setPreferredSize(new Dimension(50, mRobotSpeedField.getHeight()));
	
	    // set up as observer
	    mSimulationRobot.addObserver((Observer) mMapPanel);
	    mSimulationGrid.addObserver((Observer) mMapPanel);
	
	    // layout components
	    JPanel wrapper = new JPanel(new FlowLayout());
	    wrapper.add(mMapPanel);
	    this.add(wrapper, BorderLayout.CENTER);
	
	    JPanel bottomPanel = new JPanel();
	    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
	    bottomPanel.add(mRealRunCheckBox);
	    bottomPanel.add(new JLabel("Speed"));
	    bottomPanel.add(mRobotSpeedField);
	    bottomPanel.add(mRealRunButton);
	    bottomPanel.add(mExplorationButton);
	    bottomPanel.add(mFastestPathButton);
	    bottomPanel.add(mTimeLimitedButton);
	    bottomPanel.add(mCoverageLimitedButton);
	    bottomPanel.add(mLoadMapButton);
	    this.add(bottomPanel, BorderLayout.PAGE_END);
	
	    // set up the frame
	    pack();
	    setTitle("MDP Group 16 Simulator");
	    setResizable(false);
	    setLocationRelativeTo(null);
	    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
}













/*import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import map.Map;

public class Simulator {
	    
    private Stage primaryStage;
    public Map map;

    public Simulator(Stage primaryStage) {
        this.primaryStage = primaryStage;
        map = new Map(primaryStage);
    }
    
    public Scene getScene() throws NoSuchMethodException {
        primaryStage.resizableProperty().setValue(false);
        BorderPane root = new BorderPane();
        root.setTop(titleBox());
        root.setTop(mainContainer());
        root.setBottom(buttons());
        Scene mainScene = new Scene(root, Color.TRANSPARENT);
        return mainScene;
    }

    private HBox titleBox() {
        HBox titleBox = new HBox();
        HBox.setHgrow(titleBox, Priority.NEVER);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setSpacing(10);
        titleBox.setPadding(new Insets(15, 0, 15, 0));


        Text title = new Text("MDP SIMULATOR");
        title.getStyleClass().add("title");
        title.setFill(Color.ALICEBLUE);
        title.setTextAlignment(TextAlignment.RIGHT);

        titleBox.getChildren().add(title);
        return titleBox;
    }

    private Pane mainContainer() {
        Pane mainContainer = new Pane();
        HBox.setHgrow(mainContainer, Priority.ALWAYS);
        
        GridPane grid = new GridPane();
        //grid.setPadding(new Insets(30, 50, 30, 50));
        
        Rectangle r;
        for (int i = 1; i <= 20; i++) {
            for (int j = 1; j <= 15; j++) {
            		if (map.isStartZone(i, j)) {
            			r = map.setStart(); 
            		}
            		else if (map.isGoalZone(i, j)) {
            			r = map.setGoal();
            		}
            		else {
            			r = map.setUnexplored();
            		}
            		
            		map.addBoardField(j - 1, i - 1, r);
            		grid.addRow(i, r);
            }
        }
        
        Circle c = new Circle(100, 100, 35, Color.NAVY);
        
        
        
        mainContainer.getChildren().addAll(grid,c);
        return mainContainer;
    }

    private HBox buttons() throws NoSuchMethodException {
        HBox buttons = new HBox();
        HBox.setHgrow(buttons, Priority.ALWAYS);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(15, 12, 15, 12));
        buttons.setSpacing(15);
        buttons.setStyle("-fx-background-color: #336699;");

        Button buttonLoadMap = new Button("Load Arena");
        buttonLoadMap.setPrefSize(100, 20);
        
        Button buttonExploration = new Button("Exploration");
        buttonExploration.setPrefSize(100, 20);
 
        Button buttonFastestPath = new Button("Fastest Path");
        buttonFastestPath.setPrefSize(100, 20);
        
        buttons.getChildren().addAll(buttonLoadMap, buttonExploration, buttonFastestPath);
        return buttons;
        
    }

}
*/