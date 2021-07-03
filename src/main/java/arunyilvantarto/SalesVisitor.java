package arunyilvantarto;

import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;

public interface SalesVisitor {

    default void begin(){}

    default void beginPeriod(SellingPeriod period) {}

    default void sale(Sale sale) {}

    default void endPeriod(SellingPeriod period){}

    default void end() {}
}
