package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.domain.Article;
import arunyilvantarto.operations.AdminOperation;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.tbee.javafx.scene.layout.MigPane;

public class ArticlesTab {

    public final Main app;
    private final MigPane articleViewContainer = new MigPane("fill", "0[]0", "0[]0");
    private ArticleView visibleArticleView;

    public ArticlesTab(Main app) {
        this.app = app;
    }

    public Node build() {
        Button newArticleButton = new Button("Új árucikk");
        newArticleButton.setOnAction(evt -> newArticle());

        TableView<Article> table = articlesTable();

        return new MigPane("fill", "[] [grow 1]", "[] [grow 1]").
                add(newArticleButton, "grow").
                add(articleViewContainer, "spany, grow, wrap").
                add(table, "grow");
    }

    public void onEvent(AdminOperation op) {
        if (visibleArticleView != null)
            visibleArticleView.onEvent(op);
    }

    @SuppressWarnings("unchecked")
    private TableView<Article> articlesTable() {
        TableView<Article> table = new TableView<>();
        table.setPlaceholder(new Label("Nincs termék bejegyezve"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Article, String> nameColumn = new TableColumn<>("Név");
        nameColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().name));

        TableColumn<Article, String> barcodeColumn = new TableColumn<>("Vonalkód");
        barcodeColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().barCode));

        TableColumn<Article, String> priceColumn = new TableColumn<>("Eladási ár");
        priceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().barCode));

        TableColumn<Article, Integer> quantityColumn = new TableColumn<>("Mennyiség");
        quantityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().products.stream().mapToInt(p -> p.stockQuantity).sum()));

        // TODO kéne szólni JavaFX-eseknek, hogy csináljanak végre normális column resize policy-t, egyrészt JDK-8089280,
        //      másrészt nem is lehet értelmes módon most column weight-ot megadni (asszem azt még Swingben is lehet)

        nameColumn.setMinWidth(200);
        barcodeColumn.setMinWidth(100);
        priceColumn.setMinWidth(100);
        quantityColumn.setMinWidth(100);
        table.getColumns().addAll(nameColumn, barcodeColumn, priceColumn, quantityColumn);

        table.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> showArticle(newValue));
        table.getItems().addAll(app.dataRoot.articles);

        return table;
    }

    private void showArticle(Article article) {
        articleViewContainer.getChildren().clear();
        articleViewContainer.add((visibleArticleView = new ArticleView(this, article)).build(), "grow");
    }

    private void newArticle() {

    }

}
