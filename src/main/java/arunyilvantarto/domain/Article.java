package arunyilvantarto.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.Instant;
import java.util.List;

public class Article {

    public Instant timestamp;

    public String name;

    public String barCode;

    public int sellingPrice;

    @JsonManagedReference
    public List<Item> items;

    public int stockQuantity;
}
