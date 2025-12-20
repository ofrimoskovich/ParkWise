package control;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;

public final class DbUtil {
    private DbUtil() {}
    /** Same idea as getIntAny but for double. Returns defaultValue if missing/NULL. */
    public static double getDoubleAny(ResultSet rs, double defaultValue, String... possibleNames) throws SQLException {
        for (String name : possibleNames) {
            if (name != null && hasColumn(rs, name)) {
                double v = rs.getDouble(name);
                return rs.wasNull() ? defaultValue : v;
            }
        }

        String[] normalizedWanted = new String[possibleNames.length];
        for (int i = 0; i < possibleNames.length; i++) {
            normalizedWanted[i] = normalize(possibleNames[i]);
        }

        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String col = md.getColumnLabel(i);
            if (col == null || col.isBlank()) col = md.getColumnName(i);
            String normCol = normalize(col);
            for (String w : normalizedWanted) {
                if (w != null && w.equals(normCol)) {
                    double v = rs.getDouble(i);
                    return rs.wasNull() ? defaultValue : v;
                }
            }
        }
        return defaultValue;
    }

    /** Reads a java.time.LocalDate from an Access Date/Time column (or returns null). */
    public static java.time.LocalDate getLocalDateAny(ResultSet rs, String... possibleNames) throws SQLException {
        for (String name : possibleNames) {
            if (name != null && hasColumn(rs, name)) {
                java.sql.Date d = rs.getDate(name);
                return d == null ? null : d.toLocalDate();
            }
        }

        String[] normalizedWanted = new String[possibleNames.length];
        for (int i = 0; i < possibleNames.length; i++) {
            normalizedWanted[i] = normalize(possibleNames[i]);
        }

        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String col = md.getColumnLabel(i);
            if (col == null || col.isBlank()) col = md.getColumnName(i);
            String normCol = normalize(col);
            for (String w : normalizedWanted) {
                if (w != null && w.equals(normCol)) {
                    java.sql.Date d = rs.getDate(i);
                    return d == null ? null : d.toLocalDate();
                }
            }
        }
        return null;
    }
    /** Returns true if the result set contains a column with this label (case-insensitive). */
    public static boolean hasColumn(ResultSet rs, String label) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String name = md.getColumnLabel(i);
            if (name == null || name.isBlank()) name = md.getColumnName(i);
            if (name != null && name.equalsIgnoreCase(label)) return true;
        }
        return false;
    }

    /**
     * Try to read a String column using multiple possible column names.
     * Returns null if none exist or value is SQL NULL.
     */
    public static String getStringAny(ResultSet rs, String... possibleNames) throws SQLException {
        for (String name : possibleNames) {
            if (name != null && hasColumn(rs, name)) {
                return rs.getString(name);
            }
        }
        // also try by normalized comparison against existing columns
        String normalizedWanted[] = new String[possibleNames.length];
        for (int i = 0; i < possibleNames.length; i++) {
            normalizedWanted[i] = normalize(possibleNames[i]);
        }

        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String col = md.getColumnLabel(i);
            if (col == null || col.isBlank()) col = md.getColumnName(i);
            String normCol = normalize(col);
            for (String w : normalizedWanted) {
                if (w != null && w.equals(normCol)) {
                    return rs.getString(i);
                }
            }
        }
        return null;
    }

    /** Same as getStringAny but for int. Returns defaultValue if missing or NULL. */
    public static int getIntAny(ResultSet rs, int defaultValue, String... possibleNames) throws SQLException {
        for (String name : possibleNames) {
            if (name != null && hasColumn(rs, name)) {
                int v = rs.getInt(name);
                return rs.wasNull() ? defaultValue : v;
            }
        }

        String normalizedWanted[] = new String[possibleNames.length];
        for (int i = 0; i < possibleNames.length; i++) {
            normalizedWanted[i] = normalize(possibleNames[i]);
        }

        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String col = md.getColumnLabel(i);
            if (col == null || col.isBlank()) col = md.getColumnName(i);
            String normCol = normalize(col);
            for (String w : normalizedWanted) {
                if (w != null && w.equals(normCol)) {
                    int v = rs.getInt(i);
                    return rs.wasNull() ? defaultValue : v;
                }
            }
        }
        return defaultValue;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")     // remove spaces
                .replaceAll("_", "")        // remove underscores
                .replaceAll("-", "");       // remove hyphens
    }
}
