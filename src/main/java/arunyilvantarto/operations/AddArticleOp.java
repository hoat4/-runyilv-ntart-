package arunyilvantarto.operations;

import arunyilvantarto.Main;
import arunyilvantarto.domain.Article;
import arunyilvantarto.domain.DataRoot;

public class AddArticleOp implements AdminOperation {

    public  final Article article;

    public AddArticleOp(Article article) {
        this.article = article;
    }

    @Override
    public void execute(DataRoot data, Main main) {
        data.articles.add(article);
    }

    @Override
    public void undo(DataRoot data, Main main) {
        if (!data.articles.removeIf(a -> a.name.equals(article.name)))
            throw new RuntimeException(article.name + " not in existing articles");
    }

    @Override
    public boolean isCollapsibleWith(AdminOperation other) {
        return false;
    }
}
