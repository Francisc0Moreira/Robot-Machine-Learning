package sampleRobots;

import robocode.*;
import robocode.Robot;
import utils.Utils;

import java.awt.geom.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.*;
import impl.UIConfiguration;
import interf.IPoint;
import java.util.List;
import java.awt.event.MouseEvent;

import impl.Point;

/**
 * This Robot uses the model provided to guess whether it will hit or miss an
 * enemy.
 * This is a very basic model, trained specifically on the following enemies:
 * Corners, Crazy, SittingDuck, Walls.
 * It is not expected to do great...
 */
public class IntelligentRobot extends AdvancedRobot {

    EasyPredictModelWrapper model;

    private List<Rectangle> obstacles;
    public static UIConfiguration conf;
    private List<IPoint> points;
    private HashMap<ScannedRobotEvent, Rectangle> enemies;
    long bulletsReceived = 0;
    private int currentPoint = -1;

    @Override
    public void run() {

        super.run();

        obstacles = new ArrayList<>();
        enemies = new HashMap<>();
        conf = new UIConfiguration((int) getBattleFieldWidth(), (int) getBattleFieldHeight(), obstacles);

        System.out.println("Reading model from folder: " + getDataDirectory());
        try {
            model = new EasyPredictModelWrapper(MojoModel.load(
                    this.getDataFile("GBM_150T_15MD.zip").getAbsolutePath()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        while (true) {
            setTurnRadarLeft(360);
            Random rand = new Random();
            setAllColors(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));

            if (currentPoint >= 0) {
                IPoint point = points.get(currentPoint);

                if (Utils.getDistance(this, point.getX(), point.getY()) < 2) {
                    currentPoint++;

                    if (currentPoint >= points.size())
                        currentPoint = -1;
                }
            }
            this.execute();
        }
    }

    public static Point2D.Double getEnemyCoordinates(Robot robot, double bearing, double distance) {
        double angle = Math.toRadians((robot.getHeading() + bearing) % 360);

        return new Point2D.Double((robot.getX() + Math.sin(angle) * distance),
                (robot.getY() + Math.cos(angle) * distance));
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        super.onHitByBullet(event);

        double x = getX();
        double y = getY();
        double direction = getHeading();
        double numEnemies = enemies.size();
        Point2D.Double point = getEnemyCoordinates(this,
                event.getBearing(), getDistanceRemaining());
        point.x -= this.getWidth() * 2.5 / 2;
        point.y -= this.getHeight() * 2.5 / 2;

        Rectangle2D.Double rect = new Rectangle2D.Double(point.x, point.y, getWidth() * 2.5, getHeight() * 2.5);

        RowData bestRow = null;
        double minValue = Double.MAX_VALUE;

        double battlefieldWidth = getBattleFieldWidth();
        double battlefieldHeight = getBattleFieldHeight();
        List<Point2D.Double> randomCoordinates = generateRandomCoordinates(battlefieldWidth, battlefieldHeight, 100);

        for (Point2D.Double coord : randomCoordinates) {
            double newX = coord.getX();
            double newY = coord.getY();

            RowData row = new RowData();
            row.put("x", newX);
            row.put("y", newY);
            row.put("direction", direction);
            row.put("enemy_x", rect.x);
            row.put("enemy_y", rect.y);
            row.put("numEnemies", numEnemies);
            row.put("bulletsReceived", bulletsReceived);

            try {
                RegressionModelPrediction p = model.predictRegression(row);
                System.out.println("For position (" + newX + ", " + newY + "), p.value is " + p.value);

                if (p.value < minValue) {
                    minValue = p.value;
                    bestRow = row;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bestRow != null) {
            double bestX = (double) bestRow.get("x");
            double bestY = (double) bestRow.get("y");
            loggerRobotGoTo(this, bestX, bestY);
        }
        bulletsReceived++;
    }

    public List<Point2D.Double> generateRandomCoordinates(double battlefieldWidth, double battlefieldHeight,
            int count) {
        List<Point2D.Double> coordinates = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            double newX = rand.nextDouble() * battlefieldWidth;
            double newY = rand.nextDouble() * battlefieldHeight;
            coordinates.add(new Point2D.Double(newX, newY));
        }

        return coordinates;
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        super.onScannedRobot(event);
    }

    public static void loggerRobotGoTo(AdvancedRobot robot, double x, double y) {
        x -= robot.getX();
        y -= robot.getY();

        double angleToTarget = Math.atan2(x, y);
        double targetAngle = robocode.util.Utils
                .normalRelativeAngle(angleToTarget - Math.toRadians(robot.getHeading()));
        double distance = Math.hypot(x, y);
        double turnAngle = Math.atan(Math.tan(targetAngle));
        robot.setTurnRight(Math.toDegrees(turnAngle));
        if (targetAngle == turnAngle)
            robot.setAhead(distance);
        else
            robot.setBack(distance);
        robot.execute();
    }

    private void drawThickLine(Graphics g, int x1, int y1, int x2, int y2, int thickness, Color c) {

        g.setColor(c);
        int dX = x2 - x1;
        int dY = y2 - y1;

        double lineLength = Math.sqrt(dX * dX + dY * dY);

        double scale = (double) (thickness) / (2 * lineLength);

        double ddx = -scale * (double) dY;
        double ddy = scale * (double) dX;
        ddx += (ddx > 0) ? 0.5 : -0.5;
        ddy += (ddy > 0) ? 0.5 : -0.5;
        int dx = (int) ddx;
        int dy = (int) ddy;

        int xPoints[] = new int[4];
        int yPoints[] = new int[4];

        xPoints[0] = x1 + dx;
        yPoints[0] = y1 + dy;
        xPoints[1] = x1 - dx;
        yPoints[1] = y1 - dy;
        xPoints[2] = x2 - dx;
        yPoints[2] = y2 - dy;
        xPoints[3] = x2 + dx;
        yPoints[3] = y2 + dy;

        g.fillPolygon(xPoints, yPoints, 4);
    }

    @Override
    public void onPaint(Graphics2D g) {
        super.onPaint(g);

        g.setColor(Color.RED);
        obstacles.stream().forEach(x -> g.drawRect(x.x, x.y, (int) x.getWidth(), (int) x.getHeight()));

        if (points != null) {
            for (int i = 1; i < points.size(); i++)
                drawThickLine(g, points.get(i - 1).getX(), points.get(i - 1).getY(), points.get(i).getX(),
                        points.get(i).getY(), 2, Color.green);
        }
    }
}