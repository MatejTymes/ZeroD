package zerod.sample.domain;

import mtymes.javafixes.object.DataObject;

public class User extends DataObject {

    public final String firstName;
    public final String lastName;

    public User(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
