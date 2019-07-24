package com.hw.spark;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import javax.xml.bind.SchemaOutputResolver;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SparkApplication {
	private static final Logger logger = LoggerFactory.getLogger(SparkApplication.class);

	public static void main(String[] args) {

		org.apache.spark.SparkConf conf = new org.apache.spark.SparkConf();
		conf.setAppName("NMPI Calculator");
		conf.setMaster("local[*]");
		JavaSparkContext sc = new JavaSparkContext(conf);
		List<String> bigramPreparement = new ArrayList<>();

		List<String> rddStopWords = sc.textFile("/home/wanderdn/stop_words_en-xpo6.txt").collect();

		JavaRDD<String> rddWords = sc.textFile("/home/wanderdn/articles-part");

		 rddWords.collect().stream().map(String::toLowerCase).map(x->x.replaceAll("[^\\d\\w]", " ")).forEach(
				x->  Arrays.asList(x.split(" ")).stream().filter(s->!x.isEmpty()).filter(s->!s.equals("")).forEach(bigramPreparement::add));

		int totalWordsCount =  bigramPreparement.size()-1;
bigramPreparement.removeAll(rddStopWords);
		List<String> biGrams = new ArrayList<>();
		Stream.iterate(0, i -> i + 1)
				.limit(bigramPreparement.size() - 2)
				.map(x -> bigramPreparement.get(x).concat("_").concat(bigramPreparement.get(x + 1))).forEach(biGrams::add);

		HashMap<String, Integer> wordCount = new HashMap<>();
		sc.parallelize(bigramPreparement).mapToPair(w -> new Tuple2<>(w, 1))
				.reduceByKey(Integer::sum).collect().forEach(tuple ->
				wordCount.put(tuple._1, tuple._2)
		);
		int bigramCount = biGrams.size() - 1;
		List<Tuple2<String,Double>> result = sc.parallelize(biGrams).mapToPair(w ->
				new Tuple2<>(w, 1))
				.reduceByKey(Integer::sum)
				.filter(tuple -> tuple._2 >= 500)
				.mapToPair(tuple ->
						new Tuple2<>(tuple._1,

								(Math.log(((double)tuple._2 / bigramCount) / Arrays.stream(tuple._1.split("_"))
										.mapToDouble((x) ->
												(double) wordCount.get(x) / totalWordsCount)
										.reduce((x, y) ->
												Double.parseDouble(new BigDecimal(x).multiply(new BigDecimal(y)).toString()))
										.getAsDouble())
										/ Math.log((double) tuple._2 / bigramCount) * (-1)))
				).collect();
		result.stream().sorted(Comparator.comparing(Tuple2::_2)).forEach(tuple ->
				logger.debug (tuple._1 + "\t" + new DecimalFormat("#.###").format(tuple._2)));
		sc.close();
	}

}
