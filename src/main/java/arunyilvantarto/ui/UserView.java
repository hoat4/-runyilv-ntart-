package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.Security;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.User;
import arunyilvantarto.domain.User.Role;
import arunyilvantarto.operations.*;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static arunyilvantarto.ui.UIUtil.TableBuilder.UNLIMITED_WIDTH;

public class UserView {

    private final Main app;
    private final User user;

    private ComboBox<Role> roleComboBox;
    private Button changePasswordButton;

    private TableView<Sale> staffBillTable;
    private TabPane tabPane;
    private Tab staffBillTab;
    private Button userNameButton;

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
                @Override
                public void sale(Sale sale) {
                    if (sale.billID.equals(new Sale.StaffBillID(user.name)))
                        staffBillTable.getItems().add(sale);
                }
            });
        });
        return tabPane;
    }

    public boolean staffBillShown() {
        return tabPane.getSelectionModel().getSelectedItem() == staffBillTab;
    }

    public void onEvent(AdminOperation op) {
        if (op instanceof ChangePasswordOp && ((ChangePasswordOp) op).username.equals(user.name))
            changePasswordButton.setText("Jelszó módosítása");

        if (op instanceof ChangeRoleOp && ((ChangeRoleOp) op).username.equals(user.name))
            roleComboBox.getSelectionModel().select(((ChangeRoleOp) op).newRole);

        if (op instanceof RenameUserOp && ((RenameUserOp) op).newName.equals(user.name))
            userNameButton.setText(user.name);
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
                app.executeOperation(new ChangeRoleOp(user.name, user.role, value));
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

                app.executeOperation(new ChangePasswordOp(user.name, user.passwordHash, hash));
            });
        });

        changePasswordButton.visibleProperty().bind(Bindings.notEqual(roleComboBox.valueProperty(), Role.STAFF));

        userNameButton = new Button(user.name);
        userNameButton.setOnAction(evt -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Új név");
            d.setHeaderText("Felhasználó átnevezése");
            d.setContentText(user.name + " új neve: ");
            d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(Bindings.createBooleanBinding(() ->
                    d.getEditor().getText().isEmpty() || d.getEditor().getText().equals(user.name)
                    || app.dataRoot.users.stream().anyMatch(u->u.name.equals(d.getEditor().getText())), d.getEditor().textProperty()));

            d.showAndWait().ifPresent(newName -> app.executeOperation(new RenameUserOp(user.name, newName)));
        });

        CheckBox activeCheckBox = new CheckBox();
        activeCheckBox.setSelected(!user.deleted);
        activeCheckBox.selectedProperty().addListener((o, old, value) ->{
            app.executeOperation(new SetUserDeletedOp(user.name, !value));
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
        return staffBillTable = new UIUtil.TableBuilder<Sale>(List.of()).
                col("Dátum", 100, UNLIMITED_WIDTH, sale -> sale.timestamp.atZone(ZoneId.systemDefault()).format(UIUtil.DATETIME_FORMAT)).
                col("Termék", 170, UNLIMITED_WIDTH, sale -> sale.article == null ? "" : sale.article.name).
                col("Ár", 50, UNLIMITED_WIDTH, sale -> sale.pricePerProduct).
                col("Mennyiség", 80, UNLIMITED_WIDTH, sale -> sale.quantity).
                col("Összeg", 80, UNLIMITED_WIDTH, sale -> sale.pricePerProduct * sale.quantity).
                placeholder("Nincs a személyzeti számlán még egy termék sem").
                build();
    }

/*
    private Node sells() {
        TableView<Object> table = new TableView<>();

        //TableColumn<>

        return table;
    }
*/
}
