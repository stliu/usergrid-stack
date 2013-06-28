package org.usergrid.system;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.utils.MapUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author zznate
 */
@RunWith(CassandraRunner.class)
public class UsergridSystemMonitorTest {


    private UsergridSystemMonitor usergridSystemMonitor;

    @Before
    public void setupLocal() {
        usergridSystemMonitor = CassandraRunner.getBean(UsergridSystemMonitor.class);
    }

    @Test
    public void testVersionNumber() {
        assertEquals("0.1", usergridSystemMonitor.getBuildNumber());
    }

    @Test
    public void testIsCassandraAlive() {
        assertTrue(usergridSystemMonitor.getIsCassandraAlive());
    }

  @Test
  public void verifyLogDump() {
    String str = usergridSystemMonitor.formatMessage(1600L, MapUtils.hashMap("message", "hello"));

    assertTrue(StringUtils.contains(str, "hello"));

    usergridSystemMonitor.maybeLogPayload(16000L, "foo","bar","message","some text");

    usergridSystemMonitor.maybeLogPayload(16000L, new Date() );
  }

}
