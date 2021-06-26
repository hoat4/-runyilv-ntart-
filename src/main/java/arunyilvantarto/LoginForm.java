package arunyilvantarto;

import arunyilvantarto.domain.User;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.tbee.javafx.scene.layout.MigPane;

import java.util.Arrays;
import java.util.Optional;

public class LoginForm {
    ValidatableField<? extends TextField> usernameField, passwordField;

    private final ReadOps readOps;

    public LoginForm(ReadOps readOps) {
        this.readOps = readOps;
    }

    public Region buildLayout() {
        usernameField = new ValidatableField<>(new TextField());
        passwordField = new ValidatableField<>(new PasswordField());

        Button loginButton = new Button("Belépés");
        loginButton.setOnAction(evt -> {
            usernameField.clearError();
            passwordField.clearError();
            if (usernameField.field.getText().isBlank())
                usernameField.showError("Kötelező kitölteni");
            if (passwordField.field.getText().isBlank())
                passwordField.showError("Kötelező kitölteni");
            if (ValidatableField.hasError(usernameField, passwordField))
                return;


            Optional<User> o = readOps.lookupUser(usernameField.field.getText());
            if (o.isEmpty()) {
                usernameField.showError("Ilyen nevű felhasználó nem létezik");
                return;
            }

            User u = o.get();
            if (u.passwordHash == null) {
                usernameField.showError("A felhasználó létezik, de nem léphet be eladóként");
                return;
            }

            byte[] b = Security.hashPassword(passwordField.field.getText());
            if (!Arrays.equals(b, u.passwordHash)) {
                passwordField.showError("Hibás jelszó");
            }
        });

        Label title = new Label("Belépés");
        title.setFont(Font.font(24));

        return new MigPane("align 50% 50%", null,
                "[] unrelated []0[] related []0[] unrelated []").

                add(title, "span, center, wrap").

                add(new Label("Név:")).
                add(usernameField.field, "grow 1, wrap").
                add(usernameField.errorLabel, "skip 1, wrap").

                add(new Label("Jelszó:")).
                add(passwordField.field, "grow 1, wrap").
                add(passwordField.errorLabel, "skip 1, wrap").

                add(loginButton, "span, growx 1");
    }
}
