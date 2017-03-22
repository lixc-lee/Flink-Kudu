package es.accenture.flink.Job;

import es.accenture.flink.Sink.KuduSink;
import es.accenture.flink.Utils.RowSerializable;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer09;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

import java.util.Properties;
import java.util.UUID;

/**
 * Job which reads from kafka, make some changes, and writes the new data into a Kudu database
 */
public class JobStreamingInputOutput {

    public static void main(String[] args) throws Exception {

        /********Only for test, delete once finished*******/
        args[0] = "TableKafka";
        args[1] = "topicKudu";
        args[2] = "localhost";
        /**************************************************/

        if(args.length!=3){
            System.out.println( "JobStreamingInputOutput params: [TableToWrite] [Topic] [Master Address]\n");
            return;
        }

        // Params of program
        String tableName = args[0];
        String topic = args[1];
        String KUDU_MASTER = args[2];

        String [] columnNames = new String[2];
        columnNames[0] = "col1";
        columnNames[1] = "col2";

        UUID id = UUID.randomUUID();

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Properties prop = new Properties();
        prop.setProperty("bootstrap.servers", "localhost:9092");
        prop.setProperty("group.id", String.valueOf(id));
        prop.setProperty("auto.offset.reset", "latest");
        prop.setProperty("zookeeper.connect", "localhost:2181");
        prop.setProperty("topic", topic);

        DataStream<String> stream = env.addSource(new FlinkKafkaConsumer09<>(
                prop.getProperty("topic"),
                new SimpleStringSchema(),
                prop));

        DataStream<RowSerializable> stream2 = stream.map(new MyMapFunction());

        stream2.addSink(new KuduSink(KUDU_MASTER, tableName, columnNames));

        env.execute();
    }


    /**
     * Map function which receives a String, splits it, and creates as many row as word has the string
     * This row contains two fields, first field is a serial generated automatically starting in 0,
     * second field is the substring generated by the split function.
     */
        private static class MyMapFunction implements MapFunction<String, RowSerializable>{

        @Override
        public RowSerializable map(String input) throws Exception {

            RowSerializable res = new RowSerializable(2);
            Integer i = 0;
            for (String s : input.split(" ")) {
                /* Needed to prevent exception on map function if phrase has more than 3 words */
                if(i<2) res.setField(i, s);
                i++;
            }
            return res;
        }
    }
}