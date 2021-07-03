package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.Security;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.SellingPeriod;
import arunyilvantarto.domain.User;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SceneHelper;
import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.tbee.javafx.scene.layout.MigPane;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static javafx.beans.binding.Bindings.createBooleanBinding;

public class LoginForm {

    ValidatableField<? extends TextField> usernameField, passwordField;

    private final Main app;
    private final DataRoot data;
    private ProgressIndicator progressIndicator;
    private Node loginForm;

    public LoginForm(Main app) {
        this.app = app;
        this.data = app.dataRoot;

        app.executor.execute(() -> Security.hashPassword("")); // preload MessageDigest
    }

    public Region buildLayout() {
        usernameField = new ValidatableField<>(new TextField());
        passwordField = new ValidatableField<>(new PasswordField());

        Button loginButton = new Button("Belépés");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(evt -> login());

        Label title = new Label("Belépés");
        title.setFont(Font.font(24));

        progressIndicator = new ProgressIndicator(-1);
        progressIndicator.setVisible(false);
        progressIndicator.setScaleX(2);
        progressIndicator.setScaleY(2);

        return new StackPane(
                loginForm = new MigPane("align 50% 50%", null,
                        "[] unrelated []0[] related []0[] unrelated []").

                        add(title, "span, center, wrap").

                        add(new Label("Név:")).
                        add(usernameField.field, "grow 1, wrap").
                        add(usernameField.errorLabel, "skip 1, wrap").

                        add(new Label("Jelszó:")).
                        add(passwordField.field, "grow 1, wrap").
                        add(passwordField.errorLabel, "skip 1, wrap").

                        add(loginButton, "span, growx 1"),
                progressIndicator);
    }

    private void login() {
        usernameField.clearError();
        passwordField.clearError();
        if (usernameField.field.getText().isBlank())
            usernameField.showError("Kötelező kitölteni");
        if (passwordField.field.getText().isBlank())
            passwordField.showError("Kötelező kitölteni");
        if (ValidatableField.hasError(usernameField, passwordField))
            return;


        Optional<User> o = data.users.stream().filter(u -> u.name.equals(usernameField.field.getText())).findAny();
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
            return;
        }

        app.logonUser = u;

        progressIndicator.setVisible(true);

        loginForm.setOpacity(0);

        /*FadeTransition transition = new FadeTransition(Duration.seconds(0), rootNode);
        transition.setFromValue(1);
        transition.setToValue(0);
        transition.setOnFinished(evt->latch.countDown());
        transition.play();*/


        app.executor.execute(() -> {
            SellingPeriod p = SellingTab.lastSellingPeriod(app.salesIO);
            if (p != null && p.endTime == null) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);

                    TextInputDialog d = new TextInputDialog();
                    d.setTitle("Zárás");
                    d.setHeaderText("Az előző értékesítési periódus nem lett bezárva, " + p.remainingCash() + " Ft maradt elvileg a kasszában. ");
                    d.setContentText("Kasszában hagyott váltópénz: ");
                    d.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                            createBooleanBinding(() -> !d.getEditor().getText().matches("[0-9]+"), d.getEditor().textProperty()));

                    d.showAndWait().ifPresentOrElse(s -> {
                        progressIndicator.setVisible(true);

                        p.closeCash = Integer.parseInt(s);
                        app.executor.execute(() -> SellingTab.closePeriod(app, p));
                        app.executor.execute(this::loadAndShowNextPage);
                    }, () -> {
                        loginForm.setOpacity(1);
                        app.logonUser = null;
                    });
                });
            } else
                loadAndShowNextPage();
        });
    }


    private void loadAndShowNextPage() {
        AdminPage adminPage = new AdminPage(app);
        final Node n = adminPage.build();
        Scene scene = app.preload(n);
        Platform.runLater(() -> {
            app.switchPage(scene, adminPage);
        });
    }

}