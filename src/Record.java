
import java.util.ArrayList;
import java.util.List;

/**
 * 数据记录，由Json格式转换后的数据存放格式
 *
 * Json表示的记录是树形结构，只有叶节点上有数据。转换之后的格式按顺序存放叶节点的数据到fields中(数组类型节点下面描述)。存入的数据以该节点相应的类型存储。
 *
 * 如果其中有数组类型节点，则该节点当成叶节点作为一个整体存入fields中。存入的数据以Record[]类型存储。
 *
 * 换言之，每个Record对象表示一棵固定格式的树，如果其中有数组节点，则用另外一片森林表示。
 *
 */
public class Record {
    /** 所有字段的数据。字段的类型，名称等由相应的Schema表示 */
    private List<Object> fields;

    public Record(List<Object> fields) {
        this.fields = fields;
    }

    public List<Object> getField(List<Integer> idx) {

        List<Object> result = new ArrayList<>();
        //List<Object> current = this.fields;
        if(idx.size()==1){
            result.add(this.fields.get(idx.get(0)));
        }
        else if(idx.size()==2) {
            //System.out.println(idx);
            List<Object> array = (List<Object>) this.fields.get(idx.get(0));
            result = (List<Object>) array.get(idx.get(1));
        }
        else{
            //System.out.println(idx);
            List<Object> array = (List<Object>) this.fields.get(idx.get(0));
            List<Object> array2 = (List<Object>)array.get(idx.get(1));
            for(int i=0;i<array2.size();i++){
                List<Object> current = (List<Object>) array2.get(i);
                result.add(current.get(idx.get(2)));
            }
        }


        return result;
    }

}
