

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
//import javafx.util.Pair;
//import scu.mge.mdb.template.Template;

import java.util.*;

/**
 * @ClassName: .Schema.java
 * @Description: Schema类，包含转换模板，类型定义，数据转换方法，
 */
public class Schema {

    class Pair{

        private final int key;
        private final Object value;

        Pair(int k, Object v){

            key = k;
            value = v;

        }

        int getKey(){
            return key;
        }

        Object getValue(){
            return value;
        }
    }

    private String schemaName;//schema名称
    private List<String> names; // 字段名
    private Map<String, List<Integer>> idxs; // 字段名 --> 下标
    private Map<List<Integer>, String> fields;// 下标 --> 字段名
    private Map<String, Integer> type;//字段名 --> 类型
    private int leaf;//Record 第一层长度
    private Map<String, Integer> arrays;//数组字段名 --> 数组元素叶子节点个数

    /**
     * 无参构造
     * @MethodName: Schema
     * @Return
     */
    Schema(){
    }

    /**
     * 构造函数（通过template）
     * @MethodName: Schema
     */
    public Schema(String name) {
        Template template = new Template(name);
        this.idxs = template.getIdxs();
        this.schemaName = name;
        this.fields = template.getFields();
        this.arrays = template.getArrays();
        this.type = template.getType();
        this.leaf = template.getLeaf();
    }

    /**
     * 数据转换，开始递归
     * @MethodName: dataTransform
     * @Return Record
     */
    public Record dataTransform(String originaldata) {
        Object[] record = new Object[this.leaf];
        JSONObject jsonroot = JSON.parseObject(originaldata);
        List<Pair> recordpairs = fieldsTransform(jsonroot, this.schemaName);
        for (Pair pair : recordpairs) {
            record[pair.getKey()] = pair.getValue();
        }
        return new Record(Arrays.asList(record));
    }

    /**
     * 递归调用：普通JSONObject
     *
     * @MethodName: fieldsTransform
     * @Return List<Pair < Integer, Object>>
     */
    List<Pair> fieldsTransform(JSONObject jsonroot, String nodename) {
        List<Pair> result = new ArrayList<>();

        /**需要判断当前JSON节点的子节点类型**/

        /**对于容器型子节点，收集所有的返回二元组**/
        if (!this.idxs.containsKey(nodename)) {
            for (HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
                String jsonchildname = nodename + "." + entry.getKey();

                /**对于子节点为数组的情况，调用arrayTransform**/
                if (this.arrays.containsKey(jsonchildname)) {
                    JSONArray jsonchild = JSON.parseArray(entry.getValue().toString());
                    result.add(arrayTransform(jsonchild, jsonchildname));
                }

                /**对于子节点为叶子节点的情况，返回偏移量以及值**/
                else if (this.idxs.containsKey(jsonchildname)) {

                    List<Integer> idx = this.idxs.get(jsonchildname);
                    int offset = idx.get(idx.size() - 1);
                    Object returnvalue = jsonroot.getString(entry.getKey());

                    result.add(new Pair(offset, returnvalue));
                }

                /**对于子节点为容器型的情况，继续调用fieldsTransform**/
                else {
                    JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                    result.addAll(fieldsTransform(jsonchild, jsonchildname));
                }
            }
        }

        return result;
    }

    /**
     * 递归调用：JSONArray
     *
     * @MethodName: arrayTransform
     * @Return Pair<Integer, Object>
     */
    Pair arrayTransform(JSONArray jsonarray, String nodename) {

        Pair result;
        int arraycount = this.arrays.get(nodename);

        List<Record> returnvalue = new ArrayList<>();

        /** 针对元素为基本类型的数组，生成的List<Record>，Record只包含一个字段 **/
        if (arraycount == 0) {

            List<Integer> idx = this.idxs.get(nodename);
            int offset = idx.get(idx.size() - 1);

            for (int i = 0; i < jsonarray.size(); i++) {
                Object[] element = new Object[1];
                element[0] = jsonarray.getString(i);
                returnvalue.add(new Record(Arrays.asList(element)));
            }

            result = new Pair(offset, returnvalue);
            return result;
        }


        /** 针对元素含有多个叶子节点的数组，遍历数组中每一个JSON数据**/
        for (int i = 0; i < jsonarray.size(); i++) {

            Object[] elements = new Object[arraycount];
            JSONObject jsonelement = jsonarray.getJSONObject(i);

            /**针对每一个JSON数据生成Record，根据arrays设置其中字段个数，加入List**/
            for (HashMap.Entry<String, Object> entry : jsonelement.entrySet()) {
                String jsonchildname = nodename + "." + entry.getKey();

                if (this.arrays.containsKey(jsonchildname)) {
                    JSONArray jsonchild = JSON.parseArray(entry.getValue().toString());
                    Pair tmpfield = arrayTransform(jsonchild, jsonchildname);
                    elements[tmpfield.getKey()] = tmpfield.getValue();
                } else if (this.idxs.containsKey(jsonchildname)) {

                    List<Integer> idx = this.idxs.get(jsonchildname);
                    int offset = idx.get(idx.size() - 1);
                    elements[offset] = jsonelement.getString(entry.getKey());
                } else {
                    JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                    List<Pair> tmp = fieldsTransform(jsonchild, jsonchildname);
                    for (Pair pair : tmp) {
                        elements[pair.getKey()] = pair.getValue();
                    }
                }

            }

            returnvalue.add(new Record(Arrays.asList(elements)));

        }

        List<Integer> idx = this.idxs.get(nodename);
        int offset = idx.get(idx.size() - 1);

        result = new Pair(offset, returnvalue);
        return result;
    }

    /**
     * 获取字段类型
     *
     * @MethodName: getType
     * @Return int
     */
    public int getType(String f) {
        return this.type.get(f);
    }

    /**
     * 获取type
     * @MethodName: getType
     * @Return java.util.Map<java.lang.String, java.lang.Integer>
     */
    public Map<String, Integer> getAllType() {

        return type;
    }

    /**
     * 设置type
     * @MethodName: setType
     * @Return void
     */
    public void setType(Map<String, Integer> type) {

        this.type = type;
    }

    /**
     * 获取索引
     *
     * @MethodName: getValue
     * @Return java.util.List<java.lang.Integer>
     */
    public List<Integer> getValue(String field) {
        List<Integer> result = new ArrayList<>();
        if (this.idxs.containsKey(field)) result = this.idxs.get(field);

        return result;
    }

    /**
     * 设置idxs
     * @MethodName: setIdxs
     * @Return void
     */
    public void setIdxs(Map<String, List<Integer>> i){

        idxs = i;
    }

    /**
     * 获取idxs
     * @MethodName: getIdxs
     * @Return java.util.Map<java.lang.String, java.util.List < java.lang.Integer>>
     */
    public Map<String, List<Integer>> getIdxs(){

        return idxs;
    }

    /**
     * 获取schema名称
     *
     * @MethodName: getSchemaName
     * @Return java.lang.String
     */
    public String getSchemaName() {
        return this.schemaName;
    }

    /**
     * 设置schemaName
     * @MethodName: setSchemaName
     * @Return void
     */
    public void setSchemaName(String name){
        schemaName=name;
    }

    /**
     * 根据索引获取字段名
     *
     * @MethodName: getKey
     * @Return java.lang.String
     */
    public String getKey(List<Integer> value) {

        String result = fields.get(value);

        return result;
    }

    /**
     * 获取fields
     * @MethodName: getFields
     * @Return java.util.Map<java.util.List < java.lang.Integer>,java.lang.String>
     */
    public Map<List<Integer>, String> getFields(){

        return fields;
    }

    /**
     * 设置fields
     * @MethodName: setFieldst
     * @Return void
     */
    public void setFields(Map<List<Integer>, String> f){

        fields = f;
    }

    /**
     * 获取leaf
     * @MethodName: getLeaf
     * @Return int
     */
    public int getLeaf() {

        return leaf;
    }

    /**
     * 设置leaf
     * @MethodName: setLeaf
     * @Return
     */
    public void setLeaf(int leaf) {
        this.leaf = leaf;
    }

    /**
     * schema做join操作
     *
     * @MethodName: joinSchema
     * @Return Schema
     */
    public Schema joinSchema(Schema s2){

        Schema result = new Schema();
        result.setSchemaName(schemaName+"_join_"+s2.getSchemaName());
        result.setLeaf(this.leaf+s2.getLeaf());
        int offset = this.leaf;

        Map<String, List<Integer>> idxs2 = s2.getIdxs();
        Map<String, List<Integer>> newidxs = new HashMap<>(); // 字段名 --> 下标
        Map<List<Integer>, String> newfields = new HashMap<>();// 下标 --> 字段名


        for(HashMap.Entry<String, List<Integer>> entry : idxs.entrySet()) {
            String key = entry.getKey();
            List<Integer> value = entry.getValue();
            newidxs.put(key,value);
            newfields.put(value,key);
        }

        for(HashMap.Entry<String, List<Integer>> entry : idxs2.entrySet()) {
           String key = entry.getKey();
           List<Integer> value = entry.getValue();
           value.set(0,value.get(0)+offset);
           newidxs.put(key,value);
           newfields.put(value,key);
        }

        result.setIdxs(newidxs);
        result.setFields(newfields);

        Map<String,Integer> newtype = new HashMap<>();
        newtype.putAll(type);
        newtype.putAll(s2.getAllType());

        result.setType(newtype);


        return result;
    }



    /**
     * schema做projection操作
     *
     * @MethodName: projectSchema
     * @Return Schema
     */
    public Schema projectSchema(List<String> rules) {

        Schema result = new Schema();

        Map<String, List<Integer>> newidxs = new HashMap<>(); // 字段名 --> 下标
        Map<List<Integer>, String> newfields = new HashMap<>();// 下标 --> 字段名
        Map<String,Integer> newtype = new HashMap<>();

        for(String rule: rules){

            List<Integer> idx = idxs.get(rule);
            int t = type.get(rule);

            newidxs.put(rule,idx);
            newfields.put(idx,rule);
            newtype.put(rule,t);

        }

        result.setIdxs(newidxs);
        result.setFields(newfields);
        result.setType(newtype);

        return result;
    }


}


