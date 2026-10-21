package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.sql.Connection;
public class DBConnection {
    private static final String DB_URL_ENV = "DB_URL";
    private static final String DB_USERNAME_ENV = "DB_USERNAME";
    private static final String DB_PASSWORD_ENV = "DB_PASSWORD";

    public static Connection getConnection() throws SQLException {
        loadMySqlDriver();
        Properties properties = new Properties();

        try (InputStream input = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new IllegalStateException("db.properties를 찾을 수 없습니다.");
           }

            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("db.properties를 읽을 수 없습니다.", e);
        }

        return DriverManager.getConnection(
                setting(DB_URL_ENV, properties.getProperty("db.url")),
                setting(DB_USERNAME_ENV, properties.getProperty("db.username")),
                setting(DB_PASSWORD_ENV, properties.getProperty("db.password"))
        );
    }

    private static String setting(String environmentVariable, String fallback) {
        String value = System.getProperty(environmentVariable);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void loadMySqlDriver() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC 드라이버를 찾을 수 없습니다.", e);
        }
    }
}
