import com.alibaba.fastjson.JSONReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Scan算子
 * @ClassName: .ScanPqt.java
 * @Description:
 */
public class ScanPqt extends Pqt{

    /** 维护一个reader流式读取JSON文件 **/
    private JSONReader reader;


    /**
     * 构造函数，通过表名新建一个对应的Schema
     * TODO 多表查询时，需要建立一个容器存储所有基本的Schema
     * @MethodName: ScanPqt
     * @Return
     */
    public ScanPqt(int type, String text, String tablepath) {

        super(type, text);
        schema = new Schema(tablepath);
    }

    public void addChild(Pqt pqt){

    }


    /**
     * 打开文件流，初始化this.reader
     * @MethodName: open
     * @Return void
     */
    @Override
    public void open(){

        JSONReader jsonreader = null;

        try(FileInputStream file = new FileInputStream(schema.getSchemaName()+".json");
            InputStreamReader input = new InputStreamReader(file, "UTF-8")){

            jsonreader = new JSONReader(input);

            /**将reader移动至数据存储的部分**/
            jsonreader.startObject();
            jsonreader.readString();
            jsonreader.readString();
            jsonreader.readString();

            jsonreader.startArray();

        }catch(IOException e){
            e.printStackTrace();
        }


        reader = jsonreader;
    }

    /**
     * 根据需要的数量获取转换后的数据
     * @MethodName: fetch
     * @Return java.util.List<Record>
     */
    @Override
    public List<Record> fetch(int size){

        List<Record> returndata = new ArrayList<>();
        for(int i=0;i<size;i++){

            /**对于每一条JSON原数据，调用schema进行转换**/
            returndata.add(getData());

        }
        return returndata;
    }

    /**
     * 对于每一条数据进行转换
     * @MethodName: getData
     * @Return Record
     */
    Record getData(){

        if(reader.hasNext()){

            /**若当前JSON文件的数据部分没有结束，进入一条新的数据**/
            reader.startObject();
            /**移动reader至数据部分**/
            reader.readString();
            reader.readString();
            reader.readString();

            /**读取该条数据为字符串，转换为Record**/
            String originaldata = reader.readString();
            reader.endObject();
            return schema.dataTransform(originaldata);

        }
        else{
            return null;
        }

    }

    /**
     * 关闭JSON reader
     * TODO JSONReader 关闭存在一点问题
     * @MethodName: close
     * @Return void
     */
    @Override
    public void close(){

        reader.endArray();
        reader.endObject();
        reader.close();
    }


}

// select 123.XRD详细信息.XRD数据, 123.XRD详细信息.靶材, 123.XRD详细信息.物相组成 from 123 where 123.XRD详细信息.XRD数据 = 1;
