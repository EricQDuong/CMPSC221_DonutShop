package entity;

/**
 * One row of the OD_Order table: a completed checkout.
 */
public class Order
{
    private int transactionID;
    private String orderDate;   // "yyyy-MM-dd HH:mm:ss"
    private String items;       // e.g. "2x Boston Creme (Icing: Chocolate, Filling: Custard); 2x Glazed Donut"
    private double total;

    public Order(int transactionID, String orderDate, String items, double total)
    {
        this.transactionID = transactionID;
        this.orderDate = orderDate;
        this.items = items;
        this.total = total;
    }

    public int getTransactionID() {
        return transactionID;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public String getItems() {
        return items;
    }

    public double getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return "Order{" + "transactionID=" + transactionID + ", orderDate=" + orderDate + ", items=" + items + ", total=" + total + '}';
    }
}