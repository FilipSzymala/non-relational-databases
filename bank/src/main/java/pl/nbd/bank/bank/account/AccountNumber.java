package pl.nbd.bank.bank.account;

public record AccountNumber(String number) {
    public AccountNumber {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException(
                "Account number must not be blank"
            );
        }
    }
}
