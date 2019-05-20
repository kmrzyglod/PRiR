import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Start {
    private static final String TASK_DISPATCHER_SERVICE_NAME = "TaskDispatcher";
    public static void main(String[] args) {
        try {
            Registry rmiRegistry = LocateRegistry.getRegistry(null);
            TaskDispatcher taskDispatcher = new TaskDispatcher();
            TaskDispatcherInterface stub = (TaskDispatcherInterface) UnicastRemoteObject.exportObject(taskDispatcher, 0);
            rmiRegistry.rebind(TASK_DISPATCHER_SERVICE_NAME, stub);
            System.out.println("Server started");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}


class TaskDispatcher implements TaskDispatcherInterface {

    @Override
    public void setReceiverServiceName(String name) throws RemoteException {
    }

    @Override
    public void addTask(TaskInterface task, String executorServiceName, boolean priority) throws RemoteException {

    }
}

