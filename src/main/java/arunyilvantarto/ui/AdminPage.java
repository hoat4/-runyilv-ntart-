package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.OperationListener;
import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.User;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.RenameUserOp;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.tbee.javafx.scene.layout.MigPane;

public class AdminPage implements OperationListener {

    final Main main;

    private TabPane tabPane;

    private Tab articlesTab, usersTab;

    private ArticlesTab articles;
    private AddItemTab addItem;
    private UsersTab users;
    private RevenueTab revenue;
    private MessagesTab messages;

    private Label logonLabel;

    public AdminPage(Main main) {
        this.main = main;
    }

    public Node build() {
        tabPane = new TabPane();
        tabPane.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ESCAPE) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Kijelentkezés");
                alert.setHeaderText("Kijelentkezés");
                alert.setContentText("Biztos kijelentkezel?");
                ButtonType yesButton = new ButtonType("Igen", ButtonBar.ButtonData.YES);
                ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                alert.getDialogPane().getButtonTypes().setAll(yesButton, noButton);
                if (alert.showAndWait().orElse(null) == yesButton) {
                    main.logonUser = null;
                    LoginForm loginForm = new LoginForm(main);
                    main.switchPage(loginForm.buildLayout(), null);
                }
            }
        });

        articlesTab = new Tab("Árucikkek", (this.articles = new ArticlesTab(main)).build());
        articlesTab.setClosable(false);
        tabPane.getTabs().add(articlesTab);

        Tab addProductTab = new Tab("Termékfelvitel", (this.addItem = new AddItemTab(main)).build());
        addProductTab.setClosable(false);
        tabPane.getTabs().add(addProductTab);

        usersTab = new Tab("Felhasználók", (this.users = new UsersTab(main)).build());
        usersTab.setClosable(false);
        tabPane.getTabs().add(usersTab);

        Tab revenueTab = new Tab("Forgalom", (this.revenue = new RevenueTab(this)).build());
        revenueTab.setClosable(false);
        tabPane.getTabs().add(revenueTab);

        //Tab messagesTab = new Tab("Üzenetek", (this.messages = new MessagesTab(this)).build());
        //messagesTab.setClosable(false);
        //tabPane.getTabs().add(messagesTab);

        Button beginSellingButton = new Button("Kassza nyitása");
        beginSellingButton.setOnAction(evt -> {
            SellingTab.begin(main, null, null);
        });

        Button logoutButton = new Button("Kijelentkezés");
        logoutButton.setOnAction(evt -> {
            main.logonUser = null;
            LoginForm loginForm = new LoginForm(main);
            main.switchPage(loginForm.buildLayout(), null);
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        logonLabel = new Label();
        initLogonLabel();

        return new MigPane("fill, wrap 1, gap 0, ins 0", null, "[][grow]").
                add(new ToolBar(beginSellingButton, spacer, logonLabel, logoutButton), "grow").
                add(tabPane, "grow");
    }

    private void initLogonLabel() {
        logonLabel.setText("Bejelentkezve " + main.logonUser.name + "-ként");
    }

    @Override
    public void onEvent(AdminOperation op) {
        if (op instanceof RenameUserOp)
            initLogonLabel();

        articles.onEvent(op);
        addItem.onEvent(op);
        users.onEvent(op);
        revenue.onEvent(op);
    }

    public void showArticle(Article article) {
        tabPane.getSelectionModel().select(articlesTab);
        articles.showArticle(article);
    }

    public void showUser(User user) {
        tabPane.getSelectionModel().select(usersTab);
        users.showUser(user);
    }
}
