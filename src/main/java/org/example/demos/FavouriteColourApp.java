package org.example.demos;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class FavouriteColourApp {

    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,"favourite-colour-java");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"172.29.79.53:9092");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        List<String> acceptedColours = Arrays.asList("green", "blue", "red");

        KStream<String, String> inputLines = builder.stream("favourite-colour-input");

        KStream<String, String> usersAndColours = inputLines
                .filter((key, value) -> value.contains(","))
                .selectKey((key, value) -> value.split(",")[0].toLowerCase())
                .mapValues(value -> value.split(",")[1].toLowerCase())
                .filter((user, color) -> acceptedColours.contains(color));

        usersAndColours.to("user-keys-and-colours");

        KTable<String, String> usersAndColoursTable = builder.table("user-keys-and-colours");

        KTable<String, Long> favouriteColours = usersAndColoursTable
                .groupBy((user, colour) -> new KeyValue<>(colour, colour))
                .count();

        favouriteColours.toStream().to("favourite-colour-output", Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), properties);
        streams.start();

        System.out.println(streams.toString());

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));


    }


}
