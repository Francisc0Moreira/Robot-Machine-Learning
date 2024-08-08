package sampleRobots;

import robocode.*;
import robocode.Robot;
import utils.Utils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import impl.Point;
import impl.UIConfiguration;
import interf.IPoint;

public class LoggerRobot extends AdvancedRobot {

    RobocodeFileOutputStream fw;

    /**
     *
     */
    private List<Rectangle> obstacles;
    public static UIConfiguration conf;
    private List<IPoint> points;
    private HashMap<ScannedRobotEvent, Rectangle> enemies;
    long bulletsReceived = 0;

    private int currentPoint = -1;

    @Override
    public void run() {

        obstacles = new ArrayList<>();
        enemies = new HashMap<>();
        conf = new UIConfiguration((int) getBattleFieldWidth(), (int) getBattleFieldHeight(), obstacles);

        try {
            fw = new RobocodeFileOutputStream(this.getDataFile("log_robocode.csv").getAbsolutePath(), true);
            fw.write(
                    "x,y,direction,enemy_x,enemy_y,enemy_distance,numEnemies,bulletsReceived\n"
                            .getBytes());
            System.out.println("Writing to: " + fw.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            this.setTurnRadarRight(360);
            // se se está a dirigir para algum ponto
            if (currentPoint >= 0) {
                IPoint ponto = points.get(currentPoint);
                // se já está no ponto ou lá perto...
                if (Utils.getDistance(this, ponto.getX(), ponto.getY()) < 2) {
                    currentPoint++;
                    // se chegou ao fim do caminho
                    if (currentPoint >= points.size())
                        currentPoint = -1;
                }

                loggerRobotGoTo(this, ponto.getX(), ponto.getY());
            }
            setAhead(100);
            setTurnLeft(100);
            Random rand = new Random();
            setAllColors(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
            this.execute();
        }

    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {

        super.onScannedRobot(event);

        System.out.println("Enemy spotted: " + event.getName());

        Point2D.Double ponto = getEnemyCoordinates(this, event.getBearing(), event.getDistance());
        ponto.x -= this.getWidth() * 2.5 / 2;
        ponto.y -= this.getHeight() * 2.5 / 2;

        Rectangle rect = new Rectangle((int) ponto.x, (int) ponto.y, (int) (this.getWidth() * 2.5),
                (int) (this.getHeight() * 2.5));

        if (enemies.containsKey(event)) // se já existe um retângulo deste inimigo
            obstacles.remove(enemies.get(event));// remover da lista de retângulos

        obstacles.add(rect);
        enemies.put(event, rect);

        double enemyDistance = event.getDistance();
        int numEnemies = enemies.size();
        double x = getX();
        double y = getY();
        double direction = getHeading();
        try {
            String logEntry = x + "," + y + "," + direction + "," + rect.x + "," + rect.y + "," +
                    enemyDistance + "," + numEnemies + "," + bulletsReceived + "\n";
            fw.write(logEntry.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static Point2D.Double getEnemyCoordinates(Robot robot, double bearing, double distance) {
        double angle = Math.toRadians((robot.getHeading() + bearing) % 360);

        return new Point2D.Double((robot.getX() + Math.sin(angle) * distance),
                (robot.getY() + Math.cos(angle) * distance));
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

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        super.onRobotDeath(event);

        Rectangle rect = enemies.get(event.getName());
        obstacles.remove(rect);
        enemies.remove(event.getName());
    }

    @Override
    public void onMouseClicked(MouseEvent e) {
        super.onMouseClicked(e);

        Random rand = new Random();

        conf.setStart(new Point((int) this.getX(), (int) this.getY()));
        conf.setEnd(new Point(e.getX(), e.getY()));

        /*
         * TODO: Implementar a chamada ao algoritmo genético!
         *
         */
        System.out.println("Choo Choo!!!");
        points = new ArrayList<>();
        points.add(new Point((int) this.getX(), (int) this.getY()));
        int size = rand.nextInt(5); // cria um caminho aleatório com no máximo 5 nós intermédios (excetuando início
                                    // e fim)
        for (int i = 0; i < size; i++)
            points.add(new Point(rand.nextInt(conf.getWidth()), rand.nextInt(conf.getHeight())));
        points.add(new Point(e.getX(), e.getY()));

        currentPoint = 0;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        bulletsReceived++;
    }

    @Override
    public void onDeath(DeathEvent event) {
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}