/**
 * Project: DonutShop
 * Purpose Details: Holds the sales totals shown on the Sales Summary tab
 *                  (number of orders, revenue, average order, and today's
 *                  sales). Filled in by OrderDAO.getSalesSummary().
 * Course: CMPSC 221
 * Author: Eric Duong
 * Date Developed: 11/15/2026
 * Last Date Changed: 11/15/2026
 * Revision: 1.0
 */
package entity;

/**
 * A read-only report object. It is not a table, so it has no DAO of its own.
 */
public class SalesSummary
{
    private final int orderCount;
    private final double revenue;
    private final double averageOrder;
    private final int todayOrderCount;
    private final double todayRevenue;

    public SalesSummary(int orderCount, double revenue, double averageOrder,
                        int todayOrderCount, double todayRevenue)
    {
        this.orderCount = orderCount;
        this.revenue = revenue;
        this.averageOrder = averageOrder;
        this.todayOrderCount = todayOrderCount;
        this.todayRevenue = todayRevenue;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public double getRevenue() {
        return revenue;
    }

    public double getAverageOrder() {
        return averageOrder;
    }

    public int getTodayOrderCount() {
        return todayOrderCount;
    }

    public double getTodayRevenue() {
        return todayRevenue;
    }
}
