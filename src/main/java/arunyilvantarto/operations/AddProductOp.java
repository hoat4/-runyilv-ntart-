package arunyilvantarto.operations;

import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Product;

import java.util.UUID;

public class AddProductOp implements AdminOperation {

    public UUID articleID;
    public Product product;

    @Override
    public void execute(DataRoot data) {
        data.article(articleID).products.add(product);
    }

    @Override
    public void undo(DataRoot data) {
        if (!data.article(articleID).products.removeIf(p -> p.id.equals(product.id)))
            throw new RuntimeException("no product found with ID " + product.id + " in article " + articleID);
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
