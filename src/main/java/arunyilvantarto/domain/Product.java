package arunyilvantarto.domain;

import java.time.Instant;
import java.util.UUID;

public class Product {

    public UUID id;

    public Instant timestamp;

    public int purchasePrice;

    public int purchaseQuantity;

    public int stockQuantity;
}
