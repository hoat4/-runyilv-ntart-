package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.OperationListener;
import arunyilvantarto.SalesIO;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;
import arunyilvantarto.domain.User;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.ClosePeriodOp;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.util.*;

import static arunyilvantarto.ui.UIUtil.TableBuilder.UNLIMITED_WIDTH;
import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.layout.Region.USE_PREF_SIZE;

public class SellingTab implements OperationListener {

    private final Main main;

    private TableView<Sale> salesTable;

    private int sumPrice;
    private Label sumPriceLabel;
    private final SellingPeriod sellingPeriod;
    private User staffBill;
    private ToggleButton staffBillButton;

    private SellingTab(Main main, SellingPeriod sellingPeriod) {
        this.main = main;
        this.sellingPeriod = sellingPeriod;
    }

    public static void begin(Main app) {
        SellingPeriod lastSellingPeriod = lastSellingPeriod(app.salesIO);
        if (lastSellingPeriod != null && lastSellingPeriod.endTime == null)
            throw new IllegalStateException("nem volt lezárva");

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nyitás");
        dialog.setHeaderText("A kasszában elvileg XY forint váltópénz maradt.");
        dialog.setContentText("Kasszában lévő váltópénz: ");
        dialog.showAndWait().ifPresent(s -> {
            SellingPeriod sellingPeriod = new SellingPeriod();
            sellingPeriod.id = lastSellingPeriod == null ? 1 : lastSellingPeriod.id + 1;
            sellingPeriod.username = app.logonUser.name;
            sellingPeriod.beginTime = Instant.now();
            sellingPeriod.openCash = Integer.parseInt(s);
            sellingPeriod.sales = new ArrayList<>();
            app.currentSellingPeriod = sellingPeriod;

            app.salesIO.beginPeriod(sellingPeriod);

            SellingTab sellingTab = new SellingTab(app, sellingPeriod);
            app.switchPage(sellingTab.build(), sellingTab);
        });
    }

    public static SellingPeriod lastSellingPeriod(SalesIO salesIO) {
        SellingPeriod[] a = new SellingPeriod[1];
        salesIO.read(new SalesVisitor() {

            @Override
            public void beginPeriod(SellingPeriod period) {
                a[0] = period;
            }

        });
        return a[0];
    }

    public Node build() {
        sumPriceLabel = new Label();
        sumPriceLabel.setVisible(false);
        sumPriceLabel.setPadding(new Insets(10, 0, 0, 0)); // TODO gaptop miért marad ott, ha hidemode 2?
        sumPriceLabel.getStyleClass().add("sumPriceLabel");

        MigPane p = new MigPane("align center center, wrap 1", "[70%:]", "unrelated [] related [grow] 0 [] 15 [] 18").
                add(topToolbar(), "grow").
                add(salesTable(), "grow").
                add(sumPriceLabel, "hidemode 2, align right").
                add(bottomToolbar(), "grow");
        p.getStyleClass().add("selling-tab");
        return p;
    }

    private Node topToolbar() {
        Button logoutButton = new Button(main.logonUser.role == User.Role.SELLER ? "Kijelentkezés" : "Zárás");
        logoutButton.setOnAction(evt -> {
            if (close()) {
                if (main.logonUser.role == User.Role.SELLER) {
                    final LoginForm loginForm = new LoginForm(main);
                    main.switchPage(loginForm.buildLayout(), null);
                } else {
                    AdminPage adminPage = new AdminPage(main);
                    main.switchPage(adminPage.build(), adminPage);
                }
            }
        });

        TextField barcodeField = new TextField();
        barcodeField.setOnAction(evt -> {
            String barcode = barcodeField.getText();

            main.dataRoot.articles.stream().filter(a -> a.barCode.equals(barcode)).findAny().
                    ifPresentOrElse(this::addArticle, () -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Nincs ilyen termék");
                        alert.setContentText("Nincs nyilvántartva olyan termék, melynek vonalkódja " + barcode + " lenne. ");
                        alert.showAndWait();
                    });

            barcodeField.setText("");
        });
        barcodeField.focusedProperty().addListener((o, old, value)->{
            if (!value)
                barcodeField.requestFocus();
        });


        return new MigPane("align center center", "[] push [] push []").
                add(new Label("Pénztáros: " + main.logonUser.name)).
                add(barcodeField).
                add(logoutButton);
    }

    private TableView<Sale> salesTable() {
        return salesTable = new UIUtil.TableBuilder<Sale>(new ArrayList<>()).
                col("Termék", 0, UNLIMITED_WIDTH, sale -> sale.article.name).
                col("Ár", 150, 150, sale -> sale.pricePerProduct).
                col("Mennyiség", 150, 150, sale -> sale.quantity).
                col("Összesen", 150, 150, sale -> sale.pricePerProduct * sale.quantity).
                placeholder("Olvasd be a termékek vonalkódját").
                build();
    }

    private Node bottomToolbar() {
        staffBillButton = new ToggleButton("Személyzeti számlára (F4)");
        staffBillButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        staffBillButton.setOnAction(evt -> toStaffBill());
        UIUtil.assignShortcut(staffBillButton, new KeyCodeCombination(F4));

        Button selectArticleManuallyButton = new Button("Termék kiválasztása (F6)");
        selectArticleManuallyButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        selectArticleManuallyButton.setOnAction(evt -> selectArticleManually());
        UIUtil.assignShortcut(selectArticleManuallyButton, new KeyCodeCombination(F6));

        Button stornoButton = new Button("Sztornó (F7)");
        stornoButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        stornoButton.setOnAction(evt -> stornoByBarcode());
        UIUtil.assignShortcut(stornoButton, new KeyCodeCombination(F7));

        Button payByCashButton = new Button("Fizetés készpénzzel (F8)");
        payByCashButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        payByCashButton.setOnAction(evt -> payByCash());
        UIUtil.assignShortcut(payByCashButton, new KeyCodeCombination(F8));

        Button payByCardButton = new Button("Fizetés kártyával (F9)");
        payByCardButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        payByCardButton.setOnAction(evt -> payByCard());
        UIUtil.assignShortcut(payByCardButton, new KeyCodeCombination(F9));

        return new FlowPane(staffBillButton, selectArticleManuallyButton, stornoButton, payByCashButton, payByCardButton) {
            {
                setHgap(10);
                setVgap(10);
            }

            @Override
            protected double computePrefWidth(double forHeight) {
                if (forHeight != -1)
                    return super.computePrefWidth(forHeight);

                return getInsets().getLeft() + getInsets().getRight() +
                        snapSpaceX(this.getHgap()) * (getChildren().size() - 1) +
                        getChildren().stream().mapToDouble(c -> snapSizeX(c.prefWidth(-1))).sum();
            }
        };
    }

    private TableView<Article> articlesTable() {
        return new UIUtil.TableBuilder<>(main.dataRoot.articles).
                col("Árucikk", 100, UNLIMITED_WIDTH, a -> a.name).
                col("Ár", 100, 100, a -> Integer.toString(a.sellingPrice)).
                col("Mennyiség", 150, 150, a -> Integer.toString(a.stockQuantity)).
                build();
    }

    @Override
    public void onEvent(AdminOperation op) {

    }

    private void toStaffBill() {
        staffBillButton.setSelected(true);

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Személyzeti számlák");

        TableView<User> usersTable = new UIUtil.TableBuilder<>(main.dataRoot.users).
                col("Felhasználók", 0, UNLIMITED_WIDTH, a -> a.name).
                build();

        SearchableTable<User> articleSearchableTable = new SearchableTable<>(usersTable, u -> List.of(u.name));
        articleSearchableTable.textField.setFocusTraversable(true);

        Platform.runLater(articleSearchableTable.textField::requestFocus);
        dialog.getDialogPane().setContent(articleSearchableTable.build());
        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(800);
        dialog.setResizable(true);

        ButtonType addButtonType = new ButtonType("Kiválasztás", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                addButtonType,
                new ButtonType("Mégsem", ButtonBar.ButtonData.CANCEL_CLOSE)
        );
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        dialog.setResultConverter(b -> b == addButtonType ? usersTable.getSelectionModel().getSelectedItem() : null);

        staffBill = dialog.showAndWait().orElse(null);
        staffBillButton.setSelected(staffBill != null);
    }

    private void selectArticleManually() {
        Dialog<Article> dialog = new Dialog();
        dialog.setTitle("Termékek");

        TableView<Article> articlesTable = articlesTable();
        SearchableTable<Article> articleSearchableTable = new SearchableTable<>(articlesTable, a -> List.of(a.name, a.barCode));
        articleSearchableTable.textField.setFocusTraversable(true);

        Platform.runLater(articleSearchableTable.textField::requestFocus);
        dialog.getDialogPane().setContent(articleSearchableTable.build());
        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(800);
        dialog.setResizable(true);

        ButtonType addButtonType = new ButtonType("Kiválasztás", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                addButtonType,
                new ButtonType("Mégsem", ButtonBar.ButtonData.CANCEL_CLOSE)
        );
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        dialog.setResultConverter(b -> b == addButtonType ? articlesTable.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(this::addArticle);
    }


    private void addArticle(Article a) {
        final Sale s = new Sale();
        s.pricePerProduct = a.sellingPrice;
        s.quantity = 1;
        s.article = a;
        s.seller = main.logonUser.name;
        s.timestamp = Instant.now();
        s.billID = staffBill == null ? new Sale.PeriodBillID(sellingPeriod.id) : new Sale.StaffBillID(staffBill.name);
        sellingPeriod.sales.add(s);
        salesTable.getItems().add(s);
        main.salesIO.sale(s);

        sumPrice += s.quantity * s.article.sellingPrice;

        sumPriceLabel.setText("Összesen: " + sumPrice + " Ft");
        sumPriceLabel.setVisible(true);
    }

    private void payByCash() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fizetés");
        dialog.setHeaderText("Fizetés készpénzzel");
        dialog.setContentText("Kapott készpénz: ");
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        dialog.showAndWait();
    }

    private void payByCard() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fizetés");
        dialog.setHeaderText("Kártyás fizetés");
        dialog.setContentText("Készpénzben kapott részösszeg: ");
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        dialog.showAndWait();
    }

    private void stornoByBarcode() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Sztornó");
        dialog.setHeaderText("Olvasd be a törlendő termék vonalkódját");
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        dialog.showAndWait();
    }

    public boolean close() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Zárás");
        d.setHeaderText(sellingPeriod.remainingCash() + " Ft maradt elvileg a kasszában. ");
        d.setContentText("Kasszában hagyott váltópénz: ");
        d.getDialogPane().getButtonTypes().remove(ButtonType.CANCEL);
        d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                createBooleanBinding(() -> !d.getEditor().getText().matches("[0-9]+"), d.getEditor().textProperty()));

        final Optional<String> o = d.showAndWait();
        o.ifPresent(s -> {
            sellingPeriod.closeCash = Integer.parseInt(s);
            closePeriod(main, sellingPeriod);
        });
        return o.isPresent();
    }

    public static void closePeriod(Main main, SellingPeriod sellingPeriod) {
        sellingPeriod.endTime = Instant.now();
        main.salesIO.endPeriod(sellingPeriod);

        Map<String, Integer> purchasedProducts = new HashMap<>();
        for (Sale sale : sellingPeriod.sales) {
            purchasedProducts.put(sale.article.name, purchasedProducts.getOrDefault(sale.article.name, 0) + sale.quantity);
        }

        Map<String, Integer> staffBillGrowths = new HashMap<>();
        for (Sale sale : sellingPeriod.sales) {
            if (sale.billID instanceof Sale.StaffBillID) {
                String n = ((Sale.StaffBillID) sale.billID).username;
                staffBillGrowths.put(n, purchasedProducts.getOrDefault(n, 0) + sale.quantity * sale.pricePerProduct);
            }
        }

        main.executeOperation(new ClosePeriodOp(purchasedProducts, staffBillGrowths));
    }
}
