package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import DataModel.Fingerprint;
import DataModel.Location;
import DataModel.Measurement;

public class FingerprintHome extends EntityHome<Fingerprint> {

    private static final String[] TableCols = {"locationId", "measurementId"};
    private static final String TableName = "fingerprint";
    private static final String TableIdCol = "fingerprintId";
    private static final String selectFingerprints = " SELECT " + TableName + "." + TableIdCol + ", " + HomeFactory.getLocationHome().getTableColNames() + ", "
            + HomeFactory.getMapHome().getTableColNames() + ", " + HomeFactory.getMeasurementHome().getTableColNames() + ", "
            + " readinginmeasurement.readingClassName, " + HomeFactory.getWiFiReadingHome().getTableColNames() + ", "
            + HomeFactory.getGSMReadingHome().getTableColNames() + ", " + HomeFactory.getBluetoothReadingHome().getTableColNames()
            + " FROM " + TableName + " INNER JOIN location ON fingerprint.locationId = location.locationId "
            + " INNER JOIN map ON location.mapId = map.mapId INNER JOIN measurement ON fingerprint.measurementId = measurement.measurementId "
            + " LEFT OUTER JOIN readinginmeasurement ON readinginmeasurement.measurementId = measurement.measurementId "
            + " LEFT OUTER JOIN wifireading ON wifireading.wifiReadingId = readinginmeasurement.readingId "
            + " LEFT OUTER JOIN gsmreading ON gsmreading.gsmReadingId = readinginmeasurement.readingId "
            + " LEFT OUTER JOIN bluetoothreading ON bluetoothreading.bluetoothReadingId = readinginmeasurement.readingId ";
    private static final String orderFingerprints = " fingerprint.fingerprintId, fingerprint.measurementId, readinginmeasurement.readingClassName ";

    public FingerprintHome() {
        super();
    }

    /**
     * @see EntityHome#getTableIdCol()
     */
    @Override
    protected String getTableIdCol() {
        return TableIdCol;
    }

    /**
     * @see EntityHome#getTableCols()
     */
    @Override
    protected String[] getTableCols() {
        return TableCols;
    }

    /**
     * @see EntityHome#getTableName()
     */
    @Override
    protected String getTableName() {
        return TableName;
    }

    /**
     * @see EntityHome#parseResultRow(ResultSet)
     */
    @Override
    protected Fingerprint parseResultRow(ResultSet rs) throws SQLException {
        Fingerprint f = new Fingerprint();

        try {

            f.setId(rs.getInt(1));
            f.setLocation(HomeFactory.getLocationHome().parseResultRow(rs, 2));
            f.setMeasurement(HomeFactory.getMeasurementHome().parseResultRow(rs, HomeFactory.getLocationHome().getTableCols().length + 2 + HomeFactory.getMapHome().getTableCols().length + 2));

        } catch (SQLException e) {
            throw e;
        }

        return f;
    }

    /**
     * fromIndex has no effect
     *
     * @see EntityHome#parseResultRow(ResultSet, int)
     */
    @Override
    public Fingerprint parseResultRow(ResultSet rs, int fromIndex)
            throws SQLException {
        return parseResultRow(rs, 1);
    }

    /**
     * @see EntityHome#getAll()
     */
    @Override
    public List<Fingerprint> getAll() {
        return getFingerprints(-1, -1, -1);
    }

    /**
     * get {@link Fingerprint}s depending on different constraints. -1 is used
     * for no constraint
     *
     * @param fingerprintId {@link Fingerprint} primary key
     * @param locationId {@link Location} primary key
     * @param measurementId {@link Measurement} primary key
     * @return {@link List} of {@link Fingerprint} matching the constraints
     */
    private List<Fingerprint> getFingerprints(Integer fingerprintId, Integer locationId, Integer measurementId) {
        String cnst = "";
        if (fingerprintId != -1) {
            cnst += getTableName() + "." + getTableIdCol() + " = " + fingerprintId;
        } else if (locationId != -1) {
            cnst += getTableName() + "." + getTableCols()[0] + " = " + locationId;
        } else if (measurementId != -1) {
            cnst += getTableName() + "." + getTableCols()[1] + " = " + measurementId;
        }
        return get(cnst);
    }

    /**
     * @see EntityHome#getSelectSQL()
     */
    @Override
    protected String getSelectSQL() {
        return selectFingerprints;
    }

    @Override
    protected List<Fingerprint> get(String constraint) {
        List<Fingerprint> res = new ArrayList<>();

        String sql = getSelectSQL();
        if (constraint != null && constraint.length() > 0) {
            sql += " WHERE " + constraint;
        }
        String order = getOrder();
        if (order != null && order.length() > 0) {
            sql += " ORDER BY " + order;
        }

        ResultSet rs = null;
        Statement stat = null;
        try {
            stat = db.getConnection().createStatement();
            rs = stat.executeQuery(sql);
            boolean first = true;
            while (!rs.isAfterLast()) {
                /*
                 * only advance cursor the first time, because the reading vector homes (WiFiReadingVectorHome#parseResultRow(), ...)
                 * do advance the cursor one row to far to know whether there are all reading of that type fetched.
                 * If we advance the cursor one more time here, we miss one row.
                 * Unfortunately we can't go one row back (would be a cleaner solution) because the SQLite driver does only support forward cursors
                 */

                if (first) {
                    if (!rs.next()) {
                        //empty result set
                        break;
                    }
                    first = false;
                }

                res.add(parseResultRow(rs));
            }
        } catch (SQLException e) {
            // TODO: 
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stat != null) {
                    stat.close();
                }
            } catch (SQLException es) {
            	// TODO: 
            }
        }

        return res;
    }

    /**
     * @see EntityHome#getOrder()
     */
    @Override
    protected String getOrder() {
        return orderFingerprints;
    }

    /**
     * Gets the number of fingerprints for a {@link Location} id
     *
     * @param constraint SQL WHERE constraint
     * @return the number of Fingerprints
     */
    public int getCount(Integer locationId) {
        if (locationId == null || locationId == -1) {
            return -1;
        }
        return getCount(getTableName() + "." + getTableCols()[0] + " = " + locationId);
    }

    /**
     * Gets the number of fingerprints for a {@link Location}
     *
     * @param constraint SQL WHERE constraint
     * @return the number of Fingerprints
     */
    public int getCount(Location location) {
        if (location == null || location.getId() == 0 || location.getId() == -1) {
            return -1;
        }
        return getCount(location.getId());
    }

    /**
     * Gets the total number of fingerprints matching a constraint
     *
     * @param constraint SQL WHERE constraint
     * @return the number of Fingerprints
     */
    protected int getCount(String constraint) {
        int res = -1;

        String sql = "SELECT COUNT(*) FROM " + TableName;
        if (constraint != null && constraint.length() > 0) {
            sql += " WHERE " + constraint;
        }

        ResultSet rs = null;
        Statement stat = null;
        try {
            stat = DatabaseConnection.getInstance().getConnection().createStatement();
            rs = stat.executeQuery(sql);
            if (rs.next()) {
                res = rs.getInt(1);
            }
        } catch (SQLException e) {
        	// TODO: 
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stat != null) {
                    stat.close();
                }
            } catch (SQLException es) {
            	// TODO: 
            }
        }

        return res;
    }

    /**
     * get the total number of Fingerprints
     *
     * @return the number of Fingerprints
     */
    public int getCount() {
        return getCount((String) null);
    }

    /**
     * @see EntityHome#add(org.redpin.server.standalone.db.IEntity)
     */
    @Override
    public synchronized Fingerprint add(Fingerprint fprint) {
        Connection conn = db.getConnection();
        Vector<PreparedStatement> vps = new Vector<>();
        ResultSet rs = null;

        try {

            conn.setAutoCommit(false);
            Measurement m = (Measurement) fprint.getMeasurement();
            int measurementId = HomeFactory.getMeasurementHome().executeInsertUpdate(vps, m);
            m.setId(measurementId);
            // wifi
            HomeFactory.getWiFiReadingVectorHome().executeUpdate(vps, m.getWiFiReadings(), measurementId);
            // gsm
            HomeFactory.getGSMReadingVectorHome().executeUpdate(vps, m.getGsmReadings(), measurementId);
            // bluetooth
            HomeFactory.getBluetoothReadingVectorHome().executeUpdate(vps, m.getBluetoothReadings(), measurementId);

            Location l = (Location) fprint.getLocation();
            int locationId = l.getId() == null ? -1 : l.getId();
            if (locationId == -1) {
                locationId = HomeFactory.getLocationHome().executeInsertUpdate(vps, l); //.getPrimaryKeyId();
                l.setId(locationId);
            }

            int fingerprintId = executeInsertUpdate(vps, fprint);
            conn.commit();

            return getById(fingerprintId);

        } catch (SQLException e) {
        	// TODO: 
        } finally {
            try {
                conn.setAutoCommit(true);
                if (rs != null) {
                    rs.close();
                }
                for (PreparedStatement p : vps) {
                    if (p != null) {
                        p.close();
                    }
                }
            } catch (SQLException es) {
            	// TODO: 
            }
        }
        return null;
    }

    /**
     * get the fingerprint by its {@link Fingerprint} primary key
     *
     * @param id primary key
     * @return {@link Fingerprint}
     */
    @Override
    public Fingerprint getById(Integer id) {
        if (id == null) {
            return null;
        }
        List<Fingerprint> res = getFingerprints(id, -1, -1);
        return res == null || res.size() == 0 ? null : res.get(0);
    }

    /**
     * get the fingerprints by its {@link Fingerprint} location id
     *
     * @param id primary key
     * @return {@link Fingerprint}
     */
    public List<Fingerprint> getByLocationId(Integer id) {
        if (id == null) {
            return new ArrayList<>();
        }
        return getFingerprints(-1, id, -1);
    }

    /**
     * get the fingerprint by its {@link Fingerprint} measurement id
     *
     * @param id primary key
     * @return {@link Fingerprint}
     */
    public Fingerprint getByMeasurementId(Integer id) {
        if (id == null) {
            return null;
        }
        List<Fingerprint> res = getFingerprints(-1, -1, id);
        return res == null || res.size() == 0 ? null : res.get(0);
    }

    /**
     * @see EntityHome#fillInStatement(PreparedStatement,
     * org.redpin.server.standalone.db.IEntity, int)
     */
    @Override
    public int fillInStatement(PreparedStatement ps, Fingerprint t, int fromIndex) throws SQLException {
        return fillInStatement(ps, new Object[]{((Location) t.getLocation()).getId(), ((Measurement) t.getMeasurement()).getId()},
                new int[]{Types.INTEGER, Types.INTEGER},
                fromIndex);
    }

    /**
     * @see EntityHome#remove(org.redpin.server.standalone.db.IEntity)
     */
    @Override
    protected boolean remove(String constraint) {

        String fingerprintsCnst = (constraint != null && constraint.length() > 0) ? constraint : "1=1";

        String measurementsCnst = HomeFactory.getMeasurementHome().getTableIdCol() + " IN (SELECT " + HomeFactory.getFingerprintHome().getTableCols()[1]
                + " FROM " + HomeFactory.getFingerprintHome().getTableName()
                + " WHERE (" + fingerprintsCnst + ")) ";
        String readingInMeasurementCnst = " IN (SELECT readingId FROM readinginmeasurement WHERE (" + measurementsCnst + ")) ";

        String sql_m = " DELETE FROM " + HomeFactory.getMeasurementHome().getTableName() + " WHERE " + measurementsCnst;
        String sql_wifi = " DELETE FROM " + HomeFactory.getWiFiReadingHome().getTableName()
                + " WHERE " + HomeFactory.getWiFiReadingHome().getTableIdCol() + readingInMeasurementCnst;
        String sql_gsm = " DELETE FROM " + HomeFactory.getGSMReadingHome().getTableName()
                + " WHERE " + HomeFactory.getGSMReadingHome().getTableIdCol() + readingInMeasurementCnst;
        String sql_bluetooth = " DELETE FROM " + HomeFactory.getBluetoothReadingHome().getTableName()
                + " WHERE " + HomeFactory.getBluetoothReadingHome().getTableIdCol() + readingInMeasurementCnst;

        String sql_rinm = "DELETE FROM readinginmeasurement WHERE " + measurementsCnst;
        String sql_fp = "DELETE FROM " + HomeFactory.getFingerprintHome().getTableName() + " WHERE " + fingerprintsCnst;

        Statement stat = null;

        try {
            int res = -1;
            db.getConnection().setAutoCommit(false);
            stat = db.getConnection().createStatement();
            if (db.getConnection().getMetaData().supportsBatchUpdates()) {

                stat.addBatch(sql_wifi);
                stat.addBatch(sql_gsm);
                stat.addBatch(sql_bluetooth);
                stat.addBatch(sql_rinm);
                stat.addBatch(sql_fp);
                stat.addBatch(sql_m);
                int results[] = stat.executeBatch();
                if (results != null && results.length > 0) {
                    res = results[results.length - 2];
                }
            } else {
                stat.executeUpdate(sql_wifi);
                stat.executeUpdate(sql_gsm);
                stat.executeUpdate(sql_bluetooth);
                stat.executeUpdate(sql_rinm);
                res = stat.executeUpdate(sql_fp);
                stat.executeUpdate(sql_m);
            }
            db.getConnection().commit();
            return res > 0;
        } catch (SQLException e) {
        	// TODO: 
        } finally {
            try {
                db.getConnection().setAutoCommit(true);
                if (stat != null) {
                    stat.close();
                }
            } catch (SQLException es) {
            	// TODO: 
            }
        }
        return false;

    }

}
