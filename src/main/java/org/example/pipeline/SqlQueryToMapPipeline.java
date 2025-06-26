package org.example.pipeline;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.function.ToResultSetFunction;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.DataConnectionRef;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Product;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqlQueryToMapPipeline
{
    private final HazelcastInstance instance;

    @Scheduled(cron = "0 0 23 * * *")
    public void processInitial()
    {
        this.process(this.initialQuery());
    }

    @Scheduled(cron = "0 */2 * * * *")
    public void processIncremental()
    {
        this.process(this.incrementalQuery(LocalDateTime.now()
                                                        .minusMinutes(1)));
    }

    private void process(ToResultSetFunction resultSetFn)
    {
        JetService jet = this.instance.getJet();

        Pipeline p = Pipeline.create();
        p.readFrom(this.createJdbcSource(resultSetFn))
         .map(product -> Util.entry(product.getId(), product))
         .peek()
         .writeTo(Sinks.map("product"));

        try
        {
            jet.newJob(p)
               .join();
        }
        catch (Exception e)
        {
            log.error("Error processing pipeline: {}", e.getMessage(), e);
        }
    }

    private BatchSource<Product> createJdbcSource(ToResultSetFunction resultSetFn)
    {
        return Sources.jdbc(
                DataConnectionRef.dataConnectionRef("h2-testdb"),
                resultSetFn,
                resultSet -> Product.builder()
                                    .id(resultSet.getInt("id"))
                                    .name(resultSet.getString("name"))
                                    .description(resultSet.getString("description"))
                                    .price(resultSet.getDouble("price"))
                                    .last_modified(resultSet.getTimestamp("last_modified")
                                                           .toLocalDateTime())
                                    .build());
    }

    private ToResultSetFunction incrementalQuery(LocalDateTime after)
    {
        return (con, parallelism, index) ->
        {
            String query = "SELECT * FROM dbo.product WHERE last_modified > ? AND id MOD ? = 0";
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setTimestamp(1, Timestamp.valueOf(after));
            stmt.setInt(2, index);
            return stmt.executeQuery();
        };
    }

    private ToResultSetFunction initialQuery()
    {
        return (con, parallelism, index) ->
        {
            String query = "SELECT * FROM dbo.product WHERE id MOD ? = 0";
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setInt(1, index);
            return stmt.executeQuery();
        };
    }
}
