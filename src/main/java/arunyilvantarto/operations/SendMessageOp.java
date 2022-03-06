package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Message;

public class SendMessageOp implements AdminOperation {

    public Message message;

    public SendMessageOp() {
    }

    public SendMessageOp(Message message) {
        this.message = message;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        data.messages.add(message);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        data.messages.removeIf(msg -> msg.timestamp.equals(message.timestamp));
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
