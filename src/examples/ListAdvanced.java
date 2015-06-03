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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.naofum.thinreports.ThinReportsGenerator;

/**
 * List Advanced example
 * 
 * @author Naofumi Fukue
 * 
 */
public class ListAdvanced {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		Map<String, Object> map = new HashMap<String, Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			int pages = 0;
			int lines = 0;
			int totalLines = 0;
			for (int i = 0; i < 30; i++) {
				// new page
				if (lines >= 21) {
					pages++;
					map.put("detail", list);
					map.put("footer", "Page row count: " + lines);
					generator.addPage("advanced_list.tlf", map);
					map = new HashMap<String, Object>();
					list = new ArrayList<Map<String, Object>>();
					totalLines += lines;
					lines = 0;
				}
				// detail
				Map<String, Object> detail = new HashMap<String, Object>();
				detail.put("detail", "Detail#" + String.valueOf(i));
				list.add(detail);
				lines++;
			}
			// new page
			if (lines > 0) {
				pages++;
				totalLines += lines;
				map.put("detail", list);
				map.put("footer", "Page row count: " + lines);
				map.put("page_footer", "Row count: " + totalLines);
				generator.addPage("advanced_list.tlf", map);
			}

			generator.generate("advanced_list.pdf");
			// generator.generateXml("advanced_list.xml");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}

}
