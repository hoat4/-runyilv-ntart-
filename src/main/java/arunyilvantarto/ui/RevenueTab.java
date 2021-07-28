package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.OperationListener;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.ClosePeriodOp;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import org.tbee.javafx.scene.layout.MigPane;

import java.util.*;
import java.util.function.Consumer;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.beans.binding.Bindings.isNull;

public class RevenueTab implements OperationListener {

    private final Main main;
    private final AdminPage adminPage;
    private TableView<SellingPeriod> periodTable;
    private TableView<Sale> salesInPeriodTable;

    private int cash, creditCardAmount;
    private Button cashButton, creditCardAmountButton;

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
        Map<SellingPeriod, Integer> openCashMismatch = new HashMap<>();
        Map<SellingPeriod, Integer> closeCashMismatch = new HashMap<>();

        List<SellingPeriod> periods = new ArrayList<>();
        cash = 0;
        creditCardAmount = 0;
        main.salesIO.read(new SalesVisitor() {
            @Override
            public void beginPeriod(SellingPeriod period) {
                if (period.openCash != cash)
                    openCashMismatch.put(period, cash);
                periods.add(period);
            }

            @Override
            public void endPeriod(SellingPeriod period) {
                int c = period.remainingCash(period.closeCreditCardAmount - period.openCreditCardAmount);
                if (period.closeCash != c)
                    closeCashMismatch.put(period, c);

                cash = period.closeCash;
                creditCardAmount = period.closeCreditCardAmount;
            }

            @Override
            public void modifyCash(int cash, int creditCardAmount) {
                RevenueTab.this.cash = cash;
                RevenueTab.this.creditCardAmount = creditCardAmount;
            }
        });

        periodTable = new UIUtil.TableBuilder<>(periods).
                col("Nyitás", 120, 180, p -> UIUtil.toDateString(p.beginTime)).
                col("Zárás", 120, 180, p -> p.endTime == null ? "" : UIUtil.toDateString(p.endTime)).
                col("Eladó", 120, 170, p -> p.username).
                customCol("Nyitó kp.", 100, 90, p -> {
                    Label lbl = new Label(p.openCash + " Ft");
                    if (openCashMismatch.containsKey(p)) {
                        lbl.getStyleClass().add("cash-mismatch-cell");
                        Label expectedCashLabel = new Label(" (" + openCashMismatch.get(p) + ")");
                        expectedCashLabel.getStyleClass().add("expected-cash-label");
                        expectedCashLabel.setAlignment(Pos.CENTER);
                        return new HBox(lbl, expectedCashLabel);
                    } else
                        return lbl;
                }).
                customCol("Záró kp.", 100, 90, p -> {
                    Label lbl = new Label(p.endTime == null ? "" : p.closeCash + " Ft");
                    if (closeCashMismatch.containsKey(p)) {
                        lbl.getStyleClass().add("cash-mismatch-cell");
                        Label expectedCashLabel = new Label(" (" + closeCashMismatch.get(p) + ")");
                        expectedCashLabel.getStyleClass().add("expected-cash-label");
                        expectedCashLabel.setAlignment(Pos.CENTER);
                        return new HBox(lbl, expectedCashLabel);
                    }
                    return lbl;

                }).
                col("Nyitó bk.", 80, 90, p -> p.openCreditCardAmount + " Ft").
                col("Záró bk.", 80, 90, p -> p.endTime == null ? "" : p.closeCreditCardAmount + " Ft").
                col("Forgalom", 90, 150, p -> p.revenue() + " Ft").
                build();

        salesInPeriodTable = new UIUtil.TableBuilder<Sale>(List.of()).
                col("Termék", 100, UIUtil.TableBuilder.UNLIMITED_WIDTH, s -> s.article.name).
                col("Mennyiség", 120, 100, s -> s.quantity).
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

        MenuItem showSellerMenuItem = new MenuItem("Eladó megtekintése");
        showSellerMenuItem.disableProperty().bind(isNull(periodTable.getSelectionModel().selectedItemProperty()));
        showSellerMenuItem.setOnAction(evt -> {
            adminPage.showUser(main.dataRoot.user(periodTable.getSelectionModel().getSelectedItem().username));
        });
        periodTable.setContextMenu(new ContextMenu(showSellerMenuItem));

        Node cashPanel = new MigPane().
                add(new Label("Készpénz: ")).
                add(cashButton = new Button(), "grow, wrap").
                add(new Label("Bankkártyán: ")).
                add(creditCardAmountButton = new Button(), "grow, wrap");

        cashButton.setText(cash + " Ft");
        creditCardAmountButton.setText(creditCardAmount + " Ft");

        cashButton.setOnAction(evt -> modifyCash("Kaszában lévő készpénz", v -> cash = v, cashButton));
        creditCardAmountButton.setOnAction(evt -> modifyCash("Bankkártyán lévő összeg", v -> creditCardAmount = v, creditCardAmountButton));
        TitledPane cashPane = new TitledPane("Kassza", cashPanel);
        cashPane.setCollapsible(false);

        MigPane p = new MigPane("fill, hidemode 2", "[grow 1][grow 2]", "[][grow]").
                add(periodTable, "spany 2, grow").
                add(cashPane, "wrap, grow").
                add(salesInPeriodTable, "grow");
        p.getStyleClass().add("revenue-tab");
        return p;
    }

    private void modifyCash(String displayName, Consumer<Integer> setter, Button button) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Kassza módosítása");
        dialog.setHeaderText(displayName + " módosítása");
        dialog.setContentText(displayName + ": ");
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().
                bind(createBooleanBinding(() -> !dialog.getEditor().getText().matches("[0-9]+"),
                        dialog.getEditor().textProperty()));
        dialog.showAndWait().ifPresent(s -> {
            setter.accept(Integer.parseInt(s));
            button.setText(s + " Ft");
            main.salesIO.modifyCash(main.logonUser.name, cash, creditCardAmount);
        });
    }

}
