package arunyilvantarto.ui;

import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.Item;
import arunyilvantarto.operations.*;
import arunyilvantarto.ui.UIUtil.LocalDateStringConverter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static javafx.beans.binding.Bindings.createBooleanBinding;

public class ArticleView {

    private final ArticlesTab articlesTab;
    final Article article;

    private TableView<Item> itemTable;
    private Button priceButton;
    private Button barcodeButton;
    private Button quantityButton;

    public ArticleView(ArticlesTab articlesTab, Article article) {
        this.articlesTab = articlesTab;
        this.article = article;
    }

    public Node build() {
        Button newProductButton = new Button("Új beszerzés");
        newProductButton.setOnAction(evt -> newItem());

        TitledPane titledPane = new TitledPane(article.name,
                new MigPane("fill", "[grow 2][grow 1]", "[] [] [grow]").
                        add(articlePropertiesForm()).
                        add(articleStatistics(), "spany, grow, wrap").
                        add(newProductButton, "grow, wrap").
                        add(itemTable(), "grow"));
        titledPane.setCollapsible(false);
        return titledPane;
    }

    public void onEvent(AdminOperation op) {
        if (op instanceof AddItemOp) {
            AddItemOp a = (AddItemOp) op;
            if (a.articleID.equals(article.name)) {
                itemTable.getItems().add(a.product);
                quantityButton.setText(Integer.toString(article.stockQuantity));
            }
        } else if (op instanceof DeleteItemOp) {
            DeleteItemOp a = (DeleteItemOp) op;
            if (a.articleName.equals(article.name)) {
                itemTable.getItems().remove(a.item);
                quantityButton.setText(Integer.toString(article.stockQuantity));
            }
        } else if (op instanceof ChangeArticleOp) {
            ChangeArticleOp c = (ChangeArticleOp) op;
            if (!c.articleID.equals(article.name))
                return;

            switch (c.property) {
                case BARCODE:
                    barcodeButton.setText(c.newValue == null ? "Beállítás" : (String) c.newValue);
                    break;
                case PRICE:
                    priceButton.setText(c.newValue.toString());
                    break;
                case QUANTITY:
                    quantityButton.setText(c.newValue.toString());
                    break;
                default:
                    throw new UnsupportedOperationException(c.property.toString());
            }
        } else if (op instanceof ClosePeriodOp) {
            quantityButton.setText(Integer.toString(article.stockQuantity));
        }
    }

    private TableView<Item> itemTable() {
        itemTable = new TableView<>();
        itemTable.getItems().addAll(article.items);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Item, String> dateColumn = new TableColumn<>("Dátum");
        dateColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                ISO_LOCAL_DATE.format(c.getValue().timestamp.atZone(ZoneId.systemDefault()))));
        dateColumn.setMinWidth(100);
        itemTable.getColumns().add(dateColumn);

        TableColumn<Item, Integer> stockQuantityColumn = new TableColumn<>("Mennyiség");
        stockQuantityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().purchaseQuantity));
        stockQuantityColumn.setMinWidth(100);
        itemTable.getColumns().add(stockQuantityColumn);

        TableColumn<Item, Integer> purchasePriceColumn = new TableColumn<>("Ár");
        purchasePriceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().purchasePrice));
        purchasePriceColumn.setMinWidth(100);
        itemTable.getColumns().add(purchasePriceColumn);


        MenuItem deleteMenuItem = new MenuItem("Törlés");
        deleteMenuItem.setOnAction(evt -> {
            Item item = itemTable.getSelectionModel().getSelectedItem();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Termékpéldány törlése");
            alert.setContentText("Biztos törlöd?");

            ButtonType deleteButtonType = new ButtonType("Törlés", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Mégsem", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getDialogPane().getButtonTypes().setAll(deleteButtonType, cancelButtonType);

            if (alert.showAndWait().orElse(null) == deleteButtonType) {
                articlesTab.main.executeOperation(new DeleteItemOp(article.name, item));
            }
        });
        deleteMenuItem.disableProperty().bind(createBooleanBinding(() ->
                itemTable.getSelectionModel().getSelectedItem() == null, itemTable.getSelectionModel().selectedItemProperty()));
        itemTable.setContextMenu(new ContextMenu(deleteMenuItem));

        return itemTable;
    }

    private Node articlePropertiesForm() {
        priceButton = new Button(Integer.toString(article.sellingPrice));
        barcodeButton = new Button(article.barCode == null ? "Beállítás" : article.barCode);
        quantityButton = new Button(Integer.toString(article.stockQuantity));

        priceButton.setOnAction(evt -> {
            TextInputDialog d = new TextInputDialog(Integer.toString(article.sellingPrice));
            d.setTitle("Termék ára");
            d.setContentText("Ár: ");
            d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(
                    () -> !d.getEditor().getText().matches("[0-9]+"), d.getEditor().textProperty()
            ));
            d.showAndWait().ifPresent(s -> {
                articlesTab.main.executeOperation(new ChangeArticleOp(article.name,
                        ChangeArticleOp.ArticleProperty.PRICE, article.sellingPrice, Integer.parseInt(s)));
            });
        });
        barcodeButton.setOnAction(evt -> {
            TextInputDialog d = new TextInputDialog(article.barCode);
            d.setTitle("Vonalkód beállítása");
            d.setContentText("Vonalkód: ");
            d.getEditor().setText("");
            d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(
                    () -> !UIUtil.isBarcode(d.getEditor().getText()) 
                          && !d.getEditor().getText().isEmpty(), d.getEditor().textProperty()
            ));
            UIUtil.barcodeField(d.getEditor(), null);
            d.showAndWait().ifPresent(s -> {
                articlesTab.main.executeOperation(new ChangeArticleOp(article.name,
                        ChangeArticleOp.ArticleProperty.BARCODE, article.barCode, s.isEmpty() ? null : s));
            });
        });
        quantityButton.setOnAction(evt -> {
            TextInputDialog d = new TextInputDialog(Integer.toString(article.stockQuantity));
            d.setTitle("Termék mennyisége");
            d.setContentText("Darabszám: ");
            d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(
                    () -> !d.getEditor().getText().matches("[0-9]+"), d.getEditor().textProperty()
            ));
            d.showAndWait().ifPresent(s -> {
                articlesTab.main.executeOperation(new ChangeArticleOp(article.name,
                        ChangeArticleOp.ArticleProperty.QUANTITY, article.stockQuantity, Integer.parseInt(s)));
            });
        });

        return new MigPane().
                add(new Label("Név: ")).
                add(new Label(article.name), "grow, wrap").
                add(new Label("Mennyiség: ")).
                add(quantityButton, "grow, wrap").
                add(new Label("Eladási ár: ")).
                add(priceButton, "grow, wrap").
                add(new Label("Vonalkód: ")).
                add(barcodeButton, "grow, wrap");
    }

    private Node articleStatistics() {
        if (true)
            return new Label();

        DatePicker fromDatePicker = new DatePicker(LocalDate.now().minusDays(10));
        fromDatePicker.setConverter(new LocalDateStringConverter());
        DatePicker toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setConverter(new LocalDateStringConverter());

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Nap");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Eladott termékek");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (LocalDate d = fromDatePicker.getValue(); !d.isAfter(toDatePicker.getValue()); d = d.plusDays(1))
            series.getData().add(new XYChart.Data<>(d.toString(), (int) (Math.random() * 100)));
        chart.getData().add(series);

        chart.setLegendVisible(false);

        MigPane datePickers = new MigPane().
                add(new Label("Mettől: ")).
                add(fromDatePicker, "grow, wrap").
                add(new Label("Meddig: ")).
                add(toDatePicker, "grow, wrap");

        return new MigPane("fill", null, "[] [grow]").
                add(datePickers, "wrap, align center center").
                add(chart, "span, grow");
    }

    private void newItem() {
        Dialog<Item> dialog = new Dialog<>();
        dialog.setTitle("Új beszerzés");
        dialog.setHeaderText("Beszerzés hozzáadása");

        DialogPane d = dialog.getDialogPane();
        ButtonType addButtonType = new ButtonType("Hozzáadás", ButtonBar.ButtonData.OK_DONE);

        d.getButtonTypes().addAll(
                addButtonType,
                new ButtonType("Mégsem", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        TextField quantityField = new TextField();
        TextField priceField = new TextField();
        TextField expirationField = new TextField();
        expirationField.setPromptText("éééé-hh-nn");

        Platform.runLater(quantityField::requestFocus);

        d.setContent(new MigPane().
                add(new Label("Mennyiség: ")).
                add(quantityField, "wrap").
                add(new Label("Beszerzési ár: ")).
                add(priceField, "wrap").
                add(new Label("Lejárat: ")).
                add(expirationField, "wrap"));
        d.getStylesheets().add("/arunyilvantarto/app.css");

        d.lookupButton(addButtonType).disableProperty().bind(createBooleanBinding(() ->
                        UIUtil.isNotInt(quantityField.getText()) || UIUtil.isNotInt(priceField.getText()) ||
                                (!UIUtil.isLocalDate(expirationField.getText()) && !expirationField.getText().isEmpty()),
                quantityField.textProperty(), priceField.textProperty(), expirationField.textProperty()));

        dialog.setResultConverter(b -> {
            if (b == addButtonType) {
                Item p = new Item();
                p.id = UUID.randomUUID();
                p.article = article;
                p.timestamp = Instant.now();
                p.purchasePrice = Integer.parseInt(priceField.getText());
                p.purchaseQuantity = Integer.parseInt(quantityField.getText());

                if (!expirationField.getText().isEmpty())
                    p.expiration = LocalDate.parse(expirationField.getText());

                return p;
            } else
                return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            AddItemOp op = new AddItemOp();
            op.articleID = article.name;
            op.product = p;
            articlesTab.main.executeOperation(op);
        });
    }

}
