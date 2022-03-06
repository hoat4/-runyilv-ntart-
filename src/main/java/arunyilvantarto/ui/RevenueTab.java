package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.OperationListener;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.domain.Message;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.ClosePeriodOp;
import arunyilvantarto.operations.RenameUserOp;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.beans.binding.Bindings.isNull;

public class RevenueTab implements OperationListener {

    private final Main main;
    private final AdminPage adminPage;
    private TableView<SellingPeriod> periodTable;
    private TableView<Sale> salesInPeriodTable;
    private List<SellingPeriod> periods = new ArrayList<>();

    private int cash, creditCardAmount;
    private Button cashButton, creditCardAmountButton;

    private PeriodCommentPanel periodOpenCommentPanel = new PeriodCommentPanel("Nyitási megjegyzés");
    private PeriodCommentPanel periodCloseCommentPanel = new PeriodCommentPanel("Zárási megjegyzés");

    public RevenueTab(AdminPage adminPage) {
        this.adminPage = adminPage;
        this.main = adminPage.main;
    }

    @Override
    public void onEvent(AdminOperation op) {
        if (op instanceof ClosePeriodOp)
            periodTable.getItems().add(((ClosePeriodOp) op).sellingPeriod);
        if (op instanceof RenameUserOp) {
            periods.forEach(p -> {
                if (p.username.equals(((RenameUserOp) op).oldName))
                    p.username = ((RenameUserOp) op).newName;
            });
            periodTable.refresh();
        }

        periodOpenCommentPanel.onEvent(op);
        periodCloseCommentPanel.onEvent(op);
    }

    public Node build() {
        Map<SellingPeriod, Integer> openCashMismatch = new HashMap<>();
        Map<SellingPeriod, Integer> closeCashMismatch = new HashMap<>();

        cash = 0;
        creditCardAmount = 0;
        main.salesIO.read(new SalesVisitor() {
            @Override
            public void beginPeriod(SellingPeriod period, String comment) {
                if (period.openCash != cash)
                    openCashMismatch.put(period, cash);
                periods.add(period);
            }

            @Override
            public void endPeriod(SellingPeriod period, String comment) {
                int c = period.remainingCash(period.closeCreditCardAmount - period.openCreditCardAmount);
                if (period.closeCash != c)
                    closeCashMismatch.put(period, c);

                cash = period.closeCash;
                creditCardAmount = period.closeCreditCardAmount;
            }

            @Override
            public void modifyCash(String username, int cash, int creditCardAmount) {
                RevenueTab.this.cash = cash;
                RevenueTab.this.creditCardAmount = creditCardAmount;
            }
        });

        periodTable = new UIUtil.TableBuilder<>(periods).
                col("Nyitás", 120, 180, p -> {
                    return dateWithCommentWarning(p.beginTime, new Message.OpenPeriodSubject(p.id));
                }).
                customCol("Zárás", 130, 180, p -> {
                    return dateWithCommentWarning(p.endTime, new Message.ClosePeriodSubject(p.id));
                }).
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
                col("Termék", 100, UIUtil.TableBuilder.UNLIMITED_WIDTH,
                        s -> s.article == null ? "" : s.article.name).
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

                periodOpenCommentPanel.setContent(main.dataRoot.messages.stream().
                        filter(m -> m.subject.equals(new Message.OpenPeriodSubject(value.id))).
                        findAny().orElse(null));

                periodCloseCommentPanel.setContent(main.dataRoot.messages.stream().
                        filter(m -> m.subject.equals(new Message.ClosePeriodSubject(value.id))).
                        findAny().orElse(null));
            }
        });

        salesInPeriodTable.setVisible(false);

        MenuItem showProductMenuItem = new MenuItem("Termék megtekintése");
        showProductMenuItem.disableProperty().bind(createBooleanBinding(() ->
                        salesInPeriodTable.getSelectionModel().getSelectedItem() == null ||
                                salesInPeriodTable.getSelectionModel().getSelectedItem().article == null,
                salesInPeriodTable.getSelectionModel().selectedItemProperty()));
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

        MigPane p = new MigPane("fill, hidemode 2, wrap 2", "[grow 1][grow 2]", "[][shrink][shrink][grow]").
                add(periodTable, "spany 4, grow").
                add(cashPane, "grow").
                add(periodOpenCommentPanel.titledPane, "grow").
                add(periodCloseCommentPanel.titledPane, "grow").
                add(salesInPeriodTable, "grow");
        p.getStyleClass().add("revenue-tab");
        return p;
    }

    private Region dateWithCommentWarning(Instant time, Message.Subject messageSubject) {
        Message msg = main.dataRoot.messages.stream().
                filter(m -> m.subject.equals(messageSubject)).
                findAny().orElse(null);
        Label dateLabel = new Label(time == null ? "" : UIUtil.toDateString(time));
        if (msg == null)
            return dateLabel;
        Label label = new Label("!");
        label.getStyleClass().add("salesperiod-closecomment-warning");
        HBox hbox = new HBox(dateLabel, label);
        hbox.setSpacing(5);
        return hbox;
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


    private static class PeriodCommentPanel {

        private TextArea textArea;
        public TitledPane titledPane;
        private final String titlePrefix;
        private Message currentMessage;

        public PeriodCommentPanel(String titlePrefix) {
            this.titlePrefix = titlePrefix;
            textArea = new TextArea();
            textArea.setEditable(false);
            textArea.setFocusTraversable(false);
            textArea.getStyleClass().add("revenuetab-periodclosecomment-textarea");
            textArea.setPrefColumnCount(15);
            textArea.setPrefRowCount(4);
            titledPane = new TitledPane();
            titledPane.setContent(textArea);
            titledPane.setCollapsible(false);
            titledPane.setVisible(false);
        }

        public void setContent(Message message) {
            this.currentMessage = message;
            titledPane.setVisible(message != null);
            if (message != null) {
                setTitle();
                textArea.setText(message.text);
            } else {
                textArea.setText("");
            }
        }

        private void setTitle() {
            if (currentMessage != null)
                titledPane.setText(titlePrefix + " " + currentMessage.sender.name + " által");
        }

        public void onEvent(AdminOperation op) {
            if (op instanceof RenameUserOp)
                setTitle();
        }
    }
}
