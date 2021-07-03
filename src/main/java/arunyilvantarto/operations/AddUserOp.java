package arunyilvantarto.operations;

import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.User;

public class AddUserOp implements AdminOperation {
    public final User user;

    public AddUserOp(User user) {
        this.user = user;
    }

    @Override
    public void execute(DataRoot data) {
        data.users.add(user);
    }

    @Override
    public void undo(DataRoot data) {
        if (!data.users.removeIf(u -> u.name.equals(user.name)))
            throw new IllegalStateException("no such user: " + user.name);
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
