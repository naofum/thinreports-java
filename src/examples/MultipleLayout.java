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
 * Multiple Layout example
 * 
 * @author Naofumi Fukue
 * 
 */
public class MultipleLayout {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		try {
			generator.addPage("multiple_layout_cover.tlf", null);
			for (int i = 0; i < 5; i++) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("content", String.valueOf(i + 1));
				generator.addPage("multiple_layout_default.tlf", map);
			}
			generator.addPage("multiple_layout_back_cover.tlf", null);
			generator.generate("multiple_layout.pdf");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}

}
