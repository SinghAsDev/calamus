# Calamus
Calamus is an end-to-end application that performs sentiment analysis on tweets. It is a Kafka only variant of [Pankh](https://github.com/SinghAsDev/pankh). Calamus replaces custom [twitter-producer](https://github.com/SinghAsDev/pankh/tree/master/twitter-kafka) in Pankh with [kafka-connect-twitter](https://github.com/Eneco/kafka-connect-twitter) and [spark streaming application](https://github.com/SinghAsDev/pankh/tree/master/kafka-spark) for performing sentiment analysis with an kafka-streams app.

# How to build
The applicaiton has following pre-requisites.

# [Apache Kafka](https://github.com/apache/kafka)
# [kafka-connect-twitter](https://github.com/Eneco/kafka-connect-twitter)

# How to run
P.S.: Note that I have used bunch of existing pieces scattered on internet to put together this
application. I have tried to adhere to any of the license requirements. If by any chance,
you see violation of any license agreement, please contact me, @singhasdev,
and we can get it resolved.
