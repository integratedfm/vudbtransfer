/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ifm.vudbtransfer;

import ifm.db.MSSqlJDBC;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author ifmuser1
 */
public class VuDBTransfer {

    /**
     * @param args the command line arguments
     */
    final static Logger logger = Logger.getLogger(VuDBTransfer.class);
    private static Properties props;
    private static List<String> errorList = new ArrayList<>();
    private static DateFormat smdf = new SimpleDateFormat("dd-MM-yy hh:mm:ss");
    private static Date cur_date = new Date();

    public static void main(String[] args) throws FileNotFoundException, IOException {
        FileInputStream fis;
        fis = new FileInputStream(new File(args[0]));///read the properties file using the first input argument
        props = new Properties();
        props.load(fis);//load all the propties
        fis.close();

        // TODO code application logic here
    }

    private static List<List<Object>> readAllRecords(String flds, String tableName) throws SQLException {
        String[] fldsa = flds.split(",");
        String sql = "SELECT TOP 10000 " + flds + " FROM " + tableName;
        List<List<Object>> recs = new ArrayList();
        Statement stmt;
        Connection con = MSSqlJDBC.getSqlServerDBConnection();
        if (con == null) {
            return null;
        }
        ResultSet rs = null;
        stmt = con.createStatement();
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
            List<Object> recList = new ArrayList();
            for (String fld : fldsa) {
                Object nco = rs.getObject(fld);
                recList.add(nco);
            }
            recs.add(recList);
        }
        return recs;
    }

    //targetFields expected to be like (fn1, fn2, fn3, ...) + " VALUES (?,?,?...)"
    private static boolean writeAllRecords(String targetflds, String tableName, List<List<Object>> recs) throws SQLException {
        String[] fldsa = targetflds.split(",");
        String sql = "INSERT INTO " + tableName + " " + targetflds;
        Connection con = MSSqlJDBC.getSqlServerDBConnection();
        if(con==null){
            return false;
        }
        PreparedStatement ps;
        //List<PreparedStatement> pstmtList = new ArrayList();
        for (List<Object> rec : recs) {
            ps = MSSqlJDBC.getPreparedStatement(sql);
            for (int k = 1; k < rec.size()+1; k++) {
                ps.setObject(k, rec.get(k-1));
                
            }
            ps.execute();
            con.commit();
            ps.close();            
        }

        return true;
    }

    public static String getProp(String pName) {
        return props.getProperty(pName);
    }

    public static String getCurrentDateTime() {
        cur_date.setTime(System.currentTimeMillis());
        String cur_tm = smdf.format(cur_date);
        return cur_tm;
    }

}
