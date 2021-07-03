package arunyilvantarto.operations;

import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.DataRoot;

public class DeleteArticleOp implements AdminOperation {

    public final Article article;

    public DeleteArticleOp(Article article) {
        this.article = article;
    }

    @Override
    public void execute(DataRoot data) {
        if (!data.articles.removeIf(a -> a.name.equals(article.name)))
            throw new RuntimeException("no such article: " + article.name);
    }

    @Override
    public void undo(DataRoot data) {
        data.articles.add(article);
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
