package org.example.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;

@Slf4j
public class DatabaseServer
{

    private static final int DEFAULT_TCP_PORT = 9092;
    private static final int DEFAULT_WEB_PORT = 8089;

    private static Connection memTestDbConnection = null;

    public static void main(String[] args)
    {
        try
        {
            int tcpPort = getPort(args, "-tcp", DEFAULT_TCP_PORT);
            int webPort = getPort(args, "-web", DEFAULT_WEB_PORT);

            Server tcpServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", String.valueOf(tcpPort))
                                     .start();
            log.info("H2 TCP server started on port {} with URL: {}", tcpPort, tcpServer.getURL());

            Server webServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", String.valueOf(webPort))
                                     .start();
            log.info("H2 Web console server started on port {} with URL: {}", webPort, webServer.getURL());

            createAndHoldInMemoryTestDb();
            createCustomerTable();
            populateCustomerTable(1000);
            createProductTable();
            populateProductTable(100);
            createCustomerOrdersTable();
            populateCustomerOrdersTable(10_000, 100, 100);

            // JDBC URL for connecting to this server
            log.info("JDBC URL for applications: jdbc:h2:tcp://localhost:{}/mem:testdb", tcpPort);
            log.info("Press Ctrl+C to stop the server");

            Runtime.getRuntime()
                   .addShutdownHook(new Thread(() ->
                                               {
                                                   log.info("\nShutting down H2 servers...");
                                                   tcpServer.stop();
                                                   webServer.stop();
                                                   closeMemTestDbConnection();
                                                   log.info("H2 servers stopped successfully.");
                                               }));

            Thread.currentThread()
                  .join();
        }
        catch (SQLException e)
        {
            log.error("Failed to start H2 database server: {}", e.getMessage(), e);
            System.exit(1);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread()
                  .interrupt();
            log.info("H2 database server interrupted.");
        }
    }

    private static void createAndHoldInMemoryTestDb()
    {
        String jdbcUrl = "jdbc:h2:mem:testdb;MODE=MSSQLServer;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS dbo;";
        String username = "sa";
        String password = "";
        try
        {
            memTestDbConnection = DriverManager.getConnection(jdbcUrl, username, password);
            log.info("In-memory 'testdb' database has been created and is held open.");
        }
        catch (SQLException e)
        {
            log.error("Error creating and holding open 'mem:testdb': {}", e.getMessage(), e);
        }
    }

    private static void createCustomerTable() throws SQLException
    {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS dbo.customer (
                    id INT PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    age INT
                )
                """;
        try (Statement stmt = memTestDbConnection.createStatement())
        {
            stmt.execute(createTableSql);
        }
    }

    private static void populateCustomerTable(int count) throws SQLException
    {
        log.info("Populating in-memory 'testdb' with {} customers...", count);
        String countSql = "SELECT COUNT(*) FROM dbo.customer";
        try (Statement stmt = memTestDbConnection.createStatement();
             var rs = stmt.executeQuery(countSql))
        {
            if (rs.next() && rs.getInt(1) >= count)
            {
                return;
            }
        }

        String insertSql = "INSERT INTO dbo.customer (id, name, email, age) VALUES (?, ?, ?, ?)";
        Random random = new Random();
        try (PreparedStatement pstmt = memTestDbConnection.prepareStatement(insertSql))
        {
            memTestDbConnection.setAutoCommit(false);
            for (int i = 1; i <= count; i++)
            {
                pstmt.setInt(1, i);
                pstmt.setString(2, "Customer" + i);
                pstmt.setString(3, "customer" + i + "@example.com");
                pstmt.setInt(4, 18 + random.nextInt(50));
                pstmt.addBatch();

                if (i % 1000 == 0 || i == count)
                {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    memTestDbConnection.commit();
                }
            }
            memTestDbConnection.setAutoCommit(true);
        }
    }

    private static void createProductTable() throws SQLException
    {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS dbo.product (
                    id INT PRIMARY KEY,
                    name VARCHAR(100),
                    description VARCHAR(255),
                    price FLOAT NOT NULL DEFAULT 0.0,
                    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
        try (Statement stmt = memTestDbConnection.createStatement())
        {
            stmt.execute(createTableSql);
        }
    }

    private static void populateProductTable(int count) throws SQLException
    {
        log.info("Populating in-memory 'testdb' with {} products...", count);
        String countSql = "SELECT COUNT(*) FROM dbo.product";
        try (Statement stmt = memTestDbConnection.createStatement();
             var rs = stmt.executeQuery(countSql))
        {
            if (rs.next() && rs.getInt(1) >= count)
            {
                return;
            }
        }

        String insertSql = "INSERT INTO dbo.product (id, name, description, price) VALUES (?, ?, ?, ?)";
        Random random = new Random();
        try (PreparedStatement pstmt = memTestDbConnection.prepareStatement(insertSql))
        {
            memTestDbConnection.setAutoCommit(false);
            for (int i = 1; i <= count; i++)
            {
                pstmt.setInt(1, i);
                pstmt.setString(2, "Product" + i);
                pstmt.setString(3, "Description for product " + i);
                double price = 10.0 + random.nextDouble() * 990.0;
                pstmt.setFloat(4, (float) price);
                pstmt.addBatch();
                if (i % 1000 == 0 || i == count)
                {
                    pstmt.executeBatch();
                    memTestDbConnection.commit();
                }
            }
            memTestDbConnection.setAutoCommit(true);
        }
    }

    private static void createCustomerOrdersTable() throws SQLException
    {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS dbo.customer_orders (
                    id INT PRIMARY KEY,
                    customer_id INT,
                    product_id INT,
                    quantity INT,
                    order_date DATE,
                    FOREIGN KEY (customer_id) REFERENCES dbo.customer(id),
                    FOREIGN KEY (product_id) REFERENCES dbo.product(id)
                )
                """;
        try (Statement stmt = memTestDbConnection.createStatement())
        {
            stmt.execute(createTableSql);
        }
    }

    private static void populateCustomerOrdersTable(int count,
                                                    int customerMaxId, int productMaxId) throws SQLException
    {
        log.info("Populating in-memory 'testdb' with {} customer orders...", count);
        String countSql = "SELECT COUNT(*) FROM dbo.customer_orders";
        try (Statement stmt = memTestDbConnection.createStatement();
             var rs = stmt.executeQuery(countSql))
        {
            if (rs.next() && rs.getInt(1) >= count)
            {
                return;
            }
        }

        String insertSql = "INSERT INTO dbo.customer_orders (id, customer_id, product_id, quantity, order_date) VALUES (?, ?, ?, ?, ?)";
        Random random = new Random();
        java.sql.Date startDate = java.sql.Date.valueOf("2023-01-01");
        long startMillis = startDate.getTime();
        long endMillis = System.currentTimeMillis();

        try (PreparedStatement pstmt = memTestDbConnection.prepareStatement(insertSql))
        {
            memTestDbConnection.setAutoCommit(false);
            for (int i = 1; i <= count; i++)
            {
                int customerId = 1 + random.nextInt(customerMaxId);
                int productId = 1 + random.nextInt(productMaxId);
                int quantity = 1 + random.nextInt(10);
                long randomMillis = startMillis + (long) (random.nextDouble() * (endMillis - startMillis));
                java.sql.Date orderDate = new java.sql.Date(randomMillis);

                pstmt.setInt(1, i);
                pstmt.setInt(2, customerId);
                pstmt.setInt(3, productId);
                pstmt.setInt(4, quantity);
                pstmt.setDate(5, orderDate);

                pstmt.addBatch();
                if (i % 10000 == 0 || i == count)
                {
                    pstmt.executeBatch();
                    memTestDbConnection.commit();
                }
            }
            memTestDbConnection.setAutoCommit(true);
        }
    }

    private static void closeMemTestDbConnection()
    {
        if (memTestDbConnection != null)
        {
            try
            {
                memTestDbConnection.close();
                log.info("In-memory 'testdb' connection closed.");
            }
            catch (SQLException e)
            {
                log.warn("Error closing in-memory 'testdb' connection: {}", e.getMessage(), e);
            }
        }
    }

    private static int getPort(String[] args, String flag, int defaultValue)
    {
        for (int i = 0; i < args.length - 1; i++)
        {
            if (args[i].equalsIgnoreCase(flag))
            {
                try
                {
                    return Integer.parseInt(args[i + 1]);
                }
                catch (NumberFormatException e)
                {
                    log.error("Invalid port number for {}: {}", flag, args[i + 1]);
                }
            }
        }
        return defaultValue;
    }
}
