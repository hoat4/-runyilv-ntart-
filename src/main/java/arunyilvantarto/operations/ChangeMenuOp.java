package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Menu;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChangeMenuOp implements AdminOperation{

    @JsonProperty
    public final Menu oldMenu;

    @JsonProperty
    public final Menu newMenu;

    @JsonCreator
    public ChangeMenuOp(@JsonProperty("oldMenu") Menu oldMenu, @JsonProperty("newMenu") Menu newMenu) {
        this.oldMenu = oldMenu;
        this.newMenu = newMenu;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        data.menus.replaceAll(menu -> menu.name.equals(this.oldMenu.name) ? this.newMenu : menu);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        data.menus.replaceAll(menu -> menu.name.equals(this.newMenu.name) ? this.oldMenu : menu);
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
