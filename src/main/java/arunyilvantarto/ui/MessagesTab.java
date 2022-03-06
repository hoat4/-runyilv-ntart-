package arunyilvantarto.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;

public class MessagesTab {

    private final AdminPage adminPage;

    public MessagesTab(AdminPage adminPage) {
        this.adminPage = adminPage;
    }

    public Node build() {
        return new Label("placeholder");
    }
}
