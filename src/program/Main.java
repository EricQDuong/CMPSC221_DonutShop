/**
 * Project: DonutShop
 * Purpose Details: Main window (JFrame) of the Oak Donuts ordering system.
 *                  Its layout comes from Main.form; this class handles every
 *                  user action: building an order and checking out, managing
 *                  menu items and their pictures, viewing, updating, and
 *                  deleting past orders, and the Sales Summary report. Also
 *                  applies the app's color theme.
 * Course: CMPSC 221
 * Author: Eric Duong
 * Date Developed: 11/8/2026
 * Last Date Changed: 11/15/2026
 * Revision: 1.1
 */
package program;

import entity.Menu;
import entity.MenuDAO;
import entity.Order;
import entity.OrderDAO;
import entity.SalesSummary;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.plaf.UIResource;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Oak Donuts (OD) - menu and ordering system.
 *
 * Main.form (IntelliJ Swing UI Designer) holds the layout and creates the
 * components declared below. This class holds the behavior.
 *
 * IMPORTANT: Run it from IntelliJ so the form gets compiled into this class.
 *
 * Tabs:
 *   Place Order   - pick items, build an order, Checkout saves it (Create)
 *   Manage Menu   - insert / update / delete menu items (full CRUD on OD_Menu),
 *                   plus a picture for each item
 *   Order History - view, update and delete saved orders (Read/Update/Delete on OD_Order)
 *   Sales Summary - totals and best sellers, calculated with SQL COUNT / SUM / AVG
 *
 * Item pictures are PNG files in the project's "images" folder, named by Item ID
 * (images/1.png is Glazed Donut). Items without a picture show the donut logo.
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

    // Item pictures: images/<Item_ID>.png, next to the donutstanddb folder.
    private static final File PICTURE_FOLDER = new File("images");
    private final Map<String, ImageIcon> pictureCache = new HashMap<>();
    private final BarChart bestSellerChart = new BarChart();

    // ---- Components created by Main.form. Each name must match a "field name" in the form. ----
    private JPanel rootPanel;
    private JTabbedPane jTabbedPaneOakDonuts;
    private JLabel jLabelTitle;

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
    private JLabel jLabelItemPicture;

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
    private JLabel jLabelMenuPicture;
    private JButton jButtonChoosePicture;

    // Order History tab
    private JTextField jTextFieldTransactionID;
    private JTextField jTextFieldOrderDate;
    private JTextField jTextFieldOrderItems;
    private JTextField jTextFieldOrderTotal;
    private JButton jButtonUpdateOrder;
    private JButton jButtonDeleteOrder;
    private JButton jButtonClearOrder;
    private JTable jTableOrders;

    // Sales Summary tab
    private JLabel jLabelStatOrders;
    private JLabel jLabelStatRevenue;
    private JLabel jLabelStatAverage;
    private JLabel jLabelStatToday;
    private JLabel jLabelStatTodayCount;
    private JPanel jPanelBestSellers;

    public Main() {
        // rootPanel and every component above are created by IntelliJ from Main.form.
        setContentPane(rootPanel);
        setTitle("Oak Donuts (OD) - Ordering System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setSize(1150, 700);
        setLocationRelativeTo(null);

        setUpOrderTab();
        setUpMenuTab();
        setUpHistoryTab();
        setUpSalesTab();
        setUpTooltips();
        applyTheme();

        refreshMenuList();
        refreshMenuTable();
        refreshOrdersTable();
        refreshCart();
        refreshSalesSummary();
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

        // Each row of the menu list shows a small picture of the item.
        jListMenu.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setIcon(pictureIcon(((Menu) value).getID(), 36));
                setIconTextGap(10);
                return this;
            }
        });
        // The big picture under Item Options shows the selected item, with its name underneath.
        jLabelItemPicture.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelItemPicture.setHorizontalTextPosition(SwingConstants.CENTER);
        jLabelItemPicture.setVerticalTextPosition(SwingConstants.BOTTOM);
    }

    private void setUpMenuTab() {
        jComboBoxMenuCategory.setModel(new DefaultComboBoxModel<>(CATEGORIES));
        jTableMenu.setModel(readOnlyModel("Item ID", "Item Name", "Category", "Price ($)"));
        jTableMenu.setFillsViewportHeight(true);

        jButtonInsertMenu.addActionListener(e -> insertMenuItem());
        jButtonUpdateMenu.addActionListener(e -> updateMenuItem());
        jButtonDeleteMenu.addActionListener(e -> deleteMenuItem());
        jButtonClearMenu.addActionListener(e -> clearMenuFields());
        jButtonChoosePicture.addActionListener(e -> choosePicture());

        // Typing or selecting an Item ID shows that item's picture.
        jLabelMenuPicture.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelMenuPicture.setHorizontalTextPosition(SwingConstants.CENTER);
        jLabelMenuPicture.setVerticalTextPosition(SwingConstants.BOTTOM);
        jTextFieldMenuID.getDocument().addDocumentListener(onTextChange(this::showMenuPicture));
        showMenuPicture();

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
                        + "Manage Menu: add, change or remove menu items, and choose their pictures.\n"
                        + "Order History: select a past order to update or delete it.\n"
                        + "Sales Summary: order count, revenue, average order and best sellers.\n\n"
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

        if (item == null) {
            jLabelItemPicture.setIcon(null);
            jLabelItemPicture.setText("Pick an item");
        } else {
            jLabelItemPicture.setIcon(pictureIcon(item.getID(), 150));
            jLabelItemPicture.setText(item.getItemName());
        }
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

    /** Shows the picture for the Item ID in the field, or a hint when no ID is entered. */
    private void showMenuPicture() {
        int id;
        try {
            id = Integer.parseInt(jTextFieldMenuID.getText().trim());
        } catch (NumberFormatException e) {
            id = -1;
        }
        if (id <= 0) {
            jLabelMenuPicture.setIcon(null);
            jLabelMenuPicture.setText("Pick an item to see its picture");
        } else {
            jLabelMenuPicture.setIcon(pictureIcon(id, 110));
            jLabelMenuPicture.setText(pictureFile(id).isFile() ? "" : "No picture yet");
        }
    }

    /** Lets the user pick an image file and saves it as this item's picture. */
    private void choosePicture() {
        execute("Choose picture", () -> {
            int id = positiveNumber(jTextFieldMenuID, "Item ID");
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose a picture for item " + id);
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Pictures (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                savePicture(id, chooser.getSelectedFile());
            }
        });
    }

    /** Copies the picture into images/<id>.png (shrunk to at most 512 px) so it stays with the project. */
    private void savePicture(int itemID, File source) {
        BufferedImage picture;
        try {
            picture = ImageIO.read(source);
        } catch (IOException e) {
            picture = null;
        }
        if (picture == null)
            throw new IllegalArgumentException("that file isn't a picture Java can open. Use a PNG, JPG, GIF or BMP.");

        double scale = Math.min(1.0, 512.0 / Math.max(picture.getWidth(), picture.getHeight()));
        int w = Math.max(1, (int) Math.round(picture.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(picture.getHeight() * scale));
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(new ImageIcon(picture.getScaledInstance(w, h, Image.SCALE_SMOOTH)).getImage(), 0, 0, null);
        g.dispose();

        try {
            Files.createDirectories(PICTURE_FOLDER.toPath());
            ImageIO.write(copy, "png", pictureFile(itemID));
        } catch (IOException e) {
            throw new IllegalStateException("the picture couldn't be saved: " + e.getMessage());
        }
        pictureCache.clear();          // forget the old picture so the new one shows everywhere
        jListMenu.repaint();
        showSelectedItem();
        showMenuPicture();
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
    // Sales Summary tab (SQL COUNT / SUM / AVG on OD_Order)
    // =====================================================================

    private void setUpSalesTab() {
        jPanelBestSellers.setLayout(new BorderLayout());
        jPanelBestSellers.add(bestSellerChart, BorderLayout.CENTER);
        bestSellerChart.setFont(jLabelStatOrders.getFont().deriveFont(Font.PLAIN, 14f));
        // Recalculate whenever a tab is opened, so the numbers are always current.
        jTabbedPaneOakDonuts.addChangeListener(e -> refreshSalesSummary());
    }

    private void refreshSalesSummary() {
        SalesSummary summary = orderDAO.getSalesSummary();
        jLabelStatOrders.setText(String.valueOf(summary.getOrderCount()));
        jLabelStatRevenue.setText(money(summary.getRevenue()));
        jLabelStatAverage.setText(money(summary.getAverageOrder()));
        jLabelStatToday.setText(money(summary.getTodayRevenue()));
        int today = summary.getTodayOrderCount();
        jLabelStatTodayCount.setText(today + (today == 1 ? " order today" : " orders today"));
        bestSellerChart.setData(itemsSold());
    }

    /** Adds up how many of each item were sold by reading every order's Items text, e.g. "2x Glazed Donut". */
    private Map<String, Integer> itemsSold() {
        Map<String, Integer> sold = new HashMap<>();
        List<Order> orders = orderDAO.getAll();
        if (orders == null) return sold;
        Pattern line = Pattern.compile("(?:^|;)\\s*(\\d{1,6})x ([^;(]+)");
        for (Order order : orders) {
            Matcher m = line.matcher(order.getItems());
            while (m.find()) {
                sold.merge(m.group(2).trim(), Integer.parseInt(m.group(1)), Integer::sum);
            }
        }
        return sold;
    }

    /** Horizontal bar chart of items sold, drawn with Graphics2D. The best seller is raspberry. */
    private static class BarChart extends JComponent {
        private List<Map.Entry<String, Integer>> rows = new ArrayList<>();

        void setData(Map<String, Integer> sold) {
            rows = new ArrayList<>(sold.entrySet());
            rows.sort((a, b) -> b.getValue() - a.getValue());          // most sold first
            if (rows.size() > 10) rows = new ArrayList<>(rows.subList(0, 10));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            if (rows.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("No orders yet. Check out an order and the best sellers show up here.",
                        12, 12 + fm.getAscent());
                g.dispose();
                return;
            }
            int labelWidth = 0;
            for (Map.Entry<String, Integer> row : rows) labelWidth = Math.max(labelWidth, fm.stringWidth(row.getKey()));
            int rowHeight = Math.max(fm.getHeight() + 8, Math.min(40, (getHeight() - 8) / rows.size()));
            int barLeft = labelWidth + 28;
            int barRoom = Math.max(20, getWidth() - barLeft - 60);
            int most = rows.get(0).getValue();
            for (int i = 0; i < rows.size(); i++) {
                int top = 4 + i * rowHeight;
                int barHeight = rowHeight - 10;
                int textY = top + (barHeight + fm.getAscent() - fm.getDescent()) / 2;
                int width = Math.max(4, (int) Math.round(rows.get(i).getValue() * (double) barRoom / most));
                g.setColor(CHOCOLATE);
                g.drawString(rows.get(i).getKey(), 12, textY);
                g.setColor(i == 0 ? RASPBERRY : CARAMEL);
                g.fillRoundRect(barLeft, top, width, barHeight, 8, 8);
                g.setColor(CHOCOLATE);
                g.drawString(String.valueOf(rows.get(i).getValue()), barLeft + width + 8, textY);
            }
            g.dispose();
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Short hints that pop up when the mouse rests on a control. */
    private void setUpTooltips() {
        jTextFieldSearch.setToolTipText("Type part of an item's name");
        jListMenu.setToolTipText("Double-click an item to add it to the order");
        jButtonAddToOrder.setToolTipText("Add the selected item to the order");
        jButtonRemoveItem.setToolTipText("Remove the selected line from the order");
        jButtonClear.setToolTipText("Empty the whole order");
        jButtonCheckout.setToolTipText("Save this order to the database");
        jButtonInsertMenu.setToolTipText("Add a new menu item with these values");
        jButtonUpdateMenu.setToolTipText("Save changes to the item with this ID");
        jButtonDeleteMenu.setToolTipText("Delete the item with this ID");
        jButtonClearMenu.setToolTipText("Clear the fields");
        jButtonChoosePicture.setToolTipText("Pick an image file to use as this item's picture");
        jButtonUpdateOrder.setToolTipText("Save changes to the selected order");
        jButtonDeleteOrder.setToolTipText("Delete the selected order");
        jButtonClearOrder.setToolTipText("Clear the fields");
    }

    private static File pictureFile(int itemID) {
        return new File(PICTURE_FOLDER, itemID + ".png");
    }

    /** The item's picture fitted into a size x size square. Items without a picture get the donut logo. */
    private ImageIcon pictureIcon(int itemID, int size) {
        return pictureCache.computeIfAbsent(itemID + "@" + size, key -> {
            File file = pictureFile(itemID);
            BufferedImage picture = null;
            if (file.isFile()) {
                try {
                    picture = ImageIO.read(file);
                } catch (IOException e) {
                    System.err.println("Couldn't read " + file + ": " + e.getMessage());
                }
            }
            return new ImageIcon(picture == null ? donutImage(size) : fit(picture, size));
        });
    }

    /** Shrinks a picture to fit inside a size x size square, keeping its shape, centered. */
    private static Image fit(BufferedImage picture, int size) {
        double scale = Math.min((double) size / picture.getWidth(), (double) size / picture.getHeight());
        int w = Math.max(1, (int) Math.round(picture.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(picture.getHeight() * scale));
        Image scaled = new ImageIcon(picture.getScaledInstance(w, h, Image.SCALE_SMOOTH)).getImage();
        BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = square.createGraphics();
        g.drawImage(scaled, (size - w) / 2, (size - h) / 2, null);
        g.dispose();
        return square;
    }

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

    // =====================================================================
    // Theme (colors, buttons, tables, donut icon)
    // =====================================================================

    private static final Color CHOCOLATE = new Color(78, 52, 46);
    private static final Color CREAM = new Color(255, 248, 236);
    private static final Color RASPBERRY = new Color(194, 24, 91);
    private static final Color CARAMEL = new Color(160, 97, 43);
    private static final Color SOFT_PINK = new Color(250, 212, 225);
    private static final Color LINE_TAN = new Color(222, 196, 160);

    private void applyTheme() {
        Image donut = donutImage(64);
        setIconImage(donut);                                    // window + taskbar icon
        jLabelTitle.setIcon(new ImageIcon(donut.getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        jLabelTitle.setIconTextGap(12);

        paintPanels(rootPanel, CREAM);
        jTabbedPaneOakDonuts.setFont(jTabbedPaneOakDonuts.getFont().deriveFont(Font.BOLD, 13f));

        // Main actions in raspberry, clear/remove in caramel, delete in chocolate.
        for (JButton b : new JButton[]{jButtonAddToOrder, jButtonCheckout, jButtonInsertMenu,
                jButtonUpdateMenu, jButtonUpdateOrder}) styleButton(b, RASPBERRY);
        for (JButton b : new JButton[]{jButtonRemoveItem, jButtonClear, jButtonClearMenu,
                jButtonClearOrder, jButtonChoosePicture}) styleButton(b, CARAMEL);
        for (JButton b : new JButton[]{jButtonDeleteMenu, jButtonDeleteOrder}) styleButton(b, CHOCOLATE);

        for (JTable t : new JTable[]{jTableCart, jTableMenu, jTableOrders}) styleTable(t);
        jTableOrders.getColumnModel().getColumn(1).setPreferredWidth(150);   // room for the full date

        jListMenu.setFont(jListMenu.getFont().deriveFont(14f));
        jListMenu.setFixedCellHeight(44);          // room for the item pictures
        jListMenu.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        jListMenu.setSelectionBackground(SOFT_PINK);
        jListMenu.setSelectionForeground(Color.BLACK);
    }

    /** Gives every panel you didn't color in the designer the same background. */
    private static void paintPanels(Container parent, Color color) {
        if (parent instanceof JPanel && parent.getBackground() instanceof UIResource) {
            parent.setBackground(color);
        }
        for (Component child : parent.getComponents()) {
            if (child instanceof JSpinner) continue;   // a spinner's text box is a panel too
            if (child instanceof Container) paintPanels((Container) child, color);
        }
    }

    /** Flat colored button. The Windows look ignores button colors unless the normal fill is turned off. */
    private static void styleButton(JButton b, Color color) {
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setContentAreaFilled(false);   // must come BEFORE setOpaque: it resets opaque to false
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setMargin(new Insets(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setRolloverEnabled(true);
        // Darker while pressed, slightly lighter on hover.
        b.getModel().addChangeListener(e -> {
            ButtonModel m = b.getModel();
            b.setBackground(m.isPressed() ? color.darker() : m.isRollover() ? blend(color, Color.WHITE, 0.15) : color);
        });
    }

    private static void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE_TAN);
        table.setSelectionBackground(SOFT_PINK);
        table.setSelectionForeground(Color.BLACK);

        // Chocolate header row with cream text.
        DefaultTableCellRenderer header = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                return this;
            }
        };
        header.setBackground(CHOCOLATE);
        header.setForeground(CREAM);
        header.setFont(table.getFont().deriveFont(Font.BOLD));
        header.setHorizontalAlignment(SwingConstants.CENTER);
        table.getTableHeader().setDefaultRenderer(header);
    }

    private static Color blend(Color a, Color b, double amount) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * amount),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
    }

    /** Draws a pink-frosted donut with sprinkles, so no image file is needed. */
    private static Image donutImage(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double s = size;

        g.setColor(new Color(214, 158, 90));                              // dough
        g.fill(new Ellipse2D.Double(s * .04, s * .04, s * .92, s * .92));
        g.setColor(new Color(240, 98, 146));                              // frosting
        g.fill(new Ellipse2D.Double(s * .11, s * .11, s * .78, s * .78));

        g.setStroke(new BasicStroke((float) (s * .05), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color[] sprinkles = {Color.WHITE, new Color(255, 214, 0), new Color(77, 182, 172)};
        for (int i = 0; i < 10; i++) {                                    // sprinkles around the ring
            double angle = i * Math.PI / 5, r = s * .27;
            double x = s / 2 + Math.cos(angle) * r, y = s / 2 + Math.sin(angle) * r;
            double dx = Math.cos(angle + 1.2) * s * .04, dy = Math.sin(angle + 1.2) * s * .04;
            g.setColor(sprinkles[i % 3]);
            g.draw(new Line2D.Double(x - dx, y - dy, x + dx, y + dy));
        }

        g.setComposite(AlphaComposite.Clear);                             // punch the hole
        g.fill(new Ellipse2D.Double(s * .36, s * .36, s * .28, s * .28));
        g.dispose();
        return img;
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
