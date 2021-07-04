package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.OperationListener;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.ClosePeriodOp;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

import java.util.ArrayList;
import java.util.List;

import static javafx.beans.binding.Bindings.isNull;

public class RevenueTab implements OperationListener {

    private final Main main;
    private final AdminPage adminPage;
    private TableView<SellingPeriod> periodTable;
    private TableView<Sale> salesInPeriodTable;

    public RevenueTab(AdminPage adminPage) {
        this.adminPage = adminPage;
        this.main = adminPage.main;
    }

    @Override
    public void onEvent(AdminOperation op) {
        if (op instanceof ClosePeriodOp)
            periodTable.getItems().add(((ClosePeriodOp) op).sellingPeriod);
    }

    public Node build() {
        List<SellingPeriod> periods = new ArrayList<>();
        main.salesIO.read(new SalesVisitor() {
            @Override
            public void beginPeriod(SellingPeriod period) {
                periods.add(period);
            }
        });

        periodTable = new UIUtil.TableBuilder<>(periods).
                col("Nyitás", 170, 180, p -> UIUtil.toDateString(p.beginTime)).
                col("Zárás", 170, 180, p -> p.endTime == null ? "" : UIUtil.toDateString(p.endTime)).
                col("Eladó", 120, 170, p -> p.username).
                col("Nyitó", 90, 90, p -> p.openCash +" Ft").
                col("Záró", 90, 90, p -> p.endTime == null ? "" : p.closeCash +" Ft").
                col("Forgalom", 120, 150, p -> p.sales.stream().mapToInt(RevenueTab::revenue).sum()+" Ft").
                build();

        salesInPeriodTable = new UIUtil.TableBuilder<Sale>(List.of()).
                col("Termék", 100, UIUtil.TableBuilder.UNLIMITED_WIDTH, s -> s.article.name).
                col("Mennyiség", 100, 100, s -> s.quantity).
                col("Bevétel", 100, 100, s -> s.pricePerProduct * s.quantity).
                placeholder("Nem volt termék eladva").
                build();

        periodTable.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            if (value == null)
                salesInPeriodTable.setVisible(false);
            else {
                salesInPeriodTable.setVisible(true);
                salesInPeriodTable.getItems().setAll(value.sales);
            }
        });

        salesInPeriodTable.setVisible(false);

        MenuItem showProductMenuItem = new MenuItem("Termék megtekintése");
        showProductMenuItem.disableProperty().bind(isNull(salesInPeriodTable.getSelectionModel().selectedItemProperty()));
        showProductMenuItem.setOnAction(evt -> {
            adminPage.showArticle(salesInPeriodTable.getSelectionModel().getSelectedItem().article);
        });
        salesInPeriodTable.setContextMenu(new ContextMenu(showProductMenuItem));

        MenuItem showSellerMenuItem= new MenuItem("Eladó megtekintése");
        showSellerMenuItem.disableProperty().bind(isNull(periodTable.getSelectionModel().selectedItemProperty()));
        showSellerMenuItem.setOnAction(evt -> {
            adminPage.showUser(main.dataRoot.user(periodTable.getSelectionModel().getSelectedItem().username));
        });
        periodTable.setContextMenu(new ContextMenu(showSellerMenuItem));

        return new MigPane("fill, hidemode 2", "[grow 1][grow 2]").
                add(periodTable, "grow").
                add(salesInPeriodTable, "grow");
    }

    private static int revenue(Sale sale) {
        if (sale.billID instanceof Sale.PeriodBillID || sale.billID instanceof Sale.PeriodCardBillID)
            return sale.pricePerProduct * sale.quantity;
        else
            return 0;
    }

}
