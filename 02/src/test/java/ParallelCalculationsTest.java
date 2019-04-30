import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ParallelCalculationsTest {

    @Test
    void checkParallelCalculationSamePoints() throws InterruptedException {
        //given
        AtomicInteger generatorCounter = new AtomicInteger(0);
        PointGeneratorInterface pointsGeneratorMock = Mockito.mock(PointGeneratorInterface.class);
        when(pointsGeneratorMock.getPoint()).thenAnswer(invocation -> {
            PointGeneratorInterface.Point2D randomPoint = new PointGeneratorInterface.Point2D(1, 1);
            generatorCounter.incrementAndGet();
            return randomPoint;
        });
        ParallelCalculations parallelCalculations = new ParallelCalculations();

        //when
        parallelCalculations.setNumberOfThreads(8);
        parallelCalculations.setPointGenerator(pointsGeneratorMock);
        parallelCalculations.createThreads();
        parallelCalculations.start();
        Thread.sleep(200);
        parallelCalculations.terminate();


        //then
        long sumFromWorkers = parallelCalculations.getSum();
        long sumFromGenerator = generatorCounter.get() * 2;
        assertEquals(sumFromGenerator, sumFromWorkers);
    }

    @Test
    void checkParallelCalculationRandomPoints() throws InterruptedException {
        //given
        List<PointGeneratorInterface.Point2D> generatedPoints = Collections.synchronizedList(new ArrayList<>());
        PointGeneratorInterface pointsGeneratorMock = Mockito.mock(PointGeneratorInterface.class);
        Random r = new Random();
        when(pointsGeneratorMock.getPoint()).thenAnswer(invocation -> {
            PointGeneratorInterface.Point2D randomPoint = new PointGeneratorInterface.Point2D(r.ints(0, PointGeneratorInterface.MAX_POSITION + 1).findFirst().getAsInt(), r.ints(0, PointGeneratorInterface.MAX_POSITION + 1).findFirst().getAsInt());
            generatedPoints.add(randomPoint);
            return randomPoint;
        });
        ParallelCalculations parallelCalculations = new ParallelCalculations();

        //when
        parallelCalculations.setNumberOfThreads(8);
        parallelCalculations.setPointGenerator(pointsGeneratorMock);
        parallelCalculations.createThreads();
        parallelCalculations.start();
        Thread.sleep(500);
        parallelCalculations.terminate();


        //then
        long sumFromWorkers = parallelCalculations.getSum();
        long sumFromGenerator = generatedPoints
                .stream()
                .map(point -> point.firstCoordinate + point.secondCoordinate)
                .reduce(0, Integer::sum);

        assertEquals(sumFromGenerator, sumFromWorkers);
    }

    @Test
    void checkParallelCalculationRandomPointsWithSuspendAndResume() throws InterruptedException {
        //given
        List<PointGeneratorInterface.Point2D> generatedPoints = Collections.synchronizedList(new ArrayList<>());
        PointGeneratorInterface pointsGeneratorMock = Mockito.mock(PointGeneratorInterface.class);
        Random r = new Random();
        when(pointsGeneratorMock.getPoint()).thenAnswer(invocation -> {
            PointGeneratorInterface.Point2D randomPoint = new PointGeneratorInterface.Point2D(r.ints(0, PointGeneratorInterface.MAX_POSITION + 1).findFirst().getAsInt(), r.ints(0, PointGeneratorInterface.MAX_POSITION + 1).findFirst().getAsInt());
            generatedPoints.add(randomPoint);
            return randomPoint;
        });
        ParallelCalculations parallelCalculations = new ParallelCalculations();

        //when
        parallelCalculations.setNumberOfThreads(8);
        parallelCalculations.setPointGenerator(pointsGeneratorMock);
        parallelCalculations.createThreads();
        parallelCalculations.start();
        Thread.sleep(200);
        parallelCalculations.suspend();
        Thread.sleep(200);
        parallelCalculations.resume();
        Thread.sleep(100);
        parallelCalculations.terminate();

        //then
        long sumFromWorkers = parallelCalculations.getSum();
        long sumFromGenerator = generatedPoints
                .stream()
                .map(point -> point.firstCoordinate + point.secondCoordinate)
                .reduce(0, Integer::sum);

        assertEquals(sumFromGenerator, sumFromWorkers);
    }



    @Test
    void parallelCalculationVsSequentialRandomPoints() throws InterruptedException {

        //given
        List<PointGeneratorInterface.Point2D> generatedPoints = Collections.synchronizedList(new ArrayList<>());
        PointGeneratorInterface pointsGeneratorMock = Mockito.mock(PointGeneratorInterface.class);
        Random r = new Random();
        when(pointsGeneratorMock.getPoint()).thenAnswer(invocation -> {
            PointGeneratorInterface.Point2D randomPoint = new PointGeneratorInterface.Point2D(r.ints(0, PointGeneratorInterface.MAX_POSITION + 1).findFirst().getAsInt(), r.ints(0, PointGeneratorInterface.MAX_POSITION + 1).findFirst().getAsInt());
            generatedPoints.add(randomPoint);
            return randomPoint;
        });
        ParallelCalculations parallelCalculations = new ParallelCalculations();
        ParallelCalculationsSequential sequentialCalculations = new ParallelCalculationsSequential();

        //when
        sequentialCalculations.setPointGenerator(pointsGeneratorMock);
        long startTime = System.nanoTime();
        sequentialCalculations.start(500);
        System.out.println("Sequential generating time : " + (System.nanoTime() - startTime) / 1000000);
        int seqentialGeneratorCalls = generatedPoints.size();


        generatedPoints.clear();
        parallelCalculations.setNumberOfThreads(8);
        parallelCalculations.setPointGenerator(pointsGeneratorMock);
        parallelCalculations.createThreads();
        parallelCalculations.start();
        Thread.sleep(500);
        parallelCalculations.terminate();
        int parallelGeneratorCalls = generatedPoints.size();

        //then
        assertTrue(parallelGeneratorCalls > seqentialGeneratorCalls);
        System.out.println("Sequential generated points : " + seqentialGeneratorCalls);
        System.out.println("Parallel generated points : " + parallelGeneratorCalls);
    }
}