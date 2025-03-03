# Hadoop ve MapReduce

Bu depo, Hadoop ve MapReduce kavramlarını uygulamalı olarak öğrenmek için hazırlanmış bir Docker tabanlı eğitim ortamı sunmaktadır. Bu örnek, bir metin dosyasındaki kelimelerin frekansını hesaplayan basit bir WordCount uygulamasını içerir.

## Gereksinimler

*   Docker ve Docker Compose'un kurulu olması gerekmektedir.
*   Java Development Kit (JDK) kurulu olmalıdır.

## Kurulum

1.  Bu repoyu klonlayın:

    ```bash
    git clone https://github.com/tuhage/hadoop-map-reduce-sample
    cd hadoop
    ```

2.  Hadoop ortamını başlatmak için `start_hadoop.sh` betiğini çalıştırın. Bu betik, gerekli Docker container'larını ayağa kaldıracak ve Hadoop servislerinin hazır olmasını bekleyecektir:

    ```bash
    chmod +x start_hadoop.sh
    ./start_hadoop.sh
    ```

    Alternatif olarak, `docker-compose up -d` komutunu kullanarak da ortamı başlatabilirsiniz.

3.  `start_hadoop.sh` betiği, aşağıdaki işlemleri gerçekleştirir:
    *   `docker-compose.yml` dosyasındaki tanımlamalara göre Hadoop bileşenlerini (NameNode, DataNode, ResourceManager, NodeManager, HistoryServer) içeren container'ları başlatır.
    *   HDFS'in "safe mode"dan çıkmasını bekler. HDFS, başlangıçta verilerin güvenliğini sağlamak için "safe mode"da başlar. Bu modda, dosya sistemi üzerinde değişiklik yapılamaz.
    *   Tüm servislerin düzgün bir şekilde çalışır hale gelmesini kontrol eder.

## WordCount Uygulaması

Bu proje, Hadoop MapReduce framework'ünü kullanarak bir metin dosyasındaki kelimelerin sayısını bulan basit bir WordCount uygulaması içerir. Uygulama, `wordcount/WordCount.java` dosyasında bulunmaktadır.

### Kod Açıklaması (`WordCount.java`)

WordCount uygulaması, üç ana bölümden oluşur: Mapper, Reducer ve ana (main) fonksiyon.

#### 1. Mapper (TokenizerMapper)

*   **Görevi:** Girdi olarak aldığı metin satırlarını kelimelere ayırır ve her kelime için `(kelime, 1)` çiftini çıktı olarak üretir.
*   **`map` fonksiyonu:**
    *   Girdi olarak bir metin satırı (`value`) alır.
    *   `StringTokenizer` kullanarak satırı kelimelere böler.
    *   Her kelime için:
        *   `word` değişkenine kelimeyi atar (`word.set(itr.nextToken())`).
        *   `context.write(word, one)` ile `(kelime, 1)` çiftini çıktı olarak yazar. Burada `one` değişkeni, her kelime için 1 değerini temsil eden bir `IntWritable` nesnesidir.

#### 2. Reducer (IntSumReducer)

*   **Görevi:** Mapper tarafından üretilen `(kelime, 1)` çiftlerini alır ve aynı kelimeye sahip olanların değerlerini (1'leri) toplayarak her kelimenin toplam sayısını bulur.
*   **`reduce` fonksiyonu:**
    *   Girdi olarak bir kelime (`key`) ve o kelimeye karşılık gelen tüm 1 değerlerinin bir listesini (`values`) alır.
    *   `sum` değişkeninde kelimenin toplam sayısını tutar.
    *   `values` listesindeki her bir `IntWritable` değeri (`val`) için `sum += val.get()` ile toplama işlemini yapar.
    *   `result.set(sum)` ile toplam sayıyı `result` değişkenine atar.
    *   `context.write(key, result)` ile `(kelime, toplam_sayı)` çiftini çıktı olarak yazar.

#### 3. Ana Fonksiyon (main)

*   **Görevi:** Hadoop MapReduce işini (job) yapılandırır ve çalıştırır.
*   **`main` fonksiyonu:**
    *   Yeni bir Hadoop konfigürasyonu (`Configuration`) oluşturur.
    *   Yeni bir MapReduce işi (`Job`) oluşturur ve işe "word count" adını verir.
    *   `job.setJarByClass(WordCount.class)`: İşin çalıştırılacağı JAR dosyasını belirler (bu sınıfın bulunduğu JAR).
    *   `job.setMapperClass(TokenizerMapper.class)`: Mapper sınıfını belirler.
    *   `job.setCombinerClass(IntSumReducer.class)`: Combiner sınıfını belirler (isteğe bağlı). Combiner, Mapper'dan çıkan verileri Reducer'a göndermeden önce yerel olarak birleştirme işlemi yapar. Bu, ağ trafiğini azaltır ve performansı artırır. Bu örnekte, Combiner ile Reducer aynı işlevi gördüğü için aynı sınıfı kullanıyoruz.
    *   `job.setReducerClass(IntSumReducer.class)`: Reducer sınıfını belirler.
    *   `job.setOutputKeyClass(Text.class)`: Çıktı anahtarının (kelime) tipini belirler (Text).
    *   `job.setOutputValueClass(IntWritable.class)`: Çıktı değerinin (sayı) tipini belirler (IntWritable).
    *   `FileInputFormat.addInputPath(job, new Path(args[0]))`: İşin girdi dosyasının yolunu belirler (komut satırından alınan ilk argüman).
    *   `FileOutputFormat.setOutputPath(job, new Path(args[1]))`: İşin çıktı dosyasının yolunu belirler (komut satırından alınan ikinci argüman).
    *   `job.waitForCompletion(true)`: İşi çalıştırır ve tamamlanmasını bekler. `true` parametresi, işin durumunu konsola yazdırmasını sağlar.
    *   `System.exit(...)`: İş başarılıysa 0, hatalıysa 1 değeriyle programdan çıkar.

### Uygulamayı Çalıştırma

WordCount uygulamasını çalıştırmak için `run_wordcount.sh` betiğini kullanabilirsiniz:

```bash
chmod +x run_wordcount.sh
./run_wordcount.sh
```

Bu betik, aşağıdaki adımları otomatik olarak gerçekleştirir:

1.  `wordcount` dizinini ve içeriğini (WordCount.java ve sample_text.txt) NameNode container'ına kopyalar.
2.  HDFS'in "safe mode"dan çıkmasını bekler.
3.  NameNode container'ı içerisinde:
    *   `/opt/wordcount/classes` dizinini oluşturur.
    *   `WordCount.java` dosyasını derler (`javac`).
    *   Derlenmiş sınıflardan `wordcount.jar` dosyasını oluşturur (`jar`).
    *   HDFS'te `/user/root/wordcount/input` dizinini oluşturur.
    *   `sample_text.txt` dosyasını HDFS'e yükler (`hdfs dfs -put`).
    *   Eğer varsa, önceki çalışmadan kalan `/user/root/wordcount/output` dizinini siler.
    *   MapReduce işini çalıştırır (`hadoop jar ...`).
4.  İşin sonuçlarını (kelime ve sayıları) konsola yazdırır (`hdfs dfs -cat ...`).
5.  Ayrıca, sonuçları HDFS web arayüzünden de (http://localhost:9870) kontrol edebileceğinizi belirtir.

### Manuel Çalıştırma (İsteğe Bağlı)

Eğer WordCount işini adım adım manuel olarak çalıştırmak isterseniz, aşağıdaki komutları kullanabilirsiniz (bu adımlar `run_wordcount.sh` betiğinin içinde de yer almaktadır):

```bash
# NameNode konteynerine bağlanın
docker exec -it namenode bash

# WordCount sınıfını derleyin
mkdir -p /wordcount/classes
javac -classpath $(hadoop classpath) -d /wordcount/classes /opt/wordcount/WordCount.java

# Jar dosyası oluşturun
jar -cvf /wordcount/wordcount.jar -C /wordcount/classes .

# HDFS'in safe mode'dan çıktığından emin olun
hdfs dfsadmin -safemode leave

# HDFS'e giriş dizini oluşturun ve örnek dosyayı yükleyin
hdfs dfs -mkdir -p /user/root/wordcount/input
hdfs dfs -put /opt/wordcount/sample_text.txt /user/root/wordcount/input

# MapReduce işini çalıştırın
hadoop jar /wordcount/wordcount.jar WordCount /user/root/wordcount/input /user/root/wordcount/output

# Sonuçları görüntüleyin
hdfs dfs -cat /user/root/wordcount/output/part-r-00000
```

## HDFS Temel Komutları

HDFS (Hadoop Distributed File System) üzerinde dosya ve dizin işlemleri yapmak için kullanabileceğiniz bazı temel komutlar:

```bash
# Dizin içeriğini listeleme
hdfs dfs -ls /

# Yeni bir dizin oluşturma
hdfs dfs -mkdir -p /user/testuser

# Dosya yükleme
hdfs dfs -put localfile.txt /user/testuser/

# Dosya indirme
hdfs dfs -get /user/testuser/somefile.txt ./

# Dosya içeriğini görüntüleme
hdfs dfs -cat /user/testuser/somefile.txt

# Dosya silme
hdfs dfs -rm /user/testuser/somefile.txt

# Dizin silme (recursive)
hdfs dfs -rm -r /user/testuser

# Safe mode durumunu kontrol etme
hdfs dfsadmin -safemode get

# Safe mode'dan çıkma
hdfs dfsadmin -safemode leave
```

## Web Arayüzleri

Hadoop servislerinin durumunu ve işlerin ilerlemesini takip etmek için aşağıdaki web arayüzlerini kullanabilirsiniz:

*   **HDFS NameNode:** http://localhost:9870
*   **YARN ResourceManager:** http://localhost:8088
*   **MapReduce History Server:** http://localhost:8188

## Sorun Giderme (Troubleshooting)

### "Name node is in safe mode" Hatası

Bu hata, HDFS'in başlangıç güvenlik modunda olduğunu gösterir. HDFS, verilerin güvenliğini sağlamak için başlangıçta "safe mode"da başlar. Bu modda, dosya sistemi üzerinde değişiklik yapılamaz. Çözmek için:

```bash
docker exec -it namenode hdfs dfsadmin -safemode leave
```

### "Input path does not exist" Hatası

Bu hata, MapReduce işinin girdi olarak belirttiğiniz dosya veya dizini HDFS'te bulamadığını gösterir. HDFS'in "safe mode"dan çıktığından ve dosyaların doğru bir şekilde HDFS'e yüklendiğinden emin olun. Gerekirse, dosyaları tekrar yüklemeyi deneyin.

### Bağlantı Sorunları

Eğer servisler arasında bağlantı sorunları yaşıyorsanız (örneğin, NameNode'a bağlanılamıyor hatası), servislerin başlamasını bekleyin veya container'ları yeniden başlatmayı deneyin:

```bash
docker-compose restart
./start_hadoop.sh
```

## Kapatma

Hadoop kümesini kapatmak için:

```bash
docker-compose down
```

Verileri tamamen silmek için (dikkatli olun, bu işlem geri alınamaz):

```bash
docker-compose down -v
```
