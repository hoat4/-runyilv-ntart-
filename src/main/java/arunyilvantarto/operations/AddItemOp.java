package arunyilvantarto.operations;

import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Item;

public class AddItemOp implements AdminOperation {

    public String articleID;
    public Item product;

    @Override
    public void execute(DataRoot data) {
        final Article article = data.article(articleID);
        article.items.add(product);
        article.stockQuantity += product.purchaseQuantity;
    }

    @Override
    public void undo(DataRoot data) {
         Article a = data.article(articleID);
        if (!a.items.removeIf(p -> p.id.equals(product.id)))
            throw new RuntimeException("no product found with ID " + product.id + " in article " + articleID);
        a.stockQuantity -= product.purchaseQuantity;
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
