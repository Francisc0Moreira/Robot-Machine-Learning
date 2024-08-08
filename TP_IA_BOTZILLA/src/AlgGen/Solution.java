package AlgGen;

import interf.IUIConfiguration;
import impl.Point;
import interf.IPoint;
import maps.Maps;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Exemplo que mostra como desenhar um caminho no visualizador.
 */
public class Solution {

    public IUIConfiguration conf;

    public final static int mutationRate = 2; // número a alterar em cada mutação
    public final static int popSize = 100; // tamanho da população
    public final static int popHereditary = 50; // soluções a manter
    public final static int popMutation = 20; // soluções a gerar por mutação
    public final static int popCross = 30; // soluções a gerar por cruzamento
    public final static int maxIterations = 200; // critério de paragem

    public Solution(IUIConfiguration conf) {
        this.conf = conf;
    }

    public List<IPoint> getAGPath() throws InterruptedException, Exception {
        Random rand = new Random();
        // O ID do mapa a usar (ver Maps.java)
        // int map_id = 6;

        // conf = Maps.getMap(map_id);
        List<List<IPoint>> population = generateInitialPopulation(conf, rand);

        List<IPoint> bestSolution = null;
        int bestFitness = Integer.MAX_VALUE;

        for (int generation = 0; generation < maxIterations; generation++) {
            // Avaliar a população
            for (List<IPoint> individual : population) {
                int fitness = calculateFitness(individual, conf);
                if (fitness < bestFitness && isPathValid(individual, conf)) {
                    bestFitness = fitness;
                    bestSolution = individual;
                    System.out.println("Nova melhor solução encontrada com fitness: " + bestFitness);
                }
            }

            // Seleção
            List<List<IPoint>> newPopulation = selectBestIndividuals(population, popHereditary);

            // Cruzamento
            newPopulation.addAll(crossIndividuals(newPopulation, popCross, conf, rand));

            // Mutação
            mutateIndividuals(newPopulation, popMutation, conf, rand);

            population = newPopulation;
        }

        return bestSolution;

    }

    private static List<List<IPoint>> generateInitialPopulation(IUIConfiguration conf, Random rand) {
        List<List<IPoint>> population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            population.add(generateRandomPath(conf, rand));
        }
        return population;
    }

    private List<List<IPoint>> selectBestIndividuals(List<List<IPoint>> population, int numToSelect) {
        population.sort(Comparator.comparingDouble(o -> calculateFitness(o, conf)));
        return new ArrayList<>(population.subList(0, Math.min(numToSelect, population.size())));
    }

    private static List<List<IPoint>> crossIndividuals(List<List<IPoint>> population, int numToCross,
            IUIConfiguration conf, Random rand) {
        List<List<IPoint>> offspring = new ArrayList<>();
        for (int i = 0; i < numToCross; i++) {
            List<IPoint> parent1 = population.get(rand.nextInt(population.size()));
            List<IPoint> parent2 = population.get(rand.nextInt(population.size()));

            // Garantir que o ponto de cruzamento é válido
            int crossoverPoint = 1 + rand.nextInt(Math.min(parent1.size(), parent2.size()) - 2);

            List<IPoint> child = new ArrayList<>();
            child.addAll(parent1.subList(0, crossoverPoint));
            child.addAll(parent2.subList(crossoverPoint, parent2.size()));

            // Garantir que o caminho termina no ponto final correto
            if (!child.get(child.size() - 1).equals(conf.getEnd())) {
                child.set(child.size() - 1, conf.getEnd());
            }

            offspring.add(child);
        }
        return offspring;
    }

    private static void mutateIndividuals(List<List<IPoint>> population, int numToMutate, IUIConfiguration conf,
            Random rand) {
        for (int i = 0; i < numToMutate; i++) {
            List<IPoint> individual = population.get(rand.nextInt(population.size()));
            mutate(individual, conf, rand);
        }
    }

    private static void mutate(List<IPoint> path, IUIConfiguration conf, Random rand) {
        for (int i = 0; i < mutationRate; i++) {
            int index = 1 + rand.nextInt(path.size() - 2); // Evita mutar o início e o fim
            path.set(index, new Point(rand.nextInt(conf.getWidth()), rand.nextInt(conf.getHeight())));
        }
    }

    // Gera um caminho aleatório
    private static List<IPoint> generateRandomPath(IUIConfiguration conf, Random rand) {
        List<IPoint> path = new ArrayList<>();
        path.add(conf.getStart());
        int size = 1 + rand.nextInt(5); // Cria um caminho aleatório com no mínimo 1 nó intermediário (excetuando início
                                        // e fim)
        for (int i = 0; i < size; i++) {
            path.add(new Point(rand.nextInt(conf.getWidth()), rand.nextInt(conf.getHeight())));
        }
        path.add(conf.getEnd());
        return path;
    }

    // Verifica se o caminho é válido
    private static boolean isPathValid(List<IPoint> path, IUIConfiguration conf) {
        return calculateCollisions(path, conf) == 0;
    }

    // Calcula o número de colisões no caminho
    private static int calculateCollisions(List<IPoint> path, IUIConfiguration conf) {
        int intersections = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Point2D.Double p1 = new Point2D.Double(path.get(i).getX(), path.get(i).getY());
            Point2D.Double p2 = new Point2D.Double(path.get(i + 1).getX(), path.get(i + 1).getY());
            Line2D.Double line = new Line2D.Double(p1, p2);

            for (int j = 0; j < conf.getObstacles().size(); j++) {
                if (conf.getObstacles().get(j).intersectsLine(line)) {
                    intersections++;
                }
            }
        }
        return intersections;
    }

    // Calcula o fitness de uma solução
    private static int calculateFitness(List<IPoint> path, IUIConfiguration conf) {
        int collisions = calculateCollisions(path, conf);
        int numNodes = path.size();
        int totalDistance = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            int dx = path.get(i + 1).getX() - path.get(i).getX();
            int dy = path.get(i + 1).getY() - path.get(i).getY();
            totalDistance += Math.sqrt(dx * dx + dy * dy);
        }

        // Fitness é penalizado por colisões e comprimento do caminho
        return totalDistance + collisions * 1 + numNodes * 1;
    }
}
