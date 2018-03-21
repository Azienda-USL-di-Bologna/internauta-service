package it.bologna.ausl.baborg.config.spring;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.sql2o.Sql2o;

@SuppressWarnings("SpringJavaAutowiringInspection")
@Configuration
public class EclipseLinkJPAConfig extends JpaBaseConfiguration {

    @Value("${eclipselink.target-database}")
    private String targetDatabase;
    @Value("${eclipselink.ddl-generation}")
    private String ddlGeneration;
    @Value("${eclipselink.weaving}")
    private String weaving;
    @Value("${eclipselink.logging.level}")
    private String logginLevel;
    @Value("${eclipselink.logging.level.sql}")
    private String logginLevelSQL;

    protected EclipseLinkJPAConfig(DataSource dataSource, JpaProperties properties, ObjectProvider<JtaTransactionManager> jtaTransactionManager, ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        super(dataSource, properties, jtaTransactionManager, transactionManagerCustomizers);
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("eclipselink.weaving", weaving);
        properties.put("eclipselink.target-database", targetDatabase);
        properties.put("eclipselink.ddl-generation", ddlGeneration);
        properties.put("eclipselink.logging.level", logginLevel);
        properties.put("eclipselink.logging.level.sql", logginLevelSQL);
        properties.put(PersistenceUnitProperties.TARGET_DATABASE_PROPERTIES, "shouldBindLiterals=false");
        return properties;
    }

    @Bean
    public InstrumentationLoadTimeWeaver loadTimeWeaver() throws Throwable {
        InstrumentationLoadTimeWeaver loadTimeWeaver = new InstrumentationLoadTimeWeaver();
        return loadTimeWeaver;
    }

    @Autowired
    private DataSource dataSource;

    @Bean
    Sql2o sql2o() {
        return new Sql2o(dataSource);
    }
}
