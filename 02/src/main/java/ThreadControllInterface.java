/**
 * Interfejs kontroli wątków obliczeniowych.
 */
public interface ThreadControllInterface {
    /**
     * Metoda pozwala na wprowadzenie do systemu informacji o wymaganej liczby
     * wątków obliczeniowych. Liczba wątków musi zostać ustawiona przed utworzeniem
     * obiektów-wątków za pomocą metody createThreads. Liczba wątków nie będzie już
     * mogła być zmieniona.
     *
     * @param threads liczba wątków obliczeniowych
     */
    public void setNumberOfThreads(int threads);

    /**
     * Metoda zleca utworzenie obiektów-wątków obliczeniowych. Po zakończeniu tej
     * metody zlecona wcześniej liczba wątków ma istnieć, ale nie mogą już być
     * uruchomione - wątki muszą być w stanie NEW.
     */
    public void createThreads();

    /**
     * Metoda pozwala na uzyskanie referencji do obiektu-wątku. Parametrem wywołania
     * jest numer wątku obliczeniowego od 0 do setNumberOfThreads-1. Metoda powinna
     * móc udostępnić obiekty-wątki zaraz po zakończeniu metody createThreads().
     * 
     * @param thread numer wątku obliczeniowego.
     * @return Referencja do obiektu klasy Thread.
     */
    public Thread getThread(int thread);

    /**
     * Metoda służąca do rozpoczęcia obliczeń. Przed wykonaniem tej metody
     * użytkownik musi wykonać jednokrotnie metody setPointGenerator,
     * setNumberOfThreads oraz createThreads. Metoda start() nie może zablokować
     * wątku użytkownika, tj. musi zakończyć się i pozwolić użytkownikowi na dalsze
     * używanie tego samego wątku. Obliczenia mają być realizowane za pomocą wątków
     * utworzonych wcześniej w wyniku wykonania metody createThreads. Po zakończeniu
     * metody start dodatkowe wątki obliczeniowe mają wykonywać pracę.
     */
    public void start();

    /**
     * Zlecenie wstrzymania obliczeń. Po zakończeniu metody suspend obliczenia nie
     * mogą być wykonywane. Czas wykonania metody suspend nie może być znacząco
     * dłuższy od czasu potrzebnego do pobrania punktu z generatora. Limit czasu
     * wykonania to maksymalnie 2x czas pobrania pojedynczego punktu z generatora.
     * Po zakończeniu metody wszystkie dodatkowo uruchomione wątki muszą znajdować
     * się w stanie <tt>WAITING</tt>, ponadto, wyniki obliczeń muszą być gotowe do
     * pobrania i zgadzać się ze wszystkimi otrzymanymi z generatora danymi.
     */
    public void suspend();

    /**
     * Zlecenie kontynuacji obliczeń wstrzymanych za pomocą suspend. Metoda nie może
     * blokować wątku użytkownika. Po zakończeniu metody wątki obliczeniowe mają
     * ponownie wykonywać pracę.
     */
    public void resume();

    /**
     * Metoda zleca zakończenie pracy wątków obliczeniowych. Po jej wykonaniu
     * wszystkie wątki obliczeniowe muszą znajdować się w stanie <tt>TERMINATED</tt>
     * a wyniki obliczeń muszą zgadzać się ze wszystkimi pobranymi punktami.
     * Maksymalny czas na wykonanie metody jest taki sam jak w przypadku metody
     * suspend. a wyniki obliczeń muszą zgadzać się ze wszystkimi pobranymi
     * punktami.
     */
    public void terminate();

}