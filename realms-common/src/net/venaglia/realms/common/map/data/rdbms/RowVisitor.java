package net.venaglia.realms.common.map.data.rdbms;

import net.venaglia.realms.common.util.UncaughtExceptionVisitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: ed
 * Date: 3/24/14
 * Time: 10:16 PM
 */
public interface RowVisitor extends UncaughtExceptionVisitor<ResultSet,SQLException> {
}
