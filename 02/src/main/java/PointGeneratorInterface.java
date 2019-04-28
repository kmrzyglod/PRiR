/**
 * Interfejs generatora punktów.
 */
public interface PointGeneratorInterface {

    /**
     * Klasa reprezentująca punkt w dwuwymiarowej przestrzeni.
     */
    public class Point2D {
        public final int firstCoordinate;
        public final int secondCoordinate;

        public Point2D(int x, int y) {
            firstCoordinate = x;
            secondCoordinate = y;
        }
    }

    /**
     * Największa, poprawna wartość współrzędnej punktu
     */
    public static final int MAX_POSITION = 127;

    /**
     * Metoda zwraca obiekt reprezentujący pojedynczy punkt. Metoda może być
     * wywoływana współbieżnie tj. wiele wątków może wywoływać tą metodę w tym samym
     * czasie.
     *
     * @return obiekt reprezentujący pojedynczy punkt w dwuwymiarowej przestrzeni.
     *         Poprawne położenia mieszczą się w przedziale od 0 do MAX_POSITION
     *         włącznie.
     */
    public Point2D getPoint();
}
