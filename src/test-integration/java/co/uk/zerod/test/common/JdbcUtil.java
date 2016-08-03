package co.uk.zerod.test.common;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.PooledDataSource;

import java.beans.PropertyVetoException;
import java.util.Optional;

public class JdbcUtil {

    public static PooledDataSource createPooledDataSource(String jdbcDriver, String jdbcUrl, Optional<String> jdbcUser, Optional<String> jdbcPassword, int poolSize) throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();

        dataSource.setDriverClass(jdbcDriver);
        dataSource.setJdbcUrl(jdbcUrl);

        jdbcUser.ifPresent(dataSource::setUser);
        jdbcPassword.ifPresent(dataSource::setPassword);

        dataSource.setMaxPoolSize(poolSize);

        return dataSource;
    }
}
