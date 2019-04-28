import java.util.Arrays;
import java.util.stream.IntStream;

public class ParallelCalculations implements ParallelCalculationsInterface, ThreadControllInterface {
    private PointGeneratorInterface pointGenerator;
    private int threadsNum;
    private Thread[] threads;
    private Worker[] workers;

    //    private int[][] histogram;
//    private int sum = 0;
    public void setPointGenerator(PointGeneratorInterface generator) {
        pointGenerator = generator;
    }

    public long getSum() {
        return Worker.getSum();
    }

    public int getCountsInBucket(int firstCoordinate, int secondCoordinate) {
        return Worker.getHistogram()[firstCoordinate][secondCoordinate];
    }

    public void setNumberOfThreads(int threads) {
        threadsNum = threads;
    }

    public void createThreads() {
        workers = IntStream
                .range(0, threadsNum)
                .mapToObj(i -> new Worker(pointGenerator))
                .toArray(Worker[]::new);

        threads = Arrays.stream(workers)
                .map(worker -> new Thread(worker))
                .toArray(Thread[]::new);
    }

    public Thread getThread(int thread) {
        return threads[thread];
    }

    public void start() {

        Worker.initialize(0, new int[PointGenerator.MAX_POSITION][PointGenerator.MAX_POSITION]);
        Arrays.stream(threads)
                .forEach(thread -> thread.start());
    }

    public void suspend() {
        Arrays.stream(workers).forEach(worker -> worker.suspend());
    }

    public void resume() {
        Arrays.stream(workers).forEach(worker -> worker.resume());

    }

    public void terminate() {
        Arrays.stream(workers).forEach(worker -> worker.terminate());

    }
}
