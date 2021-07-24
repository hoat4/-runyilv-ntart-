package arunyilvantarto.domain;

import java.time.Instant;
import java.util.List;

public class SellingPeriod {

    public int id;
    public String username;
    public Instant beginTime;
    public int openCash;
    public Instant endTime;
    public int closeCash;
    public int openCreditCardAmount;
    public int closeCreditCardAmount;

    public List<Sale> sales;

    public int remainingCash(int creditCardRevenue) {
        return sales.stream().mapToInt(s -> s.pricePerProduct * s.quantity).reduce(openCash - creditCardRevenue, Integer::sum);
    }

    public int revenue() {
        return sales.stream().mapToInt(s -> s.pricePerProduct * s.quantity).reduce(openCash, Integer::sum);
    }

    public boolean isClosed() {
        return endTime != null;
    }
}
