package arunyilvantarto.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static javafx.scene.paint.Color.color;

public class UIUtil {

    public static final DateTimeFormatter DATETIME_FORMAT =DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    public static boolean isLocalDate(String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isNotInt(String s) {
        try {
            Integer.parseInt(s);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static boolean isBarcode(String s) {
        return s.matches("[0-9]+");
    }

    public static String toDateString(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).format(UIUtil.DATETIME_FORMAT);
    }

    public static void barcodeField(TextField textField, Consumer<String> handler, Runnable spaceHandler) {
        textField.textProperty().addListener((o, old, value) -> {
            boolean pay = value.contains(" ") && spaceHandler != null; // accelerator-ként szóköz nem ment, textfield kapta el mindneképpen
            value = value.replace('ö', '0').replace(" ", "");
            textField.setText(value);
            if (pay) {
                spaceHandler.run();
                return;
            }
            if (handler != null)
                handler.accept(value);
        });
    }

    public static void assignShortcut(ButtonBase button, KeyCombination keyCombination) {
        Runnable r = button::fire;
        if (button.getScene() != null)
            button.getScene().getAccelerators().put(keyCombination, r);

        button.sceneProperty().addListener((o, old, scene) -> {
            if (old != null)
                old.getAccelerators().remove(keyCombination, r);

            if (scene != null)
                scene.getAccelerators().put(keyCombination, r);
        });
    }

    public static class TableBuilder<T> {
        public static final double UNLIMITED_WIDTH = 20000;
        private final List<T> initialData;
        private final List<TableColumn<T, ?>> columns = new ArrayList<>();
        private Consumer<T> onSelected;

        private Node placeholder;

        public TableBuilder(List<T> initialData) {
            this.initialData = initialData;
        }

        public TableBuilder<T> col(String caption, double minWidth, double maxWidth, Function<T, Object> function) {
            TableColumn<T, Object> col = new TableColumn<>(caption);
            col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(function.apply(c.getValue())));
            col.setMinWidth(minWidth);
            col.setMaxWidth(maxWidth);
            columns.add(col);
            return this;
        }

        public TableBuilder<T> customCol(String caption, double minWidth, double maxWidth, Function<T, Object> function) {
            TableColumn<T, Object> col = new TableColumn<>(caption);
            col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
            col.setCellFactory(c->new TableCell<>(){
                {
          //          setGraphic(function.apply(ge));
                }
            });
            col.setMinWidth(minWidth);
            col.setMaxWidth(maxWidth);
            columns.add(col);
            return this;
        }

        public TableBuilder<T> onSelected(Consumer<T> handler) {
            onSelected = handler;
            return this;
        }

        public TableBuilder<T> placeholder(String s) {
            Label lbl = new Label(s);
            lbl.setWrapText(true);
            lbl.setTextFill(color(.6, .6, .6));
            placeholder = lbl;
            return this;
        }

        public TableView<T> build() {
            TableView<T> table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setPlaceholder(placeholder);
            // TODO placeholder szöveg
            table.getColumns().addAll(columns);
            table.getItems().addAll(initialData);
            if (onSelected != null)
                table.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> onSelected.accept(value));
            return table;
        }

    }

    public static class LocalDateStringConverter extends StringConverter<LocalDate> {

        @Override
        public String toString(LocalDate object) {
            if (object == null)
                return null;
            return object.toString();
        }

        @Override
        public LocalDate fromString(String string) {
            if (string == null)
                return null;
            return LocalDate.parse(string);
        }
    }

    public static class IntStringConverter extends StringConverter<Integer> {

        @Override
        public String toString(Integer object) {
            if (object == null)
                return null;
            return object.toString();
        }

        @Override
        public Integer fromString(String string) {
            if (string == null)
                return null;
            return Integer.parseInt(string);
        }
    }

}
