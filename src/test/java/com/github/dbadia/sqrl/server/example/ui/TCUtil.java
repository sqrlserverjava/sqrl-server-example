package com.github.dbadia.sqrl.server.example.ui;

public class TCUtil {
	public static final String DB_URL = "jdbc:h2:mem:sqrlexample";

	public static void resetDatabase() throws Exception {
		// TODO: remove orm lite stuff
		// final JdbcConnectionSource connectionSource = new JdbcConnectionSource(DB_URL);
		// TableUtils.dropTable(connectionSource, SqrlIdentity.class, true);
		// TableUtils.dropTable(connectionSource, SqrlIdentityData.class, true);
		// TableUtils.dropTable(connectionSource, SqrlUsedNutToken.class, true);
		// SqrlDatabase.initialize(DB_URL);
	}

}
