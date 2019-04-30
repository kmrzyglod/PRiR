public class SynchronizationContext {
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

    public synchronized void terminate() {
        terminate = true;
    }
}
