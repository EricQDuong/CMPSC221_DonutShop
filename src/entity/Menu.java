package entity;

/**
 * One row of the OD_Menu table (a donut, drink, or food item).
 */
public class Menu {
    private int ID;
    private String itemName;
    private String category;
    private double price;

    public Menu(int ID, String itemName, String category, double price) {
        this.ID = ID;
        this.itemName = itemName;
        this.category = category;
        this.price = price;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // This is what a JList shows for each item, e.g. "Glazed Donut — $1.49"
    @Override
    public String toString() {
        return itemName + " — $" + String.format("%.2f", price);
    }
}