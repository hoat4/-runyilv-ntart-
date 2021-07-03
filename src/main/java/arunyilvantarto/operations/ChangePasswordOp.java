package arunyilvantarto.operations;

import arunyilvantarto.domain.DataRoot;

public class ChangePasswordOp implements AdminOperation {

    private final String username;
    private final byte[] oldPassword;
    private final byte[] newPassword;

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
