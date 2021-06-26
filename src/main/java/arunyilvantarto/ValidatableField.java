package arunyilvantarto;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.stream.Stream;

import static javafx.scene.paint.Color.color;

public class ValidatableField<F extends Control>  {
    public final F field;
    public final Label errorLabel = new Label();

    public ValidatableField(F field) {
        this.field = field;

        errorLabel.setLabelFor(field);
        errorLabel.setMaxHeight(0);
        errorLabel.setText(null);
        errorLabel.setTextFill(color(.9, 0, 0));
    }

    public static boolean hasError(ValidatableField<?>... fields) {
        return Stream.of(fields).anyMatch(f->f.errorLabel.getText() != null);
    }

    public void showError(String errorMessage) {
        errorLabel.setText(errorMessage);
        errorLabel.setMaxHeight(Double.MAX_VALUE);

        // TODO MigLayout-nak küldjünk fel issue-t, hogy
        //     egyrészt child control setManaged-ét nem veszi észre,
        //     másrészt ha utána meghívom invalidateGrid-et, akkor meg a skip-et nem veszi figyelembe,
        //     harmadrészt meg lehessen értelmes max-height animációt valahogy
    }

    public void clearError() {
        errorLabel.setMaxHeight(0);
        errorLabel.setText(null);
    }
}
