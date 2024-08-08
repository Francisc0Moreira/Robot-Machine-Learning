package sampleRobots;

import impl.Point;
import impl.UIConfiguration;
import interf.IPoint;
import robocode.*;
import robocode.Robot;
import utils.Utils;

import java.awt.geom.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import AlgGen.Solution;
import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.*;
import impl.UIConfiguration;
import interf.IPoint;
import java.util.List;

/**
 * Este Robot vai ganhar o torneio :)
 */
public class BotZilla extends AdvancedRobot {

    EasyPredictModelWrapper modelWalk;
    EasyPredictModelWrapper modelShoot;

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
            modelWalk = new EasyPredictModelWrapper(MojoModel.load(
                    this.getDataFile("GBM_150T_20MD.zip").getAbsolutePath()));
            modelShoot = new EasyPredictModelWrapper(
                    MojoModel.load(this.getDataFile("ShooterDRF_150_20.zip").getAbsolutePath()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        setTurnRadarRight(Double.POSITIVE_INFINITY);

        while (true) {
            Random rand = new Random();
            if (currentPoint >= 0) {
                IPoint point = points.get(currentPoint);

                if (Utils.getDistance(this, point.getX(), point.getY()) < 2) {
                    currentPoint++;

                    if (currentPoint >= points.size())
                        currentPoint = -1;
                }

                loggerRobotGoTo(this, point.getX(), point.getY());
            }

            setBodyColor(Color.BLACK);
            setGunColor(Color.MAGENTA);
            setRadarColor(Color.MAGENTA);
            setBulletColor(Color.MAGENTA);
            setScanColor(Color.MAGENTA);
            this.execute();
        }

    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        super.onScannedRobot(event);

        Point2D.Double coordinates = utils.Utils.getEnemyCoordinates(this, event.getBearing(), event.getDistance());
        System.out.println("Enemy " + event.getName() + " spotted at " + coordinates.x + "," + coordinates.y + "\n");

        double distance = event.getDistance();

        double HeadingGun = getGunHeading();
        double shooterHeading = getHeading();
        double shooterEnergy = getEnergy();
        double bulletPower = 3;
        double bulletTravelTime = calcularTempoImpacto(event.getDistance(), bulletPower);

        RowData row = new RowData();
        row.put("HeadingGun", HeadingGun);
        row.put("shooterEnergy", shooterEnergy);
        row.put("shooterHeading", shooterHeading);
        row.put("Shootervelocidade", getVelocity());
        row.put("distancia", distance);
        row.put("velocidade", event.getVelocity());
        row.put("energy", event.getEnergy());
        row.put("bulletTravelTime", bulletTravelTime);
        row.put("enemyX", coordinates.x);
        row.put("enemyY", coordinates.y);

        try {
            BinomialModelPrediction p = modelShoot.predictBinomial(row);
            System.out.println("Will I hit? ->" + p.label);

            // Calcular o ângulo absoluto para o inimigo
            double absoluteBearing = getHeading() + event.getBearing();
            double gunTurnAmount = absoluteBearing - getGunHeading();

            // Girar o canhão para o inimigo antes de disparar
            setTurnGunRight(gunTurnAmount);
            waitFor(new GunTurnCompleteCondition(this));

            // Disparar apenas quando o canhão estiver apontado para o inimigo
            if (p.label.equals("hit"))
                this.fire(bulletPower);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                RegressionModelPrediction p = modelWalk.predictRegression(row);
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

            conf.setStart(new Point((int) getX(), (int) getY()));
            conf.setEnd(new Point((int) bestX, (int) bestY));

            Solution ag = new Solution(conf);
            try {
                points = ag.getAGPath();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            currentPoint = 0;

        }
        bulletsReceived++;
    }

    public double calcularTempoImpacto(double distancia, double poderBala) {
        double velocidadeBala = 20 - 3 * poderBala;
        return distancia / velocidadeBala;
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

    public static Point2D.Double getEnemyCoordinates(Robot robot, double bearing, double distance) {
        double angle = Math.toRadians((robot.getHeading() + bearing) % 360);

        return new Point2D.Double((robot.getX() + Math.sin(angle) * distance),
                (robot.getY() + Math.cos(angle) * distance));
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

}
