public class Worker implements Runnable {
    private volatile static int sum;
    private volatile static int[][] histogram;
    private PointGeneratorInterface generator;
    private boolean isSuspended = false;
    private boolean terminate = false;

    public Worker(PointGeneratorInterface generator) {
        this.generator = generator;
    }

    public void run() {
        while(!terminate) {
            if(!isSuspended) {
                calculate();
            }
        }
    }

    public void suspend() {
        isSuspended = true;
    }

    public void resume() {
        isSuspended = false;
    }

    public void terminate() {
        terminate = true;
    }

    private void calculate() {
        PointGeneratorInterface.Point2D point = generator.getPoint();
        synchronized (Worker.class) {
            histogram[ point.firstCoordinate ][ point.secondCoordinate ]++;
            sum += point.firstCoordinate + point.secondCoordinate;
        }
    }

    public static int getSum() {
        return sum;
    }

    public static int[][] getHistogram() {
        return histogram;
    }

    public static void initialize(int sum, int[][] histogram) {
        Worker.sum = sum;
        Worker.histogram = histogram;
    }
}
