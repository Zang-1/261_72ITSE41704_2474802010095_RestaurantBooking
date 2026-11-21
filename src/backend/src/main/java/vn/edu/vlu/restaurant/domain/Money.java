package vn.edu.vlu.restaurant.domain;

import java.text.NumberFormat;
import java.util.Locale;

/** Money is whole VND in a long (NFR-08). This only formats it for messages, e.g. 334000 → "334,000 VND". */
public final class Money {

    private Money() {
    }

    public static String format(long amount) {
        return NumberFormat.getIntegerInstance(Locale.US).format(amount) + " VND";
    }
}
