import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
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

        //when && then
        parallelCalculations.setNumberOfThreads(4);
        parallelCalculations.setPointGenerator(pointsGeneratorMock);
        parallelCalculations.createThreads();
        parallelCalculations.start();
        Thread.sleep(200);
        parallelCalculations.suspend();
        assertSame(Thread.State.WAITING, parallelCalculations.getThread(1).getState());
        Thread.sleep(200);
        assertSame(Thread.State.WAITING, parallelCalculations.getThread(1).getState());
        parallelCalculations.resume();
        Thread.State state1 = parallelCalculations.getThread(1).getState();
        System.out.println("Thread state : " + state1);
        assertTrue(state1 == Thread.State.RUNNABLE || state1 == Thread.State.BLOCKED);
        Thread.sleep(100);
        Thread.State state2 = parallelCalculations.getThread(1).getState();
        assertTrue(state2 == Thread.State.RUNNABLE || state2 == Thread.State.BLOCKED);
        parallelCalculations.terminate();
        Thread.State state3 = parallelCalculations.getThread(1).getState();
        assertSame( Thread.State.TERMINATED, state3);

        long sumFromWorkers = parallelCalculations.getSum();
        long sumFromGenerator = generatedPoints
                .stream()
                .map(point -> point.firstCoordinate + point.secondCoordinate)
                .reduce(0, Integer::sum);

        assertEquals(sumFromGenerator, sumFromWorkers);
    }

    @Test
    void checkParallelCalculationRandomPointsMethodExecutionTime() throws InterruptedException {
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

        long generationStartTime = System.nanoTime();
        pointsGeneratorMock.getPoint();
        long generationTime = System.nanoTime() - generationStartTime;

        //when && then
        parallelCalculations.setNumberOfThreads(8);
        parallelCalculations.setPointGenerator(pointsGeneratorMock);
        parallelCalculations.createThreads();
        parallelCalculations.start();
        Thread.sleep(200);
        long suspendStartTime = System.nanoTime();
        parallelCalculations.suspend();
        long suspendTime = System.nanoTime() - suspendStartTime;
        assertTrue(suspendTime <= 2 * generationTime);
        Thread.sleep(200);
        long resumeStartTime = System.nanoTime();
        parallelCalculations.resume();
        long resumeTime = System.nanoTime() - resumeStartTime;
        assertTrue(resumeTime <= 2 * generationTime);
        Thread.sleep(100);
        parallelCalculations.terminate();
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
        parallelCalculations.setNumberOfThreads(2);
        parallelCalculations.setPointGenerator(pointsGeneratorMock);
        parallelCalculations.createThreads();
        parallelCalculations.start();
        Thread.sleep(500);
        parallelCalculations.terminate();
        int parallelGeneratorCalls = generatedPoints.size();

        //then
        System.out.println("Sequential generated points : " + seqentialGeneratorCalls);
        System.out.println("Parallel generated points : " + parallelGeneratorCalls);
        assertTrue(parallelGeneratorCalls > seqentialGeneratorCalls);

    }

    @Test
    void checkTerminate() throws InterruptedException {
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
}