/*
 * Copyright (c) 2004, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.test.jdbc2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.postgresql.test.TestUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;

/*
* Some simple tests based on problems reported by users. Hopefully these will help prevent previous
* problems from re-occurring ;-)
*
*/
class TimeTest {
  private Connection con;
  private boolean testSetTime;

  @BeforeEach
  void setUp() throws Exception {
    con = TestUtil.openDB();
    TestUtil.createTempTable(con, "testtime", "tm time, tz time with time zone");
  }

  @AfterEach
  void tearDown() throws Exception {
    TestUtil.dropTable(con, "testtime");
    TestUtil.closeDB(con);
  }

  private static long extractMillis(long time) {
    return time >= 0 ? (time % 1000) : (time % 1000 + 1000);
  }

  /*
   *
   * Test use of calendar
   */
  @Test
  void getTimeZone() throws Exception {
    final Time midnight = new Time(0, 0, 0);
    Statement stmt = con.createStatement();
    Calendar cal = Calendar.getInstance();

    cal.setTimeZone(TimeZone.getTimeZone("GMT"));

    int localOffset = Calendar.getInstance().getTimeZone().getOffset(midnight.getTime());

    // set the time to midnight to make this easy
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'00:00:00','00:00:00'")));
    assertEquals(1,
        stmt.executeUpdate(TestUtil.insertSQL("testtime", "'00:00:00.1','00:00:00.01'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime",
        "CAST(CAST(now() AS timestamp without time zone) AS time),now()")));
    ResultSet rs = stmt.executeQuery(TestUtil.selectSQL("testtime", "tm,tz"));
    assertNotNull(rs);
    assertTrue(rs.next());

    Time time = rs.getTime(1);
    Timestamp timestamp = rs.getTimestamp(1);
    assertNotNull(timestamp);

    Timestamp timestamptz = rs.getTimestamp(2);
    assertNotNull(timestamptz);

    assertEquals(midnight, time);

    time = rs.getTime(1, cal);
    assertEquals(midnight.getTime(), time.getTime() - localOffset);

    assertTrue(rs.next());

    time = rs.getTime(1);
    assertNotNull(time);
    assertEquals(100, extractMillis(time.getTime()));
    timestamp = rs.getTimestamp(1);
    assertNotNull(timestamp);

    assertEquals(100, extractMillis(timestamp.getTime()));

    assertEquals(100000000, timestamp.getNanos());

    Time timetz = rs.getTime(2);
    assertNotNull(timetz);
    assertEquals(10, extractMillis(timetz.getTime()));
    timestamptz = rs.getTimestamp(2);
    assertNotNull(timestamptz);
    assertEquals(10, extractMillis(timestamptz.getTime()));

    assertEquals(10000000, timestamptz.getNanos());

    assertTrue(rs.next());

    time = rs.getTime(1);
    assertNotNull(time);
    timestamp = rs.getTimestamp(1);
    assertNotNull(timestamp);

    timetz = rs.getTime(2);
    assertNotNull(timetz);
    timestamptz = rs.getTimestamp(2);
    assertNotNull(timestamptz);
  }

  /*
   * Tests the time methods in ResultSet
   */
  @Test
  void getTime() throws SQLException {
    Statement stmt = con.createStatement();

    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'01:02:03'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'23:59:59'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'12:00:00'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'05:15:21'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'16:21:51'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'12:15:12'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'22:12:01'")));
    assertEquals(1, stmt.executeUpdate(TestUtil.insertSQL("testtime", "'08:46:44'")));

    // Fall through helper
    timeTest();

    assertEquals(8, stmt.executeUpdate("DELETE FROM testtime"));
    stmt.close();
  }

  /*
   * Tests the time methods in PreparedStatement
   */
  @Test
  void setTime() throws SQLException {
    PreparedStatement ps = con.prepareStatement(TestUtil.insertSQL("testtime", "?"));
    Statement stmt = con.createStatement();

    ps.setTime(1, makeTime(1, 2, 3));
    assertEquals(1, ps.executeUpdate());

    ps.setTime(1, makeTime(23, 59, 59));
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, Time.valueOf("12:00:00"), Types.TIME);
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, Time.valueOf("05:15:21"), Types.TIME);
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, Time.valueOf("16:21:51"), Types.TIME);
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, Time.valueOf("12:15:12"), Types.TIME);
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, "22:12:1", Types.TIME);
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, "8:46:44", Types.TIME);
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, "5:1:2-03", Types.TIME);
    assertEquals(1, ps.executeUpdate());

    ps.setObject(1, "23:59:59+11", Types.TIME);
    assertEquals(1, ps.executeUpdate());

    // Need to let the test know this one has extra test cases.
    testSetTime = true;
    // Fall through helper
    timeTest();
    testSetTime = false;

    assertEquals(10, stmt.executeUpdate("DELETE FROM testtime"));
    stmt.close();
    ps.close();
  }

  /*
   * Helper for the TimeTests. It tests what should be in the db
   */
  private void timeTest() throws SQLException {
    Statement st = con.createStatement();
    ResultSet rs;
    Time t;

    rs = st.executeQuery(TestUtil.selectSQL("testtime", "tm"));
    assertNotNull(rs);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(1, 2, 3), t);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(23, 59, 59), t);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(12, 0, 0), t);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(5, 15, 21), t);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(16, 21, 51), t);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(12, 15, 12), t);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(22, 12, 1), t);

    assertTrue(rs.next());
    t = rs.getTime(1);
    assertNotNull(t);
    assertEquals(makeTime(8, 46, 44), t);

    // If we're checking for timezones.
    if (testSetTime) {
      assertTrue(rs.next());
      t = rs.getTime(1);
      assertNotNull(t);
      Time tmpTime = Time.valueOf("5:1:2");
      int localOffset = Calendar.getInstance().getTimeZone().getOffset(tmpTime.getTime());
      int timeOffset = 3 * 60 * 60 * 1000;
      tmpTime.setTime(tmpTime.getTime() + timeOffset + localOffset);
      assertEquals(makeTime(tmpTime.getHours(), tmpTime.getMinutes(), tmpTime.getSeconds()), t);

      assertTrue(rs.next());
      t = rs.getTime(1);
      assertNotNull(t);
      tmpTime = Time.valueOf("23:59:59");
      localOffset = Calendar.getInstance().getTimeZone().getOffset(tmpTime.getTime());
      timeOffset = -11 * 60 * 60 * 1000;
      tmpTime.setTime(tmpTime.getTime() + timeOffset + localOffset);
      assertEquals(makeTime(tmpTime.getHours(), tmpTime.getMinutes(), tmpTime.getSeconds()), t);
    }

    assertFalse(rs.next());

    rs.close();
  }

  private static Time makeTime(int h, int m, int s) {
    return Time.valueOf(TestUtil.fix(h, 2) + ":" + TestUtil.fix(m, 2) + ":" + TestUtil.fix(s, 2));
  }
}
