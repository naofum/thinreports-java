/*
 * Copyright 2015 Naofumi Fukue
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples;

import java.util.HashMap;
import java.util.Map;

import com.github.naofum.thinreports.ThinReportsGenerator;

/**
 * Chart example
 * 
 * @author Naofumi Fukue
 * 
 */
public class Chart {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		Map<String, Object> map = new HashMap<String, Object>();

		// Basic Bar Chart
		String bar_chart = getURL("cht=bhs", "chs=240x140",
				"chco=4d89f9,c6d9fd", "chd=t:10,50,60,80,40|50,60,100,40,20",
				"chds=0,160");
		map.put("bar_chart", bar_chart);

		// Basic Line Chart
		String line_chart = getURL("cht=lc", "chs=240x140",
				"chd=t:40,60,60,45,47,75,70,72");
		map.put("line_chart", line_chart);

		// Basic Pie and Radar Chart
		String pie_chart = getURL("cht=p", "chs=240x140",
				"chdl=30°|40°|50°|60°", "chd=s:Uf9a", "chl=Jan|Feb|Mar|Apr");
		String radar_chart = getURL("cht=r", "chs=140x140",
				"chm=B,FF990080,0,0,5", "chls=3,0,0", "chxt=x,y",
				"chd=t:80,30,99,60,50,20",
				"chxl=0:|Str|Vit|Agi|Dex|Int|Lux|1:|||||");
		map.put("pie_chart", pie_chart);
		map.put("radar_chart", radar_chart);

		// 3D-Pie Chart
		String pie_3d_chart = getURL("cht=p3", "chs=250x140",
				"chco=0092b9,86ad00,f2b705,bc3603", "chd=t:21,55.3,18,5.7",
				"chl=A|B|C|D");
		map.put("pie_3d_chart", pie_3d_chart);

		// QR Code
		String qr_code = getURL("cht=qr", "chs=150x150",
				"chl=http://www.thinreports.org/");
		map.put("qr_code", qr_code);

		try {
			generator.addPage("chart.tlf", map);
			generator.generate("chart.pdf");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}

	private static String getURL(String... params) {
		StringBuilder builder = new StringBuilder();
		builder.append("http://chart.googleapis.com/chart?");
		for (int i = 0; i < params.length; i++) {
			if (i > 0) {
				builder.append("&");
			}
			builder.append(params[i]);
		}
		return builder.toString();
	}

}
