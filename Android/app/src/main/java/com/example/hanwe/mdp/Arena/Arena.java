package com.example.hanwe.mdp.Arena;

import android.content.Context;

import com.example.hanwe.mdp.Configuration.Config;
import com.example.hanwe.mdp.Configuration.Operation;

public class Arena {
    private GridCell[][] gridMap;
    private Robot robot;
    private WayPoint waypoint;
    private char[] mdf1, mdf2;

    protected enum GridCell {
        UNEXPLORED, FREE_SPACE, OBSTACLE
    }

    public Arena(Context context) {
        gridMap = new GridCell[Config.ARENA_WIDTH][Config.ARENA_LENGTH];
        robot = new Robot(context, 18, 1, Robot.Direction.NORTH);
        waypoint = new WayPoint(18,1);

        mdf1 = new char[Config.ARENA_AREA];
        mdf2 = new char[Config.ARENA_AREA];

        for (int x = 0; x < Config.ARENA_WIDTH; x ++) {
            for (int y = 0; y < Config.ARENA_LENGTH; y ++) {
                gridMap[x][y] = GridCell.UNEXPLORED;

                mdf1[x * Config.ARENA_LENGTH + y] = '0';
                mdf2[x * Config.ARENA_LENGTH + y] = ' ';
            }
        }
    }

    public GridCell[][] getGridMap() {
        return gridMap;
    }

    public Robot getRobot() {
        return robot;
    }

    public WayPoint getWayPoint() {
        return waypoint;
    }

    // This method only works for AMD Tool since AMD Tool only provide '1' for obstacle and 0 for unexplored
    public void updateGridMap(String gridData) {
        int count = 0;
        for (int x = 0; x < Config.ARENA_WIDTH; x ++) {
            for (int y = 0; y < Config.ARENA_LENGTH; y++) {
                if (count < gridData.length()) {
                    if(gridData.substring(count,count+1).equalsIgnoreCase("0")) {
                        gridMap[x][y] = GridCell.UNEXPLORED;
                        mdf1[x * Config.ARENA_LENGTH + y] = '0';
                        mdf2[x * Config.ARENA_LENGTH + y] = ' ';
                    } else if(gridData.substring(count,count+1).equalsIgnoreCase("1")) {
                        gridMap[x][y] = GridCell.OBSTACLE;
                        mdf1[x * Config.ARENA_LENGTH + y] = '1';
                        mdf2[x * Config.ARENA_LENGTH + y] = '1';
                    }
                    count++;

                    /*  This code is for drawing free space. AMD does not provide the tool to show it
                    Place this code in the "0" case to get the optimal mdf1 and mdf2 string
                        gridMap[x][y] = GridCell.FREE_SPACE;
                        mdf1[x * Config.ARENA_LENGTH + y] = '1';
                        mdf2[x * Config.ARENA_LENGTH + y] = '0';*/
                }
            }
        }
    }

    public void updateGridMapFromPc(String part1, String part2) {
        int p1Count = 2;
        int p2Count = 0;
        for (int x = Config.ARENA_WIDTH - 1; x >= 0; x--) {
            for (int y = 0; y < Config.ARENA_LENGTH; y++) {
                if (p1Count < part1.length()-2) {
                    // Unexplored
                    if (part1.substring(p1Count, p1Count + 1).equalsIgnoreCase("0")) {
                        gridMap[x][y] = GridCell.UNEXPLORED;
                        mdf1[x * Config.ARENA_LENGTH + y] = '0';
                        mdf2[x * Config.ARENA_LENGTH + y] = ' ';
                    }
                    // Explored
                    else if (part1.substring(p1Count, p1Count + 1).equalsIgnoreCase("1")) {
                        // Free Space
                        if (part2.substring(p2Count, p2Count + 1).equalsIgnoreCase("0")) {
                            gridMap[x][y] = GridCell.FREE_SPACE;
                            mdf1[x * Config.ARENA_LENGTH + y] = '1';
                            mdf2[x * Config.ARENA_LENGTH + y] = '0';
                            p2Count++;
                        }
                        // Obstacle
                        else if (part2.substring(p2Count, p2Count + 1).equalsIgnoreCase("1")) {
                            gridMap[x][y] = GridCell.OBSTACLE;
                            mdf1[x * Config.ARENA_LENGTH + y] = '1';
                            mdf2[x * Config.ARENA_LENGTH + y] = '1';
                            p2Count++;
                        }
                    }
                    p1Count++;
                }
            }
        }
    }

    public String getMDF1() {
        return Operation.binaryToHex("11" + reconstructMDF(mdf1) + "11");
    }

    public String getMDF2() {
        return Operation.binaryToHex(reconstructMDF(mdf2).replaceAll(" ", ""));
    }

    public boolean isReset() {
        //Arena is considered reset if and only if all the grid cells are unexplored
        return String.valueOf(mdf2).replaceAll(" ", "").equalsIgnoreCase("");
    }

    public void resetArena() {
        robot.setPosition(18, 1);
        robot.setDirection(Robot.Direction.NORTH);
        waypoint.setPosition(18,1);

        for (int x = 0; x < Config.ARENA_WIDTH; x ++) {
            for (int y = 0; y < Config.ARENA_LENGTH; y ++) {
                gridMap[x][y] = GridCell.UNEXPLORED;

                mdf1[x * Config.ARENA_LENGTH + y] = '0';
                mdf2[x * Config.ARENA_LENGTH + y] = ' ';
            }
        }
    }

    private String reconstructMDF(char[] mdf) {
        String mdfStr = String.valueOf(mdf);
        for(int i = 0; i < mdfStr.length() / 2; i+= Config.ARENA_LENGTH) {
            String front = mdfStr.substring(i, i + Config.ARENA_LENGTH);
            String back = mdfStr.substring(mdfStr.length() - i - Config.ARENA_LENGTH, mdfStr.length() - i);
            if(i == 0) {
                mdfStr = back + mdfStr.substring(Config.ARENA_LENGTH, mdfStr.length() - i - Config.ARENA_LENGTH) + front;
            } else {
                mdfStr = mdfStr.substring(0, i) + back + mdfStr.substring(i + Config.ARENA_LENGTH, mdfStr.length() - i - Config.ARENA_LENGTH) + front + mdfStr.substring(mdfStr.length() - i, mdfStr.length());
            }
        }
        return mdfStr;
    }
}