package arunyilvantarto.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
public sealed interface InventoryEvent permits AdminOperation, SellingEvent {
}
