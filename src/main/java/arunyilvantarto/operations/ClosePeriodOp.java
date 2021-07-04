package arunyilvantarto.operations;

import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.SellingPeriod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

public class ClosePeriodOp implements AdminOperation{

    public final Map<String, Integer> purchasedProducts;
    public final Map<String, Integer> staffBillGrowths;

    @JsonIgnoreProperties("sales")
    public final SellingPeriod sellingPeriod;

    public ClosePeriodOp(SellingPeriod sellingPeriod, Map<String, Integer> purchasedProducts, Map<String, Integer> staffBillGrowths) {
        this.sellingPeriod = sellingPeriod;
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
