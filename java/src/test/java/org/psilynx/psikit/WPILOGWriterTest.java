package org.psilynx.psikit;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Random;

public class WPILOGWriterTest extends TestCase {

    public void testPutTable() {
        LogTable table = new LogTable(0L);
        table.put("test", "a");

        WPILOGWriter writer = new WPILOGWriter("data");
        writer.start();
        writer.putTable(table);
        writer.end();
    }

}