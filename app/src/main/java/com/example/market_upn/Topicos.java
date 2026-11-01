package com.example.market_upn;

import java.text.Normalizer;
import java.util.Locale;

public class Topicos {

    public static String deCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return "cat_general";
        }

        String limpio = Normalizer.normalize(
                        categoria.trim().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return "cat_" + limpio.replaceAll("[^a-z0-9]", "_");
    }
}