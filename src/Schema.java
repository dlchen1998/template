

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


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
    private Map<String,Integer> offsets;

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
    public Schema(String path) {
        Template template = new Template(path);
        this.offsets = new HashMap<>();
        this.idxs = template.getIdxs();
        this.schemaName = template.getTemplateName();
        offsets.put(schemaName,0);
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
     * 获取当前Schema在join之后的Schema
     * */
    public int getOffset(String tableName){
        return offsets.get(tableName);
    }

    /**
     * 获取所有offset
     * */
    private Map<String, Integer> getOffsets() {
        return offsets;
    }

    /**
     * 设置Offesets
     * */
    private void setOffsets(Map<String, Integer> offsets) {
        this.offsets = offsets;
    }

    /**
     * 针对该Schema，整体后移，增加偏移量
     * */
    private Map<String,Integer> incOffsets(int offset){
        Map<String,Integer> map = new HashMap<>();
        for(HashMap.Entry<String,Integer> entry: offsets.entrySet()){
            
            int o = entry.getValue();
            map.put(entry.getKey(),o+offset);

        }
        return map;
    }

    /**
     * schema做join操作
     */
    public Schema joinSchema(Schema s2){

        Schema result = new Schema();
        result.setSchemaName(schemaName+"_join_"+s2.getSchemaName());
        result.setLeaf(this.leaf+s2.getLeaf());
        int offset = this.leaf;
        
        Map<String, List<Integer>> idxs2 = s2.getIdxs();
        Map<String, List<Integer>> newIdxs = new HashMap<>(); // 字段名 --> 下标
        Map<List<Integer>, String> newFields = new HashMap<>();// 下标 --> 字段名
        Map<String,Integer> newType = new HashMap<>();
        Map<String,Integer> newOffsets = new HashMap<>();


        /**针对作为参数的Schema，需要将其添加到另一个Schema之后，所以需要记录偏移量*/
        /**将两个Schema的偏移量记录合并*/
        newOffsets.putAll(offsets);
        newOffsets.putAll(s2.incOffsets(offset));
        result.setOffsets(newOffsets);


        /**将两个Schema的Idxs合并，作为参数的Schema需要加偏移量*/
        for(HashMap.Entry<String, List<Integer>> entry : idxs.entrySet()) {
            String key = entry.getKey();
            List<Integer> value = entry.getValue();
            newIdxs.put(key,value);
            newFields.put(value,key);
        }

        for(HashMap.Entry<String, List<Integer>> entry : idxs2.entrySet()) {
           String key = entry.getKey();
           ArrayList<Integer> value = (ArrayList<Integer>)entry.getValue();
           ArrayList<Integer> v = (ArrayList<Integer>)value.clone();
           v.set(0,value.get(0)+offset);
           newIdxs.put(key,v);
           newFields.put(v,key);
        }

        result.setIdxs(newIdxs);
        result.setFields(newFields);


        /**将两个Schema的类型定义合并*/
        newType.putAll(type);
        newType.putAll(s2.getAllType());

        result.setType(newType);


        return result;
    }

    /**
     * schema做projection操作
     */
    public Schema projectSchema(List<String> rules) {

        Schema result = new Schema();

        Map<String, List<Integer>> newIdxs = new HashMap<>(); // 字段名 --> 下标
        Map<List<Integer>, String> newFields = new HashMap<>();// 下标 --> 字段名
        Map<String,Integer> newType = new HashMap<>();

        for(String rule: rules){

            List<Integer> idx = idxs.get(rule);
            int t = type.get(rule);

            newIdxs.put(rule,idx);
            newFields.put(idx,rule);
            newType.put(rule,t);

        }

        result.setIdxs(newIdxs);
        result.setFields(newFields);
        result.setType(newType);

        return result;
    }


}


