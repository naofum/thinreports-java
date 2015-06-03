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
 * End user defined characters example
 * 
 * @author Naofumi Fukue
 * 
 */
public class Eudc {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("eudc", "日本で生まれ世界が育てた言語\nuby");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("eudcFont", "file:eudc.xml");

		try {
			generator.addPage("eudc.tlf", map);
			generator.generate("eudc.pdf", params);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}

}
