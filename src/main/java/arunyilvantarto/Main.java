package arunyilvantarto;

import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Product;
import arunyilvantarto.domain.User;
import arunyilvantarto.operations.AddProductOp;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.ui.LoginForm;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.miginfocom.layout.PlatformDefaults;
import net.miginfocom.layout.UnitValue;

import java.sql.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Main extends Application {

    public final DataRoot dataRoot = makeSampleData();
    private OperationListener rootListener;

    private Scene scene;

    public void switchPage(Node newPage, OperationListener newRootListener) {
        scene.setRoot(new StackPane(newPage));
        rootListener = newRootListener;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        PlatformDefaults.setRelatedGap(new UnitValue(12), new UnitValue(12));

        LoginForm loginForm = new LoginForm(this);
        Region root = loginForm.buildLayout();

        this.scene = new Scene(root);
        scene.getStylesheets().add("/arunyilvantarto/app.css");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1500);
        primaryStage.setHeight(900);
        primaryStage.show();
    }

    private static DataRoot makeSampleData() {
        DataRoot data = new DataRoot();

        User u = new User();
        u.name = "u";
        u.role = User.Role.ROOT;
        u.passwordHash = Security.hashPassword("p");

        Article a = new Article();
        a.id = UUID.randomUUID();
        a.timestamp = Instant.now();
        a.name = "árucikknév";
        a.barCode = "3456789";
        a.sellingPrice = 1000;

        Product p = new Product();
        p.id = UUID.randomUUID();
        p.purchasePrice = 500;
        p.purchaseQuantity = 1;
        p.stockQuantity = 1;
        p.timestamp = Instant.now();
        a.products = new ArrayList<>();
        a.products.add(p);

        data.articles.add(a);

        data.users.add(u);
        return data;
    }

    public void executeAdminOperation(AdminOperation op) {
        if (rootListener != null)
            rootListener.onEvent(op);

        op.execute(dataRoot);
    }
}
