package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.domain.Article;
import arunyilvantarto.events.InventoryEvent;
import arunyilvantarto.domain.Menu;
import arunyilvantarto.events.AddMenuOp;
import arunyilvantarto.events.ChangeMenuOp;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tbee.javafx.scene.layout.MigPane;

import static javafx.beans.binding.Bindings.add;
import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.scene.control.ButtonType.CANCEL;

public class MenusTab {

    private final Main main;
    private TableView<Menu> menuTable;

    private MenuView actualMenuView;
    private MigPane menuViewContainer = new MigPane("fill", "0[]0", "0[]0");

    public MenusTab(Main main) {
        this.main = main;
    }

    public Node build() {
        menuTable = new UIUtil.TableBuilder<>(main.dataRoot.menus).
                col("Név", 80, UIUtil.TableBuilder.UNLIMITED_WIDTH, menu -> menu.name).
                col("Ár", 80, 120, menu -> menu.price + " Ft").
                onSelected(menu -> {
                    menuViewContainer.getChildren().clear();
                    if (menu == null) {
                        actualMenuView = null;
                        return;
                    }
                    menuViewContainer.add((actualMenuView = new MenuView(menu)).build(), "grow");
                }).
                build();

        Button newMenuButton = new Button("Új menü hozzáadása");
        newMenuButton.setOnAction(evt -> newMenu());

        return new MigPane("fill", "[][grow]", "[][grow]").
                add(newMenuButton, "grow").
                add(menuViewContainer, "grow, spany 2, wrap").
                add(menuTable, "grow");
    }

    public void onEvent(InventoryEvent op) {
        if (op instanceof AddMenuOp)
            menuTable.getItems().add(((AddMenuOp) op).menu);
        if (op instanceof ChangeMenuOp) {
            menuTable.refresh();
            actualMenuView.refresh();
        }
    }

    private void newMenu() {
        Dialog<Menu> dialog = new Dialog<>();
        dialog.setTitle("Új menü");
        dialog.setHeaderText("Menü létrehozása");

        DialogPane d = dialog.getDialogPane();
        ButtonType addButtonType = new ButtonType("Létrehozás", ButtonBar.ButtonData.OK_DONE);

        d.getButtonTypes().addAll(addButtonType, CANCEL);

        TextField nameField = new TextField();
        TextField priceField = new TextField();

        Platform.runLater(nameField::requestFocus);

        d.setContent(new MigPane("", "", "[][]0").
                add(new Label("Név: ")).
                add(nameField, "wrap").
                add(new Label("Ár: ")).
                add(priceField, "wrap"));
        d.getStylesheets().add("/arunyilvantarto/app.css");

        d.lookupButton(addButtonType).disableProperty().bind(createBooleanBinding(() ->
                        UIUtil.isNotInt(priceField.getText()) || nameField.getText().isBlank()
                                || main.dataRoot.menus.stream().anyMatch(m -> m.name.equals(nameField.getText())),
                nameField.textProperty(), priceField.textProperty()));

        dialog.setResultConverter(b -> {
            if (b == addButtonType) {
                Menu m = new Menu();
                m.name = nameField.getText();
                m.price = Integer.parseInt(priceField.getText());
                return m;
            } else
                return null;
        });

        dialog.showAndWait().ifPresent(menu -> {
            main.onEvent(new AddMenuOp(menu));
        });
    }

    private class MenuView {

        private final Menu menu;
        private TitledPane titledPane;
        private Button priceButton;
        private TreeTableView<Object> treeTableView;

        public MenuView(Menu menu) {
            this.menu = menu;
        }

        public Node build() {
            priceButton = new Button();
            priceButton.setOnAction(evt -> {
                TextInputDialog d = new TextInputDialog(Integer.toString(menu.price));
                d.setTitle("Menü ára");
                d.setContentText("Ár: ");
                d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(createBooleanBinding(
                        () -> !d.getEditor().getText().matches("[0-9]+"), d.getEditor().textProperty()
                ));
                d.showAndWait().ifPresent(s -> {
                    changeMenu(() -> menu.price = Integer.parseInt(s));
                });
            });

            Button addSlotButton = new Button("Termékcsoport hozzáadása");
            addSlotButton.setOnAction(evt -> {
                changeMenu(() -> menu.slots.add(new Menu.Slot()));
            });

            treeTableView = new TreeTableView<>();
            TreeTableColumn<Object, Object> articleCol = new TreeTableColumn<>("Termék");
            articleCol.setCellValueFactory(obj -> {
                Object o = obj.getValue().getValue();
                if (o instanceof Menu.Slot)
                    return new ReadOnlyObjectWrapper<>((menu.slots.indexOf((Menu.Slot) o) + 1) + ". termékcsoport");
                else
                    return new ReadOnlyObjectWrapper<>(((Article)o).name);
            });
            treeTableView.getColumns().add(articleCol);

            treeTableView.setShowRoot(false);

            TreeItem<Object> root = new TreeItem<>();


            titledPane = new TitledPane();
            titledPane.setContent(new MigPane("fill", "[][]push[]", "[][grow]").
                    add(new Label("Ár: ")).
                    add(priceButton).
                    add(addSlotButton, "wrap").
                    add(treeTableView, "grow, spanx 3"));
            titledPane.setCollapsible(false);
            refresh();
            return titledPane;
        }

        void changeMenu(Runnable runnable) {
            Menu oldMenu = menu.clone();
            runnable.run();
            main.onEvent(new ChangeMenuOp(oldMenu, menu));
        }

        void refresh() {
            titledPane.setText(menu.name);
            priceButton.setText(menu.price + " Ft");
            treeTableView.refresh();
        }
    }
}
