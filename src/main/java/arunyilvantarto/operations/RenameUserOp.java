package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.SalesIO;
import arunyilvantarto.SalesVisitor;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardOpenOption.*;

public class RenameUserOp implements AdminOperation {

    public String oldName;
    public String newName;

    public RenameUserOp() {
    }

    public RenameUserOp(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        if (data.users.stream().anyMatch(u -> u.name.equals(newName)))
            throw new RuntimeException("user '" + newName + "' already exists (tried to rename user '" + oldName + "')");
        data.user(oldName).name = newName;
        doRename(data, main, oldName, newName);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        data.user(newName).name = oldName;
        doRename(data, main, newName, oldName);
    }

    private void doRename(DataRoot data, Main main, String oldName, String newName) {
        try {
            Path path = main.salesTsvPath().resolveSibling("sales_tmp.tsv");
            try (SalesIO out = new SalesIO(data, FileChannel.open(path, CREATE_NEW, WRITE))) {
                main.salesIO.read(new UserRenamingSalesVisitor(out, oldName, newName));
            }
            main.salesIO.close();
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuuMMdd_HHmmss"));
            Files.move(main.salesTsvPath(), main.salesTsvPath().resolveSibling("sales_ren_" + now + ".tsv"));
            Files.move(path, main.salesTsvPath());
            main.salesIO = new SalesIO(main.dataRoot, FileChannel.open(main.salesTsvPath(), READ, WRITE));
        } catch (IOException e) {
            data.user(newName).name = oldName;
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }

    private static class UserRenamingSalesVisitor implements SalesVisitor {
        private final SalesIO delegate;
        private final String oldName;
        private final String newName;

        public UserRenamingSalesVisitor(SalesIO delegate, String oldName, String newName) {
            this.delegate = delegate;
            this.oldName = oldName;
            this.newName = newName;
        }

        @Override
        public void begin() {
            delegate.begin();
        }

        @Override
        public void beginPeriod(SellingPeriod period, String comment) {
            if (period.username.equals(oldName))
                period.username = newName;
            delegate.beginPeriod(period, comment);
        }

        @Override
        public void sale(Sale sale) {
            if (sale.seller.equals(oldName))
                sale.seller = newName;
            if (sale.billID instanceof Sale.StaffBillID)
                if (((Sale.StaffBillID) sale.billID).username.equals(oldName))
                    sale.billID = new Sale.StaffBillID(newName);
            delegate.sale(sale);
        }

        @Override
        public void endPeriod(SellingPeriod period, String comment) {
            if (period.username.equals(oldName))
                period.username = newName;
            delegate.endPeriod(period, comment);
        }

        @Override
        public void modifyCash(String username, int cash, int creditCardAmount) {
            if (username.equals(oldName))
                username = newName;
            delegate.modifyCash(username, cash, creditCardAmount);
        }

        @Override
        public void staffBillPay(Sale.StaffBillID bill, String administrator, int money,
                                 Instant timestamp) {
            if (bill.username.equals(oldName))
                bill = new Sale.StaffBillID(newName);
            if (administrator.equals(oldName))
                administrator = newName;
            delegate.staffBillPay(bill, administrator, money);
        }
    }
}
