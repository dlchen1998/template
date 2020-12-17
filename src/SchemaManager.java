import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SchemaManager {

    /** 表名 --> Schema */
    private Map<String, Schema> schemas = new HashMap<>();

    /**
     * 初始化，递归读取模板文件下所有模板文件
     */
    public void init() throws Exception {

        // TODO 加载所有的表，生成Schema
        getFiles("./templates");
        }

    /**
     * 通过表名查找对应Schema
     */
    public Schema getSchema(String table) {

        return schemas.get(table);
    }

    /**
     * 递归查找文件夹下所有模板，读取生成Schema
     */
    private void getFiles(String path){

        File rootPath = new File(path);

        File[] files = rootPath.listFiles();
        for(int i=0;i<files.length;i++)
        {
            if(files[i].isFile())
            {
                System.out.println( files[i].getName());
                String p = files[i].getPath();
                Schema schema = new Schema(files[i].getPath());
                schemas.put(schema.getSchemaName(),schema);

            }
            else if(files[i].isDirectory())
            {
                String p  = files[i].getPath();
                getFiles(files[i].getPath());
            }
        }
    }


}
