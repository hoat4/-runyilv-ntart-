package arunyilvantarto;

import arunyilvantarto.operations.AdminOperation;

public interface OperationListener {

    void onEvent(AdminOperation op);
}
