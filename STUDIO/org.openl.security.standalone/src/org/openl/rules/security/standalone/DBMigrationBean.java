package org.openl.rules.security.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBMigrationBean {
    private static final Logger LOG = LoggerFactory.getLogger(DBMigrationBean.class);

    private DataSource dataSource;
    private List<String> locations;

    public void init() throws SQLException, IOException {

        String databaseCode;
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
        }

        locations.add("/db/flyway/" + databaseCode);

        TreeMap<String, String> placeholders = new TreeMap<>();
        for (String location : locations) {
            fillQueries(placeholders, location + "/placeholders.properties");
        }
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setBaselineVersionAsString("0");
        flyway.setBaselineOnMigrate(true);
        flyway.setTable("openl_security_flyway");
        flyway.setPlaceholders(placeholders);

        flyway.setLocations(locations.toArray(new String[0]));
        flyway.migrate();
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setLocations(String locations) {
        this.locations = new ArrayList<>(Arrays.asList(locations.split(",")));
    }

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        URL resource = getClass().getResource(propertiesFileName);
        if (resource == null) {
            LOG.info("File '{}' is not found.", propertiesFileName);
            return;
        }
        LOG.info("Load properties from '{}'.", resource);
        InputStream is = resource.openStream();
        try {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                queries.put(key, properties.getProperty(key));
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
