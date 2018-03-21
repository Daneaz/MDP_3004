package view;


import static constant.MapConstant.*;
import static constant.RobotConstant.*;

import javafx.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.Observer;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

//import controller.ExplorationController;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import view.Map;
import model.algo.Algorithm;
import model.algo.CoverageLimitedAlgorithm;
import model.algo.ExplorationAlgorithm;
import model.algo.FastestPathAlgorithm;
import model.algo.TimeLimitedAlgorithm;
import model.physical.*;
import model.util.MessageMgr;
import model.util.SocketMgr;

public class Simulator {

	private static Grid simGrid;
	private static Robot simRobot;

	
	static Button load = new Button("Load Arena");
	static Button explore = new Button("Exploration");
	static Button fast = new Button("Fastest Path");
	static Button time = new Button("Time\nLimited");
	static Button cover = new Button("Coverage\nLimited");
	static Button actual = new Button("Actual Run");
	static Button reset = new Button("Reset");

	
	public Simulator (Grid grid, Robot robot) {
		simGrid = grid;
		simRobot = robot;
	}
		
	public static HBox titleBox() {
		HBox hbox = new HBox();
		hbox.setAlignment(Pos.CENTER);
		hbox.setSpacing(10);
		hbox.setPadding(new Insets(8, 0, 8, 0));
		hbox.setStyle("-fx-background-color: #336699;");
		
		Text title = new Text("Group 13");
		title.getStyleClass().add("title");
		title.setFill(Color.WHITE);
		
		hbox.getChildren().add(title);
		return hbox;
	}

	public static StackPane mainContainer() {
		SwingNode swingNode = new SwingNode();
		
		generateMap(swingNode);
		
		StackPane pane = new StackPane();
		pane.setAlignment(Pos.CENTER);
		pane.setPrefSize(CELL_SIZE * MAP_COLUMNS, CELL_SIZE * MAP_ROWS);
		
		AnchorPane anchorPane = new AnchorPane();
		Label start = new Label("S");
		start.setTextFill(Color.GREY);
		start.setFont(Font.font(75));
		start.setOpacity(0.6);
		Label goal = new Label("G");
		goal.setTextFill(Color.GREY);
		goal.setFont(Font.font(75));
		goal.setOpacity(0.6);
		anchorPane.getChildren().addAll(start, goal);
		
		AnchorPane.setLeftAnchor(start, 20.0);
		AnchorPane.setBottomAnchor(start, 0.0);
		AnchorPane.setRightAnchor(goal, 17.0);
		AnchorPane.setTopAnchor(goal, 0.0);
		
		pane.getChildren().addAll(swingNode,anchorPane);
		
		return pane;
	}
	
	public static void generateMap(SwingNode swingNode) {
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                	JPanel mapPanel = new Map(simGrid, simRobot);
                	simRobot.addObserver((Observer) mapPanel);
        	    		simGrid.addObserver((Observer) mapPanel);
        	    		mapPanel.setVisible(true);
        	    		swingNode.setContent(mapPanel);
            }
		});
	}
	
	public static VBox buttons() {
	
	    VBox vb = new VBox();
	    vb.setAlignment(Pos.TOP_CENTER);
	    vb.setPadding(new Insets(10, 10, 10, 10));
	    vb.setSpacing(15);
	    vb.setStyle("-fx-background-color: #336699;");
	
	    SwingNode swingMap = new SwingNode();
	    SwingNode swingTimer = new SwingNode();
	    
	    load.setPrefSize(100, 45);
	    load.setOnAction(new EventHandler<ActionEvent>() {
	        @Override public void handle(ActionEvent event) {
	        		System.out.println("Load Arena button pressed");
	        		importMap(swingMap);
	        }
	    });
	    
	    explore.setPrefSize(100, 45);
	    explore.setOnAction(new EventHandler<ActionEvent>() {
	    		@Override public void handle(ActionEvent event) {
	    			System.out.println("Exploration button pressed");
	    	        disableButtons();
	    	        new ExplorationWorker().execute();
	    	    }
	    	    class ExplorationWorker extends SwingWorker<Integer, Integer> {
	    	        @Override
	    	        protected Integer doInBackground() throws Exception {
	    	            Algorithm algorithmRunner = new ExplorationAlgorithm(ROBOT_SPEED);
	    	            algorithmRunner.start(getIsActualRun(), simRobot, simGrid);
	    	            return 1;
	    	        }
	    	        @Override
	    	        protected void done() {
	    	            super.done();
	    	            enableButtons();
	    	        }
	    	    }
	    });
	    
	    fast.setPrefSize(100, 45);
	    fast.setOnAction(new EventHandler<ActionEvent>() {
	    		@Override public void handle(ActionEvent event) {
	    			System.out.println("Fastest path button pressed");
	    	        disableButtons();
	    	        new FastestPathWorker().execute();
	    	    }
	    	    class FastestPathWorker extends SwingWorker<Integer, Integer> {
	    	        @Override
	    	        protected Integer doInBackground() throws Exception {
	    	            System.out.println("Worker started");
	    	            Algorithm algorithmRunner = new FastestPathAlgorithm(ROBOT_SPEED);
	    	            algorithmRunner.start(getIsActualRun(), simRobot, simGrid);
	    	            return 1;
	    	        }
	    	        @Override
	    	        protected void done() {
	    	            super.done();
	    	            System.out.println("Worker finished");
	    	            enableButtons();
	    	        }
	    	    }
	    });
	    
	    time.setPrefSize(100, 45);
	    time.setTextAlignment(TextAlignment.CENTER);
	    time.setOnAction(new EventHandler<ActionEvent>() {
	    		@Override public void handle(ActionEvent event) {
	    			System.out.println("Time limited button pressed");
	    	        disableButtons();
	    	        new TimeLimitedWorker().execute();
	    	    }
	    	    class TimeLimitedWorker extends SwingWorker<Integer, Integer> {
	    	        @Override
	    	        protected Integer doInBackground() throws Exception {
	    	            Algorithm algorithmRunner = new TimeLimitedAlgorithm(ROBOT_SPEED);
	    	            algorithmRunner.start(getIsActualRun(), simRobot, simGrid);
	    	            return 1;
	    	        }
	    	        @Override
	    	        protected void done() {
	    	            super.done();
	    	            enableButtons();
	    	        }
	    		}
	    });
	    
	    cover.setPrefSize(100, 45);
	    cover.setTextAlignment(TextAlignment.CENTER);
	    cover.setOnAction(new EventHandler<ActionEvent>() {
	    		@Override public void handle(ActionEvent event) {
	    			System.out.println("Coverage limited button pressed");
	    	        disableButtons();
	    	        new CoverageWorker().execute();
	    	    }
	    	    class CoverageWorker extends SwingWorker<Integer, Integer> {
	    	        @Override
	    	        protected Integer doInBackground() throws Exception {
	    	            Algorithm algorithmRunner = new CoverageLimitedAlgorithm(ROBOT_SPEED);
	    	            algorithmRunner.start(getIsActualRun(), simRobot, simGrid);
	    	            return 1;
	    	        }
	    	        @Override
	    	        protected void done() {
	    	            super.done();
	    	        }
	    		}
	    });

	    actual.setPrefSize(100, 45);
	    actual.setOnAction(new EventHandler<ActionEvent>() {
	    		@Override public void handle(ActionEvent event) {
	    			disableActualRun
	    			();
	    			System.out.println("Actual run button pressed, actual run: " + getIsActualRun());
	    			
	    	        if (getIsActualRun()) {
	    	        		disableButtons();
	    	        		
	    	        		if (!SocketMgr.getInstance().isConnected())
	    	        			SocketMgr.getInstance().openConnection();
	    	        		new PhysicalRunWorker().execute();
	    	    	        
	    	        } else {
	    	        		enableLoadArena();
	    	        		enableExploration();
	    	        		enableFastestPath();
	    	        		enableActualRun();
	    	        		SocketMgr.getInstance().closeConnection();
	    	        }
	    	    }
	    	    class PhysicalRunWorker extends SwingWorker<Integer, Integer> {
	    	        @Override
	    	        protected Integer doInBackground() throws Exception {
	    	            // receive way point

	    	        	
	    	        		String msg = SocketMgr.getInstance().receiveMessage(false);
	    	            List<Integer> robotStartPosAndWaypoints;
	    	            while ((robotStartPosAndWaypoints = MessageMgr.parseMessage(msg)) == null) {
	    	                msg = SocketMgr.getInstance().receiveMessage(false);
	    	            }
	    	            // do exploration
	    	            Algorithm explorationRunner = new ExplorationAlgorithm(1, robotStartPosAndWaypoints);
	    	            explorationRunner.start(getIsActualRun(), simRobot, simGrid);

	    	            // do fastest path
	    	            Algorithm fastestPathRunner = new FastestPathAlgorithm(1,
	    	                    robotStartPosAndWaypoints.get(3) - 1, robotStartPosAndWaypoints.get(4) - 1);
	    	            fastestPathRunner.start(getIsActualRun(), simRobot, simGrid);


	    	            
	    	            
	    	            return 1;
	    	        }
	    	        @Override
	    	        protected void done() {
	    	            super.done();
	    	            enableButtons();
	    	        }
	    		}
	    });
	    
	    reset.setPrefSize(100, 45);
	    reset.setOnAction(new EventHandler<ActionEvent>() {
	    		@Override public void handle(ActionEvent event) {
	    			System.out.println("Reset button pressed.");
	    			enableButtons();
	    	
	    		}
	    });
	    
	    /*
	    long timeStart = 
	    Timer timer = new Timer();
	    Label timerLabel = new Label();
	    */
	    
//	    vb.getChildren().addAll(load,explore,fast,time,cover,actual,reset);
	    
	    vb.getChildren().addAll(load,explore,fast,time,cover,actual);

	    return vb;
	}
	
	public static void importMap(SwingNode swing) {
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	String file_path = JOptionPane.showInputDialog(null, "Choose a Map", "Map loader", JOptionPane.QUESTION_MESSAGE);
	            if (null != file_path) {
	                try {
	                    disableButtons();
	                    simGrid.loadingFromDisk("maps/" + file_path);
	                    System.out.println("Loaded map " + file_path);
	                } catch (IOException ex) {
	                    JOptionPane.showMessageDialog(null, "File cannot be found!", "Map loader", JOptionPane.ERROR_MESSAGE);
	                } finally {
	                    enableButtons();
	                }
	            }
            }
        });
	}
	    		    
    public static void disableButtons() {
        explore.setDisable(true);
        fast.setDisable(true);
        load.setDisable(true);
        time.setDisable(true);
        cover.setDisable(true);
    }

    public static void enableButtons() {
        explore.setDisable(false);
        fast.setDisable(false);
        load.setDisable(false);
        time.setDisable(false);
        cover.setDisable(false);
    }

    public static void disableLoadArena() {
        load.setDisable(true);
    }

    public static void enableLoadArena() {
    		load.setDisable(false);
    }

    public static void disableActualRun() {
    		actual.setDisable(true);
    }

    public static void enableActualRun() {
        	actual.setDisable(false);
    }

    public static void disableFastestPath() {
    		fast.setDisable(true);
    	}

    public static void enableFastestPath() {
    		fast.setDisable(false);
    	}

    public static void disableExploration() {
        explore.setDisable(true);
    }

    public static void enableExploration() {
        explore.setDisable(false);
    }

    public static boolean getIsActualRun() {
        return actual.isDisabled();
    }
}
