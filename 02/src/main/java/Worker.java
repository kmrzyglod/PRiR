public class Worker implements Runnable {
    private SynchronizationContext synchronizationContext;
    private PointGeneratorInterface generator;

    public Worker(PointGeneratorInterface generator, SynchronizationContext synchronizationContext) {
        this.generator = generator;
        this.synchronizationContext = synchronizationContext;
    }

    public void run() {
        while (!this.synchronizationContext.terminate) {
            calculate();
        }
    }

    private void calculate() {
        PointGeneratorInterface.Point2D point = generator.getPoint();
        synchronized (this.synchronizationContext) {
            this.synchronizationContext.histogram[point.firstCoordinate][point.secondCoordinate]++;
            this.synchronizationContext.sum += point.firstCoordinate + point.secondCoordinate;
            if(this.synchronizationContext.isSuspended) {
                try {
                    synchronizationContext.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
