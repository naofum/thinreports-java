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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.github.naofum.thinreports.ThinReportsGenerator;

/**
 * Text Block example
 * 
 * @author Naofumi Fukue
 * 
 */
public class TextBlock {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("single_line_left", "Left(Default)");
		map.put("single_line_center", "Center");
		map.put("single_line_right", "Right");
		map.put("multi_line", "ThinReports Text Block Tool.\n"
				+ "ThinReports Text Block Tool.");
		map.put("datetime_format", new Date());
		map.put("number_format", 99999.9999);
		map.put("padding_format", 999);
		map.put("basic_format", 1980);

		try {
			generator.addPage("text_block.tlf", map);
			generator.generate("text_block.pdf");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}

}
