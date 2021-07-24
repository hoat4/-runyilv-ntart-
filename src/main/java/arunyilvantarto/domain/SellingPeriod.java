package arunyilvantarto.domain;

import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;

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
        return revenue() + openCash - creditCardRevenue;
    }

    public int revenue() {
        return sales.stream().collect(groupingBy(sale -> sale.paymentID)).values().stream().
                mapToInt(s -> round(s.stream().
                        filter(sale -> sale.billID instanceof Sale.PeriodBillID || sale.billID instanceof Sale.PeriodCardBillID).
                        mapToInt(sale -> sale.pricePerProduct * sale.quantity).sum())).
                sum();
    }

    public boolean isClosed() {
        return endTime != null;
    }

    public static int round(int price) {
        return (price + 2) / 5 * 5;
    }
}
