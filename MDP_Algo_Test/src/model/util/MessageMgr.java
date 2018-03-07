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
    public static String generateMapDescriptorMsg(String descriptor, int x, int y, int heading) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"robot\":\"");
        builder.append(descriptor);
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
        	
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[0]));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[1]));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[2]));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[3]));
        	robotStartPosAndWaypoints.add(Integer.parseInt(splitString[4]));
            return robotStartPosAndWaypoints;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
}
