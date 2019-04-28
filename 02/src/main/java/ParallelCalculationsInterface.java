/**
 * Interfejs systemu prostych obliczeń.
 */
public interface ParallelCalculationsInterface {
    /**
     * Metoda pozwala na przekazanie do systemu obiektu oferującego usługę
     * generowania punktów.
     *
     * @param generator generator punktów
     */
    public void setPointGenerator(PointGeneratorInterface generator);

    /**
     * Metoda zwraca sumę wszystkich współrzędnych wszystkich przekazanych punktów
     * czyli suma wszystkich wartości firstCoordinate plus suma wszystkich wartości
     * secondCoordinate.
     * 
     * @return suma wszystkich współrzędnych wszystkich pobranych z generatora
     *         punktów.
     */
    public long getSum();

    /**`
     * Metoda zwraca ile razy pobrano z generatora punkt o podanych współrzędnych
     * czyli liczebność odpowiedniego elementu dwuwymiarowego histogramu czestości
     * występowania punktów. Wynik musi uwzględniać wynik pracy wszystkich wątków.
     * Poprawne wartości parametrów należą do zakresu od 0 do
     * PointInterface.MAX_POSITION+1. Metoda musi zwracać wyniki natychmiast - nie
     * wolno w niej wykonywać żadnych obliczeń. Dopuszczalne jest jedynie pobieranie
     * danej z tablicy, w której dane będą przechowywane.
     *
     * @param firstCoordinate  pierwsza wspołrzędna pobieranego z generatora punktu
     * @param secondCoordinate druga wspołrzędna pobieranego z generatora punktu
     * @return liczba
     */
    public int getCountsInBucket(int firstCoordinate, int secondCoordinate);
}
