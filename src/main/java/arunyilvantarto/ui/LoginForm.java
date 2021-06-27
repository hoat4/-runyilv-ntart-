package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.Security;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.User;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import org.tbee.javafx.scene.layout.MigPane;

import java.util.Arrays;
import java.util.Optional;

public class LoginForm {

    ValidatableField<? extends TextField> usernameField, passwordField;

    private final Main app;
    private final DataRoot data;

    public LoginForm(Main app) {
        this.app = app;
        this.data = app.dataRoot;
    }

    public Region buildLayout() {
        usernameField = new ValidatableField<>(new TextField());
        passwordField = new ValidatableField<>(new PasswordField());

        Button loginButton = new Button("Bel�p�s");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(evt -> login());

        Label title = new Label("Bel�p�s");
        title.setFont(Font.font(24));

        return new MigPane("align 50% 50%", null,
                "[] unrelated []0[] related []0[] unrelated []").

                add(title, "span, center, wrap").

                add(new Label("N�v:")).
                add(usernameField.field, "grow 1, wrap").
                add(usernameField.errorLabel, "skip 1, wrap").

                add(new Label("Jelsz�:")).
                add(passwordField.field, "grow 1, wrap").
                add(passwordField.errorLabel, "skip 1, wrap").

                add(loginButton, "span, growx 1");
    }

    private void login() {
        usernameField.clearError();
        passwordField.clearError();
        if (usernameField.field.getText().isBlank())
            usernameField.showError("K�telez� kit�lteni");
        if (passwordField.field.getText().isBlank())
            passwordField.showError("K�telez� kit�lteni");
        if (ValidatableField.hasError(usernameField, passwordField))
            return;


        Optional<User> o = data.users.stream().filter(u->u.name.equals(usernameField.field.getText())).findAny();
        if (o.isEmpty()) {
            usernameField.showError("Ilyen nev� felhaszn�l� nem l�tezik");
            return;
        }

        User u = o.get();
        if (u.passwordHash == null) {
            usernameField.showError("A felhaszn�l� l�tezik, de nem l�phet be elad�k�nt");
            return;
        }

        byte[] b = Security.hashPassword(passwordField.field.getText());
        if (!Arrays.equals(b, u.passwordHash)) {
            passwordField.showError("Hib�s jelsz�");
            return;
        }

        AdminPage adminPage = new AdminPage(app);
        app.switchPage(adminPage.build(), adminPage);
    }
}