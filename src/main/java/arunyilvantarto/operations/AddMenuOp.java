package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Menu;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddMenuOp implements AdminOperation{

    @JsonProperty
    public final Menu menu;

    @JsonCreator
    public AddMenuOp(@JsonProperty("menu") Menu menu) {
        this.menu = menu;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        if (data.menus.stream().anyMatch(m->m.name.equals(menu.name)))
            throw new RuntimeException("menu '"+menu.name+"' already exists");
        data.menus.add(menu);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        if (!data.menus.removeIf(m->m.name.equals(menu.name)))
            throw new RuntimeException("menu '"+menu.name+"' not exists");
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
