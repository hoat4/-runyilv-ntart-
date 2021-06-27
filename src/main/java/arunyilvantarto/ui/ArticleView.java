package arunyilvantarto.ui;

import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.Product;
import arunyilvantarto.operations.AddProductOp;
import arunyilvantarto.operations.AdminOperation;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

public class ArticleView {

    private final ArticlesTab articlesTab;
    private final Article article;

    private TableView<Product> productsTable;

    public ArticleView(ArticlesTab articlesTab, Article article) {
        this.articlesTab = articlesTab;
        this.article = article;
    }

    public Node build()  {
        Button newProductButton = new Button("Új termék");
        newProductButton.setOnAction(evt -> newProduct());

        TitledPane titledPane = new TitledPane(article.name, new MigPane(null, "[grow 1][]").
                add(articlePropertiesForm()).
                add(articleStatistics(), "spany, grow, wrap").
                add(newProductButton, "grow, wrap").
                add(productsTable(), "grow"));
        titledPane.setCollapsible(false);
        return titledPane;
    }

    public void onEvent(AdminOperation op) {
        if (op instanceof AddProductOp) {
            AddProductOp a = (AddProductOp) op;
            if (a.articleID.equals(article.id))
                productsTable.getItems().add(a.product);
        }
    }

    private TableView<Product> productsTable() {
        productsTable = new TableView<>();
        productsTable.getItems().addAll(article.products);
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Product, String> dateColumn = new TableColumn<>("Dátum");
        dateColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                BASIC_ISO_DATE.format(c.getValue().timestamp.atZone(ZoneId.systemDefault()))));
        dateColumn.setMinWidth(100);
        productsTable.getColumns().add(dateColumn);

        TableColumn<Product, Integer> stockQuantityColumn = new TableColumn<>("Mennyiség");
        stockQuantityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().stockQuantity));
        stockQuantityColumn.setMinWidth(100);
        productsTable.getColumns().add(stockQuantityColumn);

        TableColumn<Product, Integer> purchasePriceColumn = new TableColumn<>("Ár");
        purchasePriceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().purchasePrice));
        purchasePriceColumn.setMinWidth(100);
        productsTable.getColumns().add(purchasePriceColumn);

        return productsTable;
    }

    private Node articlePropertiesForm() {
        TextField nameField = new TextField(article.name);
        TextField priceField = new TextField(Integer.toString(article.sellingPrice));
        TextField barcodeField = new TextField(article.barCode);

        return new MigPane().
                add(new Label("Név: ")).
                add(nameField, "grow, wrap").
                add(new Label("Eladási ár: ")).
                add(priceField, "grow, wrap").
                add(new Label("Vonalkód: ")).
                add(barcodeField, "grow, wrap");
    }

    private Node articleStatistics() {
        return new MigPane().
                add(new Label("Mettől: ")).
                add(new DatePicker(LocalDate.now().minusDays(30)), "wrap").
                add(new Label("Meddig: ")).
                add(new DatePicker(LocalDate.now()), "wrap");
    }

    private void newProduct() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Új termék");
        dialog.setHeaderText("Termék hozzáadása");

        DialogPane d = dialog.getDialogPane();
        ButtonType addButtonType = new ButtonType("Hozzáadás", ButtonBar.ButtonData.OK_DONE);

        d.getButtonTypes().addAll(
                addButtonType,
                new ButtonType("Mégsem", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        TextField quantityField = new TextField();
        TextField priceField = new TextField();
        Platform.runLater(quantityField::requestFocus);

        d.setContent(new MigPane().
                add(new Label("Mennyiség: ")).
                add(quantityField, "wrap").
                add(new Label("Beszerzési ár: ")).
                add(priceField));
        d.getStylesheets().add("/arunyilvantarto/app.css");

        d.lookupButton(addButtonType).disableProperty().bind(Bindings.createBooleanBinding(() ->
                        quantityField.getText().isEmpty() || priceField.getText().isEmpty(),
                quantityField.textProperty(), priceField.textProperty()));
        // TODO lejárat?
        dialog.setResultConverter(b -> {
            if (b == addButtonType) {
                Product p = new Product();
                p.id = UUID.randomUUID();
                p.timestamp = Instant.now();
                p.purchasePrice = Integer.parseInt(priceField.getText());
                p.purchaseQuantity = p.stockQuantity = Integer.parseInt(quantityField.getText());
                return p;
            }else
                return null;
        });

        dialog.showAndWait().ifPresent(p->{
            AddProductOp op = new AddProductOp();
            op.articleID = article.id;
            op.product = p;
            articlesTab.app.executeAdminOperation(op);
        });
    }

}
