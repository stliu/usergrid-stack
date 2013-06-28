/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.usergrid.count;

import java.util.concurrent.TimeUnit;

import com.usergrid.count.common.Count;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author zznate
 */
public class ScheduledBatcherTest {

    @Test
    public void testScheduledExecution() {
        ScheduledBatcher batcher = new ScheduledBatcher(1,1);
        batcher.setBatchSubmitter(new Slf4JBatchSubmitter());
        batcher.add(new Count("Counter","k1","c1",1));
        batcher.add(new Count("Counter","k1","c1",3));
        batcher.add(new Count("Counter","k1","c2",1));
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        batcher.add(new Count("Counter","k1","c2",1));
        assertEquals(1,batcher.getBatchSubmissionCount());

    }
}
