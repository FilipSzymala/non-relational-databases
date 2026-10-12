package pl.nbd.model.Client;

import pl.nbd.model.Client.enums.Sex;

import java.util.UUID;

public class PersonalId {
    private final UUID id;
    private String personalIdentificationNumber;
    private Sex sex;
    private String firstName;
    private String lastName;
    private String MothersName;
    private String FathersName;

    public PersonalId(String personalIdentificationNumber, Sex sex, String firstName, String lastName, String mothersName, String fathersName) {
        this.id = UUID.randomUUID();
        this.personalIdentificationNumber = personalIdentificationNumber;
        this.sex = sex;
        this.firstName = firstName;
        this.lastName = lastName;
        MothersName = mothersName;
        FathersName = fathersName;
    }

    public UUID getId() {
        return id;
    }

    public String getPersonalIdentificationNumber() {
        return personalIdentificationNumber;
    }

    public void setPersonalIdentificationNumber(String personalIdentificationNumber) {
        this.personalIdentificationNumber = personalIdentificationNumber;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMothersName() {
        return MothersName;
    }

    public void setMothersName(String mothersName) {
        MothersName = mothersName;
    }

    public String getFathersName() {
        return FathersName;
    }

    public void setFathersName(String fathersName) {
        FathersName = fathersName;
    }

    @Override
    public String toString() {
        return "PersonalId{" +
            "id=" + id +
            ", personalIdentificationNumber='" + personalIdentificationNumber + '\'' +
            ", sex=" + sex +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", MothersName='" + MothersName + '\'' +
            ", FathersName='" + FathersName + '\'' +
            '}';
    }
}
