package net.venaglia.realms.common.map.data.rdbms;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: ed
 * Date: 3/24/14
 * Time: 10:16 PM
 */
public interface RowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
