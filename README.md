# Calamus
Calamus is an end-to-end application that performs sentiment analysis on tweets. It is a Kafka only variant of [Pankh](https://github.com/SinghAsDev/pankh). Calamus replaces custom [twitter-producer](https://github.com/SinghAsDev/pankh/tree/master/twitter-kafka) in Pankh with [kafka-connect-twitter](https://github.com/Eneco/kafka-connect-twitter) and [spark streaming application](https://github.com/SinghAsDev/pankh/tree/master/kafka-spark) for performing sentiment analysis with an kafka-streams app.

# How to build
The applicaiton has following pre-requisites.

### [Apache Kafka](https://github.com/apache/kafka)
The app has been tested on Apache Kafka trunk, as of Apr 25, 2016, i.e., 0.10.1.0-SNAPSHOT.

### [kafka-connect-twitter](https://github.com/Eneco/kafka-connect-twitter)
A few changes were required to make *kafka-connect-twitter* work on Apache Kafka's trunk. [Here](https://github.com/SinghAsDev/kafka-connect-twitter/tree/kafka_0.10) is my branch with required changes.
```
gradle
./gradlew build
```


# How to run
Run kafka-connect-twitter to produce tweets to a topic, right now hard-coded to *test*. Build the app by following steps mentioned above and then run it by running following command.

```
./gradlew run
```

P.S.: Note that I have used bunch of existing pieces scattered on internet to put together this
application. I have tried to adhere to any of the license requirements. If by any chance,
you see violation of any license agreement, please contact me, @singhasdev,
and we can get it resolved.
