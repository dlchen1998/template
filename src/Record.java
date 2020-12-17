//package scu.mge.mdb.record;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据记录，由Json格式转换后的数据存放格式
 * Json表示的记录是树形结构，只有叶节点上有数据。转换之后的格式按顺序存放叶节点的数据到fields中(数组类型节点下面描述)。存入的数据以该节点相应的类型存储。
 * 如果其中有数组类型节点，则该节点当成叶节点作为一个整体存入fields中。存入的数据以Record[]类型存储。
 * 换言之，每个Record对象表示一棵固定格式的树，如果其中有数组节点，则用另外一片森林表示。
 */
public class Record {
    /**
     * 所有字段的数据。字段的类型，名称等由相应的Schema表示
     */
    private List<Object> fields;

    public Record(List<Object> fields) {
        this.fields = fields;
    }

    /**
     * 获取fields
     * @MethodName: getFields
     * @Return java.util.List<java.lang.Object>
     */
    public List<Object> getFields() {

        return fields;
    }

    /**
     * 通过索引获取数据
     * @MethodName: getField
     * @Return java.lang.Object
     */
    public Object getField(List<Integer> idx) {

        /**若该索引不指向一个数组中的叶子节点，直接取值**/
        if (idx.size() == 1) {
            return fields.get(idx.get(0));
        }
        /**否则递归获取**/
        else {
            List<Object> result = new ArrayList<>();
            getField0(result, this, idx, 0);
            return result;
        }
    }

    /**
     * 递归取得Record
     * @MethodName: getField0
     * @Return void
     */
    private void getField0(List<Object> result, Record record, List<Integer> idx, int depth) {

        if (depth == idx.size() - 1) {
            result.add(record.fields.get(idx.get(depth)));
            return;
        }

        List<Record> rr = (List<Record>) record.fields.get(idx.get(depth));
        for (Record r : rr) {
            getField0(result, r, idx, depth + 1);
        }
    }

    /**
     * 对Record进行join操作
     * @MethodName: joinRecords
     * @Return Record
     */
    public Record joinRecords(Record r){

        List<Object> newfields;

        newfields = new ArrayList<>(fields);
        newfields.addAll(r.getFields());

        Record result = new Record(newfields);
        return result;
    }

    /**
     * 获取投影过的Record
     * @MethodName: projectRecord
     * @Return Record
     */
    public Record projectRecord(List<List<Integer>> idxs){


        List<Object> newfields = new ArrayList<>();

        for(List<Integer> idx : idxs){
            Object field = this.getField(idx);
            newfields.add(field);
        }

        return new Record(newfields);
    }

}

