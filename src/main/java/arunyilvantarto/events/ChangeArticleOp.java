package arunyilvantarto.events;

import arunyilvantarto.Main;
import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.DataRoot;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.function.BiConsumer;

@JsonTypeName("ChangeArticle")
public class ChangeArticleOp implements AdminOperation {

    public final String articleID;
    public final ArticleProperty property;
    public final Object oldValue;
    public final Object newValue;

    public ChangeArticleOp(String articleID, ArticleProperty property, Object oldValue, Object newValue) {
        this.articleID = articleID;
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        Article article = data.article(articleID);
        property.setter.accept(article, newValue);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        Article article = data.article(articleID);
        property.setter.accept(article, oldValue);
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return other instanceof ChangeArticleOp && ((ChangeArticleOp) other).articleID.equals(articleID);
    }

    public enum ArticleProperty {

        BARCODE((a, v) -> a.barCode = (String) v),
        PRICE((a, v) -> a.sellingPrice = (int) v),
        QUANTITY((a, v) -> a.stockQuantity = (int) v);

        final BiConsumer<Article, Object> setter;

        ArticleProperty(BiConsumer<Article, Object> setter) {
            this.setter = setter;
        }
    }
}
