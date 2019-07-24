#!/bin/bash
./gradlew shadowJar
spark-submit --master yarn  --jars "/home/x52019q2/x5_nekrasov/spark/build/libs/hw-0.0.1.jar"  --deploy-mode client  --class "com.hw.spark.SparkApplication" /home/x52019q2/bigramms/build/libs/x5_nekrasov/hw-0.0.1.jar

