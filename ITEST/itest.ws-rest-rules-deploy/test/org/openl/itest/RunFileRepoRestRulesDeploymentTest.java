package org.openl.itest;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.worker.AsyncExecutor;

public class RunFileRepoRestRulesDeploymentTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.start("file");
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testDeployRules() {
        client.send("ui-info.get");
        client.send("admin_services_no_services.json.get");
        client.send("deployed-rules_hello_not_found.post");

        client.post("/admin/deploy", "/rules-to-deploy.zip", 201);
        client.send("deployed-rules_services.get");
        client.send("deployed-rules_methods.get");
        client.get("/admin/deploy/rules-to-deploy.zip", "/rules-to-deploy.zip");
        client.send("deployed-rules_hello.post");

        // should be always redeployed
        client.post("/admin/deploy", "/rules-to-deploy_v2.zip", 201);
        client.send("deployed-rules_services.get");
        client.send("deployed-rules_methods.get");
        client.send("deployed-rules_ui-info.get");
        client.send("deployed-rules_hello_2.post");

        client.post("/admin/deploy", "/rules-to-deploy-failed.zip", 201);
        client.send("deployed-rules_services_failed.get");
        client.send("deployed-rules_errors.get");
        client.send("deployed-rules_manifest.get");

        client.send("deployed-rules.delete");
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/empty_project.zip", 400);
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void testDeployRules_multipleDeployment() {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/multiple-deployment_v1.zip", 201);
        client.send("yaml_project_services.get");
        client.send("project1_methods.get");
        client.send("yaml_project_project2_methods.get");
        client.send("project1_sayHello.post");
        client.get("/admin/deploy/yaml_project.zip","/multiple-deployment_v1.zip");

        // should be updated
        client.post("/admin/deploy", "/multiple-deployment_v2.zip", 201);
        client.send("project1_sayHello_2.post");
        client.get("/admin/deploy/yaml_project.zip","/multiple-deployment_v2.zip");

        client.send("yaml_project_all.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void testMissingServiceMethods() {
        client.send("admin_services_no_services.json.get");
        client.send("missing-service_methods.get");
        client.send("missing-service.delete");
        client.send("missing-service.get");
        client.send("missing-service_errors.get");
        client.send("missing-service_manifest.get");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void test_EPBDS_8758_multithread() throws InterruptedException {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v1.zip", 201);
        client.send("EPBDS-8758/doSomething_v1.get");

        AsyncExecutor executor = new AsyncExecutor(() -> client.send("EPBDS-8758/doSomething.get"));
        executor.start();

        TimeUnit.SECONDS.sleep(1);
        client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v2.zip", 201);
        client.send("EPBDS-8758/doSomething_v2.get");
        client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v3.zip", 201);
        client.send("EPBDS-8758/doSomething_v3.get");
        TimeUnit.SECONDS.sleep(1);

        boolean invocationErrors = executor.stop();

        client.send("EPBDS-8758/EPBDS-8758.delete");

        assertFalse(invocationErrors);
        client.send("admin_services_no_services.json.get");
    }

    @Test
    @Ignore("Check EPBDS-10940 issue")
    public void test_EPBDS_8758_multithread2() throws Exception {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v1.zip", 201);
        client.send("EPBDS-8758/doSomething_v1.get");

        AsyncExecutor executor = new AsyncExecutor(() -> client.send("EPBDS-8758/doSomething.get"));
        executor.start();

        AsyncExecutor deployers = AsyncExecutor.start(
            () -> client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v2.zip", 201),
            () -> client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v3.zip", 201));

        TimeUnit.SECONDS.sleep(1);
        boolean deployErrors = deployers.stop();
        boolean invocationErrors = executor.stop();

        client.send("EPBDS-8758/EPBDS-8758.delete");

        assertFalse(deployErrors);
        assertFalse(invocationErrors);
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void test_EPBDS_10068_MANIFEST() {
        client.send("admin_services_no_services.json.get");
        client.post("/admin/deploy", "/EPBDS-10068/EPBDS-10068.zip", 201);
        client.send("EPBDS-10068/MANIFEST.MF.get");
        client.send("EPBDS-10068/EPBDS-10068.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void test_EPBDS_10157() {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy/EPBDS-10157", "/EPBDS-10157/EPBDS-10157.zip", 201);
        client.send("EPBDS-10157/EPBDS-10157-doSomething.get");

        client.post("/admin/deploy/EPBDS-10157_2", "/EPBDS-10157/EPBDS-10157_2.zip", 201);
        client.send("EPBDS-10157/EPBDS-10157_whitespace-doSomething.get");

        client.post("/admin/deploy/EPBDS-9902", "/EPBDS-10157/EPBDS-9902.zip", 201);
        client.send("EPBDS-10157/EPBDS-9902-doSomething.get");

        client.send("EPBDS-10157/EPBDS-10157.delete");
        client.send("EPBDS-10157/EPBDS-10157_whitespace.delete");
        client.send("EPBDS-10157/EPBDS-9902.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_10891() {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-10891/EPBDS-10891.zip", 201);
        client.send("EPBDS-10891/services.get");
        client.send("EPBDS-10891/yaml_project_all.delete");
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-10891/EPBDS-10891.zip", 201);
        client.send("EPBDS-10891/services.get");
        client.send("EPBDS-10891/yaml_project_all.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_9876() {
        client.send("admin_services_no_services.json.get");
        client.post("/admin/deploy", "/EPBDS-9876/deploy_name1_name1.zip", 201);
        client.post("/admin/deploy", "/EPBDS-9876/deploy_samename_name1.zip", 201);
        client.post("/admin/deploy", "/EPBDS-9876/deploy_url1_url1.zip", 201);
        client.post("/admin/deploy", "/EPBDS-9876/deploy_url2_url2.zip", 201);
        client.send("EPBDS-9876/deployed-rules_services.get");
        client.send("EPBDS-9876/deployed-rules.delete");
        client.send("EPBDS-9876/deployed-rules.delete2");
        client.send("EPBDS-9876/deployed-rules.delete3");
        client.send("EPBDS-9876/deployed-rules.delete4");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_11144() {
        client.send("admin_services_no_services.json.get");
        client.post("/admin/deploy/EPBDS-10916%20+Whitespaces in Name #", "/EPBDS-11144/EPBDS-10916%20+Whitespaces in Name #.zip", 201);
        client.send("EPBDS-11144/deployed-rules_services.get");
        client.get("/admin/deploy/EPBDS-10916%20+Whitespaces in Name.zip","/EPBDS-11144/EPBDS-10916%20+Whitespaces in Name #.zip");
        client.send("EPBDS-11144/delete.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_8987() {
        client.send("admin_services_no_services.json.get");
        client.post("/admin/deploy/someDeployment", "/EPBDS-8987/someDeployment.zip", 201);
        client.send("EPBDS-8987/deployed-rules_services_1.get");
        client.post("/admin/deploy/multiple-deployment_v1", "/EPBDS-8987/multiple-deployment_v1.zip", 201);
        client.send("EPBDS-8987/deployed-rules_services_2.1.get");
        client.send("EPBDS-8987/delete_all_1.delete");
        client.send("admin_services_no_services.json.get");
        client.post("/admin/deploy/deployment1", "/EPBDS-8987/deployment1.zip", 201);
        client.send("EPBDS-8987/deployed-rules_services_3.get");
        client.post("/admin/deploy/deployment2", "/EPBDS-8987/deployment2.zip", 201);
        client.send("EPBDS-8987/deployed-rules_services_4.1.get");
        client.send("EPBDS-8987/delete_all_2.delete");
        client.send("admin_services_no_services.json.get");
        client.post("/admin/deploy/deployment1", "/EPBDS-8987/deployment1.zip", 201);
        client.send("EPBDS-8987/deployed-rules_services_3.get");
        client.send("EPBDS-8987/delete_all_2.delete");
        client.send("admin_services_no_services.json.get");
    }
}
