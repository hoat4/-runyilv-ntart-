package arunyilvantarto.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Instant;

public class Sale {

    public Instant timestamp;

    public Article article;

    public int pricePerProduct;

    public int quantity;

    public String seller;

    public BillID billID;

    public int paymentID;

    public static abstract class BillID {
        @JsonCreator
        public static BillID parse(String s) {
            if (s.endsWith("-CARD"))
                return new PeriodBillID(Integer.parseInt(s.substring(0, s.length() - "-CARD".length())));

            for (int i = 0; i < s.length(); i++)
                if (s.charAt(i) < '0' || s.charAt(i) > '9')
                    return new StaffBillID(s);
            return new PeriodBillID(Integer.parseInt(s));
        }

        @JsonValue
        public abstract String toString();
    }

    public static class PeriodBillID extends BillID {
        public final int periodID;

        public PeriodBillID(int periodID) {
            this.periodID = periodID;
        }

        @Override
        public String toString() {
            return Integer.toString(periodID);
        }
    }

    public static class PeriodCardBillID extends BillID {
        public final int periodID;

        public PeriodCardBillID(int periodID) {
            this.periodID = periodID;
        }

        @Override
        public String toString() {
            return periodID + "-CARD";
        }
    }

    public static class StaffBillID extends BillID {
        public final String username;

        public StaffBillID(String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return username;
        }
    }
}
