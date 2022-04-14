package arunyilvantarto.events;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.User;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("AddUser")
public class AddUserOp implements AdminOperation {
    public final User user;

    public AddUserOp(User user) {
        this.user = user;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        if (data.users.stream().anyMatch(u -> u.name.equals(user.name)))
            throw new RuntimeException("user '" + user.name + "' already exists (tried to rename user '" + user.name + "')");
        data.users.add(user);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        if (!data.users.removeIf(u -> u.name.equals(user.name)))
            throw new IllegalStateException("no such user: " + user.name);
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
