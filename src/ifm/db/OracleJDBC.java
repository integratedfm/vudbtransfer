/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ifm.db;

import ifm.vudbtransfer.VuDBTransfer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import jdk.nashorn.internal.ir.Statement;
import org.apache.log4j.Logger;

/**
 *
 * @author ifmuser1
 */
public class OracleJDBC {
    private static String DB_CONNECTION;
    private static String DB_USER;
    private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
    private static String DB_PASSWORD;
    private static Connection dbConnection = null;
    final static Logger logger = Logger.getLogger(OracleJDBC.class);

    public static Connection getOracleDBConnection() {
        if (dbConnection == null) {
            DB_CONNECTION = VuDBTransfer.getProp("ifm.rmit.dbconnection");
            DB_USER = VuDBTransfer.getProp("ifm.rmit.db_user");
            DB_PASSWORD = VuDBTransfer.getProp("ifm.rmit.db_passwd");

            try {
                //logger.debug("Trying to get DB_Driver");
                Class.forName(DB_DRIVER);
                //logger.debug("Got DB_Driver");

            } catch (ClassNotFoundException ex) {
                logger.error(ex.getMessage());
            }
            try {
                //logger.debug("Trying to get DB Connection");
                dbConnection = DriverManager.getConnection(
                        DB_CONNECTION, DB_USER, DB_PASSWORD);
                //logger.debug("Got DB Connection");
                dbConnection.setAutoCommit(false);
                return dbConnection;

            } catch (SQLException ex) {
                logger.error(ex.getMessage());
                
            }
        }

        return dbConnection;
    }

    public static PreparedStatement getPreparedStatement(String sql) {
        PreparedStatement pstmt = null;
        try {
            Connection con = getOracleDBConnection();
            //logger.debug("Trying to get prepared statement for the connection");
            pstmt = con.prepareStatement(sql);
            //logger.debug("Got prepared statement for the connection");
            return pstmt;

        } catch (SQLException ex) {
            logger.error(ex.getMessage());
            
        }
        return pstmt;
    }

    
    
}
