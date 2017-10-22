/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ifm.vudbtransfer;

import ifm.db.MSSqlJDBC;
import ifm.db.OracleJDBC;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

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
    private static String[] FldTypes;
    final static Map<String, Integer> sqlTypeMap = new HashMap();

    static {
        sqlTypeMap.put("str", java.sql.Types.VARCHAR);
        sqlTypeMap.put("int", java.sql.Types.INTEGER);
        sqlTypeMap.put("date", java.sql.Types.DATE);
        sqlTypeMap.put("charstr", java.sql.Types.CHAR);
        sqlTypeMap.put("float", java.sql.Types.FLOAT);
        sqlTypeMap.put("numeric", java.sql.Types.NUMERIC);
    }

    ;

    public static void main(String[] args) throws FileNotFoundException, IOException, SQLException {
        FileInputStream fis;
        fis = new FileInputStream(new File(args[0]));///read the properties file using the first input argument
        props = new Properties();
        props.load(fis);//load all the propties
        fis.close();

        //*
        VuDBTransfer vuDbTx = new VuDBTransfer();
        String srcFlds = getProp("ifm.src.fields");
        String srcTable = getProp("ifm.src.tablename");
        List<List<Object>> lo = vuDbTx.readAllRecords(srcFlds, srcTable);

        String tFlds = getProp("ifm.dest.fields");
        String psTFlds = getProp("ifm.dest.fields.ps");
        //String tTable = getProp("ifm.dest.tablename");
        FldTypes = getProp("ifm.dest.fields.type").split(",");
        //*/
        //*
        boolean bres = vuDbTx.writeAllRecordsPS(psTFlds, lo, FldTypes);
        //*/
        String sqlf = getProp("ifm.sql.file");
        //  exeSqlText(sqlf);
        //writeAllRecordsSQL(tFlds, tTable, lo, FldTypes);

    }

    private static List<List<Object>> readAllRecords(String flds, String tableName) throws SQLException {
        String[] fldsa = flds.split(",");
        String sql = "SELECT TOP 10000 " + flds + " FROM " + tableName;
        List<List<Object>> recs = new ArrayList<>();
        Statement stmt;
        Connection con = MSSqlJDBC.getSqlServerDBConnection();
        if (con == null) {
            return null;
        }
        ResultSet rs = null;
        stmt = con.createStatement();
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
            List<Object> rec = new ArrayList<>();
            for (String fld : fldsa) {
                Object nco = rs.getObject(fld);
                rec.add(nco);
            }
            recs.add(rec);
        }
        rs.close();
        stmt.close();
        return recs;
    }

    //targetFields expected to be like INSERT INTO (WR_ID,DESCRIPTION,IFM_SITE_ID) VALUES (?,?,?)  "
    private static boolean writeAllRecordsSQL(String targetflds, String tableName, List<List<Object>> recs, final String[] fldTypes) throws SQLException {
        String[] fldsa = targetflds.split(",");
        //WR_ID,DESCRIPTION,IFM_SITE_ID

        StringBuilder sqlBuilder;
        Connection con = OracleJDBC.getOracleDBConnection();
        if (con == null) {
            return false;
        }
        Statement stmt = con.createStatement();

        //List<PreparedStatement> pstmtList = new ArrayList();
        for (List<Object> rec : recs) {
            sqlBuilder = new StringBuilder("INSERT INTO " + tableName + targetflds);

            for (int k = 0; k < fldTypes.length; k++) {//append variable part of the insert stmt
                Object no = rec.get(k);
                if (no == null) {
                    sqlBuilder.append("null,");
                    continue;
                }
                if (fldTypes[k].equals("str")) {
                    String s = rec.get(k).toString().trim().replaceAll("'", "");//remove char "'"  to prevent ending a string prematurely in the sql statement
                    sqlBuilder.append("'").append(s).append("',");
                } else {
                    sqlBuilder.append(rec.get(k).toString() + ",");

                }
            }
            sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);
            sqlBuilder.append(")");
            String sql = sqlBuilder.toString();
            System.out.println(sql);
            try {
                stmt.execute(sql);
                con.commit();

            } catch (SQLException ex) {
                logger.error(ex.getMessage());
                System.err.println(ex.getMessage());
            }
        }

        return true;
    }

    private static void exeSqlText(String fn) throws IOException, SQLException {
        Connection con = OracleJDBC.getOracleDBConnection();
        if (con == null) {
            return;
        }
        Statement stmt = con.createStatement();
        File f = new File(fn);
        FileReader frd = new FileReader(f);
        BufferedReader brd = new BufferedReader(frd);
        String ln;
        while ((ln = brd.readLine()) != null) {
            System.out.println(ln);
            stmt.execute(ln.trim());

        }
        con.commit();
        brd.close();
        frd.close();
        stmt.close();
        con.close();
    }

    public static String getProp(String pName) {
        return props.getProperty(pName);
    }

    public static String getCurrentDateTime() {
        cur_date.setTime(System.currentTimeMillis());
        String cur_tm = smdf.format(cur_date);
        return cur_tm;
    }

    //targetFields expected to be like (fn1, fn2, fn3, ...) + " VALUES (?,?,?...)"
    private static boolean writeAllRecordsPS(String targetTflds, List<List<Object>> recs, final String[] fldTypes) throws SQLException {

        //WR_ID,DESCRIPTION,IFM_SITE_ID
        Object[] types = {Types.INTEGER, Types.VARCHAR, Types.VARCHAR};
        String sql = targetTflds;
        Connection con = OracleJDBC.getOracleDBConnection();
        if (con == null) {
            return false;
        }
        PreparedStatement ps;
        //List<PreparedStatement> pstmtList = new ArrayList();
        for (List<Object> rec : recs) {
            ps = OracleJDBC.getPreparedStatement(sql);
            for (int k = 1; k < fldTypes.length + 1; k++) {
                int stype= sqlTypeMap.get(fldTypes[k-1].trim());
                ps.setObject(k, rec.get(k-1), stype);
                
            }
            ps.execute();
            con.commit();
            ps.close();
            System.out.println("Executed  ");
        }
        return true;
    }

}
