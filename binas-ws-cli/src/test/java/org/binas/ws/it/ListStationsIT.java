package org.binas.ws.it;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;

import org.binas.ws.*;
import org.binas.station.ws.cli.*;

/**
 * Class that tests listing stations
 */
public class ListStationsIT extends BaseIT {
	
	static Integer k;
	static CoordinatesView coordinates;
    static List<StationView> stations;

    static String stationName1 = "T09_Station1";
	static String stationName2 = "T09_Station2";
	static String stationName3 = "T09_Station3";

	@BeforeClass
	public static void setUp() {

        try {
			client.testInitStation(stationName1, 22, 7, 6, 2);
			client.testInitStation(stationName2, 80, 20, 12, 1);
            client.testInitStation(stationName2, 50, 50, 20, 0);

			k = 3;
			coordinates = new CoordinatesView();
			coordinates.setX(1);
			coordinates.setY(2);

			stations = client.listStations(k, coordinates);

		} catch (Exception e) {
			System.err.println(e.getMessage());
        }

	}

	@Test
	public void success() {
		assertEquals(k.intValue(), stations.size());
	}	
}
