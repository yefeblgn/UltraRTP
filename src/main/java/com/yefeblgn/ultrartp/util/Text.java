package com.yefeblgn.ultrartp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniMessage yardımcıları.
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    /** Ham MiniMessage çözümleme. */
    public static Component mm(String input, TagResolver... resolvers) {
        if (input == null) return Component.empty();
        try {
            return MM.deserialize(input, resolvers);
        } catch (Exception ex) {
            return Component.text(input);
        }
    }

    /** Eşya ismi/lore için: varsayılan italik kapalı. */
    public static Component item(String input, TagResolver... resolvers) {
        return mm(input, resolvers).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> itemLore(List<String> lines, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        if (lines == null) return out;
        for (String line : lines) {
            out.add(item(line, resolvers));
        }
        return out;
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Kullanıcı verisi için güvenli (etiket çözümlemez) yer tutucu. */
    public static TagResolver of(String key, String value) {
        return Placeholder.unparsed(key, value == null ? "" : value);
    }

    /** Config kaynaklı, içinde renk etiketi olabilecek yer tutucu. */
    public static TagResolver parsed(String key, String value) {
        return Placeholder.parsed(key, value == null ? "" : value);
    }

    public static TagResolver num(String key, Number value) {
        return Placeholder.unparsed(key, value == null ? "0" : String.valueOf(value));
    }

    public static TagResolver comp(String key, Component value) {
        return Placeholder.component(key, value == null ? Component.empty() : value);
    }
}
