package edu.umich.verdict.connectors.impala;

import edu.umich.verdict.Configuration;
import edu.umich.verdict.connectors.DbConnector;
import edu.umich.verdict.connectors.MetaDataManager;
import edu.umich.verdict.connectors.hive.HiveConnector;

import java.sql.SQLException;

public class ImpalaConnector extends DbConnector {
    private HiveConnector hiveConnector;
    public ImpalaConnector(Configuration conf) throws SQLException, ClassNotFoundException {
        super(conf);
        try {
            hiveConnector = new HiveConnector(conf);
        }catch (Exception e){
            //TODO: logger
            System.out.println("WARNING: Could not connect to Hive. You can query on existed samples but you cannot " +
                    "create new samples.");
        }
    }

    @Override
    protected MetaDataManager createMetaDataManager() throws SQLException {
        return new ImpalaMetaDataManager(this, hiveConnector);
    }

    @Override
    protected String getDriverClassPath() {
        return "com.cloudera.impala.jdbc41.Driver";
    }

    @Override
    protected String getProtocolName() {
        return "impala";
    }

    @Override
    protected String getDbmsName() {
        return "Impala";
    }

    @Override
    protected void connect(String user, String password) throws SQLException, ClassNotFoundException {
        super.connect(user, password);
    }

    @Override
    public void close() throws SQLException {
        super.close();
        hiveConnector.close();
    }
}
