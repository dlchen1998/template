import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.*;

/**
 * @ClassName: .Template.java
 * @Description: Template类，包含模板转换，字段类型定义，以及模板基本结构数据
 */
public class Template{


    //可能的数组格式类型
    public static final List<String> ARRAYS = new ArrayList<>(Arrays.asList("图片型","数组型","文件","表格型"));
    //可能的容器格式类型
    public static final List<String> CONTAINER = new ArrayList<>(Arrays.asList("容器型","生成器型"));

    //类型定义
    public static final int STRING = 262;
    public static final int INTEGER = 263;
    public static final int BOOL =264;
    public static final int FLOAT = 265;
    public static final int STRING_ARRAY = 266;
    public static final int INTEGER_ARRAY = 267;
    public static final int FLOAT_ARRAY = 269;

    private String templateName;//模板名
    private Map<String,List<Integer>> idxs;//字段名 --> 下标
    private Map<List<Integer>,String> fields;//下标 --> 字段名
    private Map<String,Integer> type;//字段名 --> 类型
    private int leaf;//此模板第一层长度
    private Map<String,Integer> arrays;//数组字段名 --> 数组元素叶子节点个数

    Template(){}

    /**
     * 构造函数，将原始JSON转化为templa
     * @MethodName: Template
     * @Return
     */
    public Template(String templatePath){

        List<String> path = Arrays.asList(templatePath.split("\\\\"));
        String name = path.get(path.size()-1);
        this.templateName = name.split("\\.")[0];
        this.idxs = new HashMap<>();
        this.fields = new HashMap<>();
        this.type = new HashMap<>();
        this.arrays = new HashMap<>();

        JSONObject origianlJson = JSON.parseObject(readTemplate(templatePath));
        this.leaf = transformTemplate(origianlJson.getJSONObject("template"), this.templateName,0,"_notarray");

    }


    /**
     * 读取JSON模板源文件
     * @MethodName: readTemplate
     * @Return java.lang.String
     */
    String readTemplate(String templatePath){

        String laststr = "";
        try( FileInputStream fileinputstream = new FileInputStream(templatePath);
             InputStreamReader inputstreamreader = new InputStreamReader(fileinputstream, "UTF-8");
             BufferedReader reader = new BufferedReader(inputstreamreader)) {

            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                laststr += tempString;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return laststr;
    }

    /**
     * 模板转换，递归生成，参数包含当前JSON节点，当前节点全称，当前叶子节点编号，
     * 以及当前节点属于的数组名（若不存在于数组中记为“_notarray”）
     * @MethodName: transformTemplate
     * @Return int
     */
    int transformTemplate(JSONObject jsonroot, String nodename, int count, String arrayname){

        /** 当前节点若为容器CONTAINER，对其中每个键值对继续递归**/
        if(CONTAINER.contains(jsonroot.get("_type"))||!jsonroot.containsKey("_type")){
            for(HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
                if(entry.getKey().matches("_(.*)"))continue;
                JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                String jsonchildname = nodename + "." + entry.getKey();
                count =  transformTemplate(jsonchild,jsonchildname,count,arrayname);
            }
        }

        /**当前节点若为数组ARRAYS，将该节点加入idxs，对其中每个键值对继续递归，统计该数组包含的元素的叶子节点数量；**/
        else if(ARRAYS.contains(jsonroot.get("_type"))){

            /*** 当为idxs添加新的键值对时，需要判断当前属于的数组，
             * 若不属于任何一个数组，则采用addLeaf方法，否则采用addArray方法**/
            if("_notarray".equals(arrayname)){addLeaf(jsonroot,nodename,count);}
            else{addArray(jsonroot,nodename,arrayname,count);}
            count++;

            /** 若当前节点属于ARRAYS中的“数组型”,需要根据当前节点名再对原始JSON深入一层 **/
            if("数组型".equals(jsonroot.get("_type"))){
                String[] tmp = nodename.split("\\.");
                jsonroot=JSON.parseObject(jsonroot.get(tmp[tmp.length-1]).toString());
            }
            int arraycount = 0;
            for(HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
                if(entry.getKey().matches("_(.*)"))continue;
                JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                String jsonchildname = nodename + "." + entry.getKey();
                arraycount =  transformTemplate(jsonchild,jsonchildname,arraycount,nodename);
            }

            /** 对于ARRAYS中的“数组型，文件，图片型”，
             * 需要人为地将其内部的元素拓展至下一层级便于数据转换，将其索引list添加元素“0” **/
            if(arraycount==0){
                String type = jsonroot.get("_type").toString();

                /** 若其内部元素为基本类型，将整个数组标记为字符串数组或数值数组**/
                switch (type){
                    case "数值型":
                        this.type.put(nodename,FLOAT_ARRAY);
                        break;
                    case "字符串型":
                        this.type.put(nodename,STRING_ARRAY);
                    default:
                        this.type.put(nodename,STRING_ARRAY);
                }

                List<Integer> l = this.idxs.get(nodename);
                l.add(0);
            }
            this.arrays.put(nodename,arraycount);
        }
        else{
            if("_notarray".equals(arrayname)){
                addLeaf(jsonroot,nodename,count);
                count++;

            }
            else  {
                addArray(jsonroot,nodename,arrayname,count);
                count++;
            }
        }

    return count;
    }

    /**
     * 将中文类型名称转换为统一的Integer类型
     * @MethodName: typeSwitch
     * @Return int
     */
    int typeSwitch(String t){


        switch (t){

            case "数值型":
                return FLOAT;

            case "字符串型":
                return  STRING;

            case "整数型":
                return INTEGER;

            case "图片型":
                return STRING_ARRAY;

            case "文件型":
                return STRING_ARRAY;

            case "布尔型":
                return BOOL;

            default: return 0;
        }

    }

    /**
     * 直接通过当前叶子节点的编号，加入idxs
     * @MethodName: addLeaf
     * @Return void
     */
    void addLeaf(JSONObject jsonroot, String nodename,int count){

        String type = jsonroot.getString("_type");
        this.type.put(nodename,typeSwitch(type));
        this.idxs.put(nodename,new ArrayList<>(Arrays.asList(count)));
        this.fields.put(new ArrayList<Integer>(Arrays.asList(count)),nodename);
    }

    /**
     * 由于此叶子节点处于数组中，需要获取数组的索引idx并将此叶子在数组中的位置添加至idx，以此获得叶子节点的索引
     * @MethodName: addArray
     * @Return void
     */
    void addArray(JSONObject jsonroot, String nodename, String arrayname, int count){

        String type = jsonroot.getString("_type");
        this.type.put(nodename,typeSwitch(type)+4);
        List<Integer>arrayidx = this.idxs.get(arrayname);
        List<Integer> elemidx = new ArrayList<>(arrayidx);
        elemidx.add(count);
        this.idxs.put(nodename,elemidx);
        this.fields.put(elemidx,nodename);
    }

    /**
     * 获取模板名
     * */
    public String getTemplateName(){
        return templateName;
    }

    /**
     * 获取此模板第一层叶子节点数量
     * @MethodName: getLeaf
     * @Return int
     */
    public int getLeaf(){

        return this.leaf;
    }

    /**
     * 获取 索引 --> 字段名
     * @MethodName: getFields
     * @Return java.util.Map<java.util.List < java.lang.Integer>,java.lang.String>
     */
    public Map<List<Integer>, String> getFields() {

        return this.fields;
    }

    /**
     * 获取 字段名 --> 索引
     * @MethodName: getIdxs
     * @Return java.util.Map<java.lang.String, java.util.List < java.lang.Integer>>
     */
    public Map<String, List<Integer>> getIdxs() {

        return this.idxs;
    }

    /**
     * 获取 字段名 --> 类型
     * @MethodName: getType
     * @Return java.util.Map<java.lang.String, java.lang.Integer>
     */
    public Map<String,Integer> getType(){

        return this.type;
    }

    /**
     * 获取 数组字段名 --> 数组元素叶子节点个数
     * @MethodName: getArrays
     * @Return java.util.Map<java.lang.String, java.lang.Integer>
     */
    public Map<String,Integer> getArrays(){

        return this.arrays;
    }


}




