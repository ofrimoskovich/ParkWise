package control;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class AccessDb {

    private String accdbPath;

    public AccessDb(String accdbPath) {
        this.accdbPath = accdbPath;
    }

    public String getAccdbPath() {
        return accdbPath;
    }

    public void setAccdbPath(String accdbPath) {
        this.accdbPath = accdbPath;
    }

    /**
     * Opens a new JDBC Connection to Access using UCanAccess.
     * This method guarantees that the DB file is resolved correctly
     * regardless of Eclipse / Git / run location.
     */
    public Connection open() throws SQLException {

        Path resolved = resolveAccdbPath(accdbPath);

        if (!Files.exists(resolved)) {
            throw new SQLException("Access DB file not found at: " + resolved.toAbsolutePath());
        }

        String url = "jdbc:ucanaccess://" + resolved.toAbsolutePath();
        return DriverManager.getConnection(url);
    }

    /**
     * Makes the DB path stable for all machines and run locations.
     */
    private Path resolveAccdbPath(String path) {

        Path p = Paths.get(path);

        if (p.isAbsolute())
            return p;

        // Try working directory (Eclipse Run)
        Path runDir = Paths.get(System.getProperty("user.dir")).resolve(path);
        if (Files.exists(runDir))
            return runDir;

        // Try project root (when running from /bin or /src)
        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        if (projectRoot != null) {
            Path rootTry = projectRoot.resolve(path);
            if (Files.exists(rootTry))
                return rootTry;
        }

        // Fallback – original path (will throw error later)
        return p;
    }
}