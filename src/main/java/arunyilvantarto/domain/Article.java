package arunyilvantarto.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.time.Instant;
import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name", scope = Article.class)
public class Article {

    public Instant timestamp;

    public String name;

    public String barCode;

    public int sellingPrice;

    @JsonManagedReference
    public List<Item> items;

    public int stockQuantity;

    public int staffPrice() {
        if (items.isEmpty())
            return sellingPrice;
        return items.get(items.size() - 1).purchasePrice;
    }
}
