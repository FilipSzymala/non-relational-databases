package pl.nbd.bank.bank.account;

public record AccountNumber(String number) {
    public AccountNumber {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException(
                "Account number must not be blank"
            );
        }
        if (number.length() != 64) {
            throw new IllegalArgumentException(
                "Account number must be 64 characters long"
            );
        }
    }
}
