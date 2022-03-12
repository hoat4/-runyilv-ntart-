package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.OperationListener;
import arunyilvantarto.SalesIO;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.domain.*;
import arunyilvantarto.operations.AdminOperation;
import arunyilvantarto.operations.ClosePeriodOp;
import arunyilvantarto.operations.SendMessageOp;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static arunyilvantarto.ui.UIUtil.TableBuilder.UNLIMITED_WIDTH;
import static javafx.beans.binding.Bindings.*;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.layout.Region.USE_PREF_SIZE;

public class SellingTab implements OperationListener {

    private final Main main;

    private TableView<Sale> itemsTable;

    private int sumPrice;
    private Label sumPriceLabel;
    private SellingPeriod sellingPeriod;

    private Node mainPane;
    private Node tickIconPane;

    private int paymentIDCounter;

    @SuppressWarnings("ConstantConditions")
    private final Image tickIcon = new Image(SellingTab.class.getResource("/arunyilvantarto/tickIcon.png").toString());

    private SellingTab(Main main) {
        this.main = main;
    }

    public static void begin(Main app, Runnable preloadDoneCallback, Runnable cancelCallback) {
        SellingTab sellingTab = new SellingTab(app);
        Scene scene = app.preload(sellingTab.build());

        Platform.runLater(() -> {
            if (preloadDoneCallback != null)
                preloadDoneCallback.run();

            SellingPeriodAndCash cashState = lastSellingPeriod(app.salesIO);
            SellingPeriod lastSellingPeriod = cashState.lastSellingPeriod;
            if (lastSellingPeriod != null && lastSellingPeriod.endTime == null)
                throw new IllegalStateException("nem volt lezárva");

            MoneyConfirmationResult confirmationResult = confirmMoneyInCash(sellingTab.main, cashState.cash);
            if (!confirmationResult.canContinue) {
                if (cancelCallback != null)
                    cancelCallback.run();
                return;
            }

            SellingPeriod sellingPeriod = new SellingPeriod();
            sellingPeriod.id = lastSellingPeriod == null ? 1 : lastSellingPeriod.id + 1;
            sellingPeriod.username = app.logonUser.name;
            sellingPeriod.beginTime = Instant.now();
            sellingPeriod.openCash = cashState.cash;
            sellingPeriod.sales = new ArrayList<>();
            sellingPeriod.openCreditCardAmount = cashState.creditCardAmount;
            app.currentSellingPeriod = sellingPeriod;

            if (confirmationResult.message != null) {
                confirmationResult.message.subject = new Message.OpenPeriodSubject(sellingPeriod.id);
                app.executeOperation(new SendMessageOp(confirmationResult.message));
            }

            app.salesIO.beginPeriod(sellingPeriod, confirmationResult.message == null ? null : confirmationResult.message.text);

            sellingTab.sellingPeriod = sellingPeriod;
            sellingTab.paymentIDCounter = cashState.lastPaymentID + 1;
            app.switchPage(scene, sellingTab);
        });
    }

    public static SellingPeriodAndCash lastSellingPeriod(SalesIO salesIO) {
        SellingPeriod[] p = new SellingPeriod[1];
        int[] c = new int[3];
        salesIO.read(new SalesVisitor() {

            @Override
            public void beginPeriod(SellingPeriod period, String comment) {
                p[0] = period;
            }

            @Override
            public void endPeriod(SellingPeriod period, String comment) {
                c[0] = period.closeCash;
                c[1] = period.closeCreditCardAmount;
            }

            @Override
            public void modifyCash(String username, int cash, int creditCardAmount) {
                c[0] = cash;
                c[1] = creditCardAmount;
            }

            @Override
            public void sale(Sale sale) {
                c[2] = Math.max(sale.paymentID, c[2]);
            }
        });
        return new SellingPeriodAndCash(p[0], c[0], c[1], c[2]);
    }

    public static class SellingPeriodAndCash {
        public final SellingPeriod lastSellingPeriod;
        public final int cash, creditCardAmount;
        public final int lastPaymentID;

        public SellingPeriodAndCash(SellingPeriod lastSellingPeriod, int cash, int creditCardAmount, int lastPaymentID) {
            this.lastSellingPeriod = lastSellingPeriod;
            this.cash = cash;
            this.creditCardAmount = creditCardAmount;
            this.lastPaymentID = lastPaymentID;
        }
    }

    public Node build() {
        sumPriceLabel = new Label();
        sumPriceLabel.setVisible(false);
        sumPriceLabel.setPadding(new Insets(10, 0, 0, 0)); // TODO gaptop miért marad ott, ha hidemode 2?
        sumPriceLabel.getStyleClass().add("sumPriceLabel");

        mainPane = new MigPane("align center center, wrap 1", "[70%:]", "unrelated [] related [grow] 0 [] 15 [] 18").
                add(topToolbar(), "grow").
                add(salesTable(), "grow").
                add(sumPriceLabel, "hidemode 2, align right").
                add(bottomToolbar(), "grow");

        ImageView imageView = new ImageView(tickIcon);
        imageView.setScaleX(.2);
        imageView.setScaleY(.2);
        tickIconPane = new MigPane("align center center").add(imageView);
        tickIconPane.setOpacity(0);
        tickIconPane.setMouseTransparent(true);

        StackPane sp = new StackPane(mainPane, tickIconPane);
        sp.getStyleClass().add("selling-tab");
        return sp;
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
        UIUtil.barcodeField(barcodeField, text -> {
            text = text.replaceAll("[^0-9*\\-]", "");

            if (text.length() > 20) {
                barcodeField.setText("");
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Érvénytelen vonalkód");
                alert.setHeaderText("A megadott vonalkód túl hosszú");
                alert.setContentText("Valószínűleg érvénytelen vonalkód is be lett olvasva. ");
                alert.showAndWait();
                return;
            }
            barcodeField.setText(text);

            int quantity;
            String barcode;

            if (text.contains("*")) {
                quantity = Integer.parseInt(text.substring(0, text.indexOf('*')));
                barcode = text.substring(text.indexOf('*') + 1);
                if (barcode.contains("*")) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Érvénytelen vonalkód");
                    alert.setContentText("A megadott vonalkódban egynél több csillag is szerepel. ");
                    alert.showAndWait();
                    Platform.runLater(() -> barcodeField.setText(""));
                    return;
                }
            } else {
                quantity = 1;
                barcode = text;
            }

            // if (barcode.length() > 5 && main.dataRoot.articles.stream().noneMatch(a->a.barCode != null && a.barCode.startsWith(barcode))) {
            //     Alert alert = new Alert(Alert.AlertType.WARNING);
            //     alert.setTitle("Érvénytelen vonalkód");
            //     alert.setHeaderText("Nincs a megadott vonalkódhoz tartozó termék");
            //     alert.showAndWait();
            //     Platform.runLater(() -> barcodeField.setText(""));
            //     return;
            // }

            main.dataRoot.articles.stream().filter(a -> Objects.equals(a.barCode, barcode)).findAny().ifPresent(a -> {
                addArticle(a, quantity);
                Platform.runLater(() -> barcodeField.setText(""));
            });
        });
        barcodeField.setOnKeyPressed(e -> {
            if (e.getCode() == SPACE) {
                e.consume();

                if (!barcodeField.getText().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Fizetés");
                    alert.setHeaderText("Vonalkód mező nem üres");
                    alert.setContentText("Vonalkód mező nem üres, addig nem lehet fizetni");
                    alert.showAndWait();
                    return;
                }

                if (itemsTable.getItems().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Fizetés");
                    alert.setHeaderText("Nincsenek termékek kiválasztva");
                    alert.setContentText("Nincsenek termékek kiválasztva");
                    alert.showAndWait();
                    return;
                }

                pay();
            }
        });
        barcodeField.focusedProperty().addListener((o, old, value) -> {
            if (!value)
                barcodeField.requestFocus();
        });


        return new MigPane("align center center", "[] push [] push []").
                add(new Label("Pénztáros: " + main.logonUser.name)).
                add(barcodeField).
                add(logoutButton);
    }

    private TableView<Sale> salesTable() {
        return itemsTable = new UIUtil.TableBuilder<Sale>(new ArrayList<>()).
                col("Termék", 0, UNLIMITED_WIDTH, sale -> sale.article == null ? "" : sale.article.name).
                col("Ár", 150, 150, sale -> sale.pricePerProduct).
                col("Mennyiség", 150, 150, sale -> sale.quantity).
                col("Összesen", 150, 150, sale -> sale.pricePerProduct * sale.quantity).
                placeholder("Olvasd be a termékek vonalkódját").
                build();
    }

    private Node bottomToolbar() {
        Button selectArticleManuallyButton = new Button("Termék kiválasztása (F6)");
        selectArticleManuallyButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        selectArticleManuallyButton.setOnAction(evt -> selectArticleManually());
        UIUtil.assignShortcut(selectArticleManuallyButton, new KeyCodeCombination(F6));

        Button stornoButton = new Button("Sztornó (F7)");
        stornoButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        stornoButton.setOnAction(evt -> stornoByBarcode());
        UIUtil.assignShortcut(stornoButton, new KeyCodeCombination(F7));

        Button payFromStaffBillButton = new Button("Személyzeti számlára (F8)");
        payFromStaffBillButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        payFromStaffBillButton.setOnAction(evt -> payToStaffBill());
        UIUtil.assignShortcut(payFromStaffBillButton, new KeyCodeCombination(F8));

        Button returnButton = new Button("Visszáru (F9)");
        returnButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        returnButton.setOnAction(evt -> returnByBarcode());
        UIUtil.assignShortcut(returnButton, new KeyCodeCombination(F9));

        Button payButton = new Button("Fizetés (szóköz)");
        payButton.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        payButton.setOnAction(evt -> pay());
        UIUtil.assignShortcut(payButton, new KeyCodeCombination(SPACE));

        ObservableValue<Boolean> hasNoItems = isEmpty(itemsTable.getItems());
        stornoButton.disableProperty().bind(hasNoItems);
        payFromStaffBillButton.disableProperty().bind(hasNoItems);
        payButton.disableProperty().bind(hasNoItems);

        return new FlowPane(selectArticleManuallyButton, stornoButton, payFromStaffBillButton, returnButton, payButton) {
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

    private void selectArticleManually() {
        Dialog<Article> dialog = new Dialog();
        dialog.setTitle("Termékek");

        TableView<Article> articlesTable = articlesTable();
        SearchableTable<Article> articleSearchableTable = new SearchableTable<>(articlesTable,
                a -> a.barCode == null ? List.of(a.name) : List.of(a.name, a.barCode));
        articleSearchableTable.textField.setFocusTraversable(true);

        TextField quantityField = new TextField("1");

        Platform.runLater(articleSearchableTable.textField::requestFocus);
        dialog.getDialogPane().setContent(new MigPane("fill", null, "[] unrelated [grow]").
                add(new Label("Mennyiség: ")).
                add(quantityField, "grow, wrap").
                add(articleSearchableTable.build(), "grow, gapleft 0, gapright 0, span 2"));
        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(800);
        dialog.setResizable(true);

        ButtonType addButtonType = new ButtonType("Kiválasztás", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                addButtonType,
                new ButtonType("Mégsem", ButtonBar.ButtonData.CANCEL_CLOSE)
        );
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        dialog.getDialogPane().lookupButton(addButtonType).disableProperty().bind(createBooleanBinding(() ->
                        !quantityField.getText().matches("-?[0-9]+") || articlesTable.getSelectionModel().getSelectedItem() == null,
                quantityField.textProperty(), articlesTable.getSelectionModel().selectedItemProperty()));
        dialog.setResultConverter(b -> b == addButtonType ? articlesTable.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(a -> addArticle(a, Integer.parseInt(quantityField.getText())));
    }

    private void addArticle(Article a, int quantity) {
        final Sale s = new Sale();
        s.pricePerProduct = a.sellingPrice;
        s.quantity = quantity;
        s.article = a;
        s.seller = main.logonUser.name;
        s.timestamp = Instant.now();
        s.paymentID = paymentIDCounter;
        itemsTable.getItems().add(s);

        sumPrice += s.quantity * s.article.sellingPrice;
        updateSumPrice();
    }

    private void updateSumPrice() {
        sumPriceLabel.setText("Összesen: " + sumPrice + " Ft");
        sumPriceLabel.setVisible(sumPrice != 0);
    }

    private void pay() {
        int roundedPrice = sumPrice < 0 ? (sumPrice - 2) / 5 * 5 : (sumPrice + 2) / 5 * 5;
        if (roundedPrice == 0) {
            periodPayDone();
            return;
        }

        if (roundedPrice < 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Fizetés");
            alert.setHeaderText("Visszaáru kifizetése");
            alert.setContentText("Vásárlónak járó összeg: " + -roundedPrice + " Ft");
            alert.getButtonTypes().add(ButtonType.CANCEL);
            alert.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == ButtonType.OK)
                    periodPayDone();
            });
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fizetés");
        dialog.setHeaderText("Fizetendő összeg: " + roundedPrice + " Ft");
        dialog.setContentText("Kapott készpénz: ");
        dialog.getEditor().setText(Integer.toString(roundedPrice));
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        dialog.showAndWait().ifPresent(s -> {
            int receivedCash = Integer.parseInt(s);
            if (receivedCash < roundedPrice) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hibás összeg");
                alert.setHeaderText("Alulfizetés " + (roundedPrice - receivedCash) + " forinttal");
                alert.setContentText("A kapott összeg (" + receivedCash + " Ft) " + " kevesebb, mint a vásárolt termékek árának összege. ");
                alert.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
                alert.showAndWait();
                pay();
                return;
            }

            if (receivedCash > roundedPrice) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Visszajáró");
                alert.setHeaderText("Visszajáró: " + (receivedCash - roundedPrice) + " Ft");
                alert.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
                alert.showAndWait();
            }

            periodPayDone();
        });

    }

    private void periodPayDone() {
        for (Sale sale : itemsTable.getItems())
            sale.billID = new Sale.PeriodBillID(sellingPeriod.id);

        paymentIDCounter++;
        payDone();
    }

    private void payToStaffBill() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Személyzeti számlák");

        List<User> users = main.dataRoot.users.stream().filter(User::canPurchaseWithStaffBill).collect(Collectors.toList());
        TableView<User> usersTable = new UIUtil.TableBuilder<>(users).
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

        dialog.showAndWait().ifPresent(staffBill -> {
            itemsTable.getItems().forEach(sale -> sale.billID = new Sale.StaffBillID(staffBill.name));
            payDone();
        });

    }

    private void payDone() {
        mainPane.setOpacity(0);
        tickIconPane.setOpacity(.8);

        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(.7), event -> {
            mainPane.setOpacity(1);
            tickIconPane.setOpacity(0);
        }));
        delay.play();

        sellingPeriod.sales.addAll(itemsTable.getItems());

        main.executor.execute(() -> {
            itemsTable.getItems().forEach(main.salesIO::sale);
            Platform.runLater(() -> {
                itemsTable.getItems().clear();
                sumPrice = 0;
                sumPriceLabel.setVisible(false);
            });
        });
    }

    private void stornoByBarcode() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Sztornó");
        dialog.setHeaderText("Olvasd be a törlendő termék vonalkódját");
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        UIUtil.barcodeField(dialog.getEditor(), barcode -> {
            main.dataRoot.articles.stream().filter(a -> Objects.equals(a.barCode, barcode)).findAny().ifPresent(article -> {
                dialog.close();

                List<Sale> sales = new ArrayList<>(itemsTable.getItems());
                Collections.reverse(sales);
                sales.stream().filter(s -> {
                    assert s.article != null;
                    return Objects.equals(s.article.barCode, barcode);
                }).findFirst().ifPresentOrElse(s -> {
                    itemsTable.getItems().remove(s);
                    sumPrice -= s.quantity * article.sellingPrice;
                    updateSumPrice();
                }, () -> Platform.runLater(() -> { // időnként JavaFX bug miatt a dialógus teljesen fehér volt e nélkül
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Sztornó sikertelen");
                    alert.setHeaderText("Ez a termék nem volt a megvásárolva");
                    alert.setContentText("Termék neve: " + article.name);
                    alert.showAndWait().ifPresent(b -> stornoByBarcode());
                }));
            });
        });
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(() ->
                !UIUtil.isBarcode(dialog.getEditor().getText()), dialog.getEditor().textProperty()));
        dialog.showAndWait().ifPresent(s -> {
            Platform.runLater(() -> { // időnként JavaFX bug miatt a dialógus teljesen fehér volt e nélkül
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Sztornó sikertelen");
                alert.setHeaderText("Ismeretlen vonalkód");
                alert.setContentText("Nem ismert olyan termék, melynek ez lenne a vonalkódja. ");
                alert.showAndWait();
                stornoByBarcode();
            });
        });
    }

    private void returnByBarcode() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Visszaáru");
        dialog.setHeaderText("Olvasd be a visszahozott termék vonalkódját");
        dialog.getDialogPane().getStylesheets().add("/arunyilvantarto/selling-dialog.css");
        UIUtil.barcodeField(dialog.getEditor(), barcode -> {
            main.dataRoot.articles.stream().filter(a -> Objects.equals(a.barCode, barcode)).findAny().ifPresent(article -> {
                dialog.close();
                addArticle(article, -1);
            });
        });
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(() ->
                !UIUtil.isBarcode(dialog.getEditor().getText()), dialog.getEditor().textProperty()));
        dialog.showAndWait().ifPresent(s -> {
            Platform.runLater(() -> { // időnként JavaFX bug miatt a dialógus teljesen fehér volt e nélkül
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Visszáru");
                alert.setHeaderText("Ismeretlen vonalkód");
                alert.setContentText("Nem ismert olyan termék, melynek ez lenne a vonalkódja. ");
                alert.showAndWait();
                stornoByBarcode();
            });
        });
    }

    public boolean close() {
        TextInputDialog d1 = new TextInputDialog();
        d1.setTitle("Zárás");
        d1.setContentText("Bankkártyás forgalom: ");
        d1.getDialogPane().getButtonTypes().remove(ButtonType.CANCEL);
        d1.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                createBooleanBinding(() -> !d1.getEditor().getText().matches("[0-9]+"), d1.getEditor().textProperty()));

        Optional<String> o = d1.showAndWait();
        if (o.isEmpty())
            return false;

        int creditCardRevenue = Integer.parseInt(o.get());
        sellingPeriod.closeCreditCardAmount = sellingPeriod.openCreditCardAmount + creditCardRevenue;

        int remainingCash = sellingPeriod.remainingCash(creditCardRevenue);
        MoneyConfirmationResult result = confirmMoneyInCash(main, remainingCash);
        if (!result.canContinue)
            return false;

        sellingPeriod.closeCash = remainingCash;
        if (result.message != null)
            result.message.subject = new Message.ClosePeriodSubject(sellingPeriod.id);
        closePeriod(main, sellingPeriod, result.message);
        return true;
    }

    public static MoneyConfirmationResult confirmMoneyInCash(Main main, int remainingCash) {
        while (true) {
            Alert d2 = new Alert(Alert.AlertType.CONFIRMATION);
            d2.setTitle("Zárás");
            d2.setHeaderText(remainingCash + " Ft maradt elvileg a kasszában. ");
            d2.setContentText("Rendben van a váltópénz?");
            d2.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            ButtonType pressedButton = d2.showAndWait().orElse(null);
            if (pressedButton == null || pressedButton == ButtonType.CANCEL)
                return new MoneyConfirmationResult(false, null);

            Message msg = null;
            if (pressedButton == ButtonType.NO) {
                String desc = "Írd le, hogy mit tapasztaltál a kasszában lévő váltópénzzel kapcsolatban. ";
                msg = MessagingUI.showSendMessageDialog(main, desc).orElse(null);
                if (msg == null)
                    continue;
            }

            return new MoneyConfirmationResult(true, msg);
        }
    }

    public static class MoneyConfirmationResult {

        public final boolean canContinue;
        public final Message message;

        public MoneyConfirmationResult(boolean canContinue, Message message) {
            this.canContinue = canContinue;
            this.message = message;
        }
    }

    public static void closePeriod(Main main, SellingPeriod sellingPeriod, Message closeComment) {
        sellingPeriod.endTime = closeComment != null ? closeComment.timestamp : Instant.now();
        main.salesIO.endPeriod(sellingPeriod, closeComment == null ? null : closeComment.text);

        Map<String, Integer> purchasedProducts = new HashMap<>();
        for (Sale sale : sellingPeriod.sales) {
            if (sale.article != null)
                purchasedProducts.put(sale.article.name, purchasedProducts.getOrDefault(sale.article.name, 0) + sale.quantity);
        }

        Map<String, Integer> staffBillGrowths = new HashMap<>();
        for (Sale sale : sellingPeriod.sales) {
            if (sale.billID instanceof Sale.StaffBillID) {
                String n = ((Sale.StaffBillID) sale.billID).username;
                staffBillGrowths.put(n, purchasedProducts.getOrDefault(n, 0) + sale.quantity * sale.pricePerProduct);
            }
        }

        main.executeOperation(new ClosePeriodOp(sellingPeriod, purchasedProducts, staffBillGrowths, closeComment));
    }
}
