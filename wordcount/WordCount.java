import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCount {

    // Mapper sınıfı: Girdi satırlarını kelimelere ayırır.
    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable> {

        // Her kelime için 1 değerini temsil eden IntWritable nesnesi.
        private final static IntWritable one = new IntWritable(1);
        // İşlenen kelimeyi saklamak için Text nesnesi.
        private Text word = new Text();

        // Map fonksiyonu: Her girdi satırı için çağrılır.
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            // Girdi satırını StringTokenizer ile kelimelere ayırır.
            StringTokenizer itr = new StringTokenizer(value.toString());
            // Her bir kelime için döngüye girer.
            while (itr.hasMoreTokens()) {
                // Text nesnesine sıradaki kelimeyi atar.
                word.set(itr.nextToken());
                // Kelimeyi ve 1 değerini (kelimenin bir kez geçtiğini belirtir) context'e yazar.
                // context, Mapper'ın çıktısını Reducer'a iletmek için kullanılır.
                context.write(word, one);
            }
        }
    }

    // Reducer sınıfı: Her kelimenin toplam sayısını hesaplar.
    public static class IntSumReducer
            extends Reducer<Text, IntWritable, Text, IntWritable> {

        // Toplam sonucu saklamak için IntWritable nesnesi.
        private IntWritable result = new IntWritable();

        // Reduce fonksiyonu: Bir kelime ve o kelimenin tüm tekrarları (1 değerleri) için çağrılır.
        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context) throws IOException, InterruptedException {
            int sum = 0;
            // Kelimenin tüm tekrarlarını (1 değerlerini) toplar.
            for (IntWritable val : values) {
                sum += val.get();
            }
            // Toplam sonucu result nesnesine atar.
            result.set(sum);
            // Kelimeyi ve toplam sayısını context'e yazar.
            // context, Reducer'ın çıktısını (sonucu) yazmak için kullanılır.
            context.write(key, result);
        }
    }

    // Ana (main) fonksiyon: MapReduce işini yapılandırır ve çalıştırır.
    public static void main(String[] args) throws Exception {
        // Yeni bir Hadoop konfigürasyonu oluşturur.
        Configuration conf = new Configuration();
        // Yeni bir MapReduce işi (job) oluşturur ve "word count" adını verir.
        Job job = Job.getInstance(conf, "word count");
        // İşin çalıştırılacağı JAR dosyasını belirler (bu sınıfın bulunduğu JAR).
        job.setJarByClass(WordCount.class);
        // Mapper sınıfını belirler.
        job.setMapperClass(TokenizerMapper.class);
        // Combiner sınıfını belirler (isteğe bağlı).
        // Combiner, Mapper'dan çıkan verileri Reducer'a göndermeden önce yerel olarak birleştirme işlemi yapar.
        // Bu, ağ trafiğini azaltır ve performansı artırır.
        // Bu örnekte, Combiner ile Reducer aynı işlevi gördüğü için aynı sınıfı kullanıyoruz.
        job.setCombinerClass(IntSumReducer.class);
        // Reducer sınıfını belirler.
        job.setReducerClass(IntSumReducer.class);
        // Çıktı anahtarının (kelime) tipini belirler (Text).
        job.setOutputKeyClass(Text.class);
        // Çıktı değerinin (sayı) tipini belirler (IntWritable).
        job.setOutputValueClass(IntWritable.class);
        // İşin girdi dosyasının yolunu belirler (komut satırından alınan ilk argüman).
        FileInputFormat.addInputPath(job, new Path(args[0]));
        // İşin çıktı dosyasının yolunu belirler (komut satırından alınan ikinci argüman).
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        // İşi çalıştırır ve tamamlanmasını bekler.
        // 'true' parametresi, işin durumunu konsola yazdırmasını sağlar.
        // İş başarılıysa 0, hatalıysa 1 değeriyle programdan çıkar.
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
