package pl.nbd.infrastructure.entities.account;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AccountNumberEmbeddable {
    @Column(
        name = "account_number",
        nullable = false,
        unique = true,
        length = 64
    )
    private String accountNumber;

    protected AccountNumberEmbeddable() {
    }

    public AccountNumberEmbeddable(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
