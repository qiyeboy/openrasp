package com.baidu.openrasp.cloud.httpappender;

import com.baidu.openrasp.cloud.CloudHttp;
import com.baidu.openrasp.cloud.CloudHttpPool;
import com.baidu.openrasp.cloud.model.AppenderCache;
import com.baidu.openrasp.cloud.model.CloudRequestUrl;
import com.baidu.openrasp.cloud.model.GenericResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Set;

/**
 * @description: 日志上传appender
 * @author: anyang
 * @create: 2018/09/20 09:53
 */
public class HttpAppender extends AppenderSkeleton {

    public static final Logger LOGGER = Logger.getLogger(HttpAppender.class.getPackage().getName() + ".log");

    private CloudHttp cloudHttp;

    public HttpAppender() {
        this.cloudHttp = new CloudHttpPool();
    }

    private boolean checkEntryConditions() {
        if (cloudHttp == null) {
            LogLog.warn("Http need to be initialized.");
            return false;

        } else if (this.closed) {
            LogLog.warn("Not allowed to write to a closed appender.");
            return false;

        }
        return true;
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        if (checkEntryConditions()) {
            String jsonString = new Gson().toJson(loggingEvent.getMessage());
            JsonArray jsonArray = mergeFromAppenderCache(loggingEvent.getLoggerName(), jsonString);
            String logger = getLogger(loggingEvent.getLoggerName());
            String result = cloudHttp.request(getUrl(logger), new Gson().toJson(jsonArray));
            if (result != null) {
                GenericResponse response = new Gson().fromJson(result, GenericResponse.class);
                if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
                    return;
                }
            }
            AppenderCache.setCache(logger, jsonString);
        }
    }

    private JsonArray mergeFromAppenderCache(String loggerName, String currnetLog) {
        Set<String> set = AppenderCache.getCache(getLogger(loggerName));
        JsonArray jsonArray = new JsonParser().parse(currnetLog).getAsJsonArray();
        if (set != null && !set.isEmpty()) {
            for (String log : set) {
                jsonArray.add(new JsonParser().parse(log));
            }
        }
        return jsonArray;
    }

    private String getLogger(String loggerName) {
        String name;
        if (loggerName.contains("policy_alarm")) {
            name = "policy_alarm";
        } else if (loggerName.contains("alarm")) {
            name = "alarm";
        } else {
            name = "plugin";
        }
        return name;
    }

    private String getUrl(String logger) {
        String url;
        if ("policy_alarm".equals(logger)) {
            url = CloudRequestUrl.CLOUD_POLICY_ALARM_HTTP_APPENDER_URL;
        } else if ("alarm".equals(logger)) {
            url = CloudRequestUrl.CLOUD_ALARM_HTTP_APPENDER_URL;
        } else {
            url = CloudRequestUrl.CLOUD_PLUGIN_HTTP_APPENDER_URL;
        }
        return url;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void activateOptions() {
    }
}
