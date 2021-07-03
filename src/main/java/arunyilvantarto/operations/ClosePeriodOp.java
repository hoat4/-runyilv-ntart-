package arunyilvantarto.operations;

import arunyilvantarto.domain.DataRoot;

import java.util.Map;

public class ClosePeriodOp implements AdminOperation{

    public final Map<String, Integer> purchasedProducts;
    public final Map<String, Integer> staffBillGrowths;

    public ClosePeriodOp(Map<String, Integer> purchasedProducts, Map<String, Integer> staffBillGrowths) {
        this.purchasedProducts = purchasedProducts;
        this.staffBillGrowths = staffBillGrowths;
    }

    @Override
    public void execute(DataRoot data) {
        purchasedProducts.forEach((productName, quantity)->{
            data.article(productName).stockQuantity -= quantity;
        });
        staffBillGrowths.forEach((staffName, quantity)->{
            data.user(staffName).staffBill += quantity;
        });
    }

    @Override
    public void undo(DataRoot data) {
        purchasedProducts.forEach((productName, quantity)->{
            data.article(productName).stockQuantity += quantity;
        });
        staffBillGrowths.forEach((staffName, quantity)->{
            data.user(staffName).staffBill -= quantity;
        });
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
