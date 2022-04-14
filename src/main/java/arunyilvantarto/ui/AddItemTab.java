package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.events.InventoryEvent;
import arunyilvantarto.domain.Item;
import arunyilvantarto.events.AddItemOp;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

public class AddItemTab {

    private final Main app;
    private TableView<Item> productsTable;

    public AddItemTab(Main app) {
        this.app = app;
    }

    public Node build() {
        return new MigPane("fill", "[grow 3] [grow 1]").
                add(form(), "align center center").
                add(lastProductsTable(), "grow");
    }

    public void onEvent(InventoryEvent op) {
        if (op instanceof AddItemOp)
            productsTable.getItems().add(0, ((AddItemOp) op).product);
    }

    private Node form() {
        TextField articleField = new TextField();
        TextField quantityField = new TextField();
        TextField priceField = new TextField();
        TextField expirationField = new TextField();

        articleField.setOnAction(evt -> {
            String barcode = articleField.getText();

            app.dataRoot.articles.stream().filter(a -> a.barCode.equals(barcode)).findAny().ifPresent(a -> {
                articleField.setText(a.name);
                quantityField.requestFocus();
            });

            evt.consume();
        });

        Button addProductButton = new Button("Hozzáadás");
        addProductButton.setOnAction(evt -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.article = app.dataRoot.articles.stream().filter(a -> a.name.equals(articleField.getText()) ||
                    a.barCode.equals(articleField.getText())).findAny().orElse(null);
            if (item.article == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Nincs ilyen termék");
                alert.setContentText("Nincs olyan termék bejegyezve, melynek neve vagy vonalkódja \"" + articleField.getText() + "\" lenne");
                alert.showAndWait();
                return;
            }
            item.timestamp = Instant.now();
            item.expiration = expirationField.getText().isBlank() ? null : LocalDate.parse(expirationField.getText());
            item.purchaseQuantity = Integer.parseInt(quantityField.getText());
            item.purchasePrice = Integer.parseInt(priceField.getText());

            AddItemOp addProductOp = new AddItemOp();
            addProductOp.articleID = item.article.name;
            addProductOp.product = item;
            app.onEvent(addProductOp);

            articleField.setText("");
            quantityField.setText("");
            expirationField.setText("");
            priceField.setText("");
        });
        addProductButton.setDefaultButton(true);

        addProductButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> articleField.getText().isEmpty() || UIUtil.isNotInt(quantityField.getText()) ||
                        UIUtil.isNotInt(priceField.getText()) || !expirationField.getText().isEmpty() && !UIUtil.isLocalDate(expirationField.getText()),
                articleField.textProperty(), quantityField.textProperty(), priceField.textProperty(), expirationField.textProperty()
        ));

        return new MigPane("align center center", null, "[] [] [] [] unrelated []").
                add(new Label("Árucikk neve vagy vonalkódja: ")).
                add(articleField, "wrap").
                add(new Label("Mennyiség: ")).
                add(quantityField, "wrap").
                add(new Label("Beszerzési ár: ")).
                add(priceField, "wrap").
                add(new Label("Lejárat: ")).
                add(expirationField, "wrap").
                add(addProductButton, "grow, span");
    }

    private TableView<Item> lastProductsTable() {
        productsTable = new TableView<>();
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Item, String> articleColumn = new TableColumn<>("Árucikk");
        articleColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().article.name));
        articleColumn.setMinWidth(280);
        productsTable.getColumns().add(articleColumn);

        TableColumn<Item, Integer> quantityColumn = new TableColumn<>("Mennyiség");
        quantityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().purchaseQuantity));
        quantityColumn.setMinWidth(150);
        quantityColumn.setMaxWidth(200);
        productsTable.getColumns().add(quantityColumn);

        TableColumn<Item, Integer> priceColumn = new TableColumn<>("Ár");
        priceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().purchasePrice));
        priceColumn.setMinWidth(100);
        priceColumn.setMaxWidth(150);
        productsTable.getColumns().add(priceColumn);

        productsTable.getItems().addAll(
                app.dataRoot.articles.stream().flatMap(a -> a.items.stream()).
                        sorted(Comparator.<Item, Instant>comparing(p -> p.timestamp).reversed()).
                        collect(Collectors.toList()));

        return productsTable;
    }
}
