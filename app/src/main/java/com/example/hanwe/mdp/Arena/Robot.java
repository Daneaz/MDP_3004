package com.example.hanwe.mdp.Arena;

import android.content.Context;
import com.example.hanwe.mdp.Configuration.Operation;

public class Robot {
    private int xPos, yPos;
    private Direction direction;
    private Context context;

    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    public Robot(Context context, int xPos, int yPos, Direction direction) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.direction = direction;
        this.context = context;
    }

    public int getXPos() { return xPos; }

    public int getYPos() {
        return yPos;
    }

    public void setPosition(int xPos, int yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    // takes in a boolean parameter amd, amd true means the command is sent from AMD tool
    public boolean moveForward(boolean amd) {
        switch (direction) {
            case NORTH:
                if (xPos > 1 || amd) xPos --;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;
            case SOUTH:
                if (xPos < 18 || amd) xPos ++;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;
            case EAST:
                if (yPos < 13 || amd) yPos ++;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;

            case WEST:
                if (yPos > 1 || amd) yPos --;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;
        }
        return true;
    }

    // takes in a boolean parameter amd, amd true means the command is sent from AMD tool
    public boolean reverse(boolean amd) {
        switch (direction) {
            case NORTH:
                if (xPos < 18 || amd) xPos ++;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;
            case SOUTH:
                if (xPos > 1 || amd) xPos --;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;
            case EAST:
                if (yPos > 1 || amd) yPos --;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;

            case WEST:
                if (yPos < 13 || amd) yPos ++;
                else {
                    Operation.showToast(context, "CRASH! Command not sent!");
                    return false;
                }
                break;
        }
        return true;
    }

    public void turnLeft() {
        switch (direction) {
            case NORTH:
                direction = Direction.WEST;
                break;

            case SOUTH:
                direction = Direction.EAST;
                break;

            case EAST:
                direction = Direction.NORTH;
                break;

            case WEST:
                direction = Direction.SOUTH;
                break;
        }
    }

    public void turnRight() {
        switch (direction) {
            case NORTH:
                direction = Direction.EAST;
                break;

            case SOUTH:
                direction = Direction.WEST;
                break;

            case EAST:
                direction = Direction.SOUTH;
                break;

            case WEST:
                direction = Direction.NORTH;
                break;
        }
    }
}