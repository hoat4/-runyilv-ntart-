package arunyilvantarto.domain;

import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DataRoot {

    public List<User> users = new ArrayList<>();
    public List<Article> articles = new ArrayList<>();
    public List<Message> messages = new ArrayList<>();

    public Article article(String name) {
        return findArticle(name).
                orElseThrow(() -> new RuntimeException("nincs ilyen termék: " + name));
    }

    public Optional<Article> findArticle(String name) {
        return articles.stream().filter(u -> u.name.equals(name)).findAny();
    }

    public User user(String username) {
        return users.stream().filter(u -> u.name.equals(username)).findAny().
                orElseThrow(() -> new RuntimeException("no such user: " + username));
    }
}
