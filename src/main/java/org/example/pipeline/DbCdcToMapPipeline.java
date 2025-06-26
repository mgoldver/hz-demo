package org.example.pipeline;

import java.util.Objects;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.cdc.ChangeRecord;
import com.hazelcast.jet.cdc.mysql.MySqlCdcSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DbCdcToMapPipeline
{
    private final HazelcastInstance instance;

    public void process()
    {
        JetService jet = this.instance.getJet();

        StreamSource<ChangeRecord> source = MySqlCdcSources.mysql("db source")
                                                           .setDatabaseAddress("address")
                                                           .setClusterName("cluster name")
                                                           .setDatabaseUser("id")
                                                           .setDatabasePassword("some password")
                                                           .setTableWhitelist("customer", "product", "customer_orders")
                                                           .build();

        Pipeline p = Pipeline.create();
        p.readFrom(source)
         .withoutTimestamps()
         .filter(changeRecord -> changeRecord.table()
                                             .equals("customer_orders"))
         .writeTo(Sinks.map("customer-orders",
                            r -> Objects.requireNonNull(r.key())
                                        .toMap()
                                        .get("your_key_field"),
                            r -> r.value()
                                  .toMap()
                                  .get("your_value_field")));

        try
        {
            Job job = jet.newJob(p);
            job.join();
        }
        catch (Exception e)
        {
            log.error("Error processing pipeline: {}", e.getMessage(), e);
        }
    }
}
