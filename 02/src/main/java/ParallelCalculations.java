import java.util.Arrays;
import java.util.stream.IntStream;

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
