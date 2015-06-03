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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.naofum.thinreports.ThinReportsGenerator;

/**
 * List Group Rows example
 * 
 * @author Naofumi Fukue
 * 
 */
public class ListGroup {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> detail = new HashMap<String, Object>();
		detail.put("name", "Smith");
		detail.put("blood_group", "A");
		detail.put("age", 21);
		list.add(detail);
		detail = new HashMap<String, Object>();
		detail.put("name", "Johnson");
		detail.put("blood_group", "B");
		detail.put("age", 35);
		list.add(detail);
		detail = new HashMap<String, Object>();
		detail.put("name", "James");
		detail.put("blood_group", "A");
		detail.put("age", 18);
		list.add(detail);
		detail = new HashMap<String, Object>();
		detail.put("name", "Linda");
		detail.put("blood_group", "O");
		detail.put("age", 25);
		list.add(detail);
		detail = new HashMap<String, Object>();
		detail.put("name", "Robert");
		detail.put("blood_group", "B");
		detail.put("age", 24);
		list.add(detail);
		detail = new HashMap<String, Object>();
		detail.put("name", "Mary");
		detail.put("blood_group", "O");
		detail.put("age", 39);
		list.add(detail);

		// sort
		Collections.sort(list, new ListComparator());

		// group
		List<Map<String, Object>> newlist = new ArrayList<Map<String, Object>>();
		String bloodType = "";
		for (int i = 0; i < list.size(); i++) {
			if (!bloodType.equals(list.get(i).get("blood_group"))) {
				Map<String, Object> d = new HashMap<String, Object>();
				d.put("blood_group", list.get(i).get("blood_group"));
				newlist.add(d);
				bloodType = (String) list.get(i).get("blood_group");
			}
			Map<String, Object> d = new HashMap<String, Object>();
			d.put("name", list.get(i).get("name"));
			d.put("age", list.get(i).get("age"));
			newlist.add(d);
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("detail", newlist);

		try {
			generator.addPage("group_rows.tlf", map);
			generator.generate("group_rows.pdf");
			// generator.generateXml("group_rows.xml");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}
}

class ListComparator implements Comparator<Map<String, Object>> {
	public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
		return (((String) arg0.get("blood_group")).compareTo((String) arg1
				.get("blood_group")));
	}
}
