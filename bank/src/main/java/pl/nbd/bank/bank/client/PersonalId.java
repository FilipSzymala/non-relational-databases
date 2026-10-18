package pl.nbd.bank.bank.client;

public record PersonalId(String value) {
    public PersonalId {
        if (value == null || !value.matches("\\d{11}")) {
            throw new IllegalArgumentException("PESEL must contain 11 digits");
        }
    }
}
