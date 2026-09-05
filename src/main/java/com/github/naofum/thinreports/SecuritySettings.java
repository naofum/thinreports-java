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
 * PDF security (encryption) settings that can be applied programmatically via
 * {@link TRGenerator#setSecuritySettings(SecuritySettings)}.
 *
 * <p>This mirrors the {@code security} option of the Ruby Thinreports
 * {@code report.generate} call: a user password, an owner password and a set of
 * permission flags. When set, the generated document is encrypted with a
 * 128-bit key.</p>
 */
public class SecuritySettings {

    private String userPassword = "";
    private String ownerPassword = "";
    private boolean canPrint = true;
    private boolean canModify = true;
    private boolean canExtractContent = true;

    public String getUserPassword() {
        return userPassword;
    }

    public SecuritySettings setUserPassword(String userPassword) {
        this.userPassword = userPassword == null ? "" : userPassword;
        return this;
    }

    public String getOwnerPassword() {
        return ownerPassword;
    }

    public SecuritySettings setOwnerPassword(String ownerPassword) {
        this.ownerPassword = ownerPassword == null ? "" : ownerPassword;
        return this;
    }

    public boolean isCanPrint() {
        return canPrint;
    }

    public SecuritySettings setCanPrint(boolean canPrint) {
        this.canPrint = canPrint;
        return this;
    }

    public boolean isCanModify() {
        return canModify;
    }

    public SecuritySettings setCanModify(boolean canModify) {
        this.canModify = canModify;
        return this;
    }

    public boolean isCanExtractContent() {
        return canExtractContent;
    }

    public SecuritySettings setCanExtractContent(boolean canExtractContent) {
        this.canExtractContent = canExtractContent;
        return this;
    }
}
