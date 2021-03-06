/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.singhasdev.calamus.app.sentimentanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class StopWords {
    private List<String> stopWords;
    private static StopWords singleton;

    private StopWords() {
        this.stopWords = new ArrayList<String>();
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(
                    new InputStreamReader(
                            this.getClass().getResourceAsStream("/stop-words.txt")));
            String line = null;
            while ((line = rd.readLine()) != null)
                this.stopWords.add(line);
        } catch (IOException ex) {
        } finally {
            try {
                if (rd != null) rd.close();
            } catch (IOException ex) {
            }
        }
    }

    private static StopWords get() {
        if (singleton == null)
            singleton = new StopWords();
        return singleton;
    }

    public static List<String> getWords() {
        return get().stopWords;
    }
}