package arunyilvantarto.domain;

public class User {

    public String name;

    public Role role;
    public byte[] passwordHash;
    public int staffBill;

    public enum Role {
        ROOT, ADMIN, SELLER, STAFF;

        public boolean canSell() {
            return this != STAFF;
        }
    }
}
