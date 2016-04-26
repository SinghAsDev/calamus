/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.singhasdev.calamus.app.sentimentanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.processor.TopologyBuilder;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class SentimentAnalyzer {
    private static KafkaStreams STREAMS;
    private static Logger LOG = Logger.getLogger(SentimentAnalyzer.class.getClass());

    private static class CalculateSentiment implements ProcessorSupplier<String, String> {

        @Override
        public Processor<String, String> get() {
            return new Processor<String, String>() {
                private ProcessorContext context;
                private KeyValueStore<String, String> kvStore;

                @Override
                @SuppressWarnings("unchecked")
                public void init(ProcessorContext context) {
                    this.context = context;
                    this.context.schedule(1000);
                    this.kvStore = (KeyValueStore<String, String>) context.getStateStore("SentimentAnalysis");
                }

                @Override
                public void process(String dummy, String line) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readValue(line, JsonNode.class);
                        final JsonNode payload = root.get("payload");
                        if (payload != null) {
                            JsonNode tweetIdJson = payload.get("id");
                            JsonNode createdAtJson = payload.get("createdAt");
                            JsonNode favoriteCountJson = payload.get("favoriteCount");
                            JsonNode textNodeJson = payload.get("text");
                            JsonNode userNodeJson = payload.get("user");
                            if (tweetIdJson != null && createdAtJson != null && favoriteCountJson != null &&
                                    textNodeJson != null && userNodeJson != null) {
                                Long tweetId = tweetIdJson.longValue();

                                DateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH);
                                inputFormat.setLenient(true);
                                Long timestamp = inputFormat.parse(createdAtJson.asText()).getTime();

                                Long favoriteCount = favoriteCountJson.longValue();
                                String tweet = textNodeJson.textValue();
                                Long userId = userNodeJson.get("id").longValue();
                                String userName = userNodeJson.get("name").textValue();

                                String modifiedText = tweet.replaceAll("[^a-zA-Z\\s]", "").trim().toLowerCase(Locale.getDefault());
                                for (String word : StopWords.getWords()) {
                                    modifiedText = modifiedText.replaceAll("\\b" + word + "\\b", "");
                                }

                                List<String> posWords = PositiveWords.getWords();
                                String[] words = modifiedText.split(" ");
                                int numWords = words.length;
                                int numPosWords = 0;
                                for (String word : words) {
                                    if (posWords.contains(word))
                                        numPosWords++;
                                }

                                List<String> negWords = NegativeWords.getWords();
                                int numNegWords = 0;
                                for (String word : words) {
                                    if (negWords.contains(word))
                                        numNegWords++;
                                }

                                float posPercent = (float) numPosWords / numWords;
                                float negPercent = (float) numNegWords / numWords;

                                TweetSentiment tweetSentiment = new TweetSentiment(tweetId, timestamp, favoriteCount, tweet, userId, userName, posPercent, negPercent);

                                System.out.println(tweetSentiment);
                                String tweetSentimentJson = mapper.writeValueAsString(tweetSentiment);

                                this.kvStore.put(timestamp.toString(), tweetSentimentJson);
                            }
                        }
                    } catch (IOException ex) {
                        LOG.error("IO error while processing tweets", ex);
                        LOG.trace(null, ex);
                    } catch (ParseException ex) {
                        LOG.error("Parse exception while processing tweets", ex);
                        LOG.trace(null, ex);
                    }
                    context.commit();
                }

                @Override
                public void punctuate(long timestamp) {
                    KeyValueIterator<String, String> iter = this.kvStore.all();

                    LOG.debug("----------- " + timestamp + " ----------- ");

                    while (iter.hasNext()) {
                        KeyValue<String, String> entry = iter.next();

                        //System.out.println("[" + entry.key + ", " + entry.value + "]");

                        context.forward(entry.key, entry.value);
                    }

                    iter.close();
                }

                @Override
                public void close() {
                    this.kvStore.close();
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "sentiment-analyzer");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, "localhost:2181");
        props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        TopologyBuilder builder = new TopologyBuilder();

        builder.addSource("Source", "test");

        builder.addProcessor("Process", new CalculateSentiment(), "Source");
        builder.addStateStore(Stores.create("SentimentAnalysis").withStringKeys().withStringValues().inMemory().build(), "Process");

        builder.addSink("Sink", "test-output", "Process");

        STREAMS = new KafkaStreams(builder, props);
        STREAMS.start();

        Runtime.getRuntime().addShutdownHook(new Thread("MirrorMakerShutdownHook") {
            @Override
            public void run() {
                System.out.println("Closing Calamus sentiment-analyzer.");
                STREAMS.close();
            }
        });
    }

    private static class TweetSentiment {
        public float getNegativePercentage() {
            return negativePercentage;
        }

        public long getTweetId() {
            return tweetId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getFavoriteCount() {
            return favoriteCount;
        }

        public String getTweet() {
            return tweet;
        }

        public long getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public float getPositivePercentage() {
            return positivePercentage;
        }

        TweetSentiment(long tweetId, long timestamp, long favoriteCount, String tweet, long userId,
                       String userName, float positivePercentage, float negativePercentage) {
            this.tweetId = tweetId;
            this.timestamp = timestamp;
            this.favoriteCount = favoriteCount;
            this.tweet = tweet;
            this.userId = userId;
            this.userName = userName;
            this.positivePercentage = positivePercentage;
            this.negativePercentage = negativePercentage;
        }

        private long tweetId;
        private long timestamp;
        private long favoriteCount;
        private String tweet;
        private long userId;
        private String userName;
        private float positivePercentage;
        private float negativePercentage;

        @Override
        public String toString() {
            return "TweetID: " + tweetId +
                    "\t Timestamp: " + timestamp +
                    "\t FavoriteCount: " + favoriteCount +
                    "\t Tweet: " + tweet +
                    "\t UserID: " + userId +
                    "\t UserName: " + userName +
                    "\t PositivePercent: " + positivePercentage +
                    "\t NegativePercent: " + negativePercentage;
        }
    }
}
