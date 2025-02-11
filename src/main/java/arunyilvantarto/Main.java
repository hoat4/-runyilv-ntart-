package arunyilvantarto;

import arunyilvantarto.domain.*;
import arunyilvantarto.events.AdminOperation;
import arunyilvantarto.events.InventoryEvent;
import arunyilvantarto.events.SellingEvent;
import arunyilvantarto.ui.AdminPage;
import arunyilvantarto.ui.LoginForm;
import arunyilvantarto.ui.SellingTab;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.javafx.scene.NodeHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.miginfocom.layout.PlatformDefaults;
import net.miginfocom.layout.UnitValue;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardOpenOption.*;

public class Main extends Application {

    public static final String SALES_TSV_NAME = "sales.tsv";
    public volatile DataRoot dataRoot;
    public SellingPeriod currentSellingPeriod;
    private OperationListener rootListener;
    public volatile User logonUser;
    public SalesIO salesIO;

    public final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Scene scene;
    private Stage stage;

    static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    static final ObjectWriter JSON_WRITER;

    static {
        JSON_MAPPER.registerModule(new JavaTimeModule());
        JSON_WRITER = JSON_MAPPER.writerWithDefaultPrettyPrinter();
    }

    public void switchPage(Node newPage, OperationListener newRootListener) {
        scene.setRoot(new StackPane(newPage));
        rootListener = newRootListener;
    }

    public void preload(Node node) {
        StackPane sp = new StackPane(node);
        sp.getStylesheets().add("/arunyilvantarto/app.css");
        NodeHelper.processCSS(sp);
        sp.layout();
    }

    @Override
    public void init() throws Exception {
        dataRoot = JSON_MAPPER.readValue(Files.readAllBytes(Path.of("data.json")), DataRoot.class);

        final Path tsvPath = salesTsvPath();
        if (Files.isRegularFile(tsvPath))
            salesIO = new SalesIO(dataRoot, FileChannel.open(tsvPath, READ, WRITE));
        else {
            salesIO = new SalesIO(dataRoot, FileChannel.open(tsvPath, READ, WRITE, CREATE_NEW));
            salesIO.begin();
        }
    }

    public Path salesTsvPath() {
        return Path.of(SALES_TSV_NAME);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        PlatformDefaults.setRelatedGap(new UnitValue(12), new UnitValue(12));
        stage = primaryStage;

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Hiba");
                alert.setHeaderText("Hiba");
                alert.setContentText(e.toString());
                alert.showAndWait();
            });
        });

        LoginForm loginForm = new LoginForm(this);
        Region root = loginForm.buildLayout();
        this.scene = new Scene(root);
        scene.getStylesheets().add("/arunyilvantarto/app.css");
        primaryStage.setOnCloseRequest(evt -> {
            if (rootListener instanceof SellingTab) {
                if (!((SellingTab) rootListener).close())
                    evt.consume();
            } else if (rootListener instanceof AdminPage) {
                if (!((AdminPage) rootListener).close())
                    evt.consume();
            }

        });
        primaryStage.setScene(scene);
        primaryStage.setWidth(1270);
        primaryStage.setHeight(900);
        primaryStage.show();
        primaryStage.setOnHidden(e -> {
            executor.shutdown();
        });

        if (false) {
            logonUser = dataRoot.user("u");
            loginForm.loadAndShowNextPage();
        }
    }

    private static DataRoot makeSampleData() {
        DataRoot data = new DataRoot();

        User u = new User();
        u.name = "u";
        u.role = User.Role.ROOT;
        u.passwordHash = Security.hashPassword("p");

        Article a = new Article();
        a.timestamp = Instant.now();
        a.name = "árucikknév";
        a.barCode = "3456789";
        a.sellingPrice = 1000;

        Item p = new Item();
        p.id = UUID.randomUUID();
        p.article = a;
        p.purchasePrice = 500;
        p.purchaseQuantity = 1;
        p.timestamp = Instant.now();
        a.items = new ArrayList<>();
        a.items.add(p);

        data.articles.add(a);

        data.users.add(u);
        return data;
    }

    public void runInBackground(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        });
    }

    public void onEvent(InventoryEvent event) {
        try {
            System.out.println(JSON_MAPPER.writeValueAsString(event));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (event) {
            case AdminOperation op -> op.execute(dataRoot, this);
            case SellingEvent sellingEvent -> salesIO.writeEvent(sellingEvent);
        }

        if (rootListener != null)
            rootListener.onEvent(event);

        if (event instanceof AdminOperation)
            if (Platform.isFxApplicationThread())
                executor.execute(this::writeDataToFile);
            else
                writeDataToFile();
    }

    private void writeDataToFile() {
        try {
            Files.write(Path.of("data.json"), JSON_WRITER.writeValueAsBytes(dataRoot));
        } catch (Throwable e) {
            e.printStackTrace();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Hiba");
                alert.setHeaderText("Nem sikerült elmenteni a változtatást");
                alert.setContentText(e.toString());
                alert.showAndWait();
            });
        }
    }

    public Object activePage() {
        return rootListener;
    }
}
