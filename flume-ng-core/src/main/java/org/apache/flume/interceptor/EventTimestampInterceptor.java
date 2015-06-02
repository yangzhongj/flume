/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flume.interceptor;

import org.apache.commons.lang.time.DateUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by shangri_la on 15/6/1.
 */
public class EventTimestampInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory
            .getLogger(EventTimestampInterceptor.class);

    private final boolean preserveExisting;
    private final byte[] delimiter;
    private final String[] dateFormat = new String[1];
    private final int index;

    public EventTimestampInterceptor(boolean preserveExisting, String delimiter, String dateFormatStr, int index) {
        this.preserveExisting = preserveExisting;
        this.delimiter = delimiter.getBytes();
        this.index = index;
        this.dateFormat[0] = dateFormatStr;
    }

    @Override
    public void initialize() {
        //logger.info("EventTimestampInterceptor  initializing..............");
    }

    @Override
    public Event intercept(Event event) {
        Map<String, String> headers = event.getHeaders();
        if (preserveExisting && headers.containsKey(Constants.TIMESTAMP)) {
            // we must preserve the existing timestamp
        } else {
            long now = -1;
            try {
                byte[] data = event.getBody();
                String timestamp = get(index, data);
                now = DateUtils.parseDate(timestamp, dateFormat).getTime();
                headers.put(Constants.TIMESTAMP, Long.toString(now));
            } catch (Exception e) {
                logger.warn(
                        "Setting system time as timestamp header due to this error: {}",
                        e.getMessage());
                now = System.currentTimeMillis();
                headers.put(Constants.TIMESTAMP, Long.toString(now));
            }
        }
        return event;
    }

    @Override
    public List<Event> intercept(List<Event> events) {
        for (Event event : events) {
            intercept(event);
        }
        return events;
    }

    @Override
    public void close() {
        //logger.info("EventTimestampInterceptor closing.............." );
    }



    private String get(int index , byte[] data){
        int start = -1;
        int end = -1;
        int currentIndex = 0;

        for (int i = 0; i < data.length; i++) {
            if (start == -1 && currentIndex == index){
                start = i;
            }

            for (int j = 0; j < delimiter.length; j++) {
                if (data[i + j] == delimiter[j]) {
                    if (j == delimiter.length - 1) {
                        currentIndex++; // matches delimiter, next
                        if (start != -1 && end == -1) {
                            end = i;// delimiter start index - field start index
                            // = length of the field;
                        }
                        i += delimiter.length - 1; // multi char sep handling
                    }
                } else {
                    break;
                }
            }
        }

        if (end == -1) {
            end = data.length;
        }

        return new String(data, start, end - start);
    }


    public static class Builder implements org.apache.flume.interceptor.Interceptor.Builder {

        private boolean preserveExisting = Constants.PRESERVE_DFLT;
        private String delimiter = ",";
        private String dateFormat = "yyyy-MM-dd HH:mm:ss";
        private int index = 0;

        public Interceptor build() {
            return new EventTimestampInterceptor(preserveExisting, delimiter,
                    dateFormat, index);
        }

        public void configure(Context context) {
            preserveExisting = context.getBoolean(Constants.PRESERVE, Constants.PRESERVE_DFLT);
            delimiter = context.getString(Constants.DELIMITER, delimiter);
            dateFormat = context.getString(Constants.FORMAT, dateFormat);
            index = context.getInteger(Constants.INDEX, index);
        }
    }

    public static class Constants {
        public static String TIMESTAMP = "timestamp";
        public static String PRESERVE = "preserveExisting";
        public static boolean PRESERVE_DFLT = false;

        public static String DELIMITER = "delimiter";
        public static String INDEX = "dateIndex";
        public static String FORMAT = "dateFormat";
    }


}
