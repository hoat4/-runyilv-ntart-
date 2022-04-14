package arunyilvantarto.events;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SetUserDeleted")
public class SetUserDeletedOp implements AdminOperation{

    @JsonProperty
    private final String username;
    @JsonProperty
    private final boolean deleted;

    @JsonCreator
    public SetUserDeletedOp(@JsonProperty("username") String username, @JsonProperty("deleted") boolean deleted) {
        this.username = username;
        this.deleted = deleted;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        data.user(username).deleted = deleted;
    }

    @Override
    public void undo(DataRoot data, Main main) {
        data.user(username).deleted = !deleted;
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
