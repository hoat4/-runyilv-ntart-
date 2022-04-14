package arunyilvantarto.events;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;

public non-sealed interface AdminOperation extends InventoryEvent {

    void execute(DataRoot data, Main main);

    void undo(DataRoot data, Main main);

    boolean isCollapsibleWith(AdminOperation other);
}
