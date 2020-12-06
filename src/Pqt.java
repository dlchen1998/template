import java.util.ArrayList;
import java.util.List;


/**
 * 物理算子的基类
 *
 */
public abstract class Pqt {

    /** 节点的类型 */
    protected int type;

    /** 节点的名称 */
    protected String text;

    /** 数据的模式 */
    protected Schema schema;

    /** 暂存的数据 */
    protected List<Record> data = new ArrayList<>();

    public Pqt(int type, String text) {
        this.type = type;
        this.text = text;
    }

    /**
     * 为当前节点添加儿子节点
     *
     * @param child
     */
    public abstract void addChild(Pqt child);

    /**
     * 打开节点，准备开始获取数据
     */
    public void open() {
    }

    /**
     * 已经获取数据完毕，关闭节点
     */
    public void close() {
        this.data = null;
    }

    /**
     * 获取通过当前算子操作后的数据
     *
     * @param size 需要获取的数据块大小
     * @Return 获取到的数据，如果没有数据，返回null
     */
    public abstract List<Record> fetch(int size);
}
