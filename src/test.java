import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class test {
    public static void main(String[] args){

        ScanPqt t1 = new ScanPqt(1,"scan","jointest1");
        ScanPqt t2 = new ScanPqt(1,"scan","jointest2");
        HashMap<String,List<Integer>> t3 =(HashMap<String,List<Integer>>)t2.schema.getIdxs();
        HashMap<String,List<Integer>> t4 =(HashMap<String,List<Integer>>)t3.clone();

        t1.open();
        t2.open();

        List<Record> t1result = t1.fetch(10);
        List<Record> t2result = t2.fetch(100);

        Schema joined = t1.schema.joinSchema(t2.schema);
        Record joinedr = t1result.get(0).joinRecords(t2result.get(0));

        System.out.println(joinedr.getFields());

        List<String> selectfields = new ArrayList<>();
        selectfields.add("jointest1.d");
        selectfields.add("jointest2.e.f");

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
