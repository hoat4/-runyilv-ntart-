package arunyilvantarto.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name", scope = User.class)
public class User {

    public String name;

    public Role role;
    public byte[] passwordHash;
    public int staffBill;
    public boolean deleted;

    public boolean canPurchaseWithStaffBill() {
        return !deleted;
    }

    public enum Role {
        ROOT, ADMIN, SELLER, STAFF;

        public boolean canSell() {
            return this != STAFF;
        }
    }
}
