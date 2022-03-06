package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.DataRoot;
import arunyilvantarto.domain.Item;

public class DeleteItemOp implements AdminOperation {

    public final String articleName;
    public final Item item;

    public DeleteItemOp(String articleName, Item item) {
        this.articleName = articleName;
        this.item = item;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        final Article article = data.article(articleName);
        if (!article.items.removeIf(i -> i.id.equals(item.id)))
            throw new RuntimeException("nincs ilyen item: " + item.id);
        article.stockQuantity -= item.purchaseQuantity;
    }

    @Override
    public void undo(DataRoot data, Main main) {
        Article article = data.article(articleName);
        article.items.add(item);
        article.stockQuantity += item.purchaseQuantity;
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
