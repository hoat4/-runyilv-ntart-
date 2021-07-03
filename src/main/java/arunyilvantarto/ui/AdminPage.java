package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.OperationListener;
import arunyilvantarto.operations.AdminOperation;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class AdminPage implements OperationListener {

    private final Main app;

    private ArticlesTab articlesTab;
    private AddItemTab addItemTab;
    private UsersTab usersTab;
    private RevenueTab revenueTab;

    public AdminPage(Main app) {
        this.app = app;
    }

    public Node build() {
        TabPane tabPane = new TabPane();

        Tab articlesTab = new Tab("Árucikkek", (this.articlesTab = new ArticlesTab(app)).build());
        articlesTab.setClosable(false);
        tabPane.getTabs().add(articlesTab);

        Tab addProductTab = new Tab("Termékfelvitel", (this.addItemTab = new AddItemTab(app)).build());
        addProductTab.setClosable(false);
        tabPane.getTabs().add(addProductTab);

        Tab usersTab = new Tab("Felhasználók", (this.usersTab = new UsersTab(app)).build());
        usersTab.setClosable(false);
        tabPane.getTabs().add(usersTab);

        Tab revenueTab = new Tab("Forgalom", (this.revenueTab = new RevenueTab(app)).build());
        revenueTab.setClosable(false);
        tabPane.getTabs().add(revenueTab);

        return tabPane;
    }

    @Override
    public void onEvent(AdminOperation op) {
        articlesTab.onEvent(op);
        addItemTab.onEvent(op);
        usersTab.onEvent(op);
    }
}
