
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javafx.util.Pair;

import java.util.*;

/**
 * @ClassName: .Schema.java
 * @Description:
 */
public class Schema {

    private String schemaName;
    private List<String> names; // 字段名
    private Map<String, List<Integer>> idxs; // 字段名 --> 下标
    private Map<List<Integer>,String> fields;
    private Map<String,Integer> type;
    private int leaf;
    private Map<String,Integer> arrays;


    public Schema(String name){
        Template template = new Template(name);
        this.idxs = template.getIdxs();
        this.schemaName = name;
        this.fields = template.getFields();
        this.arrays = template.getArrays();
        this.type = template.getType();
        this.leaf = template.getLeaf();
    }

    public Record dataTransform(String originaldata) {
        Object[] record = new Object[this.leaf];
        JSONObject jsonroot = JSON.parseObject(originaldata);
        List<Pair<Integer,Object>> recordpairs = fieldsTransform(jsonroot,this.schemaName);
        for(Pair<Integer,Object> pair : recordpairs){
            record[pair.getKey()] = pair.getValue();
        }
        return new Record(Arrays.asList(record));
    }

    //输入字段名，获取模板数组index，失败返回空list
    public List<Integer> getValue(String field){

        List<Integer> result = new ArrayList<>();
        if(this.idxs.containsKey(field))result=this.idxs.get(field);

        return result;
    }

    public int getType(String f){
        return this.type.get(f);
    }

    public String getSchemaName() {
        return this.schemaName;
    }

    //返回时通过数组index查找字段
    public String getKey(List<Integer> value){

        String result = fields.get(value);

        return result;
    }

    public List<Pair<Integer, Object>> fieldsTransform(JSONObject jsonroot, String nodename){

        //JSONObject data = JSON.parseObject(originaldata);
        //List<Object> leaf = new ArrayList<>(this.leafCount);
        //String type = this.typeDefine.get(nodename);

        List<Pair<Integer,Object>> result = new ArrayList<>();

        if(!this.idxs.containsKey(nodename)) {
            for(HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
                String jsonchildname = nodename + "." + entry.getKey();
                if(this.arrays.containsKey(jsonchildname)) {
                    JSONArray jsonchild = JSON.parseArray(entry.getValue().toString());
                    result.add(arrayTransform(jsonchild,jsonchildname));
                }

                else if(this.idxs.containsKey(jsonchildname)){

                    List<Integer> idx = this.idxs.get(jsonchildname);
                    int offset = idx.get(idx.size()-1);
                    Object returnvalue = jsonroot.getString(entry.getKey());

                    result.add(new Pair<>(offset,returnvalue));
                }

                else{
                    JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                    result.addAll(fieldsTransform(jsonchild,jsonchildname));
                }
            }
        }

        return result;
    }

    public Pair<Integer,Object> arrayTransform(JSONArray jsonarray,String nodename){
        Pair<Integer,Object> result;
        int arraycount = this.arrays.get(nodename);

        List<Record> returnvalue = new ArrayList<>();

        if(arraycount==0){

            List<Integer> idx = this.idxs.get(nodename);
            int offset = idx.get(idx.size()-1);
            
                for (int i = 0; i < jsonarray.size(); i++) {
                    Object[] element = new Object[1];
                    element[0] = jsonarray.getString(i);
                    returnvalue.add(new Record(Arrays.asList(element)));
                }

            result = new Pair<>(offset,returnvalue);
            return result;
        }


        for(int i=0;i < jsonarray.size();i++){

            Object[] elements = new Object[arraycount];
            JSONObject jsonelement = jsonarray.getJSONObject(i);
            List<Pair<Integer,Object>> lowerelements = new ArrayList<>();

            for(HashMap.Entry<String, Object> entry : jsonelement.entrySet()) {
                String jsonchildname = nodename + "." + entry.getKey();

                if(this.arrays.containsKey(jsonchildname)) {
                    JSONArray jsonchild = JSON.parseArray(entry.getValue().toString());
                    Pair<Integer,Object> tmpfield = arrayTransform(jsonchild,jsonchildname);
                    elements[tmpfield.getKey()] = tmpfield.getValue();
                }

                else if(this.idxs.containsKey(jsonchildname)){

                    List<Integer> idx = this.idxs.get(jsonchildname);
                    int offset = idx.get(idx.size()-1);
                    elements[offset] = jsonelement.getString(entry.getKey());
                }

                else{
                    JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                    List<Pair<Integer,Object>> tmp = fieldsTransform(jsonchild,jsonchildname);
                    for(Pair<Integer,Object> pair:tmp){
                        elements[pair.getKey()] = pair.getValue();
                    }
                }

            }

            returnvalue.add(new Record(Arrays.asList(elements)));

        }

        List<Integer> idx = this.idxs.get(nodename);
        int offset = idx.get(idx.size()-1);

        result = new Pair<>(offset,returnvalue);
        return result;
    }


    public Schema joinSchema(Schema schema){
           return new Schema("");
    }

    public Schema projectSchema(){
        return new Schema("");
    }


}
