package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;

public interface AdminOperation {

    void execute(DataRoot data, Main main);

    void undo(DataRoot data, Main main);

    boolean isCollapsibleWith(AdminOperation other);
}
