package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.Security;
import arunyilvantarto.domain.User;
import arunyilvantarto.domain.User.Role;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.ChangePasswordOp;
import arunyilvantarto.operations.ChangeRoleOp;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

import java.util.Arrays;

public class UserView {

    private final Main app;
    private final User user;

    private ComboBox<Role> roleComboBox;
    private Button changePasswordButton;

    public UserView(Main app, User user) {
        this.app = app;
        this.user = user;
    }

    public Node build() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("Beállítások", settings()));
        if (user.role.canSell())
            tabPane.getTabs().add(new Tab("Eladási forgalom", sells()));
        tabPane.getTabs().add(new Tab("Személyzeti számla", staffBill()));
        return tabPane;
    }

    public void onEvent(AdminOperation op) {
        if (op instanceof ChangePasswordOp && ((ChangePasswordOp)op).username.equals(user.name))
            changePasswordButton.setText("Jelszó módosítása");

        if (op instanceof ChangeRoleOp && ((ChangeRoleOp)op).username.equals(user.name))
            roleComboBox.getSelectionModel().select(((ChangeRoleOp)op).newRole);
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

        return new MigPane("align center center, hidemode 3").
                add(new Label("Név: ")).
                add(new Label(user.name), "wrap").
                add(new Label("Típus: ")).
                add(roleComboBox, "grow, wrap").
                add(changePasswordButton, "grow, span 2, wrap");

    }

    private Node staffBill() {
        TableView<Object> table = new TableView<>();

        //TableColumn<>

        return table;
    }

    private Node sells() {
        TableView<Object> table = new TableView<>();

        //TableColumn<>

        return table;
    }

}
