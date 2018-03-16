package model.physical;

public class Cell implements Comparable<Cell> {

	private int x;
	private int y;
	private int distance;
	private boolean explored;
	private boolean isObstacle;
	private int count = 0;
	
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
	
	void updateCount(int num){
		count += num;
		isObstacle = count > 0;
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
}
