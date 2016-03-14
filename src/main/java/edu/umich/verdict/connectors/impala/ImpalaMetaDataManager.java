package edu.umich.verdict.connectors.impala;

import edu.umich.verdict.connectors.DbConnector;
import edu.umich.verdict.connectors.hive.HiveConnector;
import edu.umich.verdict.connectors.MetaDataManager;
import edu.umich.verdict.models.Sample;

import java.sql.SQLException;

public class ImpalaMetaDataManager extends MetaDataManager {
    private HiveConnector hiveConnector;

    public ImpalaMetaDataManager(DbConnector connector, HiveConnector hiveConnector) throws SQLException {
        super(connector);
        this.hiveConnector = hiveConnector;
    }

    protected String createStratifiedSample(Sample sample, long tableSize) throws SQLException {
        String tmp1 = METADATA_DATABASE + ".temp1", tmp2 = METADATA_DATABASE + ".temp2", tmp3 = METADATA_DATABASE + ".temp3";
        executeStatement("drop table if exists " + tmp1);
        String strataCols = sample.getStrataColsStr();
        System.out.println("Collecting groups stats...");
        executeStatement("create table  " + tmp1 + " as (select " + strataCols + ", count(*) as cnt from " + sample.tableName + " group by " + strataCols + ")");
        computeTableStats(tmp1);
        long groups = getTableSize(tmp1);
        long groupLimit = (long) ((tableSize * sample.compRatio) / groups);
        executeStatement("drop table if exists " + tmp2);
        StringBuilder buf = new StringBuilder();
        for (String s : getTableCols(sample.tableName))
            buf.append(",").append(s);
        buf.delete(0, 1);
        String cols = buf.toString();
        System.out.println("Creating sample with Hive... (This can take minutes)");
        hiveConnector.executeStatement("create table " + tmp2 + " as select " + cols + " from (select " + cols + ", rank() over (partition by " + strataCols + " order by rand()) as rnk from " + sample.tableName + ") s where rnk <= " + groupLimit + "");
        executeStatement("invalidate metadata");
        executeStatement("drop table if exists " + tmp3);
        executeStatement("create table  " + tmp3 + " as (select " + strataCols + ", count(*) as cnt from " + tmp2 + " group by " + strataCols + ")");
        String joinConds = sample.getJoinCond("s", "t");
        System.out.println("Calculating group weights...");
        executeStatement("create table " + sample.getWeightsTable() + " as (select s." + strataCols.replaceAll(",", ",s.") + ", t.cnt/s.cnt as ratio, t.cnt/" + tableSize + " as weight from " + tmp1 + " as t join " + tmp3 + " as s on " + joinConds + ")");
        executeStatement("drop table if exists " + tmp1);
        executeStatement("drop table if exists " + tmp3);
        return tmp2;
    }

    protected String createUniformSample(Sample sample) throws SQLException {
        long buckets = Math.round(1 / sample.compRatio);
        String tmp1 = METADATA_DATABASE + ".temp_sample";
        System.out.println("Creating sample with Hive... (This can take minutes)");
        hiveConnector.executeStatement("drop table if exists " + tmp1);
        String create = "create table " + tmp1 + " as select * from " + sample.tableName + " tablesample(bucket 1 out of " + buckets + " on rand())";
        hiveConnector.executeStatement(create);
        return tmp1;
    }

}