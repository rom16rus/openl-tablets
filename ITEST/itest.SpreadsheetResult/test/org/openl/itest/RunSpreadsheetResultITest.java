package org.openl.itest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunSpreadsheetResultITest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startSharingClassLoader();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void SpreadsheetResult_Swagger() {
        client.get("/REST/spreadsheetresult/openapi.json", "/spreadsheetresult_openapi.resp.json");
        client.get("/REST/EPBDS-9437/openapi.json", "/EPBDS-9437_openapi.resp.json");
    }

    @Test
    public void SpreadsheetResult_REST() {
        client.post("/REST/spreadsheetresult/tiktak",
            "/spreadsheetresult_tiktak.req.json",
            "/spreadsheetresult_tiktak.resp.json");
        client.post("/REST/EPBDS-9437/tiktak", "/EPBDS-9437_tiktak.req.json", "/EPBDS-9437_tiktak.resp.json");
        client.post("/REST/EPBDS-9437/EPBDS_9437", "/EPBDS-9437_arr.req.txt", "/EPBDS-9437_arr.resp.json");
    }

    @Test
    public void performance() {
        client.post("/EPBDS-9644/mySpr1", "/EPBDS-9644_mySpr1.req.json", "/EPBDS-9644_mySpr1.resp.json");
    }
}
