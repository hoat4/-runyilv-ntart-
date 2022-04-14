package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.Security;
import arunyilvantarto.events.InventoryEvent;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.User;
import arunyilvantarto.domain.User.Role;
import arunyilvantarto.events.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static arunyilvantarto.ui.UIUtil.TableBuilder.UNLIMITED_WIDTH;
import static javafx.beans.binding.Bindings.createBooleanBinding;

public class UserView {

    private final Main app;
    private final User user;

    private ComboBox<Role> roleComboBox;
    private Button changePasswordButton;

    private TableView<StaffBillItem> staffBillTable;
    private TitledPane titledPane;
    private TabPane tabPane;
    private Tab staffBillTab;
    private Button userNameButton;
    private int staffBillDebt;
    private Label staffBillDebtLabel;

    public UserView(Main app, User user) {
        this.app = app;
        this.user = user;
    }

    public Node build(boolean showStaffBill) {
        tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("Beállítások", settings()));
        //if (user.role.canSell())
        //    tabPane.getTabs().add(new Tab("Eladási forgalom", sells()));
        tabPane.getTabs().add(staffBillTab = new Tab("Személyzeti számla", staffBill()));
        if (showStaffBill)
            tabPane.getSelectionModel().select(staffBillTab);

        app.runInBackground(() -> {
            app.salesIO.read(new SalesVisitor() {

                private int debt;

                @Override
                public void sale(Sale sale) {
                    if (sale.billID.equals(new Sale.StaffBillID(user.name))) {
                        staffBillTable.getItems().add(new StaffBillPurchaseItem(sale));
                        debt += sale.pricePerProduct * sale.quantity;
                    }
                }

                @Override
                public void staffBillPay(Sale.StaffBillID bill, String administrator, int money, Instant timestamp) {
                    if (bill.equals(new Sale.StaffBillID(user.name))) {
                        staffBillTable.getItems().add(new StaffBillPayItem(bill, administrator, money, timestamp));
                        debt -= money;
                    }
                }

                @Override
                public void end() {
                    Platform.runLater(() -> {
                        staffBillDebt += debt;
                        refreshStaffBillMoneyLabel();
                    });
                }
            });
        });
        titledPane = new TitledPane(user.name, tabPane);
        titledPane.setCollapsible(false);
        return titledPane;
    }

    public boolean staffBillShown() {
        return tabPane.getSelectionModel().getSelectedItem() == staffBillTab;
    }

    public void onEvent(InventoryEvent op) {
        if (op instanceof ChangePasswordOp && ((ChangePasswordOp) op).username.equals(user.name))
            changePasswordButton.setText("Jelszó módosítása");

        if (op instanceof ChangeRoleOp && ((ChangeRoleOp) op).username.equals(user.name))
            roleComboBox.getSelectionModel().select(((ChangeRoleOp) op).newRole);

        if (op instanceof RenameUserOp && ((RenameUserOp) op).newName.equals(user.name)) {
            userNameButton.setText(user.name);
            titledPane.setText(user.name);
        }
    }

    private Node settings() {
        roleComboBox = new ComboBox<>();
        if (app.logonUser.role == Role.ROOT)
            roleComboBox.getItems().addAll(Role.values());
        else {
            if (user.role == Role.ADMIN) {
                roleComboBox.getItems().add(Role.ADMIN);
                roleComboBox.setDisable(true);
            } else {
                roleComboBox.getItems().add(Role.STAFF);
                roleComboBox.getItems().add(Role.SELLER);
            }
        }
        roleComboBox.getSelectionModel().select(user.role);
        roleComboBox.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            if (value != user.role) {
                app.onEvent(new ChangeRoleOp(user.name, user.role, value));
            }
        });

        changePasswordButton = new Button();
        changePasswordButton.setText(user.passwordHash == null ? "Jelszó létrehozása" : "Jelszó módosítása");
        changePasswordButton.setOnAction(evt -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Új jelszó");
            d.setHeaderText("Jelszóváltoztatás");
            d.setContentText(user.name + " új jelszava: ");

            d.showAndWait().ifPresent(newPassword -> {
                byte[] hash = Security.hashPassword(newPassword);

                if (Arrays.equals(hash, user.passwordHash)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Új jelszó");
                    alert.setHeaderText("Nem változott a jelszó");
                    alert.setContentText("A megadott új jelszó megegyezik azzal, ami eddig is volt " +
                            user.name + " felhasználónak beállítva. ");
                    alert.showAndWait();
                    return;
                }

                app.onEvent(new ChangePasswordOp(user.name, user.passwordHash, hash));
            });
        });

        changePasswordButton.visibleProperty().bind(Bindings.notEqual(roleComboBox.valueProperty(), Role.STAFF));

        userNameButton = new Button(user.name);
        userNameButton.setOnAction(evt -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Új név");
            d.setHeaderText("Felhasználó átnevezése");
            d.setContentText(user.name + " új neve: ");
            d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(() ->
                    d.getEditor().getText().isEmpty() || d.getEditor().getText().equals(user.name)
                            || app.dataRoot.users.stream().anyMatch(u -> u.name.equals(d.getEditor().getText())), d.getEditor().textProperty()));

            d.showAndWait().ifPresent(newName -> app.onEvent(new RenameUserOp(user.name, newName)));
        });

        CheckBox activeCheckBox = new CheckBox();
        activeCheckBox.setSelected(!user.deleted);
        activeCheckBox.selectedProperty().addListener((o, old, value) -> {
            app.onEvent(new SetUserDeletedOp(user.name, !value));
        });

        return new MigPane("align center center, hidemode 3, wrap 2").
                add(new Label("Név: ")).
                add(userNameButton).
                add(new Label("Típus: ")).
                add(roleComboBox, "grow").
                add(new Label("Aktív: ")).
                add(activeCheckBox).
                add(changePasswordButton, "grow, span 2");
    }

    private Node staffBill() {
        staffBillTable = new UIUtil.TableBuilder<StaffBillItem>(List.of()).
                col("Dátum", 90, UNLIMITED_WIDTH, sale -> sale.date().atZone(ZoneId.systemDefault()).format(UIUtil.DATETIME_FORMAT)).
                col("Termék", 170, UNLIMITED_WIDTH,
                        item -> {
                            if (item instanceof StaffBillPayItem)
                                return "Befizetés";
                            StaffBillPurchaseItem s = (StaffBillPurchaseItem) item;
                            return s.sale.article == null ? "Ismeretlen termék" : s.sale.article.name;
                        },
                        item -> item instanceof StaffBillPayItem ? "staff-bill-pay-cell" :
                                ((StaffBillPurchaseItem) item).sale.article == null ? "unknown-article-cell" : null
                ).
                col("Ár", 50, UNLIMITED_WIDTH, item -> item instanceof StaffBillPurchaseItem
                        ? ((StaffBillPurchaseItem) item).sale.pricePerProduct + " Ft"
                        : "").
                col("Db.", 80, UNLIMITED_WIDTH, item -> item instanceof StaffBillPurchaseItem
                        ? ((StaffBillPurchaseItem) item).sale.quantity : "").
                col("Eladó", 80, UNLIMITED_WIDTH, StaffBillItem::seller).
                col("Összeg", 80, UNLIMITED_WIDTH, sale -> sale.money() + " Ft").
                placeholder("Nincs a személyzeti számlán még egy termék sem").
                build();

        staffBillTable.getStyleClass().add("staff-bill-table");

        Button payButton = new Button("Befizetés");
        payButton.setOnAction(evt -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Befizetés");
            dialog.setHeaderText("Befizetés a személyzeti számlára");
            dialog.setContentText("Összeg: ");
            dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(
                    () -> !dialog.getEditor().getText().matches("[0-9]+"),
                    dialog.getEditor().textProperty()));
            dialog.showAndWait().ifPresent(s -> {
                int money = Integer.parseInt(s);
                Sale.StaffBillID billID = new Sale.StaffBillID(user.name);
                app.onEvent(new SellingEvent.StaffBillPay(billID, app.logonUser.name, money));
                staffBillDebt -= money;
                refreshStaffBillMoneyLabel();

                // ez így nem jó hosszabb távon, majd le kéne cserélni az operationökhöz hasonlóan egy értesítéses rendszerre
                // mert a timestamp sem jó, nem egyezik a SalesIO által beírttal
                staffBillTable.getItems().add(new StaffBillPayItem(billID, app.logonUser.name, money, Instant.now()));
            });
        });

        MigPane toolbar = new MigPane("fill", "", "unrelated [] unrelated").
                add(staffBillDebtLabel = new Label(), "align left").
                add(payButton, "align right");
        VBox.setVgrow(staffBillTable, Priority.ALWAYS);
        return new VBox(
                toolbar,
                staffBillTable
        );
    }

    private void refreshStaffBillMoneyLabel() {
        if (staffBillDebt < 0)
            staffBillDebtLabel.setText("Jelenlegi túlfizetés: " + -staffBillDebt + " Ft");
        else if (staffBillDebt == 0)
            staffBillDebtLabel.setText("Nincs tartozás a számlán. ");
        else
            staffBillDebtLabel.setText("Jelenlegi tartozás: " + staffBillDebt + " Ft");
    }

    private interface StaffBillItem {

        Instant date();

        int money();

        String seller();

    }

    private static class StaffBillPurchaseItem implements StaffBillItem {

        public final Sale sale;

        public StaffBillPurchaseItem(Sale sale) {
            this.sale = sale;
        }

        @Override
        public Instant date() {
            return sale.timestamp;
        }

        @Override
        public int money() {
            return sale.pricePerProduct * sale.quantity;
        }

        @Override
        public String seller() {
            return sale.seller;
        }

    }

    private static class StaffBillPayItem implements StaffBillItem {

        public final Sale.StaffBillID bill;
        public final String administrator;
        public final int money;
        public final Instant timestamp;

        public StaffBillPayItem(Sale.StaffBillID bill, String administrator, int money, Instant timestamp) {
            this.bill = bill;
            this.administrator = administrator;
            this.money = money;
            this.timestamp = timestamp;
        }

        @Override
        public Instant date() {
            return timestamp;
        }

        @Override
        public int money() {
            return money;
        }

        @Override
        public String seller() {
            return administrator;
        }
    }
}
