package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Message;
import arunyilvantarto.domain.SellingPeriod;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

public class ClosePeriodOp implements AdminOperation {

    public final Map<String, Integer> purchasedProducts;
    public final Map<String, Integer> staffBillGrowths;

    @JsonIgnoreProperties("sales")
    public final SellingPeriod sellingPeriod;

    private final Message closeComment;

    public ClosePeriodOp(SellingPeriod sellingPeriod, Map<String, Integer> purchasedProducts,
                         Map<String, Integer> staffBillGrowths, Message closeComment) {
        this.sellingPeriod = sellingPeriod;
        this.purchasedProducts = purchasedProducts;
        this.staffBillGrowths = staffBillGrowths;
        this.closeComment = closeComment;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        purchasedProducts.forEach((productName, quantity) -> {
            data.article(productName).stockQuantity -= quantity;
        });
        staffBillGrowths.forEach((staffName, quantity) -> {
            data.user(staffName).staffBill += quantity;
        });
        if (closeComment != null)
            data.messages.add(closeComment);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        purchasedProducts.forEach((productName, quantity) -> {
            data.article(productName).stockQuantity += quantity;
        });
        staffBillGrowths.forEach((staffName, quantity) -> {
            data.user(staffName).staffBill -= quantity;
        });
        if (closeComment != null)
            data.messages.removeIf(msg -> msg.timestamp.equals(closeComment.timestamp));
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
