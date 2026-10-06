package com.portfolio.sistemaloja.db;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConexaoFonte {

    Connection getConnection() throws SQLException;
}
