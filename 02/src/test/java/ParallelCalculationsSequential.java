public class ParallelCalculationsSequential {
    private int sum = 0;
    private final int[][] histogram = new int[PointGeneratorInterface.MAX_POSITION + 1][PointGeneratorInterface.MAX_POSITION + 1];
    PointGeneratorInterface generator;
    public boolean terminate = false;

    public void setPointGenerator(PointGeneratorInterface generator) {
        this.generator = generator;
    }

    public void start(int timeInMs) {
        long startTime = System.nanoTime();
        while(System.nanoTime() - startTime <= timeInMs * 1000000) {
            PointGeneratorInterface.Point2D point = generator.getPoint();
            histogram[point.firstCoordinate][point.secondCoordinate]++;
            sum += point.firstCoordinate + point.secondCoordinate;
        }
    }
}
