/*
* Copyright 2015 FUJITSU LIMITED.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.o3project.ocnrm.model.bind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;

public class OduBindingData extends RmData {

    @Override
    public void bind(String name, String resource)
            throws JsonParseException, JsonMappingException, IOException {
        JsonFactory factory = new JsonFactory();
        JsonParser jp = factory.createParser(resource.toString());
        jp.nextToken();
        OduMapping terminationPoint = new OduMapping();
        terminationPoint.setName(name);
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken();
            if ("dpid".equals(fieldname)) {
                terminationPoint.setDpid(jp.getText());
            } else if ("port".equals(fieldname)) {
                terminationPoint.setPort(jp.getText());
            } else if ("odutype".equals(fieldname)) {
                terminationPoint.setOdutype(jp.getText());
            } else if ("ts".equals(fieldname)) {
                String ts = jp.getText();
                terminationPoint.setTs(ts);
            } else if ("tpn".equals(fieldname)) {
                terminationPoint.setTpn(jp.getText());
            } else {
                throw new IllegalStateException("Unrecognized field '" + fieldname + "'!");
            }
            bindMap.put(terminationPoint.getName(), terminationPoint);
        }
        jp.close();
    }

}
