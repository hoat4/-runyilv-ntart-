package arunyilvantarto.operations;

import arunyilvantarto.domain.DataRoot;

public class ChangePasswordOp implements AdminOperation {

    public final String username;
    public final byte[] oldPassword;
    public final byte[] newPassword;

    public ChangePasswordOp(String username, byte[] oldPassword, byte[] newPassword) {
        this.username = username;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    @Override
    public void execute(DataRoot data) {
        data.users.stream().filter(u->u.name.equals(username)).findAny().get().passwordHash = newPassword;
    }

    @Override
    public void undo(DataRoot data) {
        data.users.stream().filter(u->u.name.equals(username)).findAny().get().passwordHash = oldPassword;
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
