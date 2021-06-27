package arunyilvantarto.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataRoot {

    public List<User> users = new ArrayList<>();
    public List<Article> articles = new ArrayList<>();

    public Article article(UUID articleID) {
        return articles.stream().filter(a->a.id.equals(articleID)).findAny().get();
    }
}
