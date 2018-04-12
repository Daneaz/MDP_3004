package model.physical;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import model.util.MessageMgr;
import model.util.SocketMgr;

import static constant.RobotConstant.*;
import static constant.CommunicationConstant.*;



public class Robot extends Observable {
	private boolean getDataFromList = false;
	private boolean rightObstWithinFrontRange = false, turnBackToOriginal = false, detectNextMove = false, rightSensor = false, fastestPathCalibration = false;
	private int frontRight = 2, frontLeft = 2, frontCenter = 2, leftFront = 2, leftBack = 2;
	private int positionX = STARTING_X_POSITION;
	private int positionY = STARTING_Y_POSITION;
	private int direction = NORTH;
	private List<Sensor> sensor;
	private Grid grid;
	private List<String> dataList;
	private int dataListCount;
	
	public int getFrontRight() {
		return frontRight;
	}

	public int getFrontLeft() {
		return frontLeft;
	}

	public int getFrontCenter() {
		return frontCenter;
	}

	public int getLeftFront() {
		return leftFront;
	}

	public int getLeftBack() {
		return leftBack;
	}

	public Robot(Grid grid, List<Sensor> sensor){
		this.grid = grid;
		this.sensor = sensor;
		for(int i = 0; i < sensor.size(); i++){
			sensor.get(i).setSensorOnRobot(this);
		}
		
		dataListCount = 0;
		
		dataList = new ArrayList();
		
		//Sensor Data for Week10_2.jpg
		/*dataList.add("6,9,9,1,1,5");
		dataList.add("5,8,8,1,1,4");
		dataList.add("4,7,7,1,1,3");
		dataList.add("3,6,6,1,1,7");
		dataList.add("2,6,6,1,1,7");
		dataList.add("1,5,5,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("6,6,5,1,1,5");
		dataList.add("5,5,4,4,1,5");
		dataList.add("4,4,3,4,1,5");
		dataList.add("3,3,2,4,4,5");
		dataList.add("2,2,2,4,4,4");
		dataList.add("1,1,1,1,4,4");
		dataList.add("3,3,3,1,1,6");
		dataList.add("2,2,2,1,1,6");
		dataList.add("4,4,2,4,4,2");
		dataList.add("4,4,4,2,2,7");
		dataList.add("4,4,4,1,2,7");
		dataList.add("3,3,3,4,2,7");
		dataList.add("2,2,2,4,1,7");
		dataList.add("1,1,1,4,4,7");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,4,7");
		dataList.add("3,3,3,4,1,7");
		dataList.add("2,2,2,4,1,7");
		dataList.add("1,1,1,4,4,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("1,4,4,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,2,2,7");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("1,1,1,1,1,8");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,4");
		dataList.add("4,4,4,1,1,6");
		dataList.add("3,3,3,1,1,6");
		dataList.add("2,2,2,1,1,6");
		dataList.add("1,1,1,1,1,6");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("3,3,3,4,1,7");
		dataList.add("2,2,2,4,4,3");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,1,4,3");
		dataList.add("3,3,3,4,4,7");
		dataList.add("2,2,2,4,1,7");
		dataList.add("1,1,1,4,4,7");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("5,4,3,4,4,7");
		dataList.add("4,3,2,4,1,3");
		dataList.add("4,2,1,4,4,2");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,8");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,6");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,5");
		dataList.add("4,4,4,1,1,5");
		dataList.add("4,4,4,1,1,5");
		dataList.add("5,2,3,1,1,5");
		dataList.add("4,1,2,1,1,7");
		dataList.add("4,4,4,2,4,7");
		dataList.add("4,4,4,3,1,7");
		dataList.add("4,2,2,4,2,7");
		dataList.add("2,3,4,3,3,7");
		dataList.add("1,2,3,1,3,7");
		dataList.add("4,4,2,3,1,7");
		dataList.add("4,4,1,4,2,7");
		dataList.add("2,3,4,2,4,6");
		dataList.add("1,2,4,1,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,2,2");
		dataList.add("2,4,4,2,4,7");
		dataList.add("1,4,4,1,3,7");
		dataList.add("4,4,4,4,1,3");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,2,4,7");
		dataList.add("4,4,4,1,3,7");
		dataList.add("4,4,4,4,2,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,4,4,4");
		dataList.add("4,4,4,4,2,7");
		dataList.add("2,3,4,4,4,7");
		dataList.add("1,2,3,1,4,7");
		dataList.add("2,2,2,3,1,7");
		dataList.add("1,1,1,4,2,7");*/
		
		
		//Sensor Data for Week10_1.jpg
		/*dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,3");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,2,1,1,7");
		dataList.add("4,4,1,1,1,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,3,3,2");
		dataList.add("4,4,4,3,1,2");
		dataList.add("4,4,4,3,3,2");
		dataList.add("3,3,3,4,4,7");
		dataList.add("2,2,2,1,4,7");
		dataList.add("1,1,1,1,4,7");
		dataList.add("4,4,4,1,1,4");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("3,3,3,4,4,7");
		dataList.add("4,4,4,4,4,4");
		dataList.add("4,4,4,1,4,4");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,4,4,5");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,5");
		dataList.add("1,1,1,1,1,6");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,2");
		dataList.add("2,2,2,4,1,2");
		dataList.add("1,1,1,4,4,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,4,1,2");
		dataList.add("2,4,4,4,1,2");
		dataList.add("1,4,4,4,4,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,4");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,2,1,4,7");
		dataList.add("4,4,1,4,1,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,5");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("1,1,1,2,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,3");
		dataList.add("2,4,4,1,1,7");
		dataList.add("1,4,4,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,2,4,1,3");
		dataList.add("4,4,1,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("1,1,1,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("3,3,3,1,1,7");
		dataList.add("2,2,2,1,1,7");
		dataList.add("1,1,1,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,5");
		dataList.add("5,5,8,4,4,7");
		dataList.add("4,4,7,1,4,7");
		dataList.add("3,3,6,4,4,5");
		dataList.add("2,2,5,4,1,6");
		dataList.add("1,1,4,4,4,7");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("1,1,1,4,4,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,6");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,3,2,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,7");*/
		
		//Sensor Data for Week12_1.jpg
		/*dataList.add("4,4,4,1,1,7");
		dataList.add("3,3,3,1,1,7");
		dataList.add("2,2,2,1,1,7");
		dataList.add("1,1,1,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,3");
		dataList.add("4,4,4,2,4,3");
		dataList.add("4,4,4,2,1,3");
		dataList.add("2,2,2,1,1,5");
		dataList.add("1,1,1,1,1,5");
		dataList.add("4,4,4,1,1,3");
		dataList.add("4,4,4,4,1,7");
		dataList.add("2,2,2,4,1,7");
		dataList.add("1,1,1,4,4,7");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,4,7");
		dataList.add("3,6,6,4,1,7");
		dataList.add("2,6,6,4,1,4");
		dataList.add("1,5,5,5,5,4");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,2,4,7");
		dataList.add("4,4,4,2,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("1,2,2,4,4,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("2,2,4,4,4,3");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,6");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,3");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,3");
		dataList.add("2,4,4,1,1,7");
		dataList.add("1,4,4,1,1,5");
		dataList.add("4,4,4,4,1,7");
		dataList.add("3,4,4,4,4,7");
		dataList.add("4,4,4,4,4,4");
		dataList.add("4,4,4,1,4,3");
		dataList.add("4,4,4,4,4,7");
		dataList.add("2,2,4,4,1,7");
		dataList.add("1,1,4,4,4,5");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,4,1,7");
		dataList.add("2,4,4,4,4,2");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,1,4,4");
		dataList.add("4,4,4,1,4,4");
		dataList.add("4,4,4,4,1,4");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,6");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,2,4,7");
		dataList.add("4,4,4,2,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,3");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,1,1,2");
		dataList.add("4,4,4,4,2,7");
		dataList.add("2,4,4,4,4,6");
		dataList.add("1,4,4,1,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,2,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,2,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,4");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");*/		
		
		//Sensor Data for Week11_1.jpg
		/*dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,3");
		dataList.add("4,4,4,1,1,7");
		dataList.add("3,3,3,1,1,7");
		dataList.add("2,2,2,1,1,7");
		dataList.add("1,1,1,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,3");
		dataList.add("4,4,4,4,1,3");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,5");
		dataList.add("4,4,4,1,4,5");
		dataList.add("4,4,4,4,1,5");
		dataList.add("4,4,4,4,1,4");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,2,4,2");
		dataList.add("4,4,4,2,1,2");
		dataList.add("4,4,4,2,2,2");
		dataList.add("2,3,4,4,4,7");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("1,1,1,4,4,3");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,2,4,2");
		dataList.add("4,4,4,2,1,2");
		dataList.add("1,1,1,2,2,2");
		dataList.add("1,4,4,1,1,3");
		dataList.add("2,4,4,4,1,7");
		dataList.add("1,4,4,4,4,3");
		dataList.add("4,4,4,2,2,2");
		dataList.add("4,4,4,1,4,3");
		dataList.add("4,4,4,4,4,4");
		dataList.add("4,4,4,4,1,5");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,4");
		dataList.add("3,6,6,1,1,7");
		dataList.add("2,5,5,1,1,7");
		dataList.add("1,4,4,1,1,6");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,5");
		dataList.add("4,4,4,1,4,4");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,4,1,2");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,4,4,7");
		dataList.add("1,1,1,1,4,5");
		dataList.add("4,4,4,1,1,3");
		dataList.add("3,8,8,1,1,3");
		dataList.add("2,8,8,1,1,7");
		dataList.add("1,8,8,1,1,7");
		dataList.add("4,4,2,4,1,7");
		dataList.add("4,4,1,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,1,3");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("1,1,1,1,4,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,1,1,4");
		dataList.add("3,8,8,1,1,7");
		dataList.add("2,7,7,1,1,7");
		dataList.add("1,7,7,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,3");
		dataList.add("4,4,4,1,4,3");
		dataList.add("4,4,4,1,1,3");
		dataList.add("4,4,4,4,1,3");
		dataList.add("4,4,4,4,1,3");
		dataList.add("4,4,4,4,4,3");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");*/
		
		
		// SensorData
		/*dataList.add("8,8,8,1,1,7");
		dataList.add("8,8,8,1,1,7");
		dataList.add("8,8,8,1,1,7");
		dataList.add("8,8,8,1,1,7");
		dataList.add("8,8,8,1,1,7");
		dataList.add("8,8,8,1,1,7");
		dataList.add("2,2,2,1,1,7");
		dataList.add("1,1,1,1,1,7");
		dataList.add("4,4,4,1,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,2,4,3");
		dataList.add("4,4,4,2,1,3");
		dataList.add("1,1,1,2,2,3");
		dataList.add("4,4,4,2,2,3");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,4,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,1,4,7");
		dataList.add("4,4,4,4,1,7");
		dataList.add("2,2,2,4,1,7");
		dataList.add("1,1,1,4,4,7");
		dataList.add("4,4,4,4,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("4,4,4,1,4,2");
		dataList.add("2,2,2,1,1,2");
		dataList.add("4,4,4,2,2,1");*/	
	}
	
	public void setFastestPathCalibration(boolean fastestPathCalibration) {
		this.fastestPathCalibration = fastestPathCalibration;
	}

	public void setDetectNextMove(boolean detectNextMove) {
		this.detectNextMove = detectNextMove;
	}
	
	public int getPositionX() {
		// TODO Auto-generated method stub
		return positionX;
	}

	public int getPositionY() {
		// TODO Auto-generated method stub
		return positionY;
	}

	public int getDirection() {
		// TODO Auto-generated method stub
		return direction;
	}
	
	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	public void setPositionX(int x) {
		positionX = x;
	}
	
	public void setPositionY(int y) {
		positionY = y;
	}
	
	public int getCenterPositionX(){
		return positionX + 1;
	}
	
	public int getCenterPositionY(){
		return positionY + 1;
	}
	
	public boolean isWithinRobot(int x, int y) {
		return y < getPositionY()+2 && y >= getPositionY() && x < getPositionX()+2 
				&& x >= getPositionX();
	}
	
	boolean flag= true;
	
	public void sense(boolean realRun) {
        if (realRun) {
        	if(flag == true) {
        		flag = false;
        		SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "D");
        	}
        	
        		int[] sensorReadings = null;
        		
        		if(getDataFromList) {
        			try {
						Thread.sleep(1000);
						sensorReadings = MessageMgr.parseSensorData(dataList.get(dataListCount), sensor.size());
						dataListCount++;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		} else {
        			String sensorData = SocketMgr.getInstance().receiveMessage(true);
                    while ((sensorReadings = MessageMgr.parseSensorData(sensorData, sensor.size())) == null) {
                    	sensorData = SocketMgr.getInstance().receiveMessage(false);
                    }
        		}
        		
                // only update the map if it is not the fastest path calibration 
                // at the start point at the end of the exploration
                if(!fastestPathCalibration) {
                	for (int i = 0; i < sensor.size(); i++) {
    	            	int heading = sensor.get(i).getRealDirection();
    	            	int range = sensor.get(i).getRange();
    		          	int x = sensor.get(i).getRealPositionX();
    		          	int y = sensor.get(i).getRealPositionY();
    		          	
    		          	// Right Sensor
    		          	/*if(i == sensor.size() - 1) {
    		          		rightSensor = true;
    		          		// Only check when the rightObstWithinFrontRange flag is false
    		          		if(!rightObstWithinFrontRange) {
    			          		// Obstacle detected
    		            		if(sensorReadings[i] <= range) {
    		            			// Since right sensor is computed from the center
    		            			// the reading 2 and 3 will be within the front sensor range
    		            			// Technically, 1 should not be given, 
    		            			// but I included it in the case so that the front sensor can correct it
    		            			if(!detectNextMove && sensorReadings[i] <= 3) {
    		            				rightObstWithinFrontRange = true;
    		            			} else {
    		            				rightObstWithinFrontRange = false;
    		            			}
    		            		}
    		            	}
    		          	} else {
    		          		rightSensor = false;
    		          	}*/
    		          	
    		           	updateMap(sensorReadings[i], heading, range, x, y, true, sensor.get(i).getAccuracy());
    	            }
                } else {
                	System.out.println("Fastest Path Calibration at Start Position.\nNo Updating of Map");
                }
        } else { 
            for (Sensor eachSensor : sensor) {
                int sensedDistance = eachSensor.sense(this.grid);          
                int direction = eachSensor.getRealDirection();
                int range = eachSensor.getRange();
                int x = eachSensor.getRealPositionX();
                int y = eachSensor.getRealPositionY();
                updateMap(sensedDistance, direction, range, x, y, false, eachSensor.getAccuracy());
                
                System.out.println("sensor "+ sensor.indexOf(eachSensor) + " X position is " +eachSensor.getRealPositionX());
                System.out.println("sensor "+ sensor.indexOf(eachSensor) + " Y position is " +eachSensor.getRealPositionY());
                System.out.println("The distance is " + sensedDistance + "\n");
            }
        }
    }


	private void updateMap(int sensedDistance, int direction, int range, int x, int y, boolean realRun, int accuracy) {
		// TODO Auto-generated method stub
	       	int updateX = x;
	       	int updateY = y;
	        boolean obstacleInfront = sensedDistance <= range;
	        int distance = Math.min(sensedDistance, range);

	        for (int i = 1; i <= distance; i++) {
	            if (direction == NORTH) {
	            	updateY = updateY - 1;
	            } else if (direction == SOUTH) {
	            	updateY = updateY + 1;
	            } else if (direction == EAST) {
	            	updateX = updateX + 1;
	            } else if (direction == WEST) {
	            	updateX = updateX - 1;
	            }
	            
	            this.grid.setIsExplored(updateX, updateY, true);
	            // if this cell is not an obstacle
	            if (!(obstacleInfront && i == distance)) {
	            	if (!realRun) {
	                	this.grid.setIsObstacle(updateX, updateY, false);
	                }
	                else {
	                    this.grid.setProbabilityOfObstacle(updateX, updateY, -accuracy); // decrement by reliability
	                }
	            } 
	            // if this cell is an obstacle
	            else { 
	            	if (!realRun) {
	                	this.grid.setIsObstacle(updateX, updateY, true);
	                } 
	                else {
	                    this.grid.setProbabilityOfObstacle(updateX, updateY, accuracy); // increment by reliability
	                }
	            }
	        }
	        
	        /*if(distance != range) {
	        	// decrease the reliability of the remaining Cells behind the obstacles
		        for (int i = distance + 1; i <= range; i++) {
		        	if (direction == NORTH) {
		            	updateY = updateY - 1;
		            } else if (direction == SOUTH) {
		            	updateY = updateY + 1;
		            } else if (direction == EAST) {
		            	updateX = updateX + 1;
		            } else if (direction == WEST) {
		            	updateX = updateX - 1;
		            }
		        	
		        	this.grid.setProbabilityOfObstacle(updateX, updateY, -accuracy); // decrement by reliability
		        	//this.grid.setIsExplored(updateX, updateY, false); // set the Cell to Unexplored
		        }
	        }*/
		        
	        /*if(realRun) {
	        	// After updating the Map, check if the right sensor updated the portion which is within the front sensor range
		        if(rightObstWithinFrontRange && rightSensor) {
		        	if(!detectNextMove) {
		        		if(!turnBackToOriginal) {
			        		SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "R");
				        	turn(RIGHT);
				        	turnBackToOriginal = true;
				        	detectNextMove = false;
				        	sense(realRun);
			        	} else {
			        		SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "L");
				            turn(LEFT);
				            turnBackToOriginal = false;
				            rightObstWithinFrontRange = false;
				            detectNextMove = true;
				            sense(realRun);
			        	}
		        	}
		        }
	        }*/
	}
	
    public void move() { // Limit position to prevent wall crash
        if (this.direction == NORTH) { 
            this.positionY--;
            for (int i = 0; i < 3; ++i) {
                this.grid.setProbabilityOfObstacle(this.positionX + i, this.positionY, -1000);
                this.grid.setIsExplored(this.positionX + i, this.positionY,
                		true);
            }
        } 
        else if (this.direction == SOUTH) {
        	this.positionY++;
            for (int i = 0; i < 3; ++i) {
            	this.grid.setProbabilityOfObstacle(this.positionX + i, this.positionY + 2, -1000);
            	this.grid.setIsExplored(this.positionX + i, this.positionY + 2, true);
            }
        } 
        else if (this.direction == EAST) {
        	this.positionX++;
            for (int i = 0; i < 3; ++i) {
            	this.grid.setProbabilityOfObstacle(this.positionX + 2, this.positionY + i, -1000);
            	this.grid.setIsExplored(this.positionX + 2, this.positionY + i, true);
            }       	
        } 
        else if (this.direction == WEST) {
        	this.positionX--;
            for (int i = 0; i < 3; ++i) {
            	this.grid.setProbabilityOfObstacle(this.positionX, this.positionY + i, -1000);
            	this.grid.setIsExplored(this.positionX, this.positionY + i, true);
            }
        }
        setChanged();
        notifyObservers();
    }
    
    public void turn(int direction) {
 
        if (direction == LEFT) {
            /*
            NORTH BECOMES WEST
            WEST BECOMES SOUTH
            SOUTH BECOMES EAST
            EAST BECOMES NORTH
             */
            this.direction += 4;
            this.direction = (this.direction - 1) % 4;
        } else if (direction == RIGHT) {
            /*
            NORTH BECOMES EAST
            EAST BECOMES SOUTH
            SOUTH BECOMES WEST
            WEST BECOMES NORTH
             */
        	this.direction = (this.direction + 1) % 4;
        }
        setChanged();
        notifyObservers();
    }
    
    
    public boolean ableToCalibrateFront() { // DIRECTLY IN FRONT OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
        	if(i == 1) continue;
            if (this.direction == NORTH) {
                if (!this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return false;
                }
            } 
            else if (this.direction == SOUTH) {
                if (!this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                	return false;
                }
            }
            else if (this.direction == EAST) {
                if (!this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                	return false;
                }
            }
            else if (this.direction == WEST) {
                if (!this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                	return false;
                }
            }
        }
        return true;
    	/*if (this.direction == NORTH) {
            if (this.grid.getIsObstacle(this.positionX, this.positionY - 1)) {
                return true;
            }
        } 
        else if (this.direction == SOUTH) {
            if (this.grid.getIsObstacle(this.positionX, this.positionY + 1)) {
            	return true;
            }
        }
        else if (this.direction == EAST) {
            if (this.grid.getIsObstacle(this.positionX + 1, this.positionY)) {
            	return true;
            }
        }
        else if (this.direction == WEST) {
            if (this.grid.getIsObstacle(this.positionX - 1, this.positionY)) {
            	return true;
            }
        }
    	
    	return false;*/
    }
    
    public boolean ableToCalibrateLeft() {   // DIRECTLY BESIDE OF ROBOT
    	for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (i == 1) continue;
            if (this.direction == NORTH) {
                if (!this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                    return false;
                }
            }
            else if (this.direction == SOUTH) {
                if (!this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                    return false;
                }
            }
            else if (this.direction == EAST) {
                if (!this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return false;
                }
            }
            else if (this.direction == WEST) {

                if (!this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean isObstacleInfront() { // DIRECTLY IN FRONT OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (this.direction == NORTH) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return true;
                }
            }
            else if (this.direction == SOUTH) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                    return true;
                }
            }
            else if (this.direction == EAST) {
                if (this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == WEST) {
                if (this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isExploredObstacleInfront() { // DIRECTLY IN FRONT OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (this.direction == NORTH) {
                if (this.grid.getIsExplored(this.positionX + i, this.positionY - 1) && this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return true;
                }
            }
            else if (this.direction == SOUTH) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                    return true;
                }
            }
            else if (this.direction == EAST) {
                if (this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == WEST) {
                if (this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isObstacleOnLeftSide() { // DIRECTLY BESIDE OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (this.direction == NORTH) {
                if (this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == SOUTH) {
                if (this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == EAST) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return true;
                }
            }
            else if (this.direction == WEST) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isObstacleOnLeftSide2() { // DIRECTLY BESIDE OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (this.direction == NORTH) {
            	if(isOutOfArenaForUturn(this.positionX - 1, this.positionY + i)) {
            		return false;
            	}
            	else if (this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                    return true;
                } else if(isOutOfArenaForUturn(this.positionX - 2, this.positionY + i))
            		return false;
            	else if (this.grid.getIsObstacle(this.positionX - 2, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == SOUTH) {
            	if(isOutOfArenaForUturn(this.positionX + 3, this.positionY + i)) {
            		return false;
            	}
            	else if (this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                    return true;
                }
            	else if(isOutOfArenaForUturn(this.positionX + 4, this.positionY + i)) {
            		return false;
            	}
            	else if (this.grid.getIsObstacle(this.positionX + 4, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == EAST) {
            	if(isOutOfArenaForUturn(this.positionX + i, this.positionY - 1))
            		return false;
            	else if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return true;
                } else if(isOutOfArenaForUturn(this.positionX + i, this.positionY - 2))
            		return false;
            	else if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 2)) {
                    return true;
                }
            }
            else if (this.direction == WEST) {
            	if(isOutOfArenaForUturn(this.positionX + i, this.positionY + 3))
            		return false;
            	else if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                    return true;
                } else if(isOutOfArenaForUturn(this.positionX + i, this.positionY + 4))
            		return false;
            	else if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 4)) {
                    return true;
                }
            }
        }
        return false;
    }
    
	public boolean isOutOfArenaForUturn(int x, int y) {
        return x < 0 || y < 0 || x >= 13 || y >= 18;
    }
    
    public boolean isObstacleOnRightSide() { // DIRECTLY BESIDE OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (this.direction == NORTH) {
                if (this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == SOUTH) {
                if (this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == EAST) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                    return true;
                }
            }
            else if (this.direction == WEST) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return true;
                }
            }
        }
        return false;
    	
    }
    
    public boolean isObstacleOnRightSide2() { // DIRECTLY BESIDE OF ROBOT
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (this.direction == NORTH) {
            	if (this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                    return true;
                }
            	else if (this.grid.getIsObstacle(this.positionX + 4, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == SOUTH) {
            	if (this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                    return true;
                }
            	else if (this.grid.getIsObstacle(this.positionX - 2, this.positionY + i)) {
                    return true;
                }
            }
            else if (this.direction == EAST) {
            	if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                    return true;
                }
            	else if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 4)) {
                    return true;
                }
            }
            else if (this.direction == WEST) {
            	if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                    return true;
                }
            	else if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 2)) {
                    return true;
                }
            }
        }
        return false;
    	
    }
    
    // returns true when there are at least 2 obstacles on the right
    public boolean isObstacleOnRightSideForCalibration() {
    	int rightObstacleCount = 0;
    	
        for (int i = 0; i < SIZE_OF_ROBOT; i++) {
            if (this.direction == NORTH) {
                if (this.grid.getIsObstacle(this.positionX + 3, this.positionY + i)) {
                	rightObstacleCount++;
                }
            }
            else if (this.direction == SOUTH) {
                if (this.grid.getIsObstacle(this.positionX - 1, this.positionY + i)) {
                	rightObstacleCount++;
                }
            }
            else if (this.direction == EAST) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY + 3)) {
                	rightObstacleCount++;
                }
            }
            else if (this.direction == WEST) {
                if (this.grid.getIsObstacle(this.positionX + i, this.positionY - 1)) {
                	rightObstacleCount++;
                }
            }
        }
        
        if(rightObstacleCount >= 2) {
        	return true;
        }
        
        return false;
    }
    
    public void resetRobot() {
    	this.positionX = STARTING_X_POSITION;
    	this.positionY = STARTING_Y_POSITION;
        this.direction = NORTH;
        setChanged();
        notifyObservers();
    }

	public void sense(boolean realRun, boolean saveReadings) {
		if (realRun) {
        	if(flag == true) {
        		flag = false;
        		SocketMgr.getInstance().sendMessage(CALL_ARDUINO, "D");
        	}
        	
        	int[] sensorReadings = null;
    		
    		if(getDataFromList) {
    			try {
					Thread.sleep(1000);
					sensorReadings = MessageMgr.parseSensorData(dataList.get(dataListCount), sensor.size());
					dataListCount++;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		} else {
    			String sensorData = SocketMgr.getInstance().receiveMessage(true);
                while ((sensorReadings = MessageMgr.parseSensorData(sensorData, sensor.size())) == null) {
                	sensorData = SocketMgr.getInstance().receiveMessage(false);
                }
    		}
                
                // only update the map if it is not the fastest path calibration 
                // at the start point at the end of the exploration
                if(!fastestPathCalibration) {
                	if(saveReadings) {
                		frontLeft = sensorReadings[0];
                    	frontCenter = sensorReadings[1];
                    	frontRight = sensorReadings[2];
                    	leftFront = sensorReadings[3];
                    	leftBack = sensorReadings[4];
                	}
                	for (int i = 0; i < sensor.size(); i++) {
    	            	int heading = sensor.get(i).getRealDirection();
    	            	int range = sensor.get(i).getRange();
    		          	int x = sensor.get(i).getRealPositionX();
    		          	int y = sensor.get(i).getRealPositionY();
    		           	updateMap(sensorReadings[i], heading, range, x, y, true, sensor.get(i).getAccuracy());
    	            }
                } else {
                	System.out.println("Fastest Path Calibration at Start Position.\nNo Updating of Map");
                }
        } else { 
            for (Sensor eachSensor : sensor) {
                int sensedDistance = eachSensor.sense(this.grid);          
                int direction = eachSensor.getRealDirection();
                int range = eachSensor.getRange();
                int x = eachSensor.getRealPositionX();
                int y = eachSensor.getRealPositionY();
                updateMap(sensedDistance, direction, range, x, y, false, eachSensor.getAccuracy());
                
                System.out.println("sensor "+ sensor.indexOf(eachSensor) + " X position is " +eachSensor.getRealPositionX());
                System.out.println("sensor "+ sensor.indexOf(eachSensor) + " Y position is " +eachSensor.getRealPositionY());
                System.out.println("The distance is " + sensedDistance + "\n");
            }
        }
	}
}
