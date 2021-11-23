package org.openl.spring.env;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiPropertySource;

/**
 * Allows to combine environment properties, default application properties and external application properties. This
 * class can be used in the following ways:
 * <ul>
 * <li>In the web.xml, using {@linkplain org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * "contextInitializerClasses"} context parameter.</li>
 * <li>In Spring java configuration like <code>new PropertySourcesLoader().initialize(applicationContext)</code></li>
 * </ul>
 * Default resolving order (next resource overides previous):
 * <li>OpenL default properties. {@link DefaultPropertySource}</li>
 * <li>Java preferences. {@link PreferencePropertySource}</li>
 * <li>Application externalized configuration. {@link ApplicationPropertySource} <br>
 * <li>Application modifiable configuration. {@link DynamicPropertySource} <br>
 * <li>Spring environment.
 * <ol>
 * <li>OS environment variables. {@link System#getenv()}</li>
 * <li>Java System properties. {@link System#getProperties()}</li>
 * <li>JNDI attributes from {@code java:comp/env}</li>
 * <li>Servlet context loadProperties parameters from
 * {@link javax.servlet.ServletContext#getInitParameter(java.lang.String)}</li>
 * <li>Servlet config loadProperties parameters from
 * {@link javax.servlet.ServletConfig#getInitParameter(java.lang.String)}</li>
 * </ol>
 * </li>
 * </ul>
 *
 * @author Yury Molchan
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * @see org.springframework.beans.factory.config.PlaceholderConfigurerSupport
 * @see org.springframework.context.ApplicationContextInitializer
 * @see ApplicationPropertySource
 */
public class PropertySourcesLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext appContext) {
        ConfigLog.LOG
            .info("The initialization of properties from 'contextInitializerClasses' context-param in web.xml");
        ConfigurableEnvironment env = appContext.getEnvironment();
        String appName = normalizeAppName(appContext.getApplicationName());
        loadEnvironment(appContext, env, appName, null);
    }

    public void initialize(ConfigurableApplicationContext appContext, ServletContext servletContext) {
        ConfigLog.LOG.info("The initialization of properties");
        ConfigurableEnvironment env = new StandardEnvironment();
        appContext.setEnvironment(env);
        String appName = normalizeAppName(servletContext.getContextPath());
        loadEnvironment(appContext, env, appName, servletContext);
    }

    private void loadEnvironment(ConfigurableApplicationContext appContext,
            ConfigurableEnvironment env,
            String appName,
            ServletContext servletContext) {
        MutablePropertySources propertySources = env.getPropertySources();
        RawPropertyResolver props = new RawPropertyResolver(propertySources);

        ConfigLog.LOG.info("Loading default properties...");
        DefaultPropertySource defaultPropertySource = new DefaultPropertySource();
        propertySources.addLast(defaultPropertySource);

        ConfigLog.LOG.info("Loading preference properties...");
        PreferencePropertySource preferencePropertySource = new PreferencePropertySource(appName);
        PreferencePropertySource.THE = preferencePropertySource;
        propertySources.addBefore(DefaultPropertySource.PROPS_NAME, preferencePropertySource);

        if (servletContext != null) {
            // Assuming that there is org.springframework.core.env.StandardEnvironment
            ConfigLog.LOG.info("Loading JNDI properties...");
            if (JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {
                propertySources.addFirst(new JndiPropertySource("JNDI properties"));
            }
            ConfigLog.LOG.info("Loading ServletContext init parameters...");
            propertySources.addFirst(new ServletContextPropertySource("ServletContext init parameters", servletContext));
        }
        ConfigLog.LOG.info("Loading ApplicationContext beans...");
        propertySources.addFirst(new ApplicationContextPropertySource(appContext));

        ConfigLog.LOG.info("Loading application properties...");
        String[] profiles = env.getActiveProfiles();
        propertySources.addBefore(PreferencePropertySource.PROPS_NAME,
            new ApplicationPropertySource(props, appName, profiles));

        ConfigLog.LOG.info("Loading OpenL System Info properties...");
        propertySources.addFirst(new SysInfoPropertySource());

        ConfigLog.LOG.info("Loading reconfigurable properties...");
        DynamicPropertySource propertySource = new DynamicPropertySource(appName, props);
        DynamicPropertySource.THE = propertySource;
        propertySources.addBefore(ApplicationPropertySource.PROPS_NAME, propertySource);

        propertySources.addBefore(DynamicPropertySource.PROPS_NAME, new DisablePropertySource(propertySources));

        ConfigLog.LOG.info("Register reference property processor...");
        propertySources.addLast(new RefPropertySource(propertySources));

        registerPropertyBean(appContext, defaultPropertySource, props);
    }

    private void registerPropertyBean(ConfigurableApplicationContext appContext,
            DefaultPropertySource defaultPropertySource,
            RawPropertyResolver props) {
        Map<String, String> propertyMap = new HashMap<>();
        for (String key : defaultPropertySource.getPropertyNames()) {
            propertyMap.put(key, props.getRawProperty(key));
        }
        appContext.addBeanFactoryPostProcessor(bf -> bf.registerSingleton("PropertyBean",
            new PropertyBean(defaultPropertySource.getSource(), propertyMap)));
    }

    private static String normalizeAppName(String appName) {
        if (appName.isEmpty()) {
            return "";
        }
        return appName.replace('/', ' ').replace('\\', ' ').trim().replace(' ', '-');
    }

}
