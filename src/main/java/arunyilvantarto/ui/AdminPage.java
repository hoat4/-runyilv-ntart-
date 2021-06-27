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
    private UsersTab usersTab;
    private RevenueTab revenueTab;

    public AdminPage(Main app) {
        this.app = app;
    }

    public Node build() {
        TabPane tabPane = new TabPane();

        Tab articlesTab = new Tab("Termékek", (this.articlesTab = new ArticlesTab(app)).build());
        articlesTab.setClosable(false);
        tabPane.getTabs().add(articlesTab);

        Tab usersTab = new Tab("Felhasználók",(this.usersTab = new UsersTab()).build());
        usersTab.setClosable(false);
        tabPane.getTabs().add(usersTab);

        Tab revenueTab = new Tab("Forgalom", (this.revenueTab = new RevenueTab()).build());
        revenueTab.setClosable(false);
        tabPane.getTabs().add(revenueTab);

        return tabPane;
    }

    @Override
    public void onEvent(AdminOperation op) {
        articlesTab.onEvent(op);
    }
}
