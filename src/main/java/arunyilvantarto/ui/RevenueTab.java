package arunyilvantarto.ui;

import arunyilvantarto.Main;
import arunyilvantarto.domain.SellingPeriod;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.tbee.javafx.scene.layout.MigPane;

public class RevenueTab {
    private final Main app;

    public RevenueTab(Main app) {
        this.app = app;
    }

    public Node build() {
        Button button = new Button("Kassza nyitása");
        button.setOnAction(evt->{
            SellingTab.begin(app);
        });

        return new MigPane().
                add(button);
    }
}
