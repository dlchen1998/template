import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnData {

    public String record2JSON(Record r,Schema s, List<String> select){

        Map<String,String> returndata = new HashMap<>();


        for(int i=0;i<select.size();i++){

            String name = select.get(i);
            int type = s.getType(name);
            Object v = r.getFields().get(i);

            if(type==266||type==269){

                String result = "[";

                List<String> values = (List<String>)v;
                for(int j=0;j<values.size();j++){

                    if(j==values.size()-1){
                        result+=values.get(j);
                        result+="]";
                    }
                    else{
                        result+=values.get(j);
                        result+=",";
                    }
                }

                returndata.put(name,result);
            }
            else{

                returndata.put(name,(String)v);

            }


        }

        return JSON.toJSONString(returndata);
    }


}
