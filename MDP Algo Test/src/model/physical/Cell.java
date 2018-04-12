package model.physical;

public class Cell implements Comparable<Cell> {

	private int x;
	private int y;
	private int distance;
	private boolean explored;
	private boolean isObstacle;
	private int count = 0;
	
	/*private boolean isFastest;
	private boolean isChecking;
	private boolean isClosedSet;*/
	
	Cell() {}
	
	public Cell (int x, int y){
		
		this.x = x;
		this.y = y;
	}
	
	public int getX(){
		return this.x;
	}
	
	public int getY(){
		return this.y;
	}
	
	private int getDistance(){
		return this.distance;
	}
	
	public void setDistance(int distance){
		this.distance = distance;
	}
	
	public boolean getExplored(){
		return this.explored;
	}
	
	 void setExplored(boolean explored){
		this.explored = explored;
	}
	
	public boolean getIsObstacle(){
		return this.isObstacle;
	}
	
	void setIsObstacle(boolean isObstacle){
		this.isObstacle = isObstacle;
	}
	
	int updateCount(int num){
		count += num;
		boolean prevIsObstacle = this.isObstacle;
		isObstacle = count > 0;
		
		if(prevIsObstacle && !isObstacle) {
			return -1;
		} else if(!prevIsObstacle && isObstacle) {
			return 1;
		}
		
		return 0;
	}
	
	@Override
	public int compareTo(Cell o) {
		// TODO Auto-generated method stub
		if(this.distance > o.getDistance())
			return 1;
		else if (this.distance < o.getDistance())
			return -1;
		else 
			return 0;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Cell){
			Cell otherCell = (Cell)o;
			if(otherCell.getY() == getY() && otherCell.getX() == getX())
				return true;
		}
		
		return false;
	}

	/*public boolean getIsFastestPath() {
		return isFastest;
	}
	
	public void setIsFastestPath(boolean isFastest) {
		this.isFastest = isFastest;
	}

	public boolean getIsChecking() {
		return isChecking;
	}
	
	public void setisChecking(boolean isChecking) {
		this.isChecking = isChecking;
	}
	
	public boolean getIsClosedSet() {
		return isClosedSet;
	}
	
	public void setIsClosedSet(boolean isClosedSet) {
		this.isClosedSet = isClosedSet;
	}*/
}
