/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.i18n.addressinput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.EnumMap;

public class LocalDataSource implements DataSource {
    private static final String DATA_PATH = "/countryinfo.txt";

    public LocalDataSource() {
    }

    public static final Map<String, String> DATA;

    static {
        DATA = new HashMap<String, String>();
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(LocalDataSource.class.getResourceAsStream(DATA_PATH),
                            "utf-8"));
            String line = null;
            while (null != (line = br.readLine())) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                int x = line.indexOf('=');
                DATA.put(line.substring(0, x), line.substring(x + 1));
            }
        } catch (IOException e) {
            System.err.println("unable to create map: " + e.getMessage());
        }
    }

    private boolean isCountryKey(String hierarchyKey) {
        Util.checkNotNull(hierarchyKey, "Cannot use null as a key");
        return hierarchyKey.split("/").length == 2;
    }

    private String getCountryKey(String hierarchyKey) {
        if (hierarchyKey.split("/").length <= 1) {
            throw new RuntimeException("Cannot get country key with key '" + hierarchyKey + "'");
        }
        if (isCountryKey(hierarchyKey)) {
            return hierarchyKey;
        }

        String[] parts = hierarchyKey.split("/");

        return new StringBuilder().append(parts[0])
                .append("/")
                .append(parts[1])
                .toString();
    }

    @Override
    public AddressVerificationNodeData get(String key) {
        String jsonString = DATA.get(key);
        if (jsonString != null) {
          JsoMap jso = null;
          try {
              jso = JsoMap.buildJsoMap(jsonString);
          } catch (JSONException e) {
            System.err.println("unable to get: " + e.getMessage());
            return null;
          }
          if (jso != null) {
              return createNodeData(jso);
          }
        }
        return null;
    }

    @Override
    public AddressVerificationNodeData getDefaultData(String key) {
        // root data
        if (key.split("/").length == 1) {
          return get(key);
        }

        return get(getCountryKey(key));
    }

    /**
     * Returns the contents of the JSON-format string as a map.
     */
    protected AddressVerificationNodeData createNodeData(JsoMap jso) {
        Map<AddressDataKey, String> map =
                new EnumMap<AddressDataKey, String>(AddressDataKey.class);

        JSONArray arr = jso.getKeys();
        for (int i = 0; i < arr.length(); i++) {
            try {
                AddressDataKey key = AddressDataKey.get(arr.getString(i));

                if (key == null) {
                    // Not all keys are supported by Android, so we continue if we encounter one
                    // that is not used.
                    continue;
                }

                String value = jso.get(key.toString().toLowerCase());
                map.put(key, value);
            } catch (JSONException e) {
                // This should not happen - we should not be fetching a key from outside the bounds
                // of the array.
            }
        }

        return new AddressVerificationNodeData(map);
    }
}
