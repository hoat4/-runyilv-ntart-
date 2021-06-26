package arunyilvantarto.domain;

public class User {

    public String name;

    public Role role;
    public byte[] passwordHash;

    public enum Role {
        ROOT, ADMIN, SELLER, STAFF
    }
}
