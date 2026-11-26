package app.swarsetu.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts database rows to JSON-friendly maps: camelCase keys, dates as yyyy-MM-dd, times as HH:mm. */
public final class Rows {
    private Rows() {}

    public static List<Map<String, Object>> camel(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) out.add(camel(r));
        return out;
    }

    public static Map<String, Object> camel(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        row.forEach((k, v) -> { if (!(v instanceof byte[])) out.put(key(k), value(v)); });
        return out;
    }

    /** MySQL returns booleans and comparisons as 0/1 numbers or Boolean depending on the column. */
    public static boolean truthy(Object o) {
        return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() != 0;
    }

    static String key(String k) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : k.toLowerCase().toCharArray()) {
            if (c == '_') { up = true; continue; }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    static Object value(Object v) {
        if (v instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (v instanceof java.sql.Time t) return t.toString().substring(0, 5);
        if (v instanceof java.time.LocalTime t) return t.toString().substring(0, 5);
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return v;
    }
}
