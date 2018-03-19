package model.util;

import java.util.ArrayList;
import java.util.List;

import static constant.MapConstant.MAP_ROWS;
import static constant.RobotConstant.*;

/**
 * Message generator
 */
public class MessageMgr {

    public static String generateFinalDescriptor(String part1, String part2) {
        return "{finaldescriptor:\"" + part1 + "," + part2 + "\"}";
    }

    /**
     * Generate map string for Android communication, note that on Android the coordinate of
     * the robot is the upper right corner.
     * @param descriptor Map descriptor in Android format
     * @param x Robot's x coordinates
     * @param y Robot's y coordinates
     * @param heading Robot's heading
     * @return Message string for sending to Android
     */
    public static String generateMapDescriptorMsg(String part1, String part2, int x, int y, int heading) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"robot\":\"");
        builder.append(part1);
        builder.append(",");
        builder.append(part2);
        builder.append(",");
        builder.append(y);
        builder.append(",");
        builder.append(x);
        builder.append(",");
        if (heading == NORTH) {
            builder.append(0);
        } else if (heading == EAST) {
            builder.append(90);
        } else if (heading == SOUTH) {
            builder.append(180);
        } else if (heading == WEST) {
            builder.append(270);
        }
        builder.append("\"}");
        return builder.toString();
    }

    /**
     * Parse waypoint message from Android, the Y coordinate received
     * starts from the bottom, so it's reversed.
     * @param msg
     * @return
     */
    public static List<Integer> parseMessage(String msg) {
    	if(msg!=null) {
    		System.out.println("I received: " + msg);
    	}
    
    	String[] splitString = msg.split(",", 5);
        List<Integer> robotStartPosAndWaypoints = new ArrayList<>();

        //Integer wayPointX, wayPointY;
        try {
            //wayPointX = Integer.parseInt(splitString[0]);
            //wayPointY = MAP_ROWS - Integer.parseInt(splitString[1]) - 1;
        	
        	/* robotStartPosAndWaypoints.get(0): Robot Start X-Coordinate,
             * robotStartPosAndWaypoints.get(1): Robot Start Y-Coordinate,
             * robotStartPosAndWaypoints.get(2): Robot Start Head Direction,
             * robotStartPosAndWaypoints.get(3): WayPoint X-Coordinate,
             * robotStartPosAndWaypoints.get(4): WayPoint Y-Coordinate,
             */
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[0].trim()));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[1].trim()));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[2].trim()));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[3].trim()));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[4].trim()));
            return robotStartPosAndWaypoints;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	public static int[] parseSensorData(String sensorData, int size) {
		// no need to process the data if the sensorData is null
		if(sensorData == null) {
			System.out.println("Sensor Data is null");
    		return null;
    	}
    
		System.out.println("Parsing Sensor Data: " + sensorData);
		
		// sensor data will be 1,2,1,1,2,6
		// using split to get the sensor data for respective sensors
    	String[] sensorReadings = sensorData.split(",", size);
    	
    	// the size of the sensorReadings does not match the number of sensors
    	if(sensorReadings.length != size) {
    		System.out.println("Invalid Sensor Data size");
    		return null;
    	} else {
    		// Instantiate a new integer array based on number of sensors
    		int[] sensorsConvertedDistance = new int[sensorReadings.length];

            try {
            	// Process every sensors data into sensorsConvertedDistance and return it
            	for(int i=0; i<sensorReadings.length; i++) {
            		/*int returnedDistance = (int)Double.parseDouble(sensorReadings[i].trim());
                    double Distance = returnedDistance;
                    if(i == (size-1)) {
                    	if(Distance <= 29) {
                    		returnedDistance = -1;
                    	} else if(Distance < 35) {
                    		returnedDistance = (int) Math.round(Distance/10.0);
                    	} else if(Distance >= 35){
                    		returnedDistance = (int) Math.ceil(Distance/10.0);
                    	}
                    } else {
                    	returnedDistance = (int) Math.ceil(Distance/10.0);
                    }*/
            		int returnedDistance;
            		
            		// Right Sensor received exact Obstacles Block Distance
            		//if (i == (size-1)) {
            		
            		// Arduino will now send the exact block distance so no need for rounding and ceiling
            		// Parse to Double in case Arduino sends 1.0 instead of 1
            		returnedDistance = (int)Double.parseDouble(sensorReadings[i].trim());
            		
            		//} 
            		// All the other sensors Ceiling the value after dividing by 10
            		/*else {
            			returnedDistance = (int)Double.parseDouble(sensorReadings[i].trim());
            			double Distance = returnedDistance;
            			returnedDistance = (int) Math.ceil(Distance/10.0);
            		}*/
                    System.out.println("Casting " + returnedDistance + "\n");
                    sensorsConvertedDistance[i] = returnedDistance;
            	}
                return sensorsConvertedDistance;
            } catch (NumberFormatException e1) {
            	e1.printStackTrace();
            	System.out.println("Invalid Sensor Data Number Format");
                return null;
            }
            catch (Exception e) {
                e.printStackTrace();
                System.out.println("Invalid Sensor Data / Invalid Data Format");
                return null;
            }
    	}
	}
}
