package model.physical;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Observable;



import static constant.MapConstant.*;




public class Grid extends Observable {

	private Cell[][] cells;
	
	public Grid(){
		cells = new Cell[MAP_COLUMNS][MAP_ROWS];
		for (int x = 0; x < MAP_COLUMNS; x++){
			for ( int y = 0; y < MAP_ROWS; y++){
				cells[x][y] = new Cell();
			}
		}
		
		reset();
		
	}
	
	public Cell[][] getCell() {
        return cells;
    }

	 public static boolean isInStartingZone(int x, int y){
		return (y < MAP_ROWS) && (y >= MAP_ROWS - ZONE_SIZE) && (x < ZONE_SIZE) && (x >= 0);
	}
	
	public static boolean isInEndingZone(int x, int y){
		return (y < ZONE_SIZE) && (y >= 0) && (x < MAP_COLUMNS) && (x >= MAP_COLUMNS - ZONE_SIZE);
                
	}
	
	public boolean isOutOfArena(int x, int y) {
        return x < 0 || y < 0 || x >= MAP_COLUMNS || y >= MAP_ROWS;
    }
	
	public boolean getIsObstacle(int x, int y){
		return isOutOfArena(x, y) || cells[x][y].getIsObstacle();
	}
	
	public void setIsObstacle(int x, int y, boolean isObstacle){
		if(isOutOfArena(x, y))
			return;
		cells[x][y].setIsObstacle(isObstacle);
		setChanged();
		notifyObservers();
	}
	
	public boolean getIsExplored(int x, int y){
		return cells[x][y].getExplored() && !isOutOfArena(x, y);
	}
	
	public void setIsExplored(int x, int y, boolean explored){
		if(!isOutOfArena(x, y)){
			cells[x][y].setExplored(explored);
			setChanged();
	        notifyObservers();
		}
		return;
	}
	
	public void setProbabilityOfObstacle(int x, int y, int num){
		if(!isOutOfArena(x, y)){
			cells[x][y].updateCount(num);			
		}
		return;
	}
	
	public double checkPercentageExplored(){
		double gridCells = 0.0;
		double cellsExplored = 0.0;
		
		for (int x = 0; x < MAP_COLUMNS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                if (cells[x][y].getExplored()) {
                    cellsExplored += 1;
                }
                gridCells += 1;
            }
        }
		
		return (cellsExplored/gridCells) * 100;
	}
	
	public void reset(){
		for (int x = 0; x < MAP_COLUMNS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                if (!isInStartingZone(x, y) && !isInEndingZone(x, y))
                    setIsExplored(x, y, false);
                else
                    setIsExplored(x, y, true);
            }
        }
	}
	
	public void clearAllObstacle(){
		for(int x = 0; x < MAP_COLUMNS; x++){
			for(int y  = 0; y < MAP_ROWS; y++){
				setIsObstacle(x, y, false);
			}
		}
	}
	
	public void loadingFromDisk(String filePath) throws IOException{
		this.reset();
		BufferedReader bufferReader = new BufferedReader(new FileReader(filePath));
		
		for(int y = 0; y < MAP_ROWS; y++) {
			String line = bufferReader.readLine();
			String [] numString = line.trim().split("\\s+");
			for(int x = 0; x < MAP_COLUMNS; x++){
				if(numString[x].equals("1")){
					this.setIsObstacle(x, y, true);
				}
				else{
					this.setIsObstacle(x, y, false);
				}
			}
		}
	}
	
	public String generateMapDescriptor1(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(11);
		for(int y = MAP_ROWS - 1; y >= 0; y--){
			for(int x = 0; x < MAP_COLUMNS; x++){
				if(getIsExplored(x, y)){
					stringBuilder.append(1);
				}
				else{
					stringBuilder.append(0);
				}
			}
		}
		
		stringBuilder.append(11);
		String string = stringBuilder.toString();
		stringBuilder = new StringBuilder();
		for (int i = 0; i < string.length() / 4; i++) {
            stringBuilder.append(Integer.toHexString(Integer.parseInt(string.substring(i * 4, (i + 1) * 4), 2)));
        }
		
		/*System.out.println("Map Descriptor 1:");
		System.out.println(stringBuilder.toString());*/
		
		return stringBuilder.toString();
	}
	
	public String generateMapDescriptor2(){
		StringBuilder stringBuilder = new StringBuilder();
		for(int y = MAP_ROWS - 1; y >= 0; y--){
			for(int x = 0; x < MAP_COLUMNS; x++){
				if (getIsExplored(x, y)) {
					if(getIsObstacle(x, y)){
						stringBuilder.append(1);
					}
					else{
						stringBuilder.append(0);
					}
				}
			}
		}
		
		while (0 != (stringBuilder.length() % 8)) {
            stringBuilder.append(0);
        }
        String string = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        for (int i = 0; i < string.length() / 4; i++) {
            stringBuilder.append(Integer.toHexString(Integer.parseInt(string.substring(i * 4, (i + 1) * 4), 2)));

        }
        /*System.out.println("Map descriptor part 2:");
        System.out.println(stringBuilder.toString());*/

        return stringBuilder.toString();
	}

	/*public void clearClosedSet() {
		for (int x = 0; x < MAP_COLUMNS; x++){
			for ( int y = 0; y < MAP_ROWS; y++){
				cells[x][y].setIsClosedSet(false);;
			}
		}
	}*/
}
