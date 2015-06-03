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
 * List Basic example
 * 
 * @author Naofumi Fukue
 * 
 */
public class ListBasic {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		Map<String, Object> map = new HashMap<String, Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			int line = 0;
			for (int i = 0; i < 30; i++) {
				// new page
				if (line >= 22) {
					map.put("detail", list);
					generator.addPage("basic_list.tlf", map);
					map = new HashMap<String, Object>();
					list = new ArrayList<Map<String, Object>>();
					line = 0;
				}
				Map<String, Object> detail = new HashMap<String, Object>();
				detail.put("detail", "row#" + String.valueOf(i));
				list.add(detail);
				line++;
			}
			// new page
			if (line > 0) {
				map.put("detail", list);
				generator.addPage("basic_list.tlf", map);
			}

			generator.generate("basic_list.pdf");
			// generator.generateXml("basic_list.xml");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}

}
