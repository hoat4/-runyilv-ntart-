package arunyilvantarto;

import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.User;

import java.util.Optional;

public class ReadOps {
    private DataRoot data;

    public ReadOps(DataRoot data) {
        this.data = data;
    }

    public Optional<User> lookupUser(String name) {
        return data.users.stream().filter(u -> u.name.equals(name)).findAny();
    }

}
