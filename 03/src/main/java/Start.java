import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class Start {
    private static final String TASK_DISPATCHER_SERVICE_NAME = "TaskDispatcher";
    public static void main(String[] args) {
        try {
            Registry rmiRegistry = LocateRegistry.getRegistry(null);
            TaskResultsProvider taskResultsProvider = new TaskResultsProvider(rmiRegistry);
            TaskDispatcher taskDispatcher = new TaskDispatcher(new TaskExecutor(rmiRegistry, taskResultsProvider), taskResultsProvider);
            TaskDispatcherInterface stub = (TaskDispatcherInterface) UnicastRemoteObject.exportObject(taskDispatcher, 0);
            rmiRegistry.rebind(TASK_DISPATCHER_SERVICE_NAME, stub);
            System.out.println("Server started");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

class TaskDispatcher implements TaskDispatcherInterface {
    private final TaskExecutor tasksExecutor;
    private final TaskResultsProvider taskResultsProvider;

    public TaskDispatcher(TaskExecutor tasksExecutor, TaskResultsProvider taskResultsProvider) {
        this.tasksExecutor = tasksExecutor;
        this.taskResultsProvider = taskResultsProvider;
    }

    @Override
    public void setReceiverServiceName(String name) throws RemoteException {
        taskResultsProvider.setReceiverServiceName(name);
    }

    @Override
    public void addTask(TaskInterface task, String executorServiceName, boolean priority) throws RemoteException {
        try {
            tasksExecutor.addTask(new Task(task, priority), executorServiceName);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class TaskExecutor {
    private final ConcurrentHashMap<String, BlockingQueue<Task>> tasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Executor> executors = new ConcurrentHashMap<>();
    private final Registry rmiRegistry;
    private final TaskResultsProvider taskResultsProvider;

    public TaskExecutor(Registry rmiRegistry, TaskResultsProvider taskResultsProvider) {
        this.rmiRegistry = rmiRegistry;
        this.taskResultsProvider = taskResultsProvider;
    }

    public void addTask(Task task, String executorServiceName) throws RemoteException, NotBoundException, InterruptedException {
        BlockingQueue<Task> tasksQue = tasks.putIfAbsent(executorServiceName, new PriorityBlockingQueue<>());
        tasksQue.put(task);
        if(!executors.containsKey(executorServiceName)) {
            ExecutorServiceInterface executor = (ExecutorServiceInterface) rmiRegistry.lookup(executorServiceName);
            ExecutorService executorService = Executors.newFixedThreadPool(executor.numberOfTasksAllowed());
            executors.putIfAbsent(executorServiceName, executorService);
            IntStream.range(0, executor.numberOfTasksAllowed()).forEach((i) -> {
                executorService.execute(() -> {
                    while(true) {
                        try {
                            Task taskFromQue = tasks.get(executorServiceName).take();
                            long execResult = executor.execute(taskFromQue.getTask());
                            taskResultsProvider.addResult(new TaskResult(taskFromQue.getTask().taskID(), execResult));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            });

        }
    }
}

class TaskResultsProvider{
    private final BlockingQueue<TaskResult> results = new LinkedBlockingQueue<>();
    private final Registry rmiRegistry;
    private String receiverServiceName;
    private ExecutorService executor;

    public TaskResultsProvider(Registry rmiRegistry) {
        this.rmiRegistry = rmiRegistry;
    }

    private void createExecutor() {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                while (true) {
//                    System.out.println("taking results...");
                    ReceiverInterface lookup = (ReceiverInterface) rmiRegistry.lookup(receiverServiceName);
                    TaskResult result = results.take();
                    lookup.result(result.getTaskID(), result.getResult());
                }
            } catch (NotBoundException | RemoteException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void setReceiverServiceName(String name) {
        receiverServiceName = name;
        if(executor == null) {
            createExecutor();
        }
    }

    public void addResult(TaskResult result) throws InterruptedException {
        results.put(result);
    }
}

class Task implements Comparable<Task> {
    private final TaskInterface task;
    private final boolean priority;

    public Task(TaskInterface task, boolean priority) {
        this.task = task;
        this.priority = priority;
    }

    public TaskInterface getTask() {
        return this.task;
    }

    public boolean isPriority() {
        return this.isPriority();
    }

    @Override
    public int compareTo(Task o) {
        int oPriority = o.priority ? 1 : 0;
        int currentPriority = this.priority ? 1 : 0;
        return currentPriority - oPriority;
    }
}

class TaskResult {
    private final long taskID;
    private final long result;

    public long getResult() {
        return result;
    }

    public long getTaskID() {
        return taskID;
    }

    public TaskResult(long taskID, long result) {
        this.taskID = taskID;
        this.result = result;
    }
}

