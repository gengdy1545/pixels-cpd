package cn.edu.ruc.iir.pixels.daemon.metadata.dao;

import cn.edu.ruc.iir.pixels.common.metadata.domain.Schema;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Table;
import cn.edu.ruc.iir.pixels.common.utils.DBUtil;
import cn.edu.ruc.iir.pixels.common.utils.LogFactory;
import org.apache.commons.logging.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableDao implements Dao<Table>
{
    public TableDao() {}

    private static final DBUtil db = DBUtil.Instance();
    private static final Log log = LogFactory.Instance().getLog();
    private static final SchemaDao schemaModel = new SchemaDao();

    @Override
    public Table getById(int id)
    {
        Connection conn = db.getConnection();
        try (Statement st = conn.createStatement())
        {
            ResultSet rs = st.executeQuery("SELECT TBL_NAME, TBL_TYPE, DBS_DB_ID FROM TBLS WHERE TBL_ID=" + id);
            if (rs.next())
            {
                Table table = new Table();
                table.setId(id);
                table.setName(rs.getString("TBL_NAME"));
                table.setType(rs.getString("TBL_TYPE"));
                table.setSchema(schemaModel.getById(rs.getInt("DBS_DB_ID")));
                return table;
            }
        } catch (SQLException e)
        {
            log.error("getById in TableDao", e);
        }

        return null;
    }

    @Override
    public List<Table> getAll()
    {
        throw new UnsupportedOperationException("getAll is not supported.");
    }

    public Table getByNameAndSchema (String name, Schema schema)
    {
        Connection conn = db.getConnection();
        try (Statement st = conn.createStatement())
        {
            ResultSet rs = st.executeQuery("SELECT TBL_ID, TBL_TYPE FROM TBLS WHERE TBL_NAME='" + name +
                    "' AND DBS_DB_ID=" + schema.getId());
            if (rs.next())
            {
                Table table = new Table();
                table.setId(rs.getInt("TBL_ID"));
                table.setName(name);
                table.setType(rs.getString("TBL_TYPE"));
                table.setSchema(schema);
                schema.addTable(table);
                return table;
            }

        } catch (SQLException e)
        {
            log.error("getByNameAndDB in TableDao", e);
        }

        return null;
    }

    public List<Table> getByName(String name)
    {
        Connection conn = db.getConnection();
        try (Statement st = conn.createStatement())
        {
            ResultSet rs = st.executeQuery("SELECT TBL_ID, TBL_TYPE, DBS_DB_ID FROM TBLS WHERE TBL_NAME='" + name + "'");
            List<Table> tables = new ArrayList<>();
            while (rs.next())
            {
                Table table = new Table();
                table.setId(rs.getInt("TBL_ID"));
                table.setName(name);
                table.setType(rs.getString("TBL_TYPE"));
                table.setSchema(schemaModel.getById(rs.getInt("DBS_DB_ID")));
                tables.add(table);
            }
            return tables;

        } catch (SQLException e)
        {
            log.error("getByName in TableDao", e);
        }

        return null;
    }

    public List<Table> getBySchema(Schema schema)
    {
        Connection conn = db.getConnection();
        try (Statement st = conn.createStatement())
        {
            ResultSet rs = st.executeQuery("SELECT TBL_ID, TBL_NAME, TBL_TYPE, DBS_DB_ID FROM TBLS WHERE DBS_DB_ID=" + schema.getId());
            List<Table> tables = new ArrayList<>();
            while (rs.next())
            {
                Table table = new Table();
                table.setId(rs.getInt("TBL_ID"));
                table.setName(rs.getString("TBL_NAME"));
                table.setType(rs.getString("TBL_TYPE"));
                table.setSchema(schema);
                tables.add(table);
            }
            return tables;

        } catch (SQLException e)
        {
            log.error("getBySchema in TableDao", e);
        }

        return null;
    }

    public boolean save (Table table)
    {
        if (exists(table))
        {
            return update(table);
        }
        else
        {
            return insert(table);
        }
    }

    /**
     * If the table with the same id or with the same db_id and table name exists,
     * this method returns false.
     * @param table
     * @return
     */
    public boolean exists (Table table)
    {
        Connection conn = db.getConnection();
        try (Statement st = conn.createStatement())
        {
            String sql = "SELECT 1 FROM TBLS WHERE TBL_ID=" + table.getId();
            if (table.getSchema() != null)
            {
                sql += " OR (DBS_DB_ID=" + table.getSchema().getId() +
                        " AND TBL_NAME='" + table.getName() + "')";
            }
            ResultSet rs = st.executeQuery(sql);
            if (rs.next())
            {
                return true;
            }
        } catch (SQLException e)
        {
            log.error("exists in TableDao", e);
        }

        return false;
    }

    public boolean insert (Table table)
    {
        Connection conn = db.getConnection();
        String sql = "INSERT INTO TBLS(" +
                "`TBL_NAME`," +
                "`TBL_TYPE`," +
                "`DBS_DB_ID`) VALUES (?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql))
        {
            pst.setString(1, table.getName());
            pst.setString(2, table.getType());
            pst.setInt(3, table.getSchema().getId());
            int flag = pst.executeUpdate();
            return flag > 0;
        } catch (SQLException e)
        {
            log.error("insert in TableDao", e);
        }
        return false;
    }

    public boolean update (Table table)
    {
        Connection conn = db.getConnection();
        String sql = "UPDATE TBLS\n" +
                "SET\n" +
                "`TBL_NAME` = ?," +
                "`TBL_TYPE` = ?\n" +
                "WHERE `TBL_ID` = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql))
        {
            pst.setString(1, table.getName());
            pst.setString(2, table.getType());
            pst.setInt(3, table.getId());
            return pst.execute();
        } catch (SQLException e)
        {
            log.error("insert in TableDao", e);
        }
        return false;
    }

    /**
     * We use cascade delete and cascade update in the metadata database.
     * If you delete a table by this method, all the layouts and columns of the table
     * will be deleted.
     * @param name
     * @param schema
     * @return
     */
    public boolean deleteByNameAndSchema (String name, Schema schema)
    {
        assert name !=null && schema != null;
        Connection conn = db.getConnection();
        String sql = "DELETE FROM TBLS WHERE TBL_NAME=? AND DBS_DB_ID=?";
        try (PreparedStatement pst = conn.prepareStatement(sql))
        {
            pst.setString(1, name);
            pst.setInt(2, schema.getId());
            return pst.executeUpdate() == 1;
        } catch (SQLException e)
        {
            log.error("delete in TableDao", e);
        }
        return false;
    }
}
