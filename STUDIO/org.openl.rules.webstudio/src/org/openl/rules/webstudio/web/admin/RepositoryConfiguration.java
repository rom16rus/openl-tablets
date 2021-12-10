package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.config.ReadOnlyPropertiesHolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

public class RepositoryConfiguration {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private String name;
    private String repoType;

    private String oldName = null;

    private final String configName;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_NAME;

    private RepositorySettings settings;

    private String errorMessage;
    private final PropertiesHolder properties;
    private final String nameWithPrefix;

    private RepositoryConfiguration configToClone;
    private FreeValueFinder valueFinder;

    public RepositoryConfiguration(String configName, PropertyResolver propertiesResolver) {
        this(configName, new ReadOnlyPropertiesHolder(propertiesResolver));
    }

    public RepositoryConfiguration(String configName, PropertiesHolder properties) {
        this.configName = configName.toLowerCase();
        this.properties = properties;
        nameWithPrefix = Comments.REPOSITORY_PREFIX + configName.toLowerCase();
        REPOSITORY_FACTORY = nameWithPrefix + ".factory";
        REPOSITORY_NAME = nameWithPrefix + ".name";

        load();
    }

    public RepositoryConfiguration(String configName,
            PropertiesHolder properties,
            RepositoryConfiguration configToClone,
            FreeValueFinder valueFinder) {
        this(configName, properties);
        this.configToClone = configToClone;
        this.valueFinder = valueFinder;
        // do not copy configName, only content
        setName(valueFinder.find("name", configToClone.getName()));
        oldName = name;

        setType(configToClone.getType());
        settings.copyContent(configToClone.getSettings());
        settings.applyRepositorySuffix(valueFinder);
    }

    public PropertiesHolder getProperties() {
        return properties;
    }

    private void load() {
        String factoryClassName = properties.getProperty(REPOSITORY_FACTORY);
        repoType = RepositoryInstatiator.getRefID(factoryClassName);
        RepositoryType repositoryType = RepositoryType.findByFactory(repoType);
        if (repoType == null) {
            // Fallback to default value
            repositoryType = RepositoryType.GIT;
            repoType = repositoryType.factoryId;
            if (factoryClassName != null) {
                //add error message
                errorMessage = "Unsupported repository type. Repository factory: " + factoryClassName + ". Was replaced with " + repoType + ".";
            }
        }
        name = properties.getProperty(REPOSITORY_NAME);
        oldName = name;
        settings = createSettings(repositoryType, properties, nameWithPrefix);
    }

    private RepositorySettings createSettings(RepositoryType repositoryType,
            PropertiesHolder properties,
            String configPrefix) {
        RepositorySettings newSettings;
        switch (repositoryType) {
            case AWS_S3:
                newSettings = new AWSS3RepositorySettings(properties, configPrefix);
                break;
            case AZURE:
                newSettings = new AzureBlobRepositorySettings(properties, configPrefix);
                break;
            case GIT:
                newSettings = new GitRepositorySettings(properties, configPrefix);
                break;
            case LOCAL:
                newSettings = new LocalRepositorySettings(properties, configPrefix);
                break;
            default:
                newSettings = new CommonRepositorySettings(properties, configPrefix, repositoryType);
                break;
        }

        return newSettings;
    }

    private void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));
        propertiesHolder.setProperty(REPOSITORY_FACTORY, repoType);
        settings.store(propertiesHolder);
    }

    public void revert() {
        properties.revertProperties(REPOSITORY_NAME, REPOSITORY_FACTORY);
        load();
        settings.revert(properties);
    }

    public PropertiesHolder getPropertiesToValidate() {
        InMemoryProperties tempProps = new InMemoryProperties(getProperties().getPropertyResolver());
        store(tempProps);
        return tempProps;
    }

    public void commit() {
        store(properties);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFolderRepository() {
        return RepositoryType.GIT.factoryId.equals(repoType);
    }

    public String getType() {
        return repoType;
    }

    public RepositoryType getRepositoryType() {
        return RepositoryType.findByFactory(repoType);
    }

    public void setType(String newRepoType) {
        if (StringUtils.isEmpty(newRepoType)) {
            return;
        }
        if (!repoType.equals(newRepoType)) {
            RepositoryType newRepositoryType = RepositoryType.findByFactory(newRepoType);

            if (newRepositoryType == null) {
                throw new IllegalArgumentException(String.format("Access type '%s' is not supported", newRepoType));
            }
            repoType = newRepoType;
            errorMessage = null;
            RepositorySettings newSettings = createSettings(newRepositoryType, properties, nameWithPrefix);
            if (configToClone != null) {
                configToClone.setType(newRepoType);
                newSettings.copyContent(configToClone.getSettings());
                newSettings.applyRepositorySuffix(valueFinder);
            } else {
                newSettings.copyContent(settings);
            }
            settings = newSettings;
            settings.onTypeChanged(newRepositoryType);
        }
    }

    public String getId() {
        return getConfigName();
    }

    public String getConfigName() {
        return configName;
    }

    boolean isNameChangedIgnoreCase() {
        return name != null && !name.equalsIgnoreCase(oldName) || name == null && oldName != null;
    }

    public RepositorySettings getSettings() {
        return settings;
    }

    protected static class NameWithNumbersComparator implements Comparator<RepositoryConfiguration> {
        private static final Pattern pattern = Pattern.compile("([^\\d]*+)(\\d*+)");

        @Override
        public int compare(RepositoryConfiguration o1, RepositoryConfiguration o2) {
            Matcher m1 = pattern.matcher(o1.getName());
            Matcher m2 = pattern.matcher(o2.getName());
            while (true) {
                boolean f1 = m1.find();
                boolean f2 = m2.find();
                if (!f1 && !f2) {
                    return 0;
                }
                if (f1 != f2) {
                    return f1 ? 1 : -1;
                }

                String s1 = m1.group(1);
                String s2 = m2.group(1);
                int compare = s1.compareToIgnoreCase(s2);
                if (compare != 0) {
                    return compare;
                }

                String n1 = m1.group(2);
                String n2 = m2.group(2);
                if (!n1.equals(n2)) {
                    if (n1.isEmpty()) {
                        return -1;
                    }
                    if (n2.isEmpty()) {
                        return 1;
                    }
                    return new BigInteger(n1).compareTo(new BigInteger(n2));
                }
            }
        }
    }
}
