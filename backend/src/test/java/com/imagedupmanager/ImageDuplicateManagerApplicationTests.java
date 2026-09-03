package com.imagedupmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ImageDuplicateManagerApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
		// Verifica que el contexto Spring arranca con JPA/Hibernate + SQLiteDialect.
	}

	@Test
	void sqliteDataSourceIsReachable() throws Exception {
		// Prueba minima de resolucion: comprueba que el driver SQLite conecta.
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SELECT 1")) {
			resultSet.next();
			assertEquals(1, resultSet.getInt(1));
		}
	}

}
