package hk.edu.hku.cs7506.smartcompanion.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeoBoundsTest {
    @Test
    public void acceptsTypicalHongKongCoordinates() {
        assertTrue(GeoBounds.isLikelyHongKongCoordinate(22.3027, 114.1772));
        assertTrue(GeoBounds.isLikelyHongKongCoordinate(22.2819, 114.1589));
    }

    @Test
    public void rejectsZeroOrOutOfRegionCoordinates() {
        assertFalse(GeoBounds.isLikelyHongKongCoordinate(0.0, 0.0));
        assertFalse(GeoBounds.isLikelyHongKongCoordinate(31.2304, 121.4737));
        assertFalse(GeoBounds.isLikelyHongKongCoordinate(Double.NaN, 114.1));
    }
}
