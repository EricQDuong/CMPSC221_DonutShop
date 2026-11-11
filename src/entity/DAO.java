/**
 * Project: DonutShop
 * Purpose Details: Generic Data Access Object interface listing the CRUD
 *                  operations (get, getAll, insert, update, delete) that
 *                  every table class must implement. Adapted from the
 *                  HotDogStand class example.
 * Course: CMPSC 221
 * Author: Eric Duong
 * Date Developed: 11/8/2026
 * Last Date Changed: 11/10/2026
 * Revision: 1.0
 */
package entity;
import java.util.List;
import java.util.Optional;
/**
 *
 * @author Gokhan
 * @param <T>
 */
public interface DAO<T>
{
    //Optional is a container used to contain not-null objects.
    //https://www.tutorialspoint.com/java8/java8_optional_class.htm
    Optional<T> get(int id);
    List<T> getAll();
    void insert(T t);
    void update(T t);
    void delete(T t);
    List<String> getColumnNames();
}
