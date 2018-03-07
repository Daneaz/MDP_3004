package model.physical;
import static constant.RobotConstant.*;

public class Sensor {

	private int xPosition;
	private int yPosition;
	private int range;
	private int direction;
	private int accuracy;
	private Robot robot;
	
	public Sensor(int xPosition, int yPosition, int range, int direction, int accuracy){
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.range = range;
		this.direction = direction;
		this.accuracy = accuracy;
	}
	
	int sense(Grid grid){
		int realDirection = getRealDirection();
		int absolutePositionX = getRealPositionX();
		int absolutePositionY = getRealPositionY();
		
		for (int i = 1; i <= range; i++) {
            if (realDirection == NORTH) {
                if (grid.getIsObstacle(absolutePositionX, absolutePositionY - i))
                    return i;
            } else if (realDirection == EAST) {
                if (grid.getIsObstacle(absolutePositionX + i, absolutePositionY))
                    return i;
            } else if (realDirection == SOUTH) {
                if (grid.getIsObstacle(absolutePositionX, absolutePositionY + i))
                    return i;
            } else if (realDirection == WEST) {
                if (grid.getIsObstacle(absolutePositionX - i, absolutePositionY))
                    return i;
            }
        }
		
		return 100;
	}

	

	
	int getRealPositionX() {
		// TODO Auto-generated method stub
		if (this.robot.getDirection() == NORTH) {        	
            return getRealRobotPositionX() + this.xPosition;
        } else if (this.robot.getDirection() == EAST) {
        	//System.out.println("Actual X position for sensor is " + (getRealRobotPositionX() - this.yPosition));
            return getRealRobotPositionX() - this.yPosition;
        } else if (this.robot.getDirection() == SOUTH) {
            return getRealRobotPositionX() - this.xPosition;
        } else if (this.robot.getDirection() == WEST) {
            return getRealRobotPositionX() + this.yPosition;
        }
        return 0;
	}

	private int getRealRobotPositionX() {
		// TODO Auto-generated method stub
		if (this.robot.getDirection() == SOUTH || this.robot.getDirection() == EAST)
            return this.robot.getPositionX() + 2;
        return this.robot.getPositionX();
	}

	int getRealPositionY() {
		// TODO Auto-generated method stub
		if (this.robot.getDirection() == NORTH) {        	
            return getRealRobotPositionY() + this.yPosition;
        } else if (this.robot.getDirection() == EAST) {
        	//System.out.println("Actual Y position for sensor is " + (getRealRobotPositionY() + this.yPosition));
            return getRealRobotPositionY() + this.xPosition;
        } else if (this.robot.getDirection() == SOUTH) {
            return getRealRobotPositionY() - this.yPosition;
        } else if (this.robot.getDirection() == WEST) {
            return getRealRobotPositionY() - this.xPosition;
        }
		return 0;
	}
	
	private int getRealRobotPositionY() {
		// TODO Auto-generated method stub
        if (this.robot.getDirection() == SOUTH || this.robot.getDirection() == WEST)
            return this.robot.getPositionY() + 2;
        return this.robot.getPositionY();
	}

	int getRealDirection() {
		// TODO Auto-generated method stub
		int realDirection = -1;
        if (this.direction == LEFT) {
        	realDirection = (this.robot.getDirection() + 3) % 4;
        } else if (this.direction == MIDDLE) {
        	realDirection = this.robot.getDirection();
        } else if (this.direction == RIGHT) {
        	realDirection = (this.robot.getDirection() + 1) % 4;
        }
        return realDirection;
	}
	
	public void setSensorOnRobot(Robot robot){
		this.robot = robot;
	}
	
	int getRange(){
		return this.range;
	}
	
	public int getAccuracy(){
		return this.accuracy;
	}

}
