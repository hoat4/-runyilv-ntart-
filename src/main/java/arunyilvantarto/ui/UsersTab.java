package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.User;
import arunyilvantarto.operations.AddUserOp;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.ChangeRoleOp;
import arunyilvantarto.operations.RenameUserOp;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

public class UsersTab {

    private final Main app;
    private final DataRoot data;

    private final MigPane userViewContainer = new MigPane("fill", "0[]0", "0[]0");

    private UserView userView;
    private TableView<User> usersTable;

    public UsersTab(Main app) {
        this.app = app;
        this.data = app.dataRoot;
    }

    public Node build() {
        return new MigPane("fill", "[][grow]", "[][grow]").
                add(newUserButton(), "grow").
                add(userViewContainer, "grow, spany, wrap").
                add(usersTable(), "grow");
    }

    public void onEvent(AdminOperation op) {
        if (op instanceof AddUserOp)
            usersTable.getItems().add(((AddUserOp)op).user);
        if (op instanceof ChangeRoleOp || op instanceof RenameUserOp)
            usersTable.refresh();
        if (userView != null)
            userView.onEvent(op);
    }

    private Button newUserButton() {
        Button button = new Button("Felhasználó hozzáadása");
        button.setOnAction(evt -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Új felhasználó");
            dialog.setHeaderText("Felhasználó hozzáadása");
            dialog.setContentText("Név: ");
            dialog.showAndWait().ifPresent(s -> {
                User user = new User();
                user.name = s;
                user.passwordHash = null;
                user.role = User.Role.STAFF;
                app.executeOperation(new AddUserOp(user));
            });
        });
        return button;
    }

    private TableView<User> usersTable() {
        usersTable = new TableView<>();
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> nameColumn = new TableColumn<>("Név");
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name));
        nameColumn.setMinWidth(230);
        usersTable.getColumns().add(nameColumn);

        TableColumn<User, String> typeColumn = new TableColumn<>("Típus");
        typeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(roleToString(c.getValue().role)));
        typeColumn.setMinWidth(180);
        usersTable.getColumns().add(typeColumn);

        usersTable.getItems().addAll(data.users);
        usersTable.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            boolean showStaffBill = userView != null && userView.staffBillShown();
            userView = new UserView(app, value);
            userViewContainer.getChildren().clear();
            userViewContainer.add(userView.build(showStaffBill), "grow");
        });

        return usersTable;
    }

    void showUser(User user) {
        usersTable.getSelectionModel().select(user);
    }

    private String roleToString(User.Role role) {
        switch (role) {
            case ADMIN:
            case ROOT:
                return "admin";
            case STAFF:
                return "vasútszemélyzet";
            case SELLER:
                return "eladó";
            default:
                throw new UnsupportedOperationException(role.toString());
        }
    }
}
