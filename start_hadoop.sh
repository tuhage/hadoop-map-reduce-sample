#!/bin/bash

# Hadoop kümesini başlatan ve hazır olmasını bekleyen yardımcı script

echo "Hadoop kümesini başlatıyoruz..."
docker-compose up -d

echo "Servislerin başlamasını bekliyoruz..."
sleep 15

echo "HDFS'in hazır olmasını kontrol ediyoruz..."
max_retries=10
retry_count=0

while [ $retry_count -lt $max_retries ]; do
  if docker exec -it namenode hdfs dfs -ls / &> /dev/null; then
    echo "HDFS erişilebilir durumda!"
    
    # Safe mode'u devre dışı bırakalım
    docker exec -it namenode hdfs dfsadmin -safemode leave || true
    
    # HDFS'in safe mode'dan çıkıp çıkmadığını kontrol edelim
    if docker exec -it namenode hdfs dfsadmin -safemode get | grep "Safe mode is OFF"; then
      echo "HDFS safe mode devre dışı bırakıldı!"
      break
    else
      echo "HDFS hala safe mode'da. Yeniden deniyoruz..."
    fi
  else
    echo "HDFS henüz erişilebilir değil. Bekliyoruz... ($((retry_count+1))/$max_retries)"
  fi
  
  retry_count=$((retry_count+1))
  sleep 15
done

if [ $retry_count -eq $max_retries ]; then
  echo "HDFS hazır değil. Lütfen manuel olarak durumu kontrol edin."
  echo "Web arayüzünden kontrol edebilirsiniz: http://localhost:9870"
  exit 1
fi

echo "YARN servisinin hazır olmasını kontrol ediyoruz..."
retry_count=0

while [ $retry_count -lt $max_retries ]; do
  if docker exec -it resourcemanager curl -s http://resourcemanager:8088 &> /dev/null; then
    echo "YARN ResourceManager erişilebilir durumda!"
    break
  else
    echo "YARN ResourceManager henüz erişilebilir değil. Bekliyoruz... ($((retry_count+1))/$max_retries)"
  fi
  
  retry_count=$((retry_count+1))
  sleep 15
done

if [ $retry_count -eq $max_retries ]; then
  echo "YARN hazır değil. Lütfen manuel olarak durumu kontrol edin."
  echo "Web arayüzünden kontrol edebilirsiniz: http://localhost:8088"
  exit 1
fi

echo "Hadoop kümesi hazır! Şimdi MapReduce işlerini çalıştırabilirsiniz."
echo "WordCount örneğini çalıştırmak için: ./run_wordcount.sh"
