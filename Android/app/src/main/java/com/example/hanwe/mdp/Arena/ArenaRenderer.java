package com.example.hanwe.mdp.Arena;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.example.hanwe.mdp.Configuration.Config;
import com.example.hanwe.mdp.R;


public class ArenaRenderer {
    private static int leftMostXPos, rightMostXPos, topMostYPos, bottomMostYPos;

    protected static void renderArena(Canvas canvas, Arena arena, int gridSize, Context context) {
        leftMostXPos  = (gridSize / 2);
        rightMostXPos = leftMostXPos + gridSize * Config.ARENA_LENGTH; // 17 + 34 * 15 = 527

        topMostYPos    = gridSize / 2;
        bottomMostYPos = topMostYPos + gridSize * Config.ARENA_WIDTH; // 17 + 34 * 20

        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);

        renderGridMap(canvas, paint, arena.getGridMap(), gridSize);
        renderStartZone(canvas, paint, gridSize);
        renderGoalZone(canvas, paint, gridSize);
        renderBorders(canvas, paint, gridSize);
        renderRobot(canvas, arena.getRobot(), gridSize, context);
        renderWayPoint(canvas, arena.getWayPoint(), gridSize, context);
    }

    private static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static void renderRobot(Canvas canvas, Robot robot, int gridSize, Context context) {
        Paint paint = new Paint();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.fa_robot)),
                gridSize * 3, gridSize * 3, false);

        Matrix matrix = new Matrix();

        if (robot.getDirection() == Robot.Direction.NORTH) {
            matrix.setRotate(0, scaledBitmap.getWidth() / 2, scaledBitmap.getHeight() / 2);

        } else if (robot.getDirection() == Robot.Direction.EAST) {
            matrix.setRotate(90, scaledBitmap.getWidth() / 2, scaledBitmap.getHeight() / 2);

        } else if (robot.getDirection() == Robot.Direction.SOUTH) {
            matrix.setRotate(180, scaledBitmap.getWidth() / 2, scaledBitmap.getHeight() / 2);

        } else if (robot.getDirection() == Robot.Direction.WEST) {
            matrix.setRotate(270, scaledBitmap.getWidth() / 2, scaledBitmap.getHeight() / 2);
        }

        matrix.postTranslate((gridSize / 2 + gridSize * (robot.getYPos() - 1))+gridSize,
                gridSize / 2 + gridSize * (robot.getXPos() - 1));
        canvas.drawBitmap(scaledBitmap, matrix, paint);
    }

    private static void renderWayPoint(Canvas canvas, WayPoint waypoint, int gridSize, Context context) {
        Paint paint = new Paint();
        Bitmap waypointBitmap = Bitmap.createScaledBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.fa_waypoint)),
                gridSize * 3, gridSize * 3, false);

        Matrix waypointMatrix = new Matrix();

        waypointMatrix.setRotate(0, waypointBitmap.getWidth() / 2, waypointBitmap.getHeight() / 2);

        waypointMatrix.postTranslate((gridSize / 2 + gridSize * (waypoint.getYPos() - 1))+gridSize,
                gridSize / 2 + gridSize * (waypoint.getXPos() - 1));
        canvas.drawBitmap(waypointBitmap, waypointMatrix, paint);
    }

    private static void renderGridMap(Canvas canvas, Paint paint, Arena.GridCell[][] gridMap, int gridSize) {
        float textX = 0, textY = 0;
        int xGridNum = 19;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(gridSize-17);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int x = 0; x < Config.ARENA_WIDTH; x ++) { // 20
            for (int y = 0; y < Config.ARENA_LENGTH; y ++) { // 15
                paint.setStyle(Paint.Style.FILL);

                float left   = gridSize / 2 + gridSize * y; // 17, 51, 85 .. 493
                float right  = left + gridSize; // 51, 85

                float top    = gridSize / 2 + gridSize * x; // 17 .. 663
                float bottom = top + gridSize; // 51 .. 697

                RectF rect = new RectF(left +gridSize, top, right+gridSize, bottom);

                switch (gridMap[x][y]) {
                    case UNEXPLORED:
                        rectFillColor(canvas, paint, rect, Config.UNEXPLORED);
                        break;

                    case FREE_SPACE:
                        rectFillColor(canvas, paint, rect, Config.FREE_SPACE);
                        break;

                    case OBSTACLE:
                        rectFillColor(canvas, paint, rect, Config.OBSTACLE);
                        break;
                }
                if(y == 0) {
                    canvas.drawText(String.valueOf(xGridNum), left+(gridSize/4), bottom-(gridSize/4), textPaint);
                    xGridNum--;
                }
                canvas.drawRect(rect, paint);
                textX = left+(gridSize/4) + gridSize;
                textY = bottom-(gridSize/4);
            }
        }
        int yGridNum = 14;
        textX += (gridSize/4);
        textY += gridSize;
        while(yGridNum > -1) {
            canvas.drawText(String.valueOf(yGridNum), textX, textY, textPaint);
            textX -= gridSize;
            yGridNum--;
        }
    }

    private static void renderStartZone(Canvas canvas, Paint paint, int gridSize) {
        for (int x = Config.ARENA_WIDTH - 3; x < Config.ARENA_WIDTH; x ++) {
            for (int y = 0; y < 3; y ++) {
                float left   = gridSize / 2 + gridSize * y;
                float right  = left + gridSize;

                float top    = gridSize / 2 + gridSize * x;
                float bottom = top + gridSize;

                RectF rect = new RectF(left+gridSize, top, right+gridSize, bottom);
                rectFillColor(canvas, paint, rect, Config.START);
            }
        }
    }

    private static void renderGoalZone(Canvas canvas, Paint paint, int gridSize) {
        for (int x = 0; x < 3; x ++) {
            for (int y = Config.ARENA_LENGTH - 3; y < Config.ARENA_LENGTH; y ++) {
                float left   = gridSize / 2 + gridSize * y;
                float right  = left + gridSize;

                float top    = gridSize / 2 + gridSize * x;
                float bottom = top + gridSize;

                RectF rect = new RectF(left+gridSize, top, right+gridSize, bottom);
                rectFillColor(canvas, paint, rect, Config.GOAL);
            }
        }
    }

    private static void renderBorders(Canvas canvas, Paint paint, int gridSize) {
        paint.setColor(Config.BORDER);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);

        int numOfVerticalLines   = Config.ARENA_LENGTH + 1; // 15 + 1
        int numOfHorizontalLines = Config.ARENA_WIDTH  + 1; // 20 + 1

        for (int i = 0; i < numOfVerticalLines; i ++) {
            float startX = leftMostXPos + i * gridSize; // 17 + 0 * 34 = 17 , 51, 85
            float stopX  = leftMostXPos + i * gridSize;

            float startY = topMostYPos;
            float stopY  = bottomMostYPos;

            canvas.drawLine(startX+gridSize, startY, stopX+gridSize, stopY, paint);
        }

        for (int i = 0; i < numOfHorizontalLines; i ++) {
            float startX = leftMostXPos;
            float stopX  = rightMostXPos;

            float startY = topMostYPos + i * gridSize;
            float stopY  = topMostYPos + i * gridSize;

            canvas.drawLine(startX+gridSize, startY, stopX+gridSize, stopY, paint);
        }
    }

    private static void rectFillColor(Canvas canvas, Paint paint, RectF rect, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rect, paint);
    }
}