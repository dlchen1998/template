import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class test {
    public static void main(String[] args){

        ScanPqt scan = new ScanPqt(1,"scan","20200601090248");
        scan.open();
        List<Record> result = scan.fetch(5);
        //scan.close();


        Record r = (Record) result.get(1);
        System.out.println(result.get(1).getField(Arrays.asList(0,0)));

    }

}
