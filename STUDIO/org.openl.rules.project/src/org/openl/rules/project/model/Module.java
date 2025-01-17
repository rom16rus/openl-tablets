package org.openl.rules.project.model;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class Module {
    private String name;
    private PathEntry rulesRootPath;
    private ProjectDescriptor project;
    private Map<String, Object> properties;
    private WebstudioConfiguration webstudioConfiguration = new WebstudioConfiguration();
    private String wildcardName;
    private String wildcardRulesRootPath;
    private MethodFilter methodFilter;

    public MethodFilter getMethodFilter() {
        return methodFilter;
    }

    public void setMethodFilter(MethodFilter methodFilter) {
        this.methodFilter = methodFilter;
    }

    public String getWildcardRulesRootPath() {
        return wildcardRulesRootPath;
    }

    public void setWildcardRulesRootPath(String wildcardRulesRootPath) {
        this.wildcardRulesRootPath = wildcardRulesRootPath;
    }

    public WebstudioConfiguration getWebstudioConfiguration() {
        return webstudioConfiguration;
    }

    public void setWebstudioConfiguration(WebstudioConfiguration webstudioConfiguration) {
        this.webstudioConfiguration = webstudioConfiguration;
    }

    public String getWildcardName() {
        return wildcardName;
    }

    public void setWildcardName(String wildcardName) {
        this.wildcardName = wildcardName;
    }

    public ProjectDescriptor getProject() {
        return project;
    }

    public void setProject(ProjectDescriptor project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PathEntry getRulesRootPath() {
        return rulesRootPath;
    }

    public Path getRulesPath() {
        return project.getProjectFolder().resolve(rulesRootPath.getPath()).toAbsolutePath();
    }

    public void setRulesRootPath(PathEntry rulesRootPath) {
        this.rulesRootPath = rulesRootPath;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getRelativeUri() {
        return Optional.ofNullable(project.getProjectFolder().getParent())
                .orElse(project.getProjectFolder())
                .toUri()
                .relativize(getRulesPath().toUri())
                .toString();
    }

    @Override
    public String toString() {
        return name;
    }

}
