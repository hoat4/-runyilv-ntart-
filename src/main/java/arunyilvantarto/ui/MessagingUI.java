package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.domain.Message;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.util.Optional;

public class MessagingUI {

    public static Optional<Message> showSendMessageDialog(Main main, String description) {
        Dialog<String> d = new Dialog<>();
        d.setTitle("Üzenetküldés");
        d.setHeaderText("Probléma jelzése a büfé vezetőjének");
        TextArea content = new TextArea();
        VBox vbox = new VBox(new Label(description), content);
        Platform.runLater(content::requestFocus);
        vbox.setSpacing(7);
        d.getDialogPane().setContent(vbox);
        d.setResultConverter(buttonType -> buttonType == ButtonType.OK ? content.getText() : null);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return d.showAndWait().map(s -> {
            Message message = new Message();
            message.sender = main.logonUser;
            message.timestamp = Instant.now();
            message.text = s;
            return message;
        });
    }
}
