package arunyilvantarto.domain;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Article {

    public UUID id;

    public Instant timestamp;

    public String name;

    public String barCode;

    public int sellingPrice;

    public List<Product> products;

}
