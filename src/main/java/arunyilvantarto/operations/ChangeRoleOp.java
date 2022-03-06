package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.User;

public class ChangeRoleOp implements AdminOperation{

    public final String username;
    public final User.Role oldRole;
    public final User.Role newRole;

    public ChangeRoleOp(String username, User.Role oldRole, User.Role newRole) {
        this.username = username;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        data.user(username).role = newRole;
    }

    @Override
    public void undo(DataRoot data, Main main) {
        data.user(username).role = oldRole;
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
