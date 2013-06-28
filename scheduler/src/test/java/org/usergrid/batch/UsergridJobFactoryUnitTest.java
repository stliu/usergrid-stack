package org.usergrid.batch;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.batch.repository.JobDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author zznate
 */
public class UsergridJobFactoryUnitTest {

  private static UUID jobId = UUID.randomUUID();

  @Test
  public void verifyBuildup() throws JobNotFoundException {
    JobDescriptor jobDescriptor = new JobDescriptor("",
            jobId,UUID.randomUUID(), null, null, null);
   
    

    List<Job> bulkJobs =  BulkTestUtils.getBulkJobFactory().jobsFrom(jobDescriptor);
    assertNotNull(bulkJobs);
    assertEquals(1, bulkJobs.size());
  }




}
