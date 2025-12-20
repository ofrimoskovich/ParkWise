package control;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized Access (ACCDB) connection helper.
 *
 * Runtime requirement: Add UCanAccess (ucanaccess + dependencies) to your
 * Eclipse build path.
 *
 * This class does NOT reference UCanAccess classes directly, so the project
 * compiles even if the jars are not present; they are required at runtime when
 * opening a connection.
 */
public final class AccessDb {

	private String accdbPath;

	/**
	 * @param accdbPath Path to .accdb file (absolute or relative to project run
	 *                  directory)
	 */
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
	 */
	public Connection open() throws SQLException {
		String normalized = Path.of(accdbPath).toAbsolutePath().toString();
		String url = "jdbc:ucanaccess://" + normalized;
		return DriverManager.getConnection(url);
	}
}
