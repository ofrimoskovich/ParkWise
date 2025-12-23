package boundary;

import javax.swing.*;
import java.awt.*;

/**
 * Simple UI helper for validation and user feedback.
 */
public final class UiUtil {

    private UiUtil() {}

    /**
     * Parses a positive integer from a text field.
     *
     * @throws IllegalArgumentException if invalid
     */
    public static int parsePositiveInt(String raw, String fieldName) {
        if (raw == null) raw = "";
        raw = raw.trim();
        if (raw.isEmpty()) throw new IllegalArgumentException(fieldName + " is required.");

        try {
            int v = Integer.parseInt(raw);
            if (v <= 0) throw new IllegalArgumentException(fieldName + " must be positive.");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer.");
        }
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        int res = JOptionPane.showConfirmDialog(parent, message, "Confirm", JOptionPane.YES_NO_OPTION);
        return res == JOptionPane.YES_OPTION;
    }
}