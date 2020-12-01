import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


import java.lang.reflect.Array;
import java.util.*;


public class Template{

    public static final List<String> arrays = new ArrayList<>(Arrays.asList("图片型","数组型","文件型","表格型"));
    public static final List<String> container = new ArrayList<>(Arrays.asList("容器型","生成器型"));

    //模板名称；转换字典，逆转换字典；叶节点数量
    private String templateName;
    private HashMap<String,List<Integer>> template;
    private HashMap<List<Integer>,String> reTransform;
    private HashMap<String,String> typeDefine;
    private int leafCount;

    //private JSONObject origianlJson;


    //初始化模板名称；成对的模板转换字典；计数模板的二维长度
    Template(){}
    Template(String templatename){

        this.templateName = templatename;
        this.template = new HashMap<>();
        this.reTransform = new HashMap<>();
        this.typeDefine = new HashMap<>();

        JSONObject origianlJson = JSON.parseObject(readTemplate());
        transformTemplate(origianlJson.getJSONObject("template"), this.templateName,0,"_notarray");

        this.leafCount = this.template.size();

    }

    //读取模板文件
    String readTemplate(){

        ScanPqt fileUtil = new ScanPqt();
        String path = this.templateName+".json";
        String originaljson = fileUtil.ReadFile(path);
        return originaljson;
    }

    //递归调用，直到JSON树的叶子节点
    int transformTemplate(JSONObject jsonroot, String nodename, int count, String arrayname){

        Boolean leafcheck = true;

        if(container.contains(jsonroot.get("_type"))){
            for(HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
                if(entry.getKey().matches("_(.*)"))continue;
                leafcheck = false;
                JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                String jsonchildname = nodename + "." + entry.getKey();
                count =  transformTemplate(jsonchild,jsonchildname,count,arrayname);
            }
        }
        else if(arrays.contains(jsonroot.get("_type"))){
            if("_notarray".equals(arrayname)){addLeaf(jsonroot,nodename,count);}
            else{addArray(jsonroot,nodename,arrayname,count);}
            count++;
            int arraycount = 0;
            for(HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
                if(entry.getKey().matches("_(.*)"))continue;
                JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                String jsonchildname = nodename + "." + entry.getKey();
                arraycount =  transformTemplate(jsonchild,jsonchildname,arraycount,nodename);
            }
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

/*

    for(HashMap.Entry<String, Object> entry : jsonroot.entrySet()) {
            if(entry.getKey().matches("_(.*)")){
             if("表格型".equals(entry.getValue())) {
                 //JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
                 String jsonchildname = nodename + "." + entry.getKey();
                 transformTemplate(,jsonchildname,0,nodename);
                 count++;
             }
             else{continue;}
            }
            leafcheck = false;
            JSONObject jsonchild = JSON.parseObject(entry.getValue().toString());
            String jsonchildname = nodename + "." + entry.getKey();
            count =  transformTemplate(jsonchild,jsonchildname,count,arrayname);
        }

        */

    return count;
    }

    void addLeaf(JSONObject jsonroot, String nodename,int count){
        String type = jsonroot.getString("_type");
        this.typeDefine.put(nodename,type);
        this.template.put(nodename,new ArrayList<>(Arrays.asList(count)));
        this.reTransform.put(new ArrayList<Integer>(Arrays.asList(count)),nodename);
    }

    void addArray(JSONObject jsonroot, String nodename, String arrayname, int count){
        String type = jsonroot.getString("_type");
        this.typeDefine.put(nodename,type);
        List<Integer>arrayidx = this.template.get(arrayname);
        List<Integer> elemidx = new ArrayList<>(arrayidx);
        elemidx.add(count);
        this.template.put(nodename,elemidx);
        this.reTransform.put(elemidx,nodename);
    }


    public String getTemplateName() {
        return templateName;
    }

    public int getLeafCount(){
        return this.leafCount;
    }

    public HashMap<List<Integer>, String> getReTransform() {
        return this.reTransform;
    }

    public HashMap<String, List<Integer>> getTemplate() {
        return this.template;
    }

    public HashMap<String,String> getTypeDefine(){
        return this.typeDefine;
    }

    //输入字段名，获取模板数组index，失败返回-1
    public List<Integer> getValue(String field){

        List<Integer> result = new ArrayList<>();
        if(template.containsKey(field))result=template.get(field);

        return result;
    }

    //返回时通过数组index查找字段
    public String getKey(int value){

        String result = reTransform.get(value);

        return result;
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

/*
    public List<Record> dataTransform(String originaldata){


        List<Record> records = new ArrayList<Record>();//Record(this.leafCount);
        JSONArray dataarray = JSON.parseObject(originaldata).getJSONArray("data");

        for (int num = 0; num < dataarray.size(); num++) {

            Record record = new Record(this.leafCount);

            for (HashMap.Entry<String, Integer> entry : this.template.entrySet()) {
                JSONObject tmpdata = dataarray.getJSONObject(num).getJSONObject("content");
                String[] nodes = entry.getKey().split("\\.");
                for (int i = 1; i < nodes.length; i++) {
                    if (i == nodes.length - 1) continue;
                    tmpdata = tmpdata.getJSONObject(nodes[i]);
                }
                Object data = new Object();
                switch (this.typeDefine.get(entry.getKey())){
                    case "字符串型": data = tmpdata.getString(nodes[nodes.length - 1]); break;
                    //此处可以增加统计
                    case "数值型": data = tmpdata.getDouble(nodes[nodes.length-1]);
                        //此处可以增加统计
                }


                switch (this.typeDefine.get(entry.getKey())) {
                    case "字符串型":
                        data = tmpdata.getString(nodes[nodes.length - 1]);
                        break;
                    //此处可以增加统计
                    case "数值型":
                        data = tmpdata.getDouble(nodes[nodes.length - 1]);
                        //此处可以增加统计
                }


                //System.out.println(data);
                record.setData(entry.getValue(), data);
            }

            records.add(record);
        }

        return records;
    }
*/


}




