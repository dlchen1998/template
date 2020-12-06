import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ScanPqt extends Pqt{

    private HashMap<String, Integer>template;
    private JSONReader reader;

    public ScanPqt(int type, String text, String tablepath) {

        super(type, text);
        this.schema = new Schema(tablepath);
    }

    @Override
    public void addChild(Pqt pqt){

    }

    public void open(){

        JSONReader jsonreader = null;
        String laststr = "";
        try{
            FileInputStream fileinputstream = new FileInputStream(this.schema.getSchemaName()+".json");
            InputStreamReader inputstreamreader = new InputStreamReader(fileinputstream, "UTF-8");
            jsonreader = new JSONReader(inputstreamreader);

            jsonreader.startObject();

            jsonreader.readString();
            jsonreader.readString();
            jsonreader.readString();
            jsonreader.startArray();

            fileinputstream.close();;
            inputstreamreader.close();
        }catch(IOException e){
            e.printStackTrace();
        }

        this.reader = jsonreader;
    }

    public List<Record> fetch(int size){
        List<Record> returndata = new ArrayList<>();
        for(int i=0;i<size;i++){

            returndata.add(getData());

        }
        return returndata;
    }

    Record getData(){

        if(this.reader.hasNext()){
            this.reader.startObject();
            System.out.println(this.reader.readString());
            System.out.println(this.reader.readString());
            System.out.println(this.reader.readString());
            String originaldata = this.reader.readString();
            this.reader.endObject();
            return this.schema.dataTransform(originaldata);
        }
        else{
            return null;
        }

    }

}

// select 123.XRD详细信息.XRD数据, 123.XRD详细信息.靶材, 123.XRD详细信息.物相组成 from 123 where 123.XRD详细信息.XRD数据 = 1;
