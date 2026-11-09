package core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Embedded Apache Derby database and one-time schema initialization. */
public class DB {
    private static final String DB_URL =
            "jdbc:derby:donutstanddb;create=true;user=donutstanddb;password=donutstanddb";
    private static DB instance;
    private final Connection mConnection;

    private DB() throws SQLException {
        mConnection = DriverManager.getConnection(DB_URL);
        try {
            initializeSchema();
        } catch (SQLException ex) {
            try { mConnection.close(); } catch (SQLException closeEx) { ex.addSuppressed(closeEx); }
            throw ex;
        }
    }

    public static synchronized DB getInstance() {
        if (instance == null) {
            try {
                instance = new DB();
            } catch (SQLException ex) {
                throw new IllegalStateException("Unable to initialize Apache Derby database", ex);
            }
        }
        return instance;
    }

    private boolean tableExists(String table) throws SQLException {
        // Derby stores unquoted identifiers in uppercase.
        try (ResultSet rs = mConnection.getMetaData().getTables(
                null, null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void initializeSchema() throws SQLException {
        // Only creates tables that don't exist yet, so saved data survives restarts.
        if (!tableExists("OD_MENU")) {
            try (Statement st = mConnection.createStatement()) {
                st.executeUpdate("CREATE TABLE OD_Menu (" +
                        "Item_ID INT NOT NULL PRIMARY KEY, " +
                        "Item_Name VARCHAR(40) NOT NULL, " +
                        "Category VARCHAR(20) NOT NULL, " +
                        "Price DECIMAL(6,2) NOT NULL)");

                // Starting menu, so the Menu list isn't empty on first launch.
                st.executeUpdate("INSERT INTO OD_Menu VALUES " +
                        "(1, 'Glazed Donut', 'Donut', 1.49), " +
                        "(2, 'Chocolate Sprinkles', 'Donut', 1.79), " +
                        "(3, 'Boston Creme', 'Donut', 1.99), " +
                        "(4, 'House Coffee', 'Drink', 2.00), " +
                        "(5, 'Latte', 'Drink', 3.00), " +
                        "(6, 'Breakfast Sandwich', 'Food', 4.50)");
            }
        }
        if (!tableExists("OD_ORDER")) {
            try (Statement st = mConnection.createStatement()) {
                st.executeUpdate("CREATE TABLE OD_Order (" +
                        "Transaction_ID INT NOT NULL PRIMARY KEY, " +
                        "Order_Date TIMESTAMP NOT NULL, " +
                        "Items VARCHAR(1000) NOT NULL, " +
                        "Total DECIMAL(8,2) NOT NULL)");
            }
        }
    }

    public int executeUpdate(String sql) throws SQLException {
        try (Statement st = mConnection.createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    // Caller must close the returned ResultSet and its associated Statement.
    public ResultSet executeQuery(String sql) throws SQLException {
        return mConnection.createStatement().executeQuery(sql);
    }

    public PreparedStatement getPreparedStatement(String sql) throws SQLException {
        return mConnection.prepareStatement(sql);
    }
}
