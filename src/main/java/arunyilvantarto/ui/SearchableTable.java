package arunyilvantarto.ui;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.tbee.javafx.scene.layout.MigPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;


public class SearchableTable<T> {

    public final TextField textField = new TextField();
    public final TableView<T> table;

    public SearchableTable(TableView<T> table, Function<T, List<String>> namesFunction) {
        this.table = table;
        List<T> items = new ArrayList<>(table.getItems());
        textField.textProperty().addListener((o, old, text)->{
            List<T> l1 = items.stream().
                    filter(e->namesFunction.apply(e).stream().anyMatch(s->s.toLowerCase().startsWith(text.toLowerCase()))).
                    collect(Collectors.toList());
            l1.addAll(items.stream().
                    filter(e->!l1.contains(e)&&
                            namesFunction.apply(e).stream().anyMatch(s->s.toLowerCase().contains(text.toLowerCase()))).
                    collect(Collectors.toList()));
            table.setItems(FXCollections.observableList(l1));
            if (table.getSelectionModel().getSelectedItem() == null)
                table.getSelectionModel().select(0);
        });
    }

    public Node build() {
        return new MigPane("fill, insets 0", null, "[] related [grow]").
                add(textField, "grow, wrap").
                add(table, "grow, wrap");
    }
}
