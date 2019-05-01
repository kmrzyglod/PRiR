import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ParallelCalculations implements ParallelCalculationsInterface, ThreadControllInterface {
    private PointGeneratorInterface pointGenerator;
    private SynchronizationContext synchronizationContext = new SynchronizationContext();
    private int threadsNum;
    private Thread[] threads;
    private Worker[] workers;

    public void setPointGenerator(PointGeneratorInterface generator) {
        pointGenerator = generator;
    }

    public long getSum() {
        return synchronizationContext.sum;
    }

    public int getCountsInBucket(int firstCoordinate, int secondCoordinate) {
        return synchronizationContext.histogram[firstCoordinate][secondCoordinate];
    }

    public void setNumberOfThreads(int threads) {
        threadsNum = threads;
    }

    public void createThreads() {
        workers = IntStream
                .range(0, threadsNum)
                .mapToObj(i -> new Worker(pointGenerator, synchronizationContext))
                .toArray(Worker[]::new);

        threads = Arrays.stream(workers)
                .map(worker -> new Thread(worker))
                .toArray(Thread[]::new);
    }

    public Thread getThread(int thread) {
        return threads[thread];
    }

    public void start() {
        Arrays.stream(threads)
                .forEach(thread -> thread.start());
    }

    public void suspend() {
        synchronizationContext.suspend();
        while(true) {
            if(Arrays.stream(threads).allMatch(thread -> thread.getState() == Thread.State.WAITING)) {
                break;
            }
        }
    }

    public void resume() {
        synchronizationContext.resume();
    }

    public void terminate() {
        synchronizationContext.terminate();
        Arrays.stream(threads).forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}


class Worker implements Runnable {
    private SynchronizationContext synchronizationContext;
    private PointGeneratorInterface generator;

    public Worker(PointGeneratorInterface generator, SynchronizationContext synchronizationContext) {
        this.generator = generator;
        this.synchronizationContext = synchronizationContext;
    }

    public void run() {
        while (!this.synchronizationContext.terminate) {
            try {
                calculate();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void calculate() throws InterruptedException {
        this.synchronizationContext.pauseNeeded();
        PointGeneratorInterface.Point2D point = generator.getPoint();
        synchronized (this.synchronizationContext) {
            this.synchronizationContext.pauseNeeded();
            this.synchronizationContext.histogram[point.firstCoordinate][point.secondCoordinate]++;
            this.synchronizationContext.pauseNeeded();
            this.synchronizationContext.sum += point.firstCoordinate + point.secondCoordinate;
        }
    }
}

class SynchronizationContext {
    public int sum = 0;
    public final int[][] histogram = new int[PointGeneratorInterface.MAX_POSITION + 1][PointGeneratorInterface.MAX_POSITION + 1];
    public boolean terminate = false;
    public boolean isSuspended = false;

    public synchronized void suspend() {
        isSuspended = true;
    }

    public synchronized void resume() {
        isSuspended = false;
        notifyAll();
    }

    public synchronized void pauseNeeded() throws InterruptedException {
        if(isSuspended) {
            wait();
        }
    }

    public synchronized void terminate() {
        terminate = true;
    }
}




