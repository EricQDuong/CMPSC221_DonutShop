package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import core.*;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * CRUD operations for the OD_Menu table.
 */
public class MenuDAO implements DAO<Menu>
{
    public MenuDAO() {

    }
    List<Menu> menuItems;

    /**
     * Get a single menu item by its ID
     * @param id
     * @return
     */
    @Override
    public Optional<Menu> get(int id) {
        DB db = DB.getInstance();
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM OD_Menu WHERE Item_ID = ?";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            Menu item = null;
            while (rs.next()) {
                item = new Menu(rs.getInt("Item_ID"), rs.getString("Item_Name"), rs.getString("Category"), rs.getDouble("Price"));
            }
            return Optional.ofNullable(item);
        } catch (SQLException ex) {
            System.err.println(ex.toString());
            return null;
        }
    }

    /**
     * Get all menu items as a List
     * @return
     */
    @Override
    public List<Menu> getAll() {
        DB db = DB.getInstance();
        ResultSet rs = null;
        menuItems = new ArrayList<>();
        try {
            String sql = "SELECT * FROM OD_Menu ORDER BY Item_ID";
            rs = db.executeQuery(sql);
            Menu item = null;
            while (rs.next()) {
                item = new Menu(rs.getInt("Item_ID"), rs.getString("Item_Name"), rs.getString("Category"), rs.getDouble("Price"));
                menuItems.add(item);
            }
            return menuItems;
        } catch (SQLException ex) {
            System.err.println(ex.toString());
            return null;
        }
    }

    /**
     * Insert a menu item into the menu table
     * @param item
     */
    @Override
    public void insert(Menu item)
    {
        DB db = DB.getInstance();
        try {
            String sql = "INSERT INTO OD_Menu(Item_ID, Item_Name, Category, Price) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setInt(1, item.getID());
            stmt.setString(2, item.getItemName());
            stmt.setString(3, item.getCategory());
            stmt.setDouble(4, item.getPrice());
            int rowInserted = stmt.executeUpdate();
            if (rowInserted > 0) {
                System.out.println("A new menu item was inserted successfully!");
            }
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }
    }

    /**
     * Update an existing menu item, matched by its ID
     * @param item
     */
    @Override
    public void update(Menu item) {
        DB db = DB.getInstance();
        try {
            String sql = "UPDATE OD_Menu SET Item_Name=?, Category=?, Price=? WHERE Item_ID=?";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setString(1, item.getItemName());
            stmt.setString(2, item.getCategory());
            stmt.setDouble(3, item.getPrice());
            stmt.setInt(4, item.getID());
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("An existing menu item was updated successfully!");
            }
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }
    }

    /**
     * Delete a menu item from the menu table
     * @param item
     */
    @Override
    public void delete(Menu item) {
        DB db = DB.getInstance();
        try {
            String sql = "DELETE FROM OD_Menu WHERE Item_ID = ?";
            PreparedStatement stmt = db.getPreparedStatement(sql);
            stmt.setInt(1, item.getID());
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("A menu item was deleted successfully!");
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
            String sql = "SELECT * FROM OD_Menu WHERE Item_ID = -1";//We just need this sql query to get the column headers
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