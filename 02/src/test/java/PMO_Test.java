import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static java.time.Duration.ofMillis;

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
		tariff.put("suspensionTest", 3.0);
		tariff.put("resumptionTest", 3.0);
		tariff.put("normalWork", 3.0);
		tariff.put("fastWork", 3.0);
		tariff.put("testValues", 3.0);
		tariff.put("testValuesResume", 3.0);
		tariff.put("testMaxPositionPlus1", 0.2);
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

	private Object createObject() {
		return PMO_InstanceHelper.fabric("ParallelCalculations", "ParallelCalculationsInterface");
	}

	@BeforeEach
	public void create() {
		Object o = createObject();

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
		if (threadsStated) {
			tci.terminate();
		}
	}

	private void getThreads(ThreadControllInterface tci, int numberOfThreads) {
		threads = new Thread[numberOfThreads];
		states = new Thread.State[numberOfThreads];
		IntStream.range(0, numberOfThreads).forEach(i -> threads[i] = tci.getThread(i));
		if (Arrays.stream(threads).anyMatch(th -> th == null)) {
			fail("Metoda getThread zwróciła null");
		}
	}

	private void prepare(int threads, PointGeneratorInterface generator) {
		prepare(pci, tci, threads, generator);
	}

	private void prepare(ParallelCalculationsInterface pci, ThreadControllInterface tci, int threads,
			PointGeneratorInterface generator) {
		tci.setNumberOfThreads(threads);
		pci.setPointGenerator(generator);
		tci.createThreads();
		getThreads(tci, threads);
	}

	private void getStates() {
		for (int i = 0; i < threads.length; i++) {
			states[i] = threads[i].getState();
		}
	}

	private void testStates(String txt, Thread.State expectedState) {
		for (int i = 0; i < states.length; i++) {
			if (states[i] != expectedState) {
				fail(txt + expectedState.toString() + " a jest " + states[i].toString());
			}
		}
	}

	private void testNotStates(String txt, Thread.State expectedState) {
		for (int i = 0; i < states.length; i++) {
			if (states[i] == expectedState) {
				fail(txt + expectedState.toString());
			}
		}
	}

//	@Disabled
	@Test
	public void initalStateTest() {
		prepare(4, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		getStates();
		testStates("Po createThreads oczekiwano ", Thread.State.NEW);
	}

//	@Disabled
	@RepeatedTest(10)
	public void terminationTest() {
		prepare(10, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		tci.start();
		tci.terminate();
		getStates();
		testStates("Po metodzie terminate oczekiwano ", Thread.State.TERMINATED);
	}

//	@Disabled
	@RepeatedTest(20)
	public void suspensionTest() {
		prepare(10, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		initalStateTest();
		tci.start();
		tci.suspend();
		getStates();
		testStates("Po metodzie suspend oczekiwano ", Thread.State.WAITING);
	}

//	@Disabled
	@RepeatedTest(20)
	public void resumptionTest() {
		prepare(10, new PMO_SimpleGenerator(new PointGeneratorInterface.Point2D(1, 1)));
		initalStateTest();
		tci.start();
		tci.suspend();
		tci.resume();
		getStates();
		testNotStates("Po metodzie resume nie oczekiwano ", Thread.State.WAITING);
		tci.suspend();
	}

	private void testValues(ParallelCalculationsInterface pci, long expectedSum, int[][] histogram) {
		long sum = pci.getSum();
		assertEquals(expectedSum, sum, "Uzyskano błędny wynik sumy");

		// powinno być bez -1, ale bled rozmiaru histogramu wykrywa inny test
		for (int i = 0; i < histogram.length-1; i++)
			for (int j = 0; j < histogram.length-1; j++) {
				assertEquals(histogram[i][j], pci.getCountsInBucket(i, j),
						"Błędny histogram na pozycji " + i + ", " + j);
			}

	}

//	@Disabled
	@RepeatedTest(10)
	public void normalWork() {

		int sizeE = PMO_PointsRepository.estimateSize(PMO_Consts.NORMAL_WORK_TEST_TIME, PMO_Consts.NORMAL_WORK_DELAY,
				PMO_Consts.NORMAL_WORK_THREADS);
		int size = sizeE + PMO_Consts.NORMAL_WORK_POINTS_SIZE_ADD;

		List<PointGeneratorInterface.Point2D> points = PMO_PointsRepository.getRepository(size);

		AtomicBoolean suspendedFlag = new AtomicBoolean(false);
		PMO_SlowPointGenerator generator = new PMO_SlowPointGenerator(suspendedFlag, PMO_Consts.NORMAL_WORK_DELAY,
				PMO_Consts.NORMAL_WORK_THREADS, points);

		prepare(PMO_Consts.NORMAL_WORK_THREADS, generator);

		assertTimeout(ofMillis(PMO_Consts.NORMAL_WORK_TEST_TIME + 5 * PMO_Consts.NORMAL_WORK_DELAY), () -> {
			tci.start();
			PMO_TimeHelper.sleep(PMO_Consts.NORMAL_WORK_TEST_TIME);
			tci.suspend();
			suspendedFlag.set(true);
			getStates();
		});

		testStates("Po metodzie suspend oczekiwano ", Thread.State.WAITING);
		generator.test();

		int ths = generator.getMaxThreads();

		assertFalse(ths == 1,
				"Program działa sekwencyjnie. Wykryto, że tylko jeden wątek wywołuje jednocześnie getPoint()");

		assertTrue(ths >= PMO_Consts.NORMAL_WORK_THREADS_REQUIRED,
				"Oczekiwano współbieżnego pobierania punktów z generatora. " + "Maksymalnie wykryto " + ths
						+ " watkow wykonujących jednocześnie getPoint()");

		double efficiency = generator.getIndex() / (double) sizeE;

		assertTrue(efficiency > PMO_Consts.NORMAL_WORK_EFFICIENCY_LIMIT, "Zbyt mała efektywność pracy. Oczekiwano "
				+ PMO_Consts.NORMAL_WORK_EFFICIENCY_LIMIT + " a jest " + efficiency);

	}

	@RepeatedTest(5)
	public void fastWork() {

		List<PointGeneratorInterface.Point2D> randomPoints = PMO_PointsRepository
				.getRepository(PMO_Consts.FAST_WORK_SIZE);

		AtomicBoolean suspendedFlag = new AtomicBoolean(false);
		PMO_SlowPointGenerator fullGenerator = new PMO_SlowPointGenerator(suspendedFlag, PMO_Consts.FAST_WORK_DELAY,
				PMO_Consts.FAST_WORK_THREADS, randomPoints);
		PMO_SlowPointGenerator halfGenerator = new PMO_SlowPointGenerator(suspendedFlag, PMO_Consts.FAST_WORK_DELAY,
				PMO_Consts.FAST_WORK_THREADS / 2, randomPoints);

		Object oFull = createObject();
		Object oHalf = createObject();

		ParallelCalculationsInterface pciFull = (ParallelCalculationsInterface) oFull;
		ThreadControllInterface tciFull = (ThreadControllInterface) oFull;

		ParallelCalculationsInterface pciHalf = (ParallelCalculationsInterface) oHalf;
		ThreadControllInterface tciHalf = (ThreadControllInterface) oHalf;

		prepare(pciFull, tciFull, PMO_Consts.FAST_WORK_THREADS, fullGenerator);
		prepare(pciHalf, tciHalf, PMO_Consts.FAST_WORK_THREADS / 2, halfGenerator);

		assertTimeout(ofMillis(PMO_Consts.FAST_WORK_TEST_TIME + 5 * PMO_Consts.NORMAL_WORK_DELAY), () -> {
			tciHalf.start();
			PMO_TimeHelper.sleep(PMO_Consts.FAST_WORK_TEST_TIME);
			tciHalf.suspend();
		});

		assertTimeout(ofMillis(PMO_Consts.FAST_WORK_TEST_TIME + 5 * PMO_Consts.NORMAL_WORK_DELAY), () -> {
			tciFull.start();
			PMO_TimeHelper.sleep(PMO_Consts.FAST_WORK_TEST_TIME);
			tciFull.suspend();
		});

		double speedUp = (double) fullGenerator.getIndex() / (double) halfGenerator.getIndex();
		System.out.println(
				"speedUp = " + speedUp + " full = " + fullGenerator.getIndex() + " half = " + halfGenerator.getIndex());

		double efficiency = speedUp / 2;

		assertTrue(efficiency > PMO_Consts.FAST_WORK_EFFICIENCY_LIMIT, "Zbyt mała efektywność pracy. Oczekiwano "
				+ PMO_Consts.FAST_WORK_EFFICIENCY_LIMIT + " a jest " + efficiency);
	}

//	@Disabled
	@RepeatedTest(25)
	public void testValues() {
		List<PointGeneratorInterface.Point2D> randomPoints;

		randomPoints = PMO_PointsRepository.getRepository(PMO_Consts.FAST_WORK_SIZE);

		PMO_FastPointGenerator randomGenerator = new PMO_FastPointGenerator(randomPoints);

		Object oRandom = createObject();

		ParallelCalculationsInterface pciRandom = (ParallelCalculationsInterface) oRandom;
		ThreadControllInterface tciRandom = (ThreadControllInterface) oRandom;

		prepare(pciRandom, tciRandom, PMO_Consts.FAST_WORK_THREADS, randomGenerator);

		assertTimeout(ofMillis(PMO_Consts.VALUES_TEST_TIME + 5 * PMO_Consts.NORMAL_WORK_DELAY), () -> {
			tciRandom.start();
			PMO_TimeHelper.sleep(PMO_Consts.VALUES_TEST_TIME);
			tciRandom.suspend();
		});

		long sum1 = randomGenerator.getSum();
		testValues(pciRandom, sum1, randomGenerator.getHistogram());
	}

//	@Disabled
	@RepeatedTest(3)
	public void testValuesResume() {
		List<PointGeneratorInterface.Point2D> randomPoints;

		randomPoints = PMO_PointsRepository.getRepository(PMO_Consts.FAST_WORK_SIZE);

		PMO_FastPointGenerator randomGenerator = new PMO_FastPointGenerator(randomPoints);

		Object oRandom = createObject();

		ParallelCalculationsInterface pciRandom = (ParallelCalculationsInterface) oRandom;
		ThreadControllInterface tciRandom = (ThreadControllInterface) oRandom;

		prepare(pciRandom, tciRandom, PMO_Consts.FAST_WORK_THREADS, randomGenerator);

		assertTimeout(ofMillis(PMO_Consts.VALUES_TEST_TIME + 5 * PMO_Consts.NORMAL_WORK_DELAY), () -> {
			tciRandom.start();
			PMO_TimeHelper.sleep(PMO_Consts.VALUES_TEST_TIME);
			tciRandom.suspend();
		});
		long sum1 = randomGenerator.getSum();

		for (int i = 0; i < PMO_Consts.RESUME_REPETITIONS; i++)
			assertTimeout(ofMillis(PMO_Consts.RESUME_TEST_TIME + 5 * PMO_Consts.NORMAL_WORK_DELAY), () -> {
				tciRandom.resume();
				PMO_TimeHelper.sleep(PMO_Consts.RESUME_TEST_TIME);
				tciRandom.suspend();
			});

		long sum2 = randomGenerator.getSum();
		testValues(pciRandom, sum2, randomGenerator.getHistogram());

		assertTrue(sum2 > sum1, "Oczekiwano wznowienia pracy po wykonaniu resume");
	}

//	@Disabled
	@Test()
	public void testMaxPositionPlus1() {
		List<PointGeneratorInterface.Point2D> randomPoints;

		int sizep1 = PointGeneratorInterface.MAX_POSITION + 1;

		randomPoints = PMO_PointsRepository.getRepository(PMO_Consts.FAST_WORK_SIZE / 4);

		IntStream.range(0, 3 * PMO_Consts.FAST_WORK_SIZE / 4).forEach(i -> {
			int pos = i % sizep1;
			randomPoints.add(new PointGeneratorInterface.Point2D(pos, pos));
		});

		PMO_FastPointGenerator randomGenerator = new PMO_FastPointGenerator(randomPoints);

		Object oRandom = createObject();

		ParallelCalculationsInterface pciRandom = (ParallelCalculationsInterface) oRandom;
		ThreadControllInterface tciRandom = (ThreadControllInterface) oRandom;

		prepare(pciRandom, tciRandom, PMO_Consts.FAST_WORK_THREADS, randomGenerator);

		assertTimeout(ofMillis(PMO_Consts.FAST_WORK_TEST_TIME + 5 * PMO_Consts.NORMAL_WORK_DELAY), () -> {
			tciRandom.start();
			tciRandom.suspend();
		});

		try {
			pciRandom.getCountsInBucket(PointGeneratorInterface.MAX_POSITION, PointGeneratorInterface.MAX_POSITION);
		} catch (ArrayIndexOutOfBoundsException e) {
			fail("Najprawdopodobniej program nie uwzględnia, że rozmiar położenie punktu MAX_POSITION jest poprawne");
		} catch (Exception e) {
			fail("Metoda getCountsInBucket wygenerowała wyjątek " + e.toString());
		}
	}

}
