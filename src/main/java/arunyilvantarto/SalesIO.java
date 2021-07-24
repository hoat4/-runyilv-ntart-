package arunyilvantarto;

import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Sale;
import arunyilvantarto.domain.SellingPeriod;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SalesIO {

    private static final int MIN_COLS = 6;
    private static final int MAX_COLS = 7;

    private static final String PERIOD_OPEN_PRODUCT_NAME = "NYITÁS";
    private static final String PERIOD_CLOSE_PRODUCT_NAME = "ZÁRÁS";
    private static final String MODIFY_CASH_PRODUCT_NAME = "KASSZAMÓDOSÍTÁS";

    private final DataRoot data;
    private final FileChannel channel;

    public SalesIO(DataRoot data, FileChannel channel) throws IOException {
        this.data = data;
        this.channel = channel;

        channel.position(channel.size());
    }

    public synchronized void begin() {
        writeImpl("Időpont\tTermék\tMennyiség\tTermékenkénti ár\tEladó\tPeriódusazonosító vagy személynév\n");
    }

    public synchronized void beginPeriod(SellingPeriod period) {
        if (period.username.isEmpty())
            throw new IllegalArgumentException("empty seller name");

        LocalDateTime timestamp = LocalDateTime.ofInstant(period.beginTime, ZoneId.systemDefault());
        writeImpl(timestamp, PERIOD_OPEN_PRODUCT_NAME, 1, -period.openCash, period.username,
                new Sale.PeriodBillID(period.id), period.openCreditCardAmount);
    }

    public synchronized void sale(Sale sale) {
        if (sale.article.name.isEmpty() || sale.seller.isEmpty())
            throw new IllegalArgumentException("empty product name or seller name");

        LocalDateTime timestamp = LocalDateTime.ofInstant(sale.timestamp, ZoneId.systemDefault());
        writeImpl(timestamp, sale.article.name, sale.quantity, sale.pricePerProduct, sale.seller, sale.billID, -1);
    }

    public synchronized void endPeriod(SellingPeriod period) {
        if (period.username.isEmpty())
            throw new IllegalArgumentException("empty seller name");

        LocalDateTime timestamp = LocalDateTime.ofInstant(period.endTime, ZoneId.systemDefault());
        writeImpl(timestamp, PERIOD_CLOSE_PRODUCT_NAME, 1, period.closeCash, period.username,
                new Sale.PeriodBillID(period.id),  period.closeCreditCardAmount);
    }

    public synchronized void modifyCash(String username, int cash, int creditCardAmount) {
        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        writeImpl(timestamp, MODIFY_CASH_PRODUCT_NAME, 0, cash, username, null, creditCardAmount);
    }

    public synchronized void read(SalesVisitor visitor) {
        try {
            currentReadPeriod = null;
            visitor.begin();
            channel.position(0);

            Reader reader = Channels.newReader(channel, UTF_8);
            String[] a = new String[MAX_COLS];
            char[] b = new char[1000];
            int i = 0, column = 0, row = 0;

            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    if (column != 0 || i != 0)
                        throw new IOException("EOF unexpected @ " + row + ", " + column);
                    break;
                } else if (ch == '\t' || ch == '\n') {
                    if (i == 0)
                        continue;
                    a[column++] = new String(b, 0, i);
                    i = 0;
                    if (ch == '\n') {
                        if (column >= MIN_COLS && column <= MAX_COLS) {
                            if (!a[0].equals("Időpont")) // header
                                handleRow(a, visitor);
                        } else
                            throw new IOException("Newline not after " + MIN_COLS+"-"+MAX_COLS + " columns " +
                                    "but " + column + " @ " + row);
                        column = 0;
                        row++;
                    }
                } else {
                    b[i++] = (char) ch;
                }
            }
            visitor.end();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SellingPeriod currentReadPeriod;

    private void handleRow(String[] row, SalesVisitor visitor) throws IOException {
        Instant timestamp = LocalDateTime.parse(row[0]).atZone(ZoneId.systemDefault()).toInstant();
        String productName = row[1];
        int quantity = Integer.parseInt(row[2]);
        int pricePerProduct = Integer.parseInt(row[3]);
        String seller = row[4];
        Sale.BillID billID = Sale.BillID.parse(row[5]);
        int creditCardAmount = row[6] == null ? 0: Integer.parseInt(row[6]);

        switch (productName) {
            case PERIOD_OPEN_PRODUCT_NAME:
                SellingPeriod p = new SellingPeriod();
                p.id = ((Sale.PeriodBillID) billID).periodID;
                p.username = seller;
                p.openCash = -pricePerProduct;
                p.beginTime = timestamp;
                p.sales = new ArrayList<>();
                p.openCreditCardAmount = creditCardAmount;
                currentReadPeriod = p;
                visitor.beginPeriod(p);
                break;
            case PERIOD_CLOSE_PRODUCT_NAME:
                if (currentReadPeriod.id != ((Sale.PeriodBillID) billID).periodID)
                    throw new IOException("period ID mismatch " + currentReadPeriod.id + " vs " +
                            ((Sale.PeriodBillID) billID).periodID);

                p = currentReadPeriod;
                p.closeCash = pricePerProduct;
                p.closeCreditCardAmount = creditCardAmount;
                p.endTime = timestamp;
                visitor.endPeriod(p);
                currentReadPeriod = null;
                break;
            case MODIFY_CASH_PRODUCT_NAME:
                visitor.modifyCash(pricePerProduct, creditCardAmount);
                break;
            default:
                Sale sale = new Sale();
                sale.timestamp = timestamp;
                sale.seller = seller;
                sale.article = data.articles.stream().filter(a -> a.name.equals(productName)).findAny().
                        orElseThrow(() -> new RuntimeException("no such article: " + productName));
                sale.quantity = quantity;
                sale.pricePerProduct = pricePerProduct;
                sale.billID = billID;
                currentReadPeriod.sales.add(sale);
                visitor.sale(sale);
                break;
        }
    }

    private void writeImpl(LocalDateTime timestamp, String productName, int quantity, int pricePerProduct,
                           String seller, Sale.BillID periodID, int creditCardAmount) {
        writeImpl(timestamp + "\t" + productName + "\t" + quantity + "\t" + pricePerProduct + "\t" + seller + "\t" +
                (periodID == null ? "-" : periodID.toString()) +
                (creditCardAmount == -1 ? "" : "\t" + creditCardAmount) + "\n");
    }

    private void writeImpl(String l) {
        try {
            channel.write(ByteBuffer.wrap(l.getBytes(UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO
        }
    }

}
