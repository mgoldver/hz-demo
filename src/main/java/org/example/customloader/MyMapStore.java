package org.example.customloader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import javax.sql.DataSource;

import com.hazelcast.map.MapStore;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Product;

@Slf4j
public class MyMapStore implements MapStore<Integer, Product>
{
    private final DataSource dataSource;

    public MyMapStore(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public Map<Integer, Product> loadAll(Collection<Integer> keys)
    {
        return Map.of();
    }

    @Override
    public Product load(Integer key)
    {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement sql = connection.prepareStatement("sql");)
        {
            sql.setInt(1, key);
            ResultSet resultSet = sql.executeQuery();
            return Product.builder()
                          .id(resultSet.getInt("id"))
                          .name(resultSet.getString("name"))
                          .description(resultSet.getString("description"))
                          .last_modified(resultSet.getTimestamp("last_modified")
                                                  .toLocalDateTime())
                          .build();
        }
        catch (Exception e)
        {
            log.warn("Error loading product with key: {}", key, e);
            return null;
        }
    }

    @Override
    public Iterable<Integer> loadAllKeys()
    {
        return null;
    }

    @Override
    public void store(Integer key, Product value)
    {

    }

    @Override
    public void storeAll(Map<Integer, Product> map)
    {

    }

    @Override
    public void delete(Integer key)
    {

    }

    @Override
    public void deleteAll(Collection<Integer> keys)
    {

    }
}
