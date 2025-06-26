package org.example.actuator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import lombok.RequiredArgsConstructor;
import org.example.pipeline.CustomerPipelineWithMapEventJournal;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "customer-jet-jobs")
@RequiredArgsConstructor
public class CustomerJetEndpoint
{

    private final HazelcastInstance hazelcastInstance;
    private final CustomerPipelineWithMapEventJournal pipelineService;

    @ReadOperation
    public Map<String, Object> getJobs()
    {
        Map<String, Object> jobsInfo = new HashMap<>();
        List<Job> jobs = hazelcastInstance.getJet()
                                          .getJobs();
        jobsInfo.put("totalJobs", jobs.size());
        jobsInfo.put("jobs", jobs.stream()
                                 .map(this::createJobInfo)
                                 .toList());
        return jobsInfo;
    }

    @ReadOperation
    public Map<String, Object> performJobOperation(@Selector String jobName, @Selector String operation)
    {
        if ("start".equals(operation))
        {
            return this.startCustomerJob(jobName);
        }
        else if ("stop".equals(operation))
        {
            return this.stopCustomerJob(jobName);
        }
        return Map.of(
                "success", false,
                "message", "Unknown command: " + operation);
    }

    private Map<String, Object> startCustomerJob(@Selector String jobName)
    {
        try
        {
            if (this.findJobByName(jobName) != null)
            {
                return Map.of(
                        "success", false,
                        "message", "Job already exists: " + jobName);
            }

            Job job = hazelcastInstance.getJet()
                                       .newJob(pipelineService.createPipeline(), pipelineService.createCustomerJobConfig(jobName));

            return Map.of(
                    "success", true,
                    "message", "Job started successfully",
                    "jobName", jobName,
                    "jobId", job.getId(),
                    "status", job.getStatus()
                                 .name());
        }
        catch (Exception e)
        {
            return Map.of(
                    "success", false,
                    "message", "Failed to start job: " + e.getMessage(),
                    "error", e.getClass()
                              .getSimpleName());
        }
    }

    private Map<String, Object> stopCustomerJob(@Selector String jobName)
    {
        try
        {
            Job job = this.findJobByName(jobName);
            if (job == null)
            {
                return Map.of(
                        "success", false,
                        "message", "Job not found: " + jobName);
            }
            job.cancel();

            return Map.of(
                    "success", true,
                    "message", "Job stopped successfully",
                    "jobName", jobName,
                    "jobId", job.getId());
        }
        catch (Exception e)
        {
            return Map.of(
                    "success", false,
                    "message", "Failed to stop job: " + e.getMessage(),
                    "error", e.getClass()
                              .getSimpleName());
        }
    }

    private Map<String, Object> createJobInfo(Job job)
    {
        Map<String, Object> info = new HashMap<>();
        info.put("id", job.getId());
        info.put("name", job.getName());
        info.put("status", job.getStatus()
                              .name());
        info.put("submissionTime", job.getSubmissionTime());
        try
        {
            info.put("isUserCancelled", job.isUserCancelled());
        }
        catch (Exception e)
        {
        }
        return info;
    }

    private Job findJobByName(String jobName)
    {
        return hazelcastInstance.getJet()
                                .getJobs()
                                .stream()
                                .filter(job -> jobName.equals(job.getName()))
                                .findFirst()
                                .orElse(null);
    }
}
