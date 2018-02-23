package model.physical;

import java.util.List;
import java.util.Observable;


import static constant.RobotConstant.*;



public class Robot extends Observable {
	private int positionX = STARTING_X_POSITION;
	private int positionY = STARTING_Y_POSITION;
	private int direction = NORTH;
	private List<Sensor> sensor;
	private Grid grid;
	
	public Robot(Grid grid, List<Sensor> sensor){
		this.grid = grid;
		this.sensor = sensor;
		for(int i = 0; i < sensor.size(); i++){
			sensor.get(i).setSensorOnRobot(this);
		}
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
	
	public void sense(boolean realRun) {
        /*if (realRun) {
            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "S");
            String sensorData = SocketMgr.getInstance().receiveMessage(true);
            while (sensorData == null) {
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "S");
                sensorData = SocketMgr.getInstance().receiveMessage(true);
            }
            String[] sensorReadings = sensorData.split(",", mSensors.size());
            for (int i = 0; i < mSensors.size(); i++) {
                int returnedDistance = Integer.parseInt(sensorReadings[i]);
                int heading = mSensors.get(i).getActualHeading();
                int range = mSensors.get(i).getRange();
                int x = mSensors.get(i).getActualPosX();
                int y = mSensors.get(i).getActualPosY();
                updateMap(returnedDistance, heading, range, x, y, true, mSensors.get(i).getReliability());
            }
        } else { */
            for (int i =0; i < this.sensor.size(); i ++) {
                int sensedDistance = sensor.get(i).sense(this.grid);          
                int direction = sensor.get(i).getRealDirection();
                int range = sensor.get(i).getRange();
                int x = sensor.get(i).getRealPositionX();
                int y = sensor.get(i).getRealPositionY();
                updateMap(sensedDistance, direction, range, x, y, false, this.sensor.get(i).getAccuracy());
                
                System.out.println("sensor "+ this.sensor.indexOf(sensor) + " X position is " +sensor.get(i).getRealPositionX());
                System.out.println("sensor "+ this.sensor.indexOf(sensor) + " Y position is " +sensor.get(i).getRealPositionY());
                System.out.println("The distance is " + sensedDistance + "\n");
            //}
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
	            // if this cell is an obstacle
	            if (!(obstacleInfront && i == distance)) {
	            	if (!realRun) {
	                	this.grid.setIsObstacle(updateX, updateY, false);
	                }
	                else {
	                    this.grid.setProbabilityOfObstacle(updateX, updateY, -accuracy); // decrement by reliability

	                }
	            }
	            
	            else { // if this cell is not an obstacle
	            	if (!realRun) {
	                	this.grid.setIsObstacle(updateX, updateY, true);
	                } 
	                else {
	                    this.grid.setProbabilityOfObstacle(updateX, updateY, accuracy); // increment by reliability
	                }
	            }
	        }
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
            if (i == 1) continue;
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
    
    public boolean isObstacleRight() { // DIRECTLY BESIDE OF ROBOT
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
    
    public void resetRobot() {
    	this.positionX = STARTING_X_POSITION;
    	this.positionY = STARTING_Y_POSITION;
        this.direction = NORTH;
        setChanged();
        notifyObservers();
    }
}
