/**
 * Project: DonutShop
 * Purpose Details: Data Access Object for the OD_Order table. Runs the SQL to
 *                  save, read, update, and delete orders, and picks the next
 *                  transaction ID at checkout.
 * Course: CMPSC 221
 * Author: Eric Duong
 * Date Developed: 11/8/2026
 * Last Date Changed: 11/10/2026
 * Revision: 1.0
 */
package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import core.*;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * CRUD operations for the OD_Order table.
 */
public class OrderDAO implements DAO<Order>
{
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrderDAO() {

    }
    List<Order> orders;

    // Builds an Order object from the current row of a result set.
    private Order fromRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("Order_Date");
        return new Order(rs.getInt("Transaction_ID"),
                ts.toLocalDateTime().format(DATE_FORMAT),
                rs.getString("Items"),
                rs.getDouble("Total"));
    }

    /**
     * Get a single order by its transaction ID
     * @param id
     * @return
     */
    @Override
    public Optional<Order> get(int id) {
        DB db = DB.getInstance();
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM OD_Order WHERE Transaction_ID = ?";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            Order order = null;
            while (rs.next()) {
                order = fromRow(rs);
            }
            return Optional.ofNullable(order);
        } catch (SQLException ex) {
            System.err.println(ex.toString());
            return null;
        }
    }

    /**
     * Get all orders as a List, oldest first
     * @return
     */
    @Override
    public List<Order> getAll() {
        DB db = DB.getInstance();
        ResultSet rs = null;
        orders = new ArrayList<>();
        try {
            String sql = "SELECT * FROM OD_Order ORDER BY Transaction_ID";
            rs = db.executeQuery(sql);
            while (rs.next()) {
                orders.add(fromRow(rs));
            }
            return orders;
        } catch (SQLException ex) {
            System.err.println(ex.toString());
            return null;
        }
    }

    /**
     * Next free transaction ID: 1001 for the first order, then highest + 1
     * @return
     */
    public int nextTransactionID() {
        DB db = DB.getInstance();
        try {
            ResultSet rs = db.executeQuery("SELECT MAX(Transaction_ID) FROM OD_Order");
            int max = rs.next() ? rs.getInt(1) : 0;   // getInt returns 0 when the table is empty
            return max == 0 ? 1001 : max + 1;
        } catch (SQLException ex) {
            System.err.println(ex.toString());
            return 1001;
        }
    }

    /**
     * Insert an order into the order table
     * @param order
     */
    @Override
    public void insert(Order order)
    {
        DB db = DB.getInstance();
        try {
            String sql = "INSERT INTO OD_Order(Transaction_ID, Order_Date, Items, Total) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setInt(1, order.getTransactionID());
            stmt.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
            stmt.setString(3, order.getItems());
            stmt.setDouble(4, order.getTotal());
            int rowInserted = stmt.executeUpdate();
            if (rowInserted > 0) {
                System.out.println("A new order was inserted successfully!");
            }
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }
    }

    /**
     * Update an existing order, matched by its transaction ID
     * @param order
     */
    @Override
    public void update(Order order) {
        DB db = DB.getInstance();
        try {
            String sql = "UPDATE OD_Order SET Order_Date=?, Items=?, Total=? WHERE Transaction_ID=?";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setTimestamp(1, Timestamp.valueOf(order.getOrderDate()));
            stmt.setString(2, order.getItems());
            stmt.setDouble(3, order.getTotal());
            stmt.setInt(4, order.getTransactionID());
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("An existing order was updated successfully!");
            }
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }
    }

    /**
     * Delete an order from the order table
     * @param order
     */
    @Override
    public void delete(Order order) {
        DB db = DB.getInstance();
        try {
            String sql = "DELETE FROM OD_Order WHERE Transaction_ID = ?";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setInt(1, order.getTransactionID());
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("An order was deleted successfully!");
            }
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }
    }

    /**
     * Get all column names in a list
     * @return
     */
    @Override
    public List<String> getColumnNames() {
        DB db = DB.getInstance();
        ResultSet rs = null;
        List<String> headers = new ArrayList<>();
        try {
            String sql = "SELECT * FROM OD_Order WHERE Transaction_ID = -1";//We just need this sql query to get the column headers
            rs = db.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            int numberCols = rsmd.getColumnCount();
            for (int i = 1; i <= numberCols; i++) {
                headers.add(rsmd.getColumnLabel(i));
            }
            return headers;
        } catch (SQLException ex) {
            System.err.println(ex.toString());
            return null;
        }
    }
}