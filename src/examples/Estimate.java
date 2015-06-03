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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.naofum.thinreports.ThinReportsGenerator;

/**
 * Estimate example
 * 
 * @author Naofumi Fukue
 * 
 */
public class Estimate {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		Map<String, Object> myInfo = new HashMap<String, Object>();
		myInfo.put("my_name", "Matsukei Co., Ltd.");
		myInfo.put("my_address", "735-211, Nogifukutomicho, Matsue-shi, Shimane, Japan");
		myInfo.put("my_post_code", "690-0046");
		myInfo.put("my_tel_number", "+81-854-32-1616");
		myInfo.put("my_fax_number", "+81-852-32-1629");

		Map<String, Object> d1 = new HashMap<String, Object>();
		d1.put("no", 1234);
		d1.put("issued_date", new Date());
		d1.put("customer_name", "Sample1 Co., Ltd.");
		d1.put("customer_address", "1234, Sample1cho, Sample1-shi, Shimane, Japan");
		d1.put("customer_post_code", "123-4567");
		d1.put("notes", "Estimate exsample1!");
		d1.putAll(myInfo);

		Map<String, Object> d2 = new HashMap<String, Object>();
		d2.put("no", 3456);
		d2.put("issued_date", new Date());
		d2.put("customer_name", "Sample2 Co., Ltd.");
		d2.put("customer_address", "3456, Sample2cho, Sample2-shi, Shimane, Japan");
		d2.put("customer_post_code", "345-6789");
		d2.put("notes", "Estimate exsample2!");
		d2.putAll(myInfo);

		double subTotal = 0;
		double total = 0;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("no", 1);
		map.put("name", "iPon6");
		map.put("rate", 199.0);
		map.put("qty", 1);
		map.put("amount", 199.0);
		subTotal += 199.0;
		total += 199.0;
		list.add(map);

		map = new HashMap<String, Object>();
		map.put("no", 2);
		map.put("name", "PearBook Pro 13-inch: 2.9GHz");
		map.put("rate", 1499.0);
		map.put("qty", 2);
		map.put("amount", 2998.0);
		subTotal += 2998.0;
		total += 2998.0;
		list.add(map);
		d1.put("detail", list);

		d1.put("sub_total", subTotal);
		d1.put("total", total);

		try {
			generator.addPage("estimate.tlf", d1);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

		try {
			subTotal = 0;
			total = 0;
			int line = 0;
			list = new ArrayList<Map<String, Object>>();

			for (int i = 0; i < 35; i++) {
				// new page
				if (line >= 29) {
					d2.put("sub_total", subTotal);
					d2.put("detail", list);
					generator.addPage("estimate.tlf", d2);
					list = new ArrayList<Map<String, Object>>();
					line = 0;
					subTotal = 0;
				}
				map = new HashMap<String, Object>();
				map.put("no", i + 1);
				map.put("name", "xxxxxxxxxx");
				map.put("rate", 500);
				map.put("qty", 1);
				map.put("amount", 500);
				subTotal += 500;
				total += 500;
				list.add(map);
				line++;
			}
			// new page
			if (line > 0) {
				d2.put("sub_total", subTotal);
				d2.put("total", total);
				d2.put("detail", list);
				generator.addPage("estimate.tlf", d2);
			}

			generator.generate("estimate.pdf");
			// generator.generateXml("estimate.xml");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}

}
