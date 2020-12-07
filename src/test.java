import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class test {
    public static void main(String[] args){

        Template template = new Template("20200601090248");
        Schema schema = new Schema("20200601090248");
        //List<Integer> idx = schema.getValue("20200601090248.ARRAY.ARRAY");
        //System.out.println(template.getTemplate());
        //System.out.println(idx);
        //System.out.println(record.getField(idx));
        //System.out.println(template.getLeafCount());

        ScanPqt scan = new ScanPqt(1,"scan","20200601090248");
        scan.open();
        List<Record> result = scan.fetch(5);

        List<Integer> idx = schema.getValue("aaaa");

        //System.out.println(idx.get(0));

        System.out.println(result.get(1).getField(Arrays.asList(0)));

    }

}
