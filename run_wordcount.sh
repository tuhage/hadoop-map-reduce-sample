#!/bin/bash

# Bu script, WordCount örneğini çalıştırmak için gerekli adımları otomatikleştirir

echo "Hadoop ortamının çalıştığından emin olun (docker-compose up -d)..."

echo "NameNode konteynerine dosyaları kopyalama..."
docker cp ./wordcount namenode:/opt/

echo "HDFS'in hazır olmasını bekliyoruz..."
# Safe mode'dan çıkmasını bekle
docker exec -it namenode bash -c "
  echo 'HDFS'in safe mode'dan çıkmasını bekliyoruz...'
  until hdfs dfsadmin -safemode get | grep 'Safe mode is OFF'; do
    hdfs dfsadmin -safemode leave || true
    echo 'Safe mode hala aktif, 10 saniye bekliyoruz...'
    sleep 10
  done
  echo 'HDFS hazır!'
"

echo "NameNode konteynerinde WordCount uygulamasını derleme ve çalıştırma..."
docker exec -it namenode bash -c "
mkdir -p /opt/wordcount/input
mkdir -p /opt/wordcount/classes

# WordCount sınıfını derleme
javac -encoding UTF-8 -classpath \$(hadoop classpath) -d /opt/wordcount/classes /opt/wordcount/WordCount.java

# JAR dosyası oluşturma
jar -cvf /opt/wordcount/wordcount.jar -C /opt/wordcount/classes .

# HDFS'e giriş dizini oluşturma
hdfs dfs -mkdir -p /user/root/wordcount/input

# Sample dosyasını HDFS'e kopyalama
hdfs dfs -put /opt/wordcount/sample_text.txt /user/root/wordcount/input

# Eğer önceki bir çıktı varsa temizleme
hdfs dfs -rm -r -f /user/root/wordcount/output

# MapReduce işini çalıştırma
hadoop jar /opt/wordcount/wordcount.jar WordCount /user/root/wordcount/input /user/root/wordcount/output

echo '=== SONUÇLAR ==='
hdfs dfs -cat /user/root/wordcount/output/part-r-00000
"

echo "İşlem tamamlandı. Sonuçları HDFS web arayüzünden de kontrol edebilirsiniz: http://localhost:9870"
