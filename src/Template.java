import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.*;


public class Template{


    public static final List<String> ARRAYS = new ArrayList<>(Arrays.asList("图片型","数组型","文件型","表格型"));
    public static final List<String> CONTAINER = new ArrayList<>(Arrays.asList("容器型","生成器型"));

    public static final int INTEGER = 1;
    public static final int FLOAT = 2;
    public static final int STRING = 3;
    public static final int BOOL = 4;
    public static final int FLOAT_ARRAY = 5;
    public static final int STRING_ARRAY = 6;

    //模板名称；转换字典，逆转换字典；叶节点数量
    private String templateName;
    private Map<String,List<Integer>> idxs;
    private Map<List<Integer>,String> fields;
    private Map<String,Integer> type;
    private int leaf;
    private Map<String,Integer> arrays;

    //private JSONObject origianlJson;


    //初始化模板名称；成对的模板转换字典；计数模板的二维长度
    Template(){}
    public Template(String templatename){

        this.templateName = templatename;
        this.idxs = new HashMap<>();
        this.fields = new HashMap<>();
        this.type = new HashMap<>();
        this.arrays = new HashMap<>();

        JSONObject origianlJson = JSON.parseObject(readTemplate());
        this.leaf = transformTemplate(origianlJson.getJSONObject("template"), this.templateName,0,"_notarray");

    }

    //读取模板文件
    String readTemplate(){

        BufferedReader reader = null;
        String laststr = "";
        try {
            FileInputStream fileinputstream = new FileInputStream(this.templateName + ".json");
            InputStreamReader inputstreamreader = new InputStreamReader(fileinputstream, "UTF-8");
            reader = new BufferedReader(inputstreamreader);
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                laststr += tempString;
            }
            reader.close();
            fileinputstream.close();
            ;
            inputstreamreader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return laststr;
    }

    //递归调用，直到JSON树的叶子节点
    int transformTemplate(JSONObject jsonroot, String nodename, int count, String arrayname){

        Boolean leafcheck = true;

        if(CONTAINER.contains(jsonroot.get("_type"))){
            for(HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
                if(entry.getKey().matches("_(.*)"))continue;
                JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                String jsonchildname = nodename + "." + entry.getKey();
                count =  transformTemplate(jsonchild,jsonchildname,count,arrayname);
            }
        }
        else if(ARRAYS.contains(jsonroot.get("_type"))){
            if("_notarray".equals(arrayname)){addLeaf(jsonroot,nodename,count);}
            else{addArray(jsonroot,nodename,arrayname,count);}
            count++;
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
            if(arraycount==0){
                List<Integer> tmpidx = this.idxs.get(nodename);
                tmpidx.add(0);
                String type = jsonroot.get("_type").toString();
                switch (type){
                    case "数值型":
                        this.type.put(nodename,FLOAT_ARRAY);
                        break;
                    case "字符串型":
                        this.type.put(nodename,STRING_ARRAY);
                }
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

    void addLeaf(JSONObject jsonroot, String nodename,int count){
        String type = jsonroot.getString("_type");
        this.type.put(nodename,typeSwitch(type));
        this.idxs.put(nodename,new ArrayList<>(Arrays.asList(count)));
        this.fields.put(new ArrayList<Integer>(Arrays.asList(count)),nodename);
    }

    void addArray(JSONObject jsonroot, String nodename, String arrayname, int count){
        String type = jsonroot.getString("_type");
        this.type.put(nodename,typeSwitch(type)+3);
        List<Integer>arrayidx = this.idxs.get(arrayname);
        List<Integer> elemidx = new ArrayList<>(arrayidx);
        elemidx.add(count);
        this.idxs.put(nodename,elemidx);
        this.fields.put(elemidx,nodename);
    }

    public int getLeaf(){
        return this.leaf;
    }

    public Map<List<Integer>, String> getFields() {
        return this.fields;
    }

    public Map<String, List<Integer>> getIdxs() {
        return this.idxs;
    }

    public Map<String,Integer> getType(){

        return this.type;
    }

    public Map<String,Integer> getArrays(){
        return this.arrays;
    }

    /*
    //跨模板join操作,当前模板与输入模板拼接
    public TemplateTransform joinTemplate(TemplateTransform t1){

        TemplateTransform result = new TemplateTransform();
        result.templateName = this.templateName+"_join_"+t1.getTemplateName();
        result.leafCount = this.leafCount+t1.getLeafCount();
        int offset = this.leafCount;

        HashMap<String, Integer> template = new HashMap<>();
        HashMap<Integer, String> retransform = new HashMap<>();

        for(HashMap.Entry<String, Integer> entry : this.template.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            template.put(key,value);
            retransform.put(value,key);
        }

        for(HashMap.Entry<String, Integer> entry : t1.getTemplate().entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue()+offset;
            template.put(key,value);
            retransform.put(value,key);
        }

        result.template=template;
        result.reTransform=retransform;

        return result;
    }

     */




}




