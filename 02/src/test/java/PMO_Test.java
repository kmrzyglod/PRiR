import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class PMO_Test {
	//////////////////////////////////////////////////////////////////////////
	private static final Map<String, Double> tariff = new HashMap<>();

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Tariff {
		double value();
	}

	static {
		tariff.put("initalStateTest", 3.0);
		tariff.put("terminationTest", 3.0);
		tariff.put("suspensionTest",3.0);
	}

	public static double getTariff(String testName) {
		return tariff.get(testName);
	}

	private ParallelCalculationsInterface pci;
	private ThreadControllInterface tci;
	private Thread threads[];
	private Thread.State states[];
	private boolean threadsStated;

	//////////////////////////////////////////////////////////////////////////

	private static void showException(Throwable e, String txt) {
		e.printStackTrace();
		fail("W trakcie pracy metody " + txt + " doszło do wyjątku " + e.toString());
	}

	////////////////////////////////////////////////////////////////////////////////

	private static <T> T tryToExecute(ThrowingSupplier<T> run, String txt, long timeLimit) {
		try {
			return assertTimeoutPreemptively(Duration.ofMillis(timeLimit), run::get, "Przekroczono limit czasu " + txt);
		} catch (Exception e) {
			showException(e, txt);
		}
		return null;
	}

	private static void tryToExecute(Runnable run, String txt, long timeLimit) {
		try {
			assertTimeoutPreemptively(Duration.ofMillis(timeLimit), run::run, "Przekroczono limit czasu " + txt);
		} catch (Exception e) {
			showException(e, txt);
		}
	}

	@BeforeEach
	public void create() {
		Object o = PMO_InstanceHelper.fabric("ParallelCalculations", "ParallelCalculationsInterface");

		try {
			pci = (ParallelCalculationsInterface) o;
		} catch (Exception e) {
			fail("Klasa ParallelCalculations nie wspiera interfejsu ParallelCalculationsInterface");
		}
		try {
			tci = (ThreadControllInterface) o;
		} catch (Exception e) {
			fail("Klasa ParallelCalculations nie wspiera interfejsu ParallelCalculationsInterface");
		}
	}
	
	@AfterEach
	public void shutdown() {
		if ( threadsStated ) {
			tci.terminate();
		}
	}

	private void getThreads(int numberOfThreads) {
		threads = new Thread[numberOfThreads];
		states = new Thread.State[numberOfThreads];
		IntStream.range(0, numberOfThreads).forEach(i -> threads[i] = tci.getThread(i));
		if (Arrays.stream(threads).anyMatch(th -> th == null)) {
			fail("Metoda getThread zwróciła null");
		}
	}

	private void prepare(int threads, PointGeneratorInterface generator) {
		tci.setNumberOfThreads(threads);
		pci.setPointGenerator(generator);
		tci.createThreads();
		getThreads(threads);
	}

	private void getStates() {
		for (int i = 0; i < threads.length; i++) {
			states[i] = threads[i].getState();
		}
	}

	private void testStates(String txt, Thread.State expectedState) {
		for (int i = 0; i < states.length; i++) {
			if (states[i] != expectedState) {
				fail(txt + expectedState.toString() + " a jest " + states[ i ].toString() );
			}
		}
	}

	private void testStatesTwoStates(String txt, Thread.State expectedState,  Thread.State alternatvieExpectedState) {
		for (int i = 0; i < states.length; i++) {
			if (states[i] != expectedState && states[i] != alternatvieExpectedState) {
				fail(txt + expectedState.toString() + " a jest " + states[ i ].toString() );
			}
		}
	}

	@Test
	public void initalStateTest() {
		prepare(4, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		getStates();
		testStates("Po createThreads oczekiwano ", Thread.State.NEW);
	}

	@RepeatedTest(10)
	public void terminationTest() {
		prepare(10, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		tci.start();
		tci.terminate();
		getStates();
		testStates("Po metodzie terminate oczekiwano ", Thread.State.TERMINATED );
	}
	
	@RepeatedTest(10)
	public void suspensionTest() {
		prepare(10, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		initalStateTest();
		tci.start();
		tci.suspend();
		getStates();
		testStates("Po metodzie suspend oczekiwano ", Thread.State.WAITING );		
	}

	@RepeatedTest(10)
	public void resumeTest() {
		prepare(10, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		initalStateTest();
		tci.start();
		tci.suspend();
		getStates();
		testStates("Po metodzie suspend oczekiwano ", Thread.State.WAITING );
		tci.resume();
		getStates();
		testStatesTwoStates("Po metodzie suspend oczekiwano ", Thread.State.RUNNABLE, Thread.State.BLOCKED);
		tci.terminate();
	}
}
