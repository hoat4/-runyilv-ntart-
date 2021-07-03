package arunyilvantarto.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Item {

    public UUID id;

    @JsonBackReference
    public Article article;

    public Instant timestamp;

    public int purchasePrice;

    public int purchaseQuantity;

    public LocalDate expiration;
}
