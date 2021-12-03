package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * @author gdm
 */
@Component
public class MasterChefUtils {
    public static enum PrimusCommands {refreshActivities, refreshBoxDatiDiArchivio, refreshDestinatari, showMessage, logout};
    private static final Logger log = LoggerFactory.getLogger(MasterChefUtils.class);

    private volatile ConcurrentMap<String, JedisPool> jpm = new ConcurrentHashMap<>(5);
    private final Object LOCK = new Object();
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public JedisPool getInstance(String host, Integer port) {
        if (port == null || port == -1) {
            port = 6379;
        }
        String hashKey = host + ":" + port.toString();
        if (jpm.get(hashKey) == null) {
            synchronized (LOCK) {
                if (jpm.get(hashKey) == null) {
                    JedisPoolConfig jpc = new JedisPoolConfig();
                    jpc.setMaxTotal(10);

                    JedisPool tmp = new JedisPool(jpc, host, port);
                    jpm.put(hashKey, tmp);
                }
            }
        }
        return jpm.get(hashKey);
    }
    
    public class MasterchefJobDescriptor {
        public String appID;
        public String jobID;
        public String returnQueue;
        public Long returnQueueTimeout;
        public List<MasterchefJob> jobList;

        public MasterchefJobDescriptor(List<MasterchefJob> jobList) {
            this.appID = "internauta";
            this.jobID = "internautaJob";
            this.returnQueue = UUID.randomUUID().toString();
            this.returnQueueTimeout = 60l;
            this.jobList = jobList;
        }
    }

    public class MasterchefJob {
        public String jobType;
        public String jobN;
        public String bag;
        public MasterchefJobParams params;

        public MasterchefJob(MasterchefJobParams params, String bag) {
            this.jobType = params.getJobType();
            this.jobN = "1";
            this.bag = bag;
            this.params = params;
        }
    }
    
    public abstract class MasterchefJobParams {
        @JsonIgnore
        public abstract String getJobType();
    }
    
    public class MasterchefJobResult {
    }
    
    public class ReporterJobParams extends MasterchefJobParams {
        public String templatePath = null;
        public String templateName;
        public Map<String, String> data;
        public QRCodeData qrCodeData;

        public ReporterJobParams(String templateName, Map<String, String> data) {
            this.templateName = templateName;
            this.data = data;
        }
    
        public ReporterJobParams(String templateName,  Map<String, String> data, String qrCodeFieldName, String qrCodeValue) {
            this(templateName, data);
            this.qrCodeData = new QRCodeData(qrCodeFieldName, qrCodeValue);
        }

        @Override
        public String getJobType() {
            return "Reporter";
        }
    }
    
    public class QRCodeData {
        public String fieldName;
        public String value;

        public QRCodeData(String fieldName, String value) {
            this.fieldName = fieldName;
            this.value = value;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    
    public class PrimusCommanderJobParams extends MasterchefJobParams {
        public String id;
        public String interval;
        public String times;
        public PrimusCommanderMessage command;

        public PrimusCommanderJobParams(String interval, String times, PrimusCommanderMessage primusMessage) {
            this.id = null;
            this.interval = interval;
            this.times = times;
            this.command = primusMessage;
        }

        @Override
        public String getJobType() {
            return "PrimusCommander";
        }
    }
    
    public class PrimusCommanderMessage {
        public List<String> dest;
        public String dest_app;
        public PrimusCommanderCommand command;

        public PrimusCommanderMessage(List<String> dest, String destApp, PrimusCommanderCommand command) {
            this.dest = dest;
            this.dest_app = destApp;
            this.command = command;
        }
    }

    public class PrimusCommanderCommand {
        public PrimusCommands command;
        public Map<String, Object> params;

        public PrimusCommanderCommand(PrimusCommands command, Map<String, Object> params) {
            this.command = command;
            this.params = params;
        }
    }
    
    public MasterchefJobDescriptor buildPrimusMasterchefJob(PrimusCommands primusCommands, Map<String, Object> params, String interval, String times, List<String> dest, String destApp) {
        PrimusCommanderCommand primusCommanderCommand = new PrimusCommanderCommand(primusCommands, params);
        PrimusCommanderMessage primusCommanderMessage = new PrimusCommanderMessage(dest, destApp, primusCommanderCommand);
        MasterchefJobParams primusCommanderJobParams = new PrimusCommanderJobParams(interval, times, primusCommanderMessage);
        MasterchefJob masterchefJob = new MasterchefJob(primusCommanderJobParams, null);
        MasterchefJobDescriptor masterchefJobDescriptor = new MasterchefJobDescriptor(Arrays.asList(masterchefJob));
        return masterchefJobDescriptor;
    }
    
    public MasterchefJobDescriptor buildReporterMasterchefJob(String templateName,  Map<String, String> data, String qrCodeFieldName, String qrCodeValue) {
        MasterchefJobParams reporterJobParams = new ReporterJobParams(templateName, data, qrCodeFieldName, qrCodeValue);
        MasterchefJob masterchefJob = new MasterchefJob(reporterJobParams, null);
        MasterchefJobDescriptor masterchefJobDescriptor = new MasterchefJobDescriptor(Arrays.asList(masterchefJob));
        return masterchefJobDescriptor;
    }
    
    public void sendMasterChefJob(MasterchefJobDescriptor masterchefJobDescriptor, AziendaParametriJson.MasterChefParmas masterChefParmas) throws JsonProcessingException {
        JedisPool jedisPool = getInstance(masterChefParmas.getRedisHost(), masterChefParmas.getRedisPort());
        try (Jedis j = jedisPool.getResource()) {
            j.lpush(masterChefParmas.getInQueue(), objectMapper.writeValueAsString(masterchefJobDescriptor));
        }
    }
    
    public String sendMasterChefJobAndWaitResult(MasterchefJobDescriptor masterchefJobDescriptor, AziendaParametriJson.MasterChefParmas masterChefParmas) throws JsonProcessingException {
        JedisPool jedisPool = getInstance(masterChefParmas.getRedisHost(), masterChefParmas.getRedisPort());
        try (Jedis j = jedisPool.getResource()) {
            j.lpush(masterChefParmas.getInQueue(), objectMapper.writeValueAsString(masterchefJobDescriptor));
            int timeout = 0;
            if (masterchefJobDescriptor.returnQueueTimeout != null)
                timeout = Math.toIntExact(masterchefJobDescriptor.returnQueueTimeout);
            List<String> res = j.blpop(timeout, masterchefJobDescriptor.returnQueue);
            if (res != null) {
                return res.get(0);
            }
        }
        return null;
    }
}
