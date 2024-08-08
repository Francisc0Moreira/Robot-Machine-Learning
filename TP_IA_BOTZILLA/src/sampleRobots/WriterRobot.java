package sampleRobots;

import robocode.*;

import java.awt.geom.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class WriterRobot extends AdvancedRobot {

    /**
     * Classe usada para guardar os dados dos robots inimigos, quando observados
     */
    private class Dados {
        double distancia; // distancia a que o robot se encontra
        double velocidade; // velocidade a que o robot inimigo se desloca
        Point2D.Double coordenadas; // coordenadas do inimigo
        double bearing; // angulo em que o inimigo se encontra
        double heading;
        double energy;

        public Dados(double d, double e, Point2D.Double coordenadas, double f, double energy, double head) {
            this.distancia = d;
            this.velocidade = e;
            this.coordenadas = coordenadas;
            this.bearing = f;
            this.energy = energy;
            this.heading = head;
        }
    }

    // objeto para escrever em ficheiro
    RobocodeFileOutputStream fw;

    // estrutura para manter a informação das balas enquanto não atingem um alvo, a
    // parede ou outra bala
    // isto porque enquanto a bala não desaparece, não sabemos se atingiu o alvo ou
    // não
    HashMap<Bullet, Dados> balasNoAr = new HashMap<>();

    @Override
    public void run() {
        super.run();

        try {
            fw = new RobocodeFileOutputStream(this.getDataFile("log_robocode.csv").getAbsolutePath(), true);
            System.out.println("Writing to: " + fw.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            setAhead(100);
            setTurnLeft(100);
            setBodyColor(Color.BLACK); // Define apenas o corpo do robô como azul
            setGunColor(Color.MAGENTA); // Define apenas o canhão do robô como verde
            setRadarColor(Color.BLACK);
            execute();
        }

    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        super.onScannedRobot(event);

        Point2D.Double coordinates = utils.Utils.getEnemyCoordinates(this, event.getBearing(), event.getDistance());
        System.out.println("Enemy " + event.getName() + " spotted at " + coordinates.x + "," + coordinates.y + "\n");

        Bullet b = fireBullet(3);

        if (b != null) {
            System.out.println("Firing at " + event.getName());
            // guardar os dados do inimigo temporariamente, até que a bala chegue ao
            // destino, para depois os escrever em ficheiro
            balasNoAr.put(b, new Dados(event.getDistance(), event.getVelocity(), coordinates,
                    event.getBearing(), event.getEnergy(), event.getHeading()));

        } else
            System.out.println("Cannot fire right now...");
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        super.onBulletHit(event);
        Dados d = balasNoAr.get(event.getBullet());
        double HeadingGun = getGunHeading();

        double shooterHeading = getHeading();
        double enemyX = d.coordenadas.x;
        double enemyY = d.coordenadas.y;
        double gunHeat = getGunHeat();
        double bulletPower = event.getBullet().getPower();
        double bulletTravelTime = calcularTempoImpacto(d.distancia, bulletPower);
        double margin = 50; // Defina o limite como 50 unidades
        boolean nearWall = isNearWall(enemyX, enemyY, margin);
        double coollingRate = getGunCoolingRate();
        double coollingTimeR = gunHeat / coollingRate;

        try {
            // testar se acertei em quem era suposto
            if (event.getName().equals(event.getBullet().getVictim()))
                fw.write((HeadingGun + "," + getVelocity() + "," + d.bearing + "," + d.heading + ","
                        + d.distancia + "," + d.velocidade + ","
                        + bulletTravelTime + "," + coollingTimeR
                        + "," + "hit\n").getBytes());
            else
                fw.write((HeadingGun + "," + getVelocity() + "," + d.bearing + "," + d.heading + ","
                        + d.distancia + "," + d.velocidade + ","
                        + bulletTravelTime + "," + coollingTimeR
                        + "," + "no_hit\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        balasNoAr.remove(event.getBullet());
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        super.onBulletMissed(event);
        Dados d = balasNoAr.get(event.getBullet());
        double HeadingGun = getGunHeading();
        double shooterHeading = getHeading();
        double enemyX = d.coordenadas.x;
        double enemyY = d.coordenadas.y;
        double bulletPower = event.getBullet().getPower();
        double bulletTravelTime = calcularTempoImpacto(d.distancia, bulletPower);
        double gunHeat = getGunHeat();
        double margin = 50; // Defina o limite como 50 unidades
        boolean nearWall = isNearWall(enemyX, enemyY, margin);
        double coollingRate = getGunCoolingRate();
        double coollingTimeR = gunHeat / coollingRate;

        try {
            fw.write((HeadingGun + "," + getVelocity() + "," + d.bearing + "," + d.heading + ","
                    + d.distancia + "," + d.velocidade + ","
                    + bulletTravelTime + "," + coollingTimeR
                    + "," + "no_hit\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        balasNoAr.remove(event.getBullet());
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        super.onBulletHitBullet(event);
        Dados d = balasNoAr.get(event.getBullet());
        double HeadingGun = getGunHeading();
        double shooterHeading = getHeading();
        double enemyX = d.coordenadas.x;
        double enemyY = d.coordenadas.y;
        double bulletPower = event.getBullet().getPower();
        double bulletTravelTime = calcularTempoImpacto(d.distancia, bulletPower);
        double gunHeat = getGunHeat();
        double margin = 50; // Defina o limite como 50 unidades
        boolean nearWall = isNearWall(enemyX, enemyY, margin);
        double coollingRate = getGunCoolingRate();
        double coollingTimeR = gunHeat / coollingRate;

        try {
            fw.write((HeadingGun + "," + getVelocity() + "," + d.bearing + "," + d.heading + ","
                    + d.distancia + "," + d.velocidade + ","
                    + bulletTravelTime + "," + coollingTimeR
                    + "," + "no_hit\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        balasNoAr.remove(event.getBullet());
    }

    public double calcularTempoImpacto(double distancia, double bulletPower) {
        double velocidadeBala = 20 - 3 * bulletPower;
        return distancia / velocidadeBala;
    }

    private boolean isNearWall(double x, double y, double margin) {
        double battlefieldWidth = getBattleFieldWidth();
        double battlefieldHeight = getBattleFieldHeight();
        return (x <= margin || x >= battlefieldWidth - margin || y <= margin || y >= battlefieldHeight - margin);
    }

    @Override
    public void onDeath(DeathEvent event) {
        super.onDeath(event);

        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        super.onRoundEnded(event);

        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
