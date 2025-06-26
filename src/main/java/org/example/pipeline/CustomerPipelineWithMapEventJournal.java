package org.example.pipeline;

import java.util.Map;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.WindowDefinition;
import lombok.RequiredArgsConstructor;
import org.example.model.Customer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerPipelineWithMapEventJournal
{
    private final HazelcastInstance instance;

    public Pipeline createPipeline()
    {
        Pipeline pipeline = Pipeline.create();
        pipeline.readFrom(Sources.<Integer, Customer>mapJournal("customer", JournalInitialPosition.START_FROM_CURRENT))
                .withIngestionTimestamps()
                .map(Map.Entry::getValue)
                .window(WindowDefinition.sliding(100, 100))
                .aggregate(AggregateOperations.averagingLong(Customer::getAge))
                .writeTo(Sinks.logger(windowResult -> String.format("Average age: %.2f", windowResult.result())));
        return pipeline;
    }

    public JobConfig createCustomerJobConfig(String jobName)
    {
        return new JobConfig()
                       .setMetricsEnabled(true)
                       .setSuspendOnFailure(true)
                       .setName(jobName);
    }
}
