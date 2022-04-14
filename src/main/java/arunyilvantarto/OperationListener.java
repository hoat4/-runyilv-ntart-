package arunyilvantarto;

import arunyilvantarto.events.InventoryEvent;

public interface OperationListener {

    void onEvent(InventoryEvent op);
}
