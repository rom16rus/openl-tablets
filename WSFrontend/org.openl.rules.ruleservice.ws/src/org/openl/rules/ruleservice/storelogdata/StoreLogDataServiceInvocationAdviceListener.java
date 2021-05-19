package org.openl.rules.ruleservice.storelogdata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.interceptors.AnnotationUtils;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdviceListener;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.storelogdata.advice.ObjectSerializerAware;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogDatas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class StoreLogDataServiceInvocationAdviceListener implements ServiceInvocationAdviceListener, ApplicationContextAware, InitializingBean {
    private final Logger log = LoggerFactory.getLogger(StoreLogDataServiceInvocationAdviceListener.class);

    private boolean storeLogDataEnabled = false;

    private ApplicationContext applicationContext;

    private Map<StoreLogDataService, Collection<Inject<?>>> supportedInjects;

    public boolean isStoreLogDataEnabled() {
        return storeLogDataEnabled;
    }

    public void setStoreLogDataEnabled(boolean storeLogDataEnabled) {
        this.storeLogDataEnabled = storeLogDataEnabled;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, StoreLogDataService> storeLogDataServices = applicationContext
            .getBeansOfType(StoreLogDataService.class);

        Collection<StoreLogDataService> activeStoreLogDataServices = storeLogDataServices.values()
            .stream()
            .filter(Objects::nonNull)
            .filter(StoreLogDataService::isEnabled)
            .collect(Collectors.toList());
        Map<StoreLogDataService, Collection<Inject<?>>> injects = new HashMap<>();
        for (StoreLogDataService storeLogDataService : activeStoreLogDataServices) {
            Collection<Inject<?>> supportedInjects = storeLogDataService.additionalInjects();
            if (supportedInjects != null) {
                injects.put(storeLogDataService, storeLogDataService.additionalInjects());
            }
        }
        this.supportedInjects = injects;
    }

    public void process(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccurredException,
            Consumer<Object> postProcessAdvice,
            Predicate<PrepareStoreLogData> predicate) {
        PrepareStoreLogDatas prepareStoreLogDatas = interfaceMethod.getAnnotation(PrepareStoreLogDatas.class);
        if (prepareStoreLogDatas != null) {
            prepare(interfaceMethod,
                args,
                result,
                lastOccurredException,
                postProcessAdvice,
                predicate,
                prepareStoreLogDatas.value());

        } else {
            PrepareStoreLogData prepareStoreLogData = interfaceMethod.getAnnotation(PrepareStoreLogData.class);
            if (prepareStoreLogData != null) {
                prepare(interfaceMethod,
                    args,
                    result,
                    lastOccurredException,
                    postProcessAdvice,
                    predicate,
                    new PrepareStoreLogData[] { prepareStoreLogData });

            }
        }
    }

    private void prepare(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccurredException,
            Consumer<Object> postProcessAdvice,
            Predicate<PrepareStoreLogData> predicate,
            PrepareStoreLogData[] prepareStoreLogDataArray) {
        Collection<Consumer<Void>> destroyFunctions = new ArrayList<>();
        try {
            StoreLogData storeLogData = null;
            IdentityHashMap<Inject<?>, Object> cache = new IdentityHashMap<>();
            for (PrepareStoreLogData storeLogging : prepareStoreLogDataArray) {
                if (predicate.test(storeLogging)) {
                    StoreLogDataAdvice storeLogDataAdvice = null;
                    try {
                        storeLogDataAdvice = storeLogging.value().newInstance();
                        postProcessAdvice.accept(storeLogDataAdvice);
                        storeLogData = processAwareInterfaces(storeLogData,
                            storeLogDataAdvice,
                            cache,
                            destroyFunctions);
                    } catch (Exception e) {
                        String msg = String.format(
                            "Failed to instantiate store log data advice for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                            MethodUtil.printQualifiedMethodName(interfaceMethod),
                            storeLogging.value().getTypeName());
                        log.error(msg, e);
                    }
                    if (storeLogDataAdvice != null) {
                        if (storeLogData == null) {
                            storeLogData = StoreLogDataHolder.get(); // Lazy local variable
                            // initialization
                        }
                        storeLogDataAdvice.prepare(storeLogData.getCustomValues(), args, result, lastOccurredException);
                    }
                }
            }
        } finally {
            destroyFunctions.forEach(e -> e.accept(null));
        }
    }

    private StoreLogData processAwareInterfaces(StoreLogData storeLogData,
            StoreLogDataAdvice storeLogDataAdvice,
            IdentityHashMap<Inject<?>, Object> cache,
            Collection<Consumer<Void>> destroyFunctions) {
        if (storeLogDataAdvice instanceof ObjectSerializerAware) {
            ObjectSerializerAware objectSerializerAware = (ObjectSerializerAware) storeLogDataAdvice;
            if (storeLogData == null) {
                storeLogData = StoreLogDataHolder.get(); // Lazy local
                // variable
                // initialization
            }
            objectSerializerAware.setObjectSerializer(storeLogData.getObjectSerializer());
        }
        for (Map.Entry<StoreLogDataService, Collection<Inject<?>>> entry : supportedInjects.entrySet()) {
            for (Inject<?> inject : entry.getValue()) {
                if (inject != null && inject.getAnnotationClass() != null) {
                    try {
                        Object resource = cache.get(inject);
                        if (resource == null) {
                            Object resource1 = AnnotationUtils
                                .inject(storeLogDataAdvice, inject.getAnnotationClass(), inject::getResource);
                            cache.put(inject, resource1);
                            if (resource1 != null) {
                                destroyFunctions.add((e) -> inject.destroy(resource1));
                            }
                        } else {
                            AnnotationUtils.inject(storeLogDataAdvice, inject.getAnnotationClass(), () -> resource);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        log.error("Failed to inject resource of class '{}' through annotation '{}'",
                            inject.getResource().getClass().getTypeName(),
                            inject.getAnnotationClass().getTypeName(),
                            e);
                    }
                } else {
                    log.error("Aware interface is null. Check store log data service implementation '{}'.",
                        entry.getKey().getClass().getTypeName());
                }
            }
        }
        return storeLogData;
    }

    @Override
    public void beforeServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccurredException,
            Consumer<Object> postProcessAdvice) {
        if (isStoreLogDataEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccurredException,
                postProcessAdvice,
                e -> e.before() && e.bindToServiceMethodAdvice().equals(serviceMethodAdvice.getClass()));
        }
    }

    @Override
    public void afterServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccurredException,
            Consumer<Object> postProcessAdvice) {
        if (isStoreLogDataEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccurredException,
                postProcessAdvice,
                e -> !e.before() && e.bindToServiceMethodAdvice().equals(serviceMethodAdvice.getClass()));
        }
    }

    @Override
    public void beforeMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccurredException,
            Consumer<Object> postProcessAdvice) {
        if (isStoreLogDataEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccurredException,
                postProcessAdvice,
                e -> e.before() && e.bindToServiceMethodAdvice().equals(PrepareStoreLogData.Default.class));
        }
    }

    @Override
    public void afterMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccurredException,
            Consumer<Object> postProcessAdvice) {
        if (isStoreLogDataEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccurredException,
                postProcessAdvice,
                e -> !e.before() && e.bindToServiceMethodAdvice().equals(PrepareStoreLogData.Default.class));
        }
    }

}
