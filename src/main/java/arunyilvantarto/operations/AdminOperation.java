package arunyilvantarto.operations;

import arunyilvantarto.domain.DataRoot;

public interface AdminOperation {


    void execute(DataRoot data);

    void undo(DataRoot data);

    boolean isCollapsibleWith(AdminOperation other);
}
