package com.github.freewu32.nacos.config;

import com.github.freewu32.nacos.NacosConfiguration;
import com.github.freewu32.nacos.client.AbstractNacosClient;
import com.github.freewu32.nacos.client.NacosClient;
import com.github.freewu32.nacos.condition.RequiresNacos;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.*;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.jackson.env.JsonPropertySourceLoader;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * nacos配置加载客户端
 */
@Singleton
@RequiresNacos
@Requires(beans = AbstractNacosClient.class)
@Requires(property = ConfigurationClient.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@BootstrapContextCompatible
public class NacosConfigurationClient implements ConfigurationClient {

    private final Map<String, PropertySourceLoader> loaderByFormatMap = new ConcurrentHashMap<>();

    private NacosConfiguration.NacosConfigDiscoveryConfiguration discoveryConfiguration;

    private NacosClient nacosClient;

    public NacosConfigurationClient(NacosConfiguration configuration,
                                    NacosClient nacosClient,
                                    Environment environment) {
        this.discoveryConfiguration = configuration.getConfiguration();
        this.nacosClient = nacosClient;
        initLoaders(environment);
    }

    /**
     * 处理配置加载器
     */
    private void initLoaders(Environment environment) {
        Collection<PropertySourceLoader> loaders = environment.getPropertySourceLoaders();
        for (PropertySourceLoader loader : loaders) {
            Set<String> extensions = loader.getExtensions();
            for (String extension : extensions) {
                loaderByFormatMap.put(extension, loader);
            }
        }
    }

    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        return Flowable.fromCallable(() -> {
            //获取文件类型
            String type = discoveryConfiguration.getType();
            //解析主配置
            String properties = nacosClient.getConfigs(discoveryConfiguration.getDataId(),
                    discoveryConfiguration.getGroup(), discoveryConfiguration.getTenant());
            PropertySource propertySource = genSourceForConfigValue(type, properties);
            if (discoveryConfiguration.isAutoRefresh()) {
                //TODO 监听配置修改
            }
            return propertySource;
        });
    }

    /**
     * 解析数据
     */
    private PropertySource genSourceForConfigValue(String type, String configValue) {
        PropertySourceLoader propertySourceLoader = resolveLoader(type);
        Map<String, Object> m = propertySourceLoader.read(discoveryConfiguration.getDataId(),
                configValue.getBytes());
        return new MapPropertySource(NacosConfiguration.ID + "-" + discoveryConfiguration.getDataId(),
                m);
    }

    private PropertySourceLoader resolveLoader(String formatName) {
        return loaderByFormatMap.computeIfAbsent(formatName, f -> defaultLoader(formatName));
    }

    private PropertySourceLoader defaultLoader(String format) {
        try {
            switch (format) {
                case "json":
                    return new JsonPropertySourceLoader();
                case "properties":
                    return new PropertiesPropertySourceLoader();
                case "yml":
                case "yaml":
                    return new YamlPropertySourceLoader();
                default:
                    throw new ConfigurationException("Unsupported properties file format: " + format);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new ConfigurationException("Unsupported properties file format: " + format);
    }

    @Override
    public String getDescription() {
        return NacosConfiguration.ID;
    }
}
