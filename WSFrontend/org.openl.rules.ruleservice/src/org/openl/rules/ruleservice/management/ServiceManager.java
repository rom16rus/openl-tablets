package org.openl.rules.ruleservice.management;

import java.util.Collection;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;

/**
 * Starts rule service.
 *
 * @author Marat Kamalov
 *
 */
public interface ServiceManager {
    /**
     * Determine services to be deployed on start.
     */
    void start();

    /**
     * Redeploys all deployed services.
     */
    void redeployAll();

    /**
     * Deploys the specified service.
     *
     * @param service Service to deploy.
     * @throws RuleServiceDeployException
     */
    void deploy(OpenLService service) throws RuleServiceDeployException;

    /**
     * Undeploys currently running service.
     *
     * @param deployPath deployPath of the service to undeploy.
     * @throws RuleServiceDeployException
     */
    void undeploy(String deployPath) throws RuleServiceUndeployException;

    /**
     * Searches for the service from currently running with the specified deployPath or null if service with specified
     * deployPath wasn't deployed.
     *
     * @param deployPath deployPath of the service to find.
     * @return Service with the specified deployPath or null if service with specified deployPath wasn't deployed.
     */
    OpenLService getServiceByDeploy(String deployPath);

    Collection<OpenLService> getServices();

    /**
     * Searches for the services from currently running with the specified deploymentName
     *
     * @param deploymentName deployment name of the service to find
     * @return services collection with specified deploymentName or empty if services with specified deploymentName
     *         wasn't deployed.
     */
    Collection<OpenLService> getServicesByDeployment(String deploymentName);
}
