import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class test {
    public static void main(String[] args) throws Exception {

        SchemaManager manager = new SchemaManager();
        manager.init();

        Schema schema1 = manager.getSchema("ZL114A_铝合金数据-2");
        Schema schema2 = manager.getSchema("ZL114A_铝合金数据-3");

        Schema schema3 = schema1.joinSchema(schema2);


        ScanPqt t1 = new ScanPqt(1,"scan","jointest1");
        ScanPqt t2 = new ScanPqt(1,"scan","jointest2");
        ScanPqt t3 = new ScanPqt(1,"scan","jointest3");
        //HashMap<String,List<Integer>> t3 =(HashMap<String,List<Integer>>)t2.schema.getIdxs();
        //<String,List<Integer>> t4 =(HashMap<String,List<Integer>>)t3.clone();

        t1.open();
        t2.open();
        t3.open();

        List<Record> t1result = t1.fetch(10);
        List<Record> t2result = t2.fetch(100);
        List<Record> t3result = t3.fetch(100);

        Schema joined = t1.schema.joinSchema(t2.schema);
        joined = joined.joinSchema(t3.schema);
        Record joinedr = t1result.get(0).joinRecords(t2result.get(0));
        joinedr = joinedr.joinRecords(t3result.get(0));

        System.out.println(joinedr.getFields());

        List<String> selectfields = new ArrayList<>();
        selectfields.add("jointest1.d");
        selectfields.add("jointest2.e.f");
        selectfields.add("jointest3.i.j");

        List<List<Integer>> idxs = new ArrayList<>();

        for(String field: selectfields){
            List<Integer> tmp = joined.getValue(field);
            idxs.add(tmp);
        }

        Schema projected = joined.projectSchema(selectfields);
        Record projectedr = joinedr.projectRecord(idxs);


        ReturnData rt = new ReturnData();

        System.out.println(rt.record2JSON(projectedr,projected,selectfields));

    }

}
