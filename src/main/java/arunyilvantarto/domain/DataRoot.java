package arunyilvantarto.domain;

import java.util.ArrayList;
import java.util.List;

public class DataRoot {

    public List<User> users = new ArrayList<>();
    public List<Article> articles = new ArrayList<>();

    public Article article(String name) {
        return articles.stream().filter(u -> u.name.equals(name)).findAny().
                orElseThrow(() -> new RuntimeException("nincs ilyen termék: " + name));
    }

    public User user(String username) {
        return users.stream().filter(u -> u.name.equals(username)).findAny().
                orElseThrow(() -> new RuntimeException("no such user: " + username));
    }
}
