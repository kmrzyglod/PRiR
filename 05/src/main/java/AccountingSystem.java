import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AccountingSystem implements AccountingSystemInterface {
    private final Map<String, PhoneInterface> phones = new HashMap<>();
    private final BillingModule billingModule;
    private final ConnectionsManager connectionsManager;

    public AccountingSystem() {
        billingModule = new BillingModule(phones);
        connectionsManager = new ConnectionsManager(phones, billingModule);
    }

    @Override
    public void phoneRegistration(String number, PhoneInterface phone) {
        phones.put(number, phone);
    }

    @Override
    public long subscriptionPurchase(String number, long time) {
        return billingModule.subscriptionPurchase(number, time);
    }

    @Override
    public Optional<Long> getRemainingTime(String number) {
        return billingModule.getRemainingTime(number);
    }

    @Override
    public boolean connection(String numberFrom, String numberTo) {
            return connectionsManager.connection(numberFrom, numberTo);
    }

    @Override
    public void disconnection(String number) {
        connectionsManager.disconnection(number);
    }

    @Override
    public Optional<Long> getBilling(String numberFrom, String numberTo) {
        return billingModule.getBilling(numberFrom, numberTo);
    }

    @Override
    public Optional<Boolean> isConnected(String number) {
        return connectionsManager.isConnected(number);
    }
}

class BillingModule {
    private final Map<String, Long> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Long> billing = new ConcurrentHashMap<>();
    private final Map<String, PhoneInterface> phones;

    public BillingModule(Map<String, PhoneInterface> phones) {
        this.phones = phones;
    }

    public long subscriptionPurchase(String number, long time) {
        subscriptions.putIfAbsent(number, 0L);
        return subscriptions.compute(number, (key, value) -> value + time);
    }

    public Optional<Long> getRemainingTime(String number) {
        return Optional.ofNullable(subscriptions.get(number));
    }

    public void addBilling(Connection connection) {
        String key = connection.getFromNumber() + "_" + connection.getToNumber();
        billing.putIfAbsent(key, 0L);
        billing.compute(key, (k, value) -> value + connection.getConnectionTime());
    }

    public Optional<Long> getBilling(String numberFrom, String numberTo) {
        String key = numberFrom + "_" + numberTo;
        synchronized (this){
            return Optional.ofNullable((phones.containsKey(numberFrom) && phones.containsKey(numberTo)) ? billing.getOrDefault(key, 0L) : null );
        }
    }

    public long substractTime(String number, long time) {
        return subscriptions.compute(number, (k, value) -> value - time);
    }
}

class ConnectionsManager {
    private final Map<String, PhoneInterface> phones;
    private final BillingModule billingModule;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Set<String> potentialConnections = new HashSet<>();

    public ConnectionsManager(Map<String, PhoneInterface> phones, BillingModule billingModule) {
        this.phones = phones;
        this.billingModule = billingModule;

    }

    public boolean connection(String numberFrom, String numberTo) {
        synchronized (this) {
            if (!phones.containsKey(numberFrom)
                    || !phones.containsKey(numberTo)
                    || billingModule.getRemainingTime(numberFrom).get() <= 0
                    || potentialConnections.contains(numberFrom)
                    || potentialConnections.contains(numberTo)
                    || connections.containsKey(numberFrom)
                    || connections.containsKey(numberTo)) {
                return false;
            }
            potentialConnections.add(numberFrom);
            potentialConnections.add(numberTo);
        }

        boolean connectionResult = phones.get(numberTo).newConnection(numberFrom);
        potentialConnections.remove(numberFrom);
        potentialConnections.remove(numberTo);

        if (!connectionResult) {
            return false;
        }

        Connection newConnection = new Connection(numberFrom, numberTo);
        connections.put(numberFrom, newConnection);
        connections.put(numberTo, newConnection);

        ScheduledExecutorService billingCounterService = Executors.newSingleThreadScheduledExecutor();
        newConnection.setCostCalculation(billingCounterService);
        billingCounterService.scheduleWithFixedDelay(()-> {
            newConnection.addConnectionTime(30);
            billingModule.substractTime(numberFrom, 30 );

            if(billingModule.getRemainingTime(numberFrom).get() <= 0) {
                disconnection(numberFrom);
            }
        }, 0, 30, TimeUnit.MILLISECONDS);

        return true;
    }

    public void disconnection(String number) {
        synchronized(this) {
            var connectionToRemove = connections.get(number);
            phones.get(connectionToRemove.getFromNumber()).connectionClosed(connectionToRemove.getToNumber());
            phones.get(connectionToRemove.getToNumber()).connectionClosed(connectionToRemove.getFromNumber());
            connectionToRemove.getCostCalculation().shutdown();
            connections.remove(connectionToRemove.getFromNumber());
            connections.remove(connectionToRemove.getToNumber());
            billingModule.addBilling(connectionToRemove);
        }
    }

    public Optional<Boolean> isConnected(String number) {
        synchronized(this) {
            return Optional.ofNullable(phones.containsKey(number) ? connections.containsKey(number) : null);
        }
    }
}

class Connection {
    private final String fromNumber;
    private final String toNumber;
    private long connectionTime = 0;
    private ScheduledExecutorService billingCounterService;

    public Connection(String fromNumber, String toNumber) {
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;

    }

    public String getFromNumber() {
        return fromNumber;
    }

    public String getToNumber() {
        return toNumber;
    }

    public long getConnectionTime() {
        return connectionTime;
    }

    public void addConnectionTime(long time) {
        connectionTime += time;
    }

    public void setCostCalculation(ScheduledExecutorService billingCounterService) {
        this.billingCounterService = billingCounterService;
    }

    public ScheduledExecutorService getCostCalculation() {
        return this.billingCounterService;
    }
}
