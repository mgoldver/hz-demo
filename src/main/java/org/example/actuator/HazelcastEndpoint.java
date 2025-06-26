package org.example.actuator;

import java.util.HashMap;
import java.util.Map;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "hazelcast")
public class HazelcastEndpoint
{

    private final HazelcastInstance hazelcastInstance;

    public HazelcastEndpoint(HazelcastInstance hazelcastInstance)
    {
        this.hazelcastInstance = hazelcastInstance;
    }

    @ReadOperation
    public Map<String, Object> hazelcastInfo()
    {
        Map<String, Object> details = new HashMap<>();
        details.put("clusterName", hazelcastInstance.getConfig()
                                                    .getClusterName());
        details.put("clusterState", hazelcastInstance.getCluster()
                                                     .getClusterState()
                                                     .toString());
        details.put("clusterSize", hazelcastInstance.getCluster()
                                                    .getMembers()
                                                    .size());
        details.put("clusterVersion", hazelcastInstance.getCluster()
                                                       .getClusterVersion()
                                                       .toString());
        details.put("memberUuid", hazelcastInstance.getCluster()
                                                   .getLocalMember()
                                                   .getUuid()
                                                   .toString());
        details.put("memberAddress", hazelcastInstance.getCluster()
                                                      .getLocalMember()
                                                      .getAddress()
                                                      .toString());
        details.put("maps", hazelcastInstance.getDistributedObjects()
                                             .stream()
                                             .filter(distributedObject -> distributedObject.getServiceName()
                                                                                           .equals("hz:impl:mapService"))
                                             .map(DistributedObject::getName)
                                             .toList());
        return details;
    }

    @ReadOperation
    public Map<String, Object> mapInfo(@Selector String mapName)
    {
        Map<String, Object> details = new HashMap<>();
        details.put("mapName", mapName);
        details.put("size", hazelcastInstance.getMap(mapName)
                                             .size());
        return details;
    }

    @ReadOperation
    public Map<String, Object> mapData(@Selector String mapName, @Selector int key)
    {
        Map<String, Object> details = new HashMap<>();
        details.put("mapName", mapName);
        details.put("key", key);
        details.put("value", hazelcastInstance.getMap(mapName)
                                             .get(key));
        return details;
    }

    @WriteOperation
    public Map<String, String> clearMap(@Selector String mapName)
    {
        Map<String, String> result = new HashMap<>();
        try
        {
            hazelcastInstance.getMap(mapName)
                             .clear();
            result.put("status", "success");
            result.put("message", "Map " + mapName + " cleared successfully");
        }
        catch (Exception e)
        {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
}
