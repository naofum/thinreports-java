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
package com.github.naofum.thinreports;

/**
 * Library-wide settings: paths to the IPA TTF font files used to embed CJK
 * text, plus rendering tweaks.
 */
public class Settings {

    private String ipagochic = "ipag.ttf";
    private String ipamincho = "ipam.ttf";
    private String ipapgochic = "ipapg.ttf";
    private String ipapmincho = "ipapm.ttf";

    public String getIpagochic() {
        return ipagochic;
    }

    public void setIpagochic(String ipagochic) {
        this.ipagochic = ipagochic;
    }

    public String getIpamincho() {
        return ipamincho;
    }

    public void setIpamincho(String ipamincho) {
        this.ipamincho = ipamincho;
    }

    public String getIpapgochic() {
        return ipapgochic;
    }

    public void setIpapgochic(String ipapgochic) {
        this.ipapgochic = ipapgochic;
    }

    public String getIpapmincho() {
        return ipapmincho;
    }

    public void setIpapmincho(String ipapmincho) {
        this.ipapmincho = ipapmincho;
    }
}
