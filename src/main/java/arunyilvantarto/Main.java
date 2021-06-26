package arunyilvantarto;

import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.User;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import net.miginfocom.layout.PlatformDefaults;
import net.miginfocom.layout.UnitValue;

import java.security.MessageDigest;

public class Main extends Application {

    private final DataRoot dataRoot = makeSampleData();
    private final ReadOps readOps = new ReadOps(dataRoot);

    @Override
    public void start(Stage primaryStage) throws Exception {
        PlatformDefaults.setRelatedGap(new UnitValue(12), new UnitValue(12));

        Region root = new LoginForm(readOps).buildLayout();

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/arunyilvantarto/app.css");
        primaryStage.setScene(scene);
        primaryStage.setWidth(500);
        primaryStage.setHeight(500);
        primaryStage.show();
    }

    private static DataRoot makeSampleData() {
        DataRoot data = new DataRoot();

        User u = new User();
        u.name = "u";
        u.role = User.Role.ROOT;
        u.passwordHash = Security.hashPassword("p");

        data.users.add(u);
        return data;
    }
}
