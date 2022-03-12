package arunyilvantarto.domain;

import com.fasterxml.jackson.annotation.JsonIdentityReference;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Menu {

    public String name;
    public int price;
    public List<Slot> slots = new ArrayList<>();

    @Override
    public Menu clone() {
        Menu m = new Menu();
        m.name = name;
        m.price = price;
        m.slots = slots.stream().map(Slot::clone).collect(Collectors.toList());
        return m;
    }

    public static class Slot {

        @JsonIdentityReference
        public List<Article> articles = new ArrayList<>();

        @Override
        public Slot clone() {
            Slot slot = new Slot();
            slot.articles = new ArrayList<>(articles);
            return slot;
        }
    }
}
