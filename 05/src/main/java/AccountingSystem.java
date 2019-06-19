import java.util.Optional;

public class AccountingSystem implements AccountingSystemInterface {
    @Override
    public void phoneRegistration(String number, PhoneInterface phone) {

    }

    @Override
    public long subscriptionPurchase(String number, long time) {
        return 0;
    }

    @Override
    public Optional<Long> getRemainingTime(String number) {
        return Optional.empty();
    }

    @Override
    public boolean connection(String numberFrom, String numberTo) {
        return false;
    }

    @Override
    public void disconnection(String number) {

    }

    @Override
    public Optional<Long> getBilling(String numberFrom, String numberTo) {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> isConnected(String number) {
        return Optional.empty();
    }
}
