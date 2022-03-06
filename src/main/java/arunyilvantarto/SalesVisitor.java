package arunyilvantarto;

import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;

public interface SalesVisitor {

    default void begin(){}

    default void beginPeriod(SellingPeriod period, String comment) {}

    default void sale(Sale sale) {}

    default void endPeriod(SellingPeriod period, String comment){}

    default void modifyCash(String username, int cash, int creditCardAmount) {}

    default void end() {}

}
