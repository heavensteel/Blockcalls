package com.floventy.blockcalls.data;

import java.util.List;

/**
 * Data models for pre-made block lists fetched from the internet.
 */
public class PreMadeBlockLists {

    public static class Pattern {
        public final String value;
        public Pattern(String value) { this.value = value; }
    }

    public static class Category {
        public final String id;
        public final String name;
        public final String description;
        public final List<String> patterns;

        public Category(String id, String name, String description, List<String> patterns) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.patterns = patterns;
        }
    }

    public static class Country {
        public final String code;
        public final String name;
        public final String flag;
        public final List<Category> categories;

        public Country(String code, String name, String flag, List<Category> categories) {
            this.code = code;
            this.name = name;
            this.flag = flag;
            this.categories = categories;
        }

        public String getDisplayName() {
            return flag + "  " + name;
        }
    }
}
