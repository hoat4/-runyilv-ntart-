package arunyilvantarto.events;

import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;
import com.fasterxml.jackson.annotation.JsonTypeName;

public sealed interface SellingEvent extends InventoryEvent {

    @JsonTypeName("BeginPeriod")
    record BeginPeriodEvent(SellingPeriod period, String comment) implements SellingEvent {
    }

    @JsonTypeName("Sale")
    record SaleEvent(Sale sale) implements SellingEvent {
    }

    @JsonTypeName("EndPeriod")
    record EndPeriodEvent(SellingPeriod period, String comment) implements SellingEvent {
    }

    @JsonTypeName("ModifyCash")
    record ModifyCashEvent(String username, int cash, int creditCardAmount) implements SellingEvent {
    }

    @JsonTypeName("StaffBillPay")
    record StaffBillPay(Sale.StaffBillID bill, String administrator, int money) implements SellingEvent {
    }
}
