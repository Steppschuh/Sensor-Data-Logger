package net.steppschuh.datalogger.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimeTrackerTests {

    @Test
    public void toString_onValidTracking() throws Exception {
        long startTimestamp = 1462716746547l;
        long stopTimestamp = 1462716776547l;

        TimeTracker timeTracker = new TimeTracker("Test");
        timeTracker.setStartTimestamp(startTimestamp);
        timeTracker.setStopTimestamp(stopTimestamp);

        String actual = timeTracker.toString();
        String expected = "Test: 1462716746547 - 1462716776547 (30000ms)";
        assertEquals(expected, actual);
    }

    @Test
    public void duration_onValidEvents() throws Exception {
        long duration = 100;

        TimeTracker timeTracker = new TimeTracker("Test");
        timeTracker.start();
        Thread.sleep(duration);
        timeTracker.stop();

        long actual = timeTracker.getDuration();
        assertTrue("Invalid duration tracked", duration <= actual && actual < duration + 10);
    }

    @Test
    public void duration_onMissingStartEvent() throws Exception {
        TimeTracker timeTracker = new TimeTracker("Test");
        timeTracker.stop();

        long actual = timeTracker.getDuration();
        assertEquals(0, actual);
    }

    @Test
    public void duration_onMissingStopEvent() throws Exception {
        TimeTracker timeTracker = new TimeTracker("Test");
        timeTracker.start();

        long actual = timeTracker.getDuration();
        assertEquals(0, actual);
    }

}
