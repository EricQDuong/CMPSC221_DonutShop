package program;

import entity.Menu;
import entity.MenuDAO;
import entity.Order;
import entity.OrderDAO;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Oak Donuts (OD) - menu and ordering system.
 *
 * Main.form (IntelliJ Swing UI Designer) holds the layout and creates the
 * components declared below. This class holds the behavior.
 *
 * IMPORTANT: Run it from IntelliJ so the form gets compiled into this class.
 *
 * Tabs:
 *   Order         - pick items, build an order, Checkout saves it (Create)
 *   Manage Menu   - insert / update / delete menu items (full CRUD on OD_Menu)
 *   Order History - view, update and delete saved orders (Read/Update/Delete on OD_Order)
 */
public class Main extends JFrame {
    private static final String[] CATEGORIES = {"Donut", "Drink", "Food"};
    private static final String[] FILTER_CATEGORIES = {"All", "Donut", "Drink", "Food"};
    private static final String[] ICINGS = {"None", "Chocolate", "Vanilla", "Strawberry", "Maple"};
    private static final String[] FILLINGS = {"None", "Custard", "Jelly", "Cream", "Lemon"};
    private static final double TAX_RATE = 0.06;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MenuDAO menuDAO = new MenuDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    // The order being built on the Order tab. It lives in memory until Checkout saves it.
    private final List<CartLine> cart = new ArrayList<>();
    private final DefaultListModel<Menu> menuListModel = new DefaultListModel<>();

    // ---- Components created by Main.form. Each name must match a "field name" in the form. ----
    private JPanel rootPanel;
    private JTabbedPane jTabbedPaneOakDonuts;

    // Order tab
    private JComboBox<String> jComboBoxCategory;
    private JTextField jTextFieldSearch;
    private JComboBox<String> jComboBoxIcing;
    private JComboBox<String> jComboBoxFilling;
    private JList<Menu> jListMenu;
    private JSpinner jSpinnerQty;
    private JLabel jLabelUnitPrice;
    private JButton jButtonAddToOrder;
    private JTable jTableCart;
    private JLabel jLabelSubtotal;
    private JLabel jLabelTax;
    private JLabel jLabelTotal;
    private JButton jButtonRemoveItem;
    private JButton jButtonClear;
    private JButton jButtonCheckout;

    // Manage Menu tab
    private JTextField jTextFieldMenuID;
    private JTextField jTextFieldMenuName;
    private JComboBox<String> jComboBoxMenuCategory;
    private JTextField jTextFieldMenuPrice;
    private JButton jButtonInsertMenu;
    private JButton jButtonUpdateMenu;
    private JButton jButtonDeleteMenu;
    private JButton jButtonClearMenu;
    private JTable jTableMenu;

    // Order History tab
    private JTextField jTextFieldTransactionID;
    private JTextField jTextFieldOrderDate;
    private JTextField jTextFieldOrderItems;
    private JTextField jTextFieldOrderTotal;
    private JButton jButtonUpdateOrder;
    private JButton jButtonDeleteOrder;
    private JButton jButtonClearOrder;
    private JTable jTableOrders;

    public Main() {
        // rootPanel and every component above are created by IntelliJ from Main.form.
        setContentPane(rootPanel);
        setTitle("Oak Donuts (OD) - Ordering System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setSize(1150, 640);
        setLocationRelativeTo(null);

        setUpOrderTab();
        setUpMenuTab();
        setUpHistoryTab();

        refreshMenuList();
        refreshMenuTable();
        refreshOrdersTable();
        refreshCart();
    }

    // =====================================================================
    // Setup
    // =====================================================================

    private void setUpOrderTab() {
        jComboBoxCategory.setModel(new DefaultComboBoxModel<>(FILTER_CATEGORIES));
        jComboBoxIcing.setModel(new DefaultComboBoxModel<>(ICINGS));
        jComboBoxFilling.setModel(new DefaultComboBoxModel<>(FILLINGS));
        jSpinnerQty.setModel(new SpinnerNumberModel(1, 1, 99, 1));
        jListMenu.setModel(menuListModel);
        jListMenu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        jTableCart.setModel(readOnlyModel("Item", "Options", "Qty", "Price", "Total"));
        jTableCart.getColumnModel().getColumn(1).setPreferredWidth(220);
        jTableCart.setFillsViewportHeight(true);

        // Filtering: changing the category or typing in Search re-filters the menu list.
        jComboBoxCategory.addActionListener(e -> refreshMenuList());
        jTextFieldSearch.getDocument().addDocumentListener(onTextChange(this::refreshMenuList));

        jListMenu.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedItem();
        });
        // Double-clicking a menu item is a shortcut for "Add to Order".
        jListMenu.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) addToOrder();
            }
        });

        jButtonAddToOrder.addActionListener(e -> addToOrder());
        jButtonRemoveItem.addActionListener(e -> removeCartLine());
        jButtonClear.addActionListener(e -> clearCart());
        jButtonCheckout.addActionListener(e -> checkout());
    }

    private void setUpMenuTab() {
        jComboBoxMenuCategory.setModel(new DefaultComboBoxModel<>(CATEGORIES));
        jTableMenu.setModel(readOnlyModel("Item ID", "Item Name", "Category", "Price ($)"));
        jTableMenu.setFillsViewportHeight(true);

        jButtonInsertMenu.addActionListener(e -> insertMenuItem());
        jButtonUpdateMenu.addActionListener(e -> updateMenuItem());
        jButtonDeleteMenu.addActionListener(e -> deleteMenuItem());
        jButtonClearMenu.addActionListener(e -> clearMenuFields());

        // Selecting a table row copies its values into the edit fields.
        jTableMenu.getSelectionModel().addListSelectionListener(e -> {
            int row = jTableMenu.getSelectedRow();
            if (e.getValueIsAdjusting() || row < 0) return;
            int r = jTableMenu.convertRowIndexToModel(row);
            jTextFieldMenuID.setText(cell(jTableMenu, r, 0));
            jTextFieldMenuName.setText(cell(jTableMenu, r, 1));
            jComboBoxMenuCategory.setSelectedItem(cell(jTableMenu, r, 2));
            jTextFieldMenuPrice.setText(cell(jTableMenu, r, 3));
        });
    }

    private void setUpHistoryTab() {
        // The transaction ID comes from the selected row, so it can't be typed over by mistake.
        jTextFieldTransactionID.setEditable(false);
        jTableOrders.setModel(readOnlyModel("Transaction ID", "Date", "Items", "Total ($)"));
        jTableOrders.getColumnModel().getColumn(2).setPreferredWidth(360);
        jTableOrders.setFillsViewportHeight(true);

        jButtonUpdateOrder.addActionListener(e -> updateOrder());
        jButtonDeleteOrder.addActionListener(e -> deleteOrder());
        jButtonClearOrder.addActionListener(e -> clearOrderFields());

        jTableOrders.getSelectionModel().addListSelectionListener(e -> {
            int row = jTableOrders.getSelectedRow();
            if (e.getValueIsAdjusting() || row < 0) return;
            int r = jTableOrders.convertRowIndexToModel(row);
            jTextFieldTransactionID.setText(cell(jTableOrders, r, 0));
            jTextFieldOrderDate.setText(cell(jTableOrders, r, 1));
            jTextFieldOrderItems.setText(cell(jTableOrders, r, 2));
            jTextFieldOrderTotal.setText(cell(jTableOrders, r, 3));
        });
    }

    // The File / Category / Help menu bar. Swing UI Designer can't build menu bars, so it's done in code.
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem newOrder = new JMenuItem("New Order");
        newOrder.addActionListener(e -> {
            clearCart();
            jTabbedPaneOakDonuts.setSelectedIndex(0);
        });
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        file.add(newOrder);
        file.addSeparator();
        file.add(exit);

        JMenu category = new JMenu("Category");
        for (String name : FILTER_CATEGORIES) {
            JMenuItem item = new JMenuItem(name);
            item.addActionListener(e -> {
                jComboBoxCategory.setSelectedItem(name);
                jTabbedPaneOakDonuts.setSelectedIndex(0);
            });
            category.add(item);
        }

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Oak Donuts (OD) - Menu & Ordering System\n\n"
                        + "Order: pick an item, set options and quantity, Add to Order, then Checkout.\n"
                        + "Manage Menu: add, change or remove menu items.\n"
                        + "Order History: select a past order to update or delete it.\n\n"
                        + "Data is stored in an Apache Derby database (donutstanddb).",
                "About Oak Donuts", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(file);
        bar.add(category);
        bar.add(help);
        return bar;
    }

    // =====================================================================
    // Order tab
    // =====================================================================

    /** Reloads the menu list from the database, applying the Category filter and Search text. */
    private void refreshMenuList() {
        Menu previouslySelected = jListMenu.getSelectedValue();
        String category = (String) jComboBoxCategory.getSelectedItem();
        String search = jTextFieldSearch.getText().trim().toLowerCase();

        menuListModel.clear();
        for (Menu item : allMenuItems()) {
            boolean categoryMatches = "All".equals(category) || item.getCategory().equalsIgnoreCase(category);
            boolean searchMatches = item.getItemName().toLowerCase().contains(search);
            if (categoryMatches && searchMatches) {
                menuListModel.addElement(item);
            }
        }

        // Keep the same item selected if it's still in the list.
        if (previouslySelected != null) {
            for (int i = 0; i < menuListModel.size(); i++) {
                if (menuListModel.get(i).getID() == previouslySelected.getID()) {
                    jListMenu.setSelectedIndex(i);
                    break;
                }
            }
        }
        showSelectedItem();
    }

    /** Shows the selected item's price; icing and filling only apply to donuts. */
    private void showSelectedItem() {
        Menu item = jListMenu.getSelectedValue();
        jLabelUnitPrice.setText("Unit: " + money(item == null ? 0 : item.getPrice()));
        boolean isDonut = item != null && item.getCategory().equalsIgnoreCase("Donut");
        jComboBoxIcing.setEnabled(isDonut);
        jComboBoxFilling.setEnabled(isDonut);
    }

    private void addToOrder() {
        Menu item = jListMenu.getSelectedValue();
        if (item == null) {
            showError("Pick an item from the menu first.");
            return;
        }
        int qty = (Integer) jSpinnerQty.getValue();
        String options = selectedOptions(item);

        // Same item with the same options? Just increase the quantity on that line.
        boolean merged = false;
        for (CartLine line : cart) {
            if (line.item.getID() == item.getID() && line.options.equals(options)) {
                line.qty += qty;
                merged = true;
                break;
            }
        }
        if (!merged) cart.add(new CartLine(item, options, qty));

        // Reset the choices so they don't carry over to the next item by accident.
        jComboBoxIcing.setSelectedIndex(0);
        jComboBoxFilling.setSelectedIndex(0);
        jSpinnerQty.setValue(1);
        refreshCart();
    }

    private String selectedOptions(Menu item) {
        if (!item.getCategory().equalsIgnoreCase("Donut")) return "-";
        List<String> parts = new ArrayList<>();
        String icing = (String) jComboBoxIcing.getSelectedItem();
        String filling = (String) jComboBoxFilling.getSelectedItem();
        if (!"None".equals(icing)) parts.add("Icing: " + icing);
        if (!"None".equals(filling)) parts.add("Filling: " + filling);
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private void removeCartLine() {
        int row = jTableCart.getSelectedRow();
        if (row < 0) {
            showError("Select a line in the order to remove it.");
            return;
        }
        cart.remove(jTableCart.convertRowIndexToModel(row));
        refreshCart();
    }

    private void clearCart() {
        cart.clear();
        jSpinnerQty.setValue(1);
        refreshCart();
    }

    /** Redraws the order table and the Subtotal / Tax / Total labels. */
    private void refreshCart() {
        DefaultTableModel model = (DefaultTableModel) jTableCart.getModel();
        model.setRowCount(0);
        for (CartLine line : cart) {
            model.addRow(new Object[]{
                    line.item.getItemName(), line.options, line.qty,
                    money(line.item.getPrice()), money(line.total())
            });
        }
        double subtotal = cartSubtotal();
        double tax = round2(subtotal * TAX_RATE);
        jLabelSubtotal.setText(money(subtotal));
        jLabelTax.setText(money(tax));
        jLabelTotal.setText(money(round2(subtotal + tax)));
    }

    private double cartSubtotal() {
        double subtotal = 0;
        for (CartLine line : cart) subtotal += line.total();
        return round2(subtotal);
    }

    /** Saves the current order to OD_Order with a new transaction ID and today's date. */
    private void checkout() {
        if (cart.isEmpty()) {
            showError("The order is empty. Add something first.");
            return;
        }
        execute("Checkout", () -> {
            double subtotal = cartSubtotal();
            double total = round2(subtotal + round2(subtotal * TAX_RATE));

            List<String> lines = new ArrayList<>();
            for (CartLine line : cart) lines.add(line.describe());
            String items = String.join("; ", lines);
            if (items.length() > 1000)
                throw new IllegalArgumentException("this order has too many lines to save. Split it into two orders.");

            int transactionID = orderDAO.nextTransactionID();
            Order order = new Order(transactionID, LocalDateTime.now().format(DATE_FORMAT), items, total);
            orderDAO.insert(order);
            if (!exists(orderDAO.get(transactionID)))
                throw new IllegalStateException("the order could not be saved. Check the console for the database error.");

            JOptionPane.showMessageDialog(this,
                    "Thank you!\nTransaction #" + transactionID + "\nTotal due: " + money(total)
                            + "\n(This is a mockup - no payment processed.)",
                    "Checkout", JOptionPane.INFORMATION_MESSAGE);
            clearCart();
            refreshOrdersTable();
        });
    }

    // =====================================================================
    // Manage Menu tab (CRUD on OD_Menu)
    // =====================================================================

    private Menu menuItemFromForm() {
        return new Menu(
                positiveNumber(jTextFieldMenuID, "Item ID"),
                maxLength(required(jTextFieldMenuName, "Item name"), 40, "Item name"),
                (String) jComboBoxMenuCategory.getSelectedItem(),
                price(jTextFieldMenuPrice, "Price", 9999.99));
    }

    private void insertMenuItem() {
        execute("Insert menu item", () -> {
            Menu item = menuItemFromForm();
            if (exists(menuDAO.get(item.getID())))
                throw new IllegalArgumentException("Item ID " + item.getID() + " already exists.");
            menuDAO.insert(item);
            menuChanged();
            clearMenuFields();
        });
    }

    private void updateMenuItem() {
        execute("Update menu item", () -> {
            Menu item = menuItemFromForm();
            if (!exists(menuDAO.get(item.getID())))
                throw new IllegalArgumentException("there is no menu item with ID " + item.getID() + ".");
            menuDAO.update(item);
            menuChanged();
        });
    }

    private void deleteMenuItem() {
        execute("Delete menu item", () -> {
            int id = positiveNumber(jTextFieldMenuID, "Item ID");
            Optional<Menu> found = menuDAO.get(id);
            if (!exists(found))
                throw new IllegalArgumentException("there is no menu item with ID " + id + ".");
            if (!confirm("Delete \"" + found.get().getItemName() + "\" from the menu?")) return;
            menuDAO.delete(found.get());
            menuChanged();
            clearMenuFields();
        });
    }

    /** After any menu change, refresh both the Manage Menu table and the Order tab list. */
    private void menuChanged() {
        refreshMenuTable();
        refreshMenuList();
    }

    private void refreshMenuTable() {
        DefaultTableModel model = (DefaultTableModel) jTableMenu.getModel();
        model.setRowCount(0);
        for (Menu item : allMenuItems()) {
            model.addRow(new Object[]{
                    item.getID(), item.getItemName(), item.getCategory(),
                    String.format("%.2f", item.getPrice())
            });
        }
    }

    private void clearMenuFields() {
        jTableMenu.clearSelection();
        jTextFieldMenuID.setText("");
        jTextFieldMenuName.setText("");
        jComboBoxMenuCategory.setSelectedIndex(0);
        jTextFieldMenuPrice.setText("");
    }

    // =====================================================================
    // Order History tab (Read / Update / Delete on OD_Order)
    // =====================================================================

    private int selectedTransactionID() {
        String text = jTextFieldTransactionID.getText().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("select an order in the table first.");
        return Integer.parseInt(text);
    }

    private void updateOrder() {
        execute("Update order", () -> {
            int id = selectedTransactionID();
            Order order = new Order(id,
                    orderDate(jTextFieldOrderDate),
                    maxLength(required(jTextFieldOrderItems, "Items"), 1000, "Items"),
                    price(jTextFieldOrderTotal, "Total", 999999.99));
            if (!exists(orderDAO.get(id)))
                throw new IllegalArgumentException("order #" + id + " no longer exists.");
            orderDAO.update(order);
            refreshOrdersTable();
        });
    }

    private void deleteOrder() {
        execute("Delete order", () -> {
            int id = selectedTransactionID();
            Optional<Order> found = orderDAO.get(id);
            if (!exists(found))
                throw new IllegalArgumentException("order #" + id + " no longer exists.");
            if (!confirm("Delete order #" + id + "?")) return;
            orderDAO.delete(found.get());
            refreshOrdersTable();
            clearOrderFields();
        });
    }

    private void refreshOrdersTable() {
        DefaultTableModel model = (DefaultTableModel) jTableOrders.getModel();
        model.setRowCount(0);
        List<Order> orders = orderDAO.getAll();
        if (orders == null) return;
        for (Order o : orders) {
            model.addRow(new Object[]{
                    o.getTransactionID(), o.getOrderDate(), o.getItems(),
                    String.format("%.2f", o.getTotal())
            });
        }
    }

    private void clearOrderFields() {
        jTableOrders.clearSelection();
        jTextFieldTransactionID.setText("");
        jTextFieldOrderDate.setText("");
        jTextFieldOrderItems.setText("");
        jTextFieldOrderTotal.setText("");
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** One line of the current order: which item, its options, and how many. */
    private static class CartLine {
        final Menu item;
        final String options;
        int qty;

        CartLine(Menu item, String options, int qty) {
            this.item = item;
            this.options = options;
            this.qty = qty;
        }

        double total() {
            return round2(item.getPrice() * qty);
        }

        // e.g. "2x Boston Creme (Icing: Chocolate, Filling: Custard)"
        String describe() {
            return qty + "x " + item.getItemName() + ("-".equals(options) ? "" : " (" + options + ")");
        }
    }

    private List<Menu> allMenuItems() {
        List<Menu> items = menuDAO.getAll();
        return items == null ? new ArrayList<>() : items;
    }

    // The DAOs return null instead of an Optional when the database reports an error.
    private static boolean exists(Optional<?> result) {
        return result != null && result.isPresent();
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String cell(JTable table, int row, int column) {
        Object value = table.getModel().getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private static DocumentListener onTextChange(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String money(double value) {
        return String.format("$%.2f", value);
    }

    private static String required(JTextField field, String label) {
        String value = field.getText().trim();
        if (value.isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return value;
    }

    private static String maxLength(String value, int max, String label) {
        if (value.length() > max)
            throw new IllegalArgumentException(label + " can be at most " + max + " characters.");
        return value;
    }

    private static int positiveNumber(JTextField field, String label) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number greater than 0.");
        }
    }

    private static double price(JTextField field, String label, double max) {
        String text = required(field, label).replace("$", "").trim();
        try {
            double value = Double.parseDouble(text);
            if (value < 0 || value > max) throw new NumberFormatException();
            return round2(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number from 0 to " + max + " (example: 1.99).");
        }
    }

    private static String orderDate(JTextField field) {
        String text = required(field, "Date");
        if (text.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) text += ":00";
        try {
            return LocalDateTime.parse(text, DATE_FORMAT).format(DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Date must look like 2026-11-15 09:30:00.");
        }
    }

    /** Runs a button action and shows any validation problem as a pop-up instead of crashing. */
    private void execute(String action, Runnable work) {
        try {
            work.run();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(action + ": " + ex.getMessage());
        } catch (RuntimeException ex) {
            showError(action + " failed: " + ex);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Oak Donuts", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirm(String question) {
        return JOptionPane.showConfirmDialog(this, question, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static void main(String[] args) {
        // Use the native Windows look instead of the default Swing look.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // If it isn't available, Swing's default look is used.
        }
        // Swing windows must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            try {
                new Main().setVisible(true);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null, ex.toString(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
    }
}